#!/usr/bin/env bash
# one-time setup of the production vm (oracle cloud, ubuntu 22.04): docker, open 80/443, swap, log rotation,
# nightly database backup. run as: sudo bash setup-vm.sh
set -euo pipefail
OWNER="${SUDO_USER:-ubuntu}"

# docker engine + compose plugin (the official convenience script)
if ! command -v docker > /dev/null; then
  curl -fsSL https://get.docker.com | sh
fi
usermod -aG docker "$OWNER"

# container logs rotate instead of filling the disk
cat > /etc/docker/daemon.json <<'EOF'
{ "log-driver": "json-file", "log-opts": { "max-size": "10m", "max-file": "3" } }
EOF
systemctl restart docker

# oracle's ubuntu images only allow ssh in iptables: open http and https (tcp + udp for http/3)
for rule in "tcp 80" "tcp 443" "udp 443"; do
  set -- $rule
  iptables -C INPUT -p "$1" --dport "$2" -m state --state NEW -j ACCEPT 2> /dev/null \
    || iptables -I INPUT 6 -p "$1" --dport "$2" -m state --state NEW -j ACCEPT
done
netfilter-persistent save

# 4 GB swap so a build-up of java heaps never kills the box
if [ ! -f /swapfile ]; then
  fallocate -l 4G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

# the app folder (the deploy job copies the compose file and scripts here) + nightly backup at 03:00
mkdir -p /opt/gridpulse/backups
chown -R "$OWNER" /opt/gridpulse
cat > /etc/cron.d/gridpulse-backup <<'EOF'
0 3 * * * root bash /opt/gridpulse/backup.sh >> /var/log/gridpulse-backup.log 2>&1
EOF

echo "done: log out and in again (docker group), then put .env into /opt/gridpulse"
