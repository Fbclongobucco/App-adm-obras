#!/usr/bin/env bash
# Teste de carga da API. Exige Docker; o k6 roda pela imagem grafana/k6.
#
#   perf/run.sh --target perf  --scenario medio --threads on
#   perf/run.sh --target local --scenario pequeno --threads off
#
# --target perf   sobe PostgreSQL e a app (perfil prod, CPU/memoria limitadas) em perf/compose.yaml,
#                 cria o admin de carga e a massa, roda o roteiro e derruba tudo (KEEP=true mantem de pe).
# --target local  usa a app ja rodando no perfil local (./mvnw spring-boot:run) em BASE_URL. Cria a massa
#                 so se a base nao tiver obras. --threads so registra o modo: suba a app com
#                 APP_VIRTUAL_THREADS=true|false conforme a medicao.
# Opcoes: --vus N (10), --duration D (2m), --think S (0). Ambiente: DB_POOL_MAX_SIZE, JAVA_OPTS, KEEP.
# Resumo em perf/results/<data>-<alvo>-<cenario>-<modo>.json
set -euo pipefail

PERF_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(dirname "$PERF_DIR")"
K6_IMAGE=grafana/k6:1.3.0

target="" scenario="pequeno" threads="on" vus=10 duration=2m think=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --target) target="$2"; shift 2 ;;
    --scenario) scenario="$2"; shift 2 ;;
    --threads) threads="$2"; shift 2 ;;
    --vus) vus="$2"; shift 2 ;;
    --duration) duration="$2"; shift 2 ;;
    --think) think="$2"; shift 2 ;;
    *) echo "opcao desconhecida: $1" >&2; exit 2 ;;
  esac
done
[[ "$target" == local || "$target" == perf ]] || { echo "use --target local|perf" >&2; exit 2; }
[[ "$threads" == on || "$threads" == off ]] || { echo "use --threads on|off" >&2; exit 2; }

# O k6 grava o resumo em perf/results com o usuario do host
export PERF_UID="$(id -u)" PERF_GID="$(id -g)"
mkdir -p "$PERF_DIR/results"

stamp="$(date +%Y%m%d-%H%M%S)"
result="$stamp-$target-$scenario-$threads.json"
commit="$(git -C "$ROOT_DIR" rev-parse --short HEAD)$(git -C "$ROOT_DIR" diff --quiet || echo '-dirty')"
cpu_model="$(grep -m1 'model name' /proc/cpuinfo 2>/dev/null | cut -d: -f2 | xargs || uname -m)"
mem_total="$(awk '/MemTotal/ {printf "%.0f GiB", $2/1048576}' /proc/meminfo 2>/dev/null || echo '?')"
machine="$cpu_model; $(nproc) CPUs; $mem_total"
[[ "$target" == perf ]] && machine="$machine; app e banco limitados a 2 CPUs e 1 GiB cada"

meta=(-e META_TARGET="$target" -e META_SCENARIO="$scenario" -e META_THREADS="$threads"
      -e META_POOL="${DB_POOL_MAX_SIZE:-10}" -e META_COMMIT="$commit" -e META_MACHINE="$machine"
      -e VUS="$vus" -e DURATION="$duration" -e THINK_TIME="$think")

# Espera a app responder (qualquer status HTTP) em ate 180 s
wait_up() {
  for _ in $(seq 1 180); do
    [[ "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$1/api/auth/login")" != 000 ]] && return 0
    sleep 1
  done
  echo "a app em $1 nao respondeu em 180 s" >&2
  return 1
}

wait_login() {
  local url=$1 email=$2 password=$3
  for _ in $(seq 1 180); do
    if curl -sf -o /dev/null -H 'Content-Type: application/json' \
         -d "{\"email\":\"$email\",\"password\":\"$password\"}" "$url/api/auth/login"; then
      return 0
    fi
    sleep 1
  done
  echo "login em $url nao respondeu em 180 s" >&2
  return 1
}

if [[ "$target" == perf ]]; then
  compose=(docker compose -f "$PERF_DIR/compose.yaml")
  port="${PERF_APP_PORT:-8090}"
  [[ "${KEEP:-false}" == true ]] || trap '"${compose[@]}" down -v --remove-orphans > /dev/null 2>&1' EXIT
  export APP_VIRTUAL_THREADS=$([[ "$threads" == on ]] && echo true || echo false)
  "${compose[@]}" down -v --remove-orphans > /dev/null 2>&1 || true
  "${compose[@]}" up -d --build postgres app
  wait_up "http://localhost:$port"
  "${compose[@]}" exec -T postgres psql -q -U app -d app_adm_obras < "$PERF_DIR/admin.sql"
  "${compose[@]}" exec -T postgres psql -q -U app -d app_adm_obras \
    -c 'create extension if not exists pg_stat_statements'
  psql_perf() { "${compose[@]}" exec -T postgres psql -U app -d app_adm_obras "$@"; }
  wait_login "http://localhost:$port" perf-admin@perf.test perf-admin-123
  creds=(-e BASE_URL=http://app:8080 -e ADMIN_EMAIL=perf-admin@perf.test -e ADMIN_PASSWORD=perf-admin-123)
  "${compose[@]}" run --rm "${creds[@]}" -e SCENARIO="$scenario" k6 run -q /scripts/seed.js
  # Zera as estatisticas depois da massa: o top reflete so o roteiro de carga
  psql_perf -q -c 'select pg_stat_statements_reset()' > /dev/null
  "${compose[@]}" run --rm "${creds[@]}" "${meta[@]}" -e RESULT_FILE="/results/$result" \
    k6 run -q /scripts/script.js
  psql_perf -c "select calls, round(total_exec_time::numeric, 1) as total_ms,
      round(mean_exec_time::numeric, 3) as mean_ms, rows, left(regexp_replace(query, '\s+', ' ', 'g'), 400) as query
    from pg_stat_statements where query not ilike '%pg_stat_statements%'
    order by total_exec_time desc limit 20" > "$PERF_DIR/results/${result%.json}-pg-top.txt"
else
  base_url="${BASE_URL:-http://localhost:8080}"
  email="${ADMIN_EMAIL:-admin@admin.com}" password="${ADMIN_PASSWORD:-admin123}"
  wait_login "$base_url" "$email" "$password"
  # Rede do host: no Linux o firewall costuma bloquear host.docker.internal a partir da bridge
  k6=(docker run --rm --network host --user "$PERF_UID:$PERF_GID" -v "$PERF_DIR:/scripts" -w /scripts
      -e BASE_URL="$base_url" -e ADMIN_EMAIL="$email" -e ADMIN_PASSWORD="$password")
  "${k6[@]}" -e SCENARIO="$scenario" -e SKIP_IF_SEEDED=true "$K6_IMAGE" run -q seed.js
  "${k6[@]}" "${meta[@]}" -e RESULT_FILE="/scripts/results/$result" "$K6_IMAGE" run -q script.js
fi

[[ -s "$PERF_DIR/results/$result" ]] || { echo "resumo nao foi gravado: perf/results/$result" >&2; exit 1; }
echo "resumo: perf/results/$result"
