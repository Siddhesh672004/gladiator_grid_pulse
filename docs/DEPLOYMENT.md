# Deploying GridPulse

GridPulse runs in production on one Oracle Cloud "Always Free" ARM virtual machine. Everything runs in Docker:
MySQL, Redis, Kafka, the five Spring Boot services, and Caddy, a web server that gets HTTPS certificates by itself,
serves the Angular site and passes `/api` to the gateway. Only Caddy is reachable from the internet (ports 80 and 443);
the rest talk over a private Docker network.

GitHub Actions builds the images and rolls them out. Your part is a one-time setup of the VM, the domain and the
secrets.

| File | What it does |
|---|---|
| `deploy/docker-compose.prod.yml` | the production stack (images from GitHub Container Registry, memory and log limits, health checks) |
| `deploy/Caddyfile` | HTTPS, the site files, `/api/*` to the gateway, security headers, caching |
| `deploy/Dockerfile.web` | the `web` image: Caddy plus the Angular build |
| `deploy/setup-vm.sh` | one-time VM setup: Docker, firewall, swap, log rotation, nightly backup |
| `deploy/backup.sh` | dumps every database, keeps 7 days |
| `deploy/.env.prod.example` | the list of production settings and secrets |
| `.github/workflows/ci.yml` | the `deploy` job at the end of the pipeline |

## 1. Create the VM

1. Sign up at Oracle Cloud and choose a home region close to your users (Always Free resources live there).
2. Compute, Instances, Create instance:
   - Image: **Canonical Ubuntu 22.04** (the aarch64 build).
   - Shape: **VM.Standard.A1.Flex** (Ampere), **4 OCPUs, 24 GB memory**. This fits in the Always Free allowance.
   - Networking: keep the default new virtual cloud network (VCN) with a public subnet and **assign a public IPv4 address**.
   - SSH keys: paste your public key, or let Oracle make a pair and download the private key.
3. Write down the public IP address. The default user on Ubuntu images is `ubuntu`.
4. Check that you can log in: `ssh -i <your-key> ubuntu@<public-ip>`.

## 2. Open ports 80 and 443 in the VCN

The VM has two firewalls: Oracle's security list (outside the VM) and iptables (inside, handled by `setup-vm.sh`).

Networking, Virtual cloud networks, your VCN, Security Lists, Default Security List, Add Ingress Rules:

| Source CIDR | Protocol | Destination port | Why |
|---|---|---|---|
| `0.0.0.0/0` | TCP | 80 | Let's Encrypt check and the redirect to HTTPS |
| `0.0.0.0/0` | TCP | 443 | the site |
| `0.0.0.0/0` | UDP | 443 | optional: HTTP/3 |

Keep port 22 (SSH) as it is. Do not open 3306, 6379, 9092 or 8080: nothing else needs to be reachable.

## 3. Get a domain name (DuckDNS)

1. Log in at https://www.duckdns.org and add a subdomain, for example `gridpulse` (gives `gridpulse.duckdns.org`).
2. Set its "current ip" to the VM's public IP and save.
3. Check from your computer: `nslookup gridpulse.duckdns.org` must show the VM's IP.

The VM's address normally stays the same while the instance exists; a reserved public IP (also free) is the safer
choice. Whenever the address changes, update it in DuckDNS.

## 4. Prepare the VM

```bash
scp -i <your-key> deploy/setup-vm.sh ubuntu@<public-ip>:~
ssh -i <your-key> ubuntu@<public-ip>
sudo bash setup-vm.sh
exit                     # log out and in again so the docker group applies
```

`setup-vm.sh` installs Docker with the Compose plugin, rotates container logs (3 files of 10 MB), opens 80/443 in
iptables, adds 4 GB of swap, creates `/opt/gridpulse/backups` and a cron entry that runs `backup.sh` every night at 03:00.

## 5. Write the production settings

```bash
scp -i <your-key> deploy/.env.prod.example ubuntu@<public-ip>:/opt/gridpulse/.env
ssh -i <your-key> ubuntu@<public-ip>
chmod 600 /opt/gridpulse/.env
nano /opt/gridpulse/.env
```

Fill in every value. Make random secrets with `openssl rand -base64 48 | tr -d '\n/+=' | cut -c1-64`.

| Setting | Notes |
|---|---|
| `DOMAIN` | e.g. `gridpulse.duckdns.org` (no `https://`) |
| `ACME_EMAIL` | Let's Encrypt sends certificate notices here |
| `GHCR_OWNER` | your GitHub user name in **lower case** (the images are `ghcr.io/<owner>/gridpulse-*`) |
| `JWT_SECRET` | 64+ random characters |
| `INTERNAL_API_KEY`, `TICKET_SECRET` | 32+ random characters each |
| `DB_PASSWORD` | also becomes the MySQL root password on the first start |
| `SUPER_ADMIN_EMAIL`, `SUPER_ADMIN_PASSWORD` | the owner account, created or updated on every start of the core |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | a Gmail address and an app password (see step 8) |
| `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET` | Razorpay **test mode** keys (`rzp_test_...`) |
| `CLOUDINARY_*` | required: the core refuses to start in production without them (track pictures) |
| `GEMINI_API_KEY` (+ `_2`, `_3`), `GROQ_API_KEY` | optional: without them the FAQ chat uses word matching and feedback stays "not analysed" |

The services run with `SPRING_PROFILES_ACTIVE=prod`. At start each one checks its required settings and stops with a
clear message (`set ...`) if one is missing; see "Troubleshooting".

## 6. Connect GitHub to the VM

Create a key pair only for deployments and allow it on the VM:

```bash
ssh-keygen -t ed25519 -f gridpulse-deploy -N ""          # on your computer
cat gridpulse-deploy.pub | ssh -i <your-key> ubuntu@<public-ip> 'cat >> ~/.ssh/authorized_keys'
```

In the GitHub repository, Settings, Secrets and variables, Actions:

| Kind | Name | Value |
|---|---|---|
| Variable | `DEPLOY_ENABLED` | `true` (the deploy job is skipped until this is set) |
| Variable | `DOMAIN` | the same domain as in `.env` |
| Secret | `VM_HOST` | the VM's public IP |
| Secret | `VM_USER` | `ubuntu` |
| Secret | `VM_SSH_KEY` | the whole content of the private key file `gridpulse-deploy` |

## 7. What the deploy job does

It runs only on a push to `main`, only after the backend tests, the frontend build and the end-to-end smoke test
passed, and only when `DEPLOY_ENABLED` is `true`. Two deploys never run at the same time.

1. Downloads the service jars and the Angular build made earlier in the same pipeline.
2. Builds six images for `linux/arm64` and `linux/amd64` (core, notification, payment, ai, gateway, web) and pushes
   them to `ghcr.io/<owner>/gridpulse-<module>` with two tags: `latest` and the commit SHA.
3. Copies `docker-compose.prod.yml`, `backup.sh` and `docker/mysql/init.sql` (as `mysql-init.sql`) to `/opt/gridpulse`.
4. Logs the VM in to ghcr.io with the job's own token, then runs
   `TAG=<commit sha> docker compose -f docker-compose.prod.yml pull`, `up -d --remove-orphans` and `docker image prune -f`.
5. Waits until `https://<DOMAIN>/api/faqs` answers and the home page contains `<app-root`, then prints the live link.

To deploy the first time, push to `main` (or re-run the latest workflow on `main`) after steps 1 to 6.

## 8. First start

The first start takes a few minutes: MySQL creates the four schemas from `mysql-init.sql`, each Java service has a
3-minute start period before its health check counts, the gateway waits for core, payment and ai, and Caddy waits for
the gateway. Caddy then gets the certificate from Let's Encrypt.

```bash
cd /opt/gridpulse
docker compose -f docker-compose.prod.yml ps     # every service "healthy", web "running"
```

Then check, in this order:
- **Admin login**: open `https://<DOMAIN>/admin-login` and log in with `SUPER_ADMIN_EMAIL` / `SUPER_ADMIN_PASSWORD`.
  Add a track (this needs Cloudinary) and a practice session.
- **Gmail**: the Google account needs 2-Step Verification; then Google Account, Security, App passwords, create one
  and put it in `MAIL_PASSWORD` (spaces are ignored). `docker compose -f docker-compose.prod.yml logs notification | grep EMAIL`
  shows whether the login worked. Sign up as a driver: the code email must arrive.
- **Razorpay test mode**: book the session as that driver. The checkout window is Razorpay's test mode; use one of the
  test cards or UPI ids from Razorpay's test documentation. No real money moves. Reject the booking as admin and watch
  the badge go to REFUNDED within about a minute.

## 9. Backups and restore

Every night at 03:00 `backup.sh` writes `/opt/gridpulse/backups/gridpulse-<date>.sql.gz` (all databases, one
consistent dump) and deletes files older than 7 days. The log is `/var/log/gridpulse-backup.log`.

```bash
bash /opt/gridpulse/backup.sh                                            # a backup now
scp -i <your-key> 'ubuntu@<public-ip>:/opt/gridpulse/backups/*.sql.gz' .  # keep copies off the VM too
```

Restore (this replaces the data of every GridPulse database):

```bash
cd /opt/gridpulse
docker compose -f docker-compose.prod.yml stop web gateway core payment ai notification
gunzip -c backups/gridpulse-2026-10-19.sql.gz \
  | docker compose -f docker-compose.prod.yml exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD"'
docker compose -f docker-compose.prod.yml up -d
```

## 10. Updating

- **Code**: merge to `main`. The pipeline tests everything and deploys the new images.
- **Settings**: edit `/opt/gridpulse/.env`, then `docker compose -f docker-compose.prod.yml up -d --force-recreate`.
  A new `SUPER_ADMIN_PASSWORD` is applied at the next start of the core.
- **By hand** (same images as the last deploy): `docker compose -f docker-compose.prod.yml pull` and `up -d`.

## 11. Rolling back

Every deploy is tagged with its commit SHA, so an older version is one command away. Pick the SHA of a good commit
(GitHub, the commit list or the Actions run), then on the VM:

```bash
cd /opt/gridpulse
TAG=<sha> docker compose -f docker-compose.prod.yml up -d
```

`docker image prune -f` only removes untagged images, so older versions usually are still on the VM. If Docker has to
download them, log in first with a GitHub personal access token that has `read:packages`:
`echo <token> | docker login ghcr.io -u <github-user> --password-stdin`. The next deploy from `main` replaces the
rolled-back version, so revert the bad commit on `main` as well.

Hibernate only adds tables and columns, so no data is lost and an older version usually runs on the newer schema.
Take a backup (`bash backup.sh`) before rolling back across a release that changed the data model.

## 12. Logs

```bash
cd /opt/gridpulse
docker compose -f docker-compose.prod.yml ps                        # state and health of every container
docker compose -f docker-compose.prod.yml logs -f --tail=100 core   # one service, live
docker compose -f docker-compose.prod.yml logs --since=1h payment   # the last hour
docker stats --no-stream                                            # memory and CPU per container
```

The services log mostly warnings and errors, plus their start line. Passwords, codes, tokens and request bodies are
never logged.

## 13. Troubleshooting

**"Out of capacity" when creating the instance.** Ampere capacity in a region is often used up. Try another
availability domain, try again later (early morning often works), or create it with fewer OCPUs and resize later.
Upgrading the account to "Pay As You Go" usually helps and Always Free resources stay free.

**No HTTPS certificate.** `docker compose -f docker-compose.prod.yml logs web` shows Caddy's reason. Check that:
- `nslookup <DOMAIN>` gives the VM's IP;
- port 80 and 443 are open in the security list (step 2) and in iptables (`sudo iptables -L INPUT -n`);
- `DOMAIN` in `.env` has no `https://` and no trailing slash.
Let's Encrypt limits failed attempts, so fix the cause before restarting `web` many times.

**The site does not answer.** From your computer `curl -I http://<DOMAIN>` should give a redirect to HTTPS. No
answer at all means a port is closed (security list or iptables) or `web` is not running. `502` means the gateway is
not healthy yet: look at `docker compose -f docker-compose.prod.yml ps`.

**A service keeps restarting.** Read its log. A line starting with `set ...` (for example "set CLOUDINARY_CLOUD_NAME,
CLOUDINARY_API_KEY and CLOUDINARY_API_SECRET") names the missing setting in `.env`. MySQL access errors mean
`DB_PASSWORD` changed after the first start: the MySQL volume keeps the old root password.

**Memory.** Each Java service gets `-Xmx512m` (change it with `APP_JAVA_OPTS` in `.env`). `free -h` and
`docker stats` show the use; the 4 GB swap from `setup-vm.sh` is a safety net, not working memory.

**The deploy job fails at "Roll out on the vm".** Check the `VM_HOST`, `VM_USER` and `VM_SSH_KEY` secrets, and that
`/opt/gridpulse/.env` exists (the job does not copy it). `GHCR_OWNER` must be the lower-case owner of the repository.
