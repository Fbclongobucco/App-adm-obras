#!/bin/sh
# Backup do PostgreSQL de producao (servico "backup" do compose.yaml).
# Um pg_dump em formato custom (-Fc) a cada BACKUP_INTERVAL_SECONDS, em /backups; apaga os mais antigos
# que BACKUP_KEEP_DAYS dias. Conexao por PGHOST/PGDATABASE/PGUSER/PGPASSWORD.
# Restaurar: pg_restore --clean --if-exists --no-owner --no-acl -d <banco> <arquivo>.dump
set -eu

KEEP_DAYS="${BACKUP_KEEP_DAYS:-7}"
INTERVAL="${BACKUP_INTERVAL_SECONDS:-86400}"

while true; do
  stamp="$(date -u +%Y%m%d-%H%M%S)"
  target="/backups/${PGDATABASE}-${stamp}.dump"
  # Grava em arquivo temporario: um dump interrompido nunca fica com o nome final
  if pg_dump -Fc -f "${target}.partial"; then
    mv "${target}.partial" "${target}"
    echo "backup ok: ${target} ($(du -h "${target}" | cut -f1))"
  else
    rm -f "${target}.partial"
    echo "backup FALHOU em ${stamp}" >&2
  fi
  find /backups -name "${PGDATABASE}-*.dump" -mtime "+${KEEP_DAYS}" -print -delete
  sleep "${INTERVAL}"
done
