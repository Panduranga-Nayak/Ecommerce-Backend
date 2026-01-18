#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="${ROOT_DIR}/logs"

KEEP_DOCKER="false"
if [ "${1:-}" = "--keep-docker" ]; then
  KEEP_DOCKER="true"
fi

stop_pid() {
  local name="$1"
  local pid_file="${LOG_DIR}/${name}.pid"
  if [ ! -f "${pid_file}" ]; then
    echo "No pid file for ${name}; skipping"
    return
  fi

  local pid
  pid="$(cat "${pid_file}")"
  if ! kill -0 "${pid}" >/dev/null 2>&1; then
    echo "${name} not running (pid ${pid}); removing pid file"
    rm -f "${pid_file}"
    return
  fi

  echo "Stopping ${name} (pid ${pid})..."
  kill "${pid}" >/dev/null 2>&1 || true

  for _ in $(seq 1 30); do
    if ! kill -0 "${pid}" >/dev/null 2>&1; then
      rm -f "${pid_file}"
      echo "Stopped ${name}"
      return
    fi
    sleep 1
  done

  echo "Force stopping ${name} (pid ${pid})..."
  kill -9 "${pid}" >/dev/null 2>&1 || true
  rm -f "${pid_file}"
}

kill_port() {
  local port="$1"
  if ! command -v lsof >/dev/null 2>&1; then
    echo "lsof not available; cannot check port ${port}"
    return
  fi
  local pids
  pids="$(lsof -nP -iTCP:${port} -sTCP:LISTEN -t 2>/dev/null || true)"
  if [ -z "${pids}" ]; then
    return
  fi
  echo "Killing processes on port ${port}: ${pids}"
  for pid in ${pids}; do
    kill "${pid}" >/dev/null 2>&1 || true
  done
  sleep 1
  pids="$(lsof -nP -iTCP:${port} -sTCP:LISTEN -t 2>/dev/null || true)"
  if [ -n "${pids}" ]; then
    echo "Force killing processes on port ${port}: ${pids}"
    for pid in ${pids}; do
      kill -9 "${pid}" >/dev/null 2>&1 || true
    done
  fi
}

if [ -d "${LOG_DIR}" ]; then
  stop_pid "notification-service"
  stop_pid "payment-service"
  stop_pid "order-service"
  stop_pid "cart-service"
  stop_pid "product-catalog-service"
  stop_pid "user-service"
  stop_pid "service-discovery"
else
  echo "No logs directory found at ${LOG_DIR}; skipping service shutdown"
fi

kill_port 8081
kill_port 8082
kill_port 8083
kill_port 8084
kill_port 8085
kill_port 8086
kill_port 8761

if [ "${KEEP_DOCKER}" = "false" ]; then
  if command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
    echo "Stopping Docker dependencies..."
    docker compose -f "${ROOT_DIR}/docker-compose.yml" down
    for name in ecommerce-mysql ecommerce-redis ecommerce-kafka; do
      if docker ps -a --format '{{.Names}}' | grep -qx "${name}"; then
        echo "Removing leftover container ${name}..."
        docker rm -f "${name}" >/dev/null 2>&1 || true
      fi
    done
  else
    echo "Docker not available/running; skipping docker compose down"
  fi
else
  echo "Skipping docker compose down (--keep-docker)"
fi
