#!/usr/bin/env bash
# nightly dump of every database (cron, see setup-vm.sh); the last 7 days are kept
set -euo pipefail
cd /opt/gridpulse
docker compose -f docker-compose.prod.yml exec -T mysql \
  sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --all-databases --single-transaction --routines' \
  | gzip > "backups/gridpulse-$(date +%F).sql.gz"
find backups -name 'gridpulse-*.sql.gz' -mtime +7 -delete
