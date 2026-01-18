#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="${ROOT_DIR}/logs"
mkdir -p "${LOG_DIR}"

ENV_FILE="${ROOT_DIR}/.env"
if [ -f "${ENV_FILE}" ]; then
  set -a
  # shellcheck disable=SC1090
  source "${ENV_FILE}"
  set +a
fi

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

require_cmd docker
require_cmd curl

JAVA_BIN=""
if [ -n "${JAVA_HOME:-}" ] && [ -x "${JAVA_HOME}/bin/java" ]; then
  JAVA_BIN="${JAVA_HOME}/bin/java"
elif command -v java >/dev/null 2>&1; then
  JAVA_BIN="$(command -v java)"
else
  echo "Java not found. Install JDK 17 and set JAVA_HOME or ensure java is on PATH." >&2
  exit 1
fi

JAVA_VERSION="$("${JAVA_BIN}" -version 2>&1 | head -n 1)"
if ! echo "${JAVA_VERSION}" | grep -q '"17\.'; then
  echo "Java 17 required. Detected: ${JAVA_VERSION}" >&2
  exit 1
fi

if ! docker info >/dev/null 2>&1; then
  echo "Docker is not running. Start Docker Desktop and retry." >&2
  exit 1
fi

remove_container_if_exists() {
  local name="$1"
  if docker ps -a --format '{{.Names}}' | grep -qx "${name}"; then
    echo "Removing existing container ${name} to avoid name conflicts..."
    docker rm -f "${name}" >/dev/null 2>&1 || true
  fi
}

echo "Starting infrastructure containers..."
remove_container_if_exists "ecommerce-mysql"
remove_container_if_exists "ecommerce-redis"
remove_container_if_exists "ecommerce-kafka"
docker compose -f "${ROOT_DIR}/docker-compose.yml" up -d

echo "Building services..."
"${ROOT_DIR}/mvnw" clean package -DskipTests

wait_for_http() {
  local name="$1"
  local url="$2"
  local attempts=60
  for _ in $(seq 1 "${attempts}"); do
    if curl -fsS "${url}" >/dev/null 2>&1; then
      echo "OK: ${name} is healthy"
      return 0
    fi
    sleep 2
  done
  echo "Timed out waiting for ${name} at ${url}" >&2
  return 1
}

start_service() {
  local name="$1"
  local jar="$2"
  local port="$3"
  local pid_file="${LOG_DIR}/${name}.pid"
  local log_file="${LOG_DIR}/${name}.stdout.log"

  if [ -f "${pid_file}" ] && kill -0 "$(cat "${pid_file}")" >/dev/null 2>&1; then
    echo "Skipping ${name} (already running, pid $(cat "${pid_file}"))"
    return 0
  fi

  if [ ! -f "${jar}" ]; then
    echo "Missing jar: ${jar}" >&2
    exit 1
  fi

  echo "Starting ${name}..."
  nohup "${JAVA_BIN}" -jar "${jar}" >"${log_file}" 2>&1 &
  echo $! > "${pid_file}"
  wait_for_http "${name}" "http://localhost:${port}/actuator/health"
}

start_service "service-discovery" "${ROOT_DIR}/service-discovery/target/service-discovery-0.0.1-SNAPSHOT.jar" 8761
start_service "user-service" "${ROOT_DIR}/user-service/target/user-service-0.0.1-SNAPSHOT.jar" 8081
start_service "product-catalog-service" "${ROOT_DIR}/product-catalog-service/target/product-catalog-service-0.0.1-SNAPSHOT.jar" 8082
start_service "cart-service" "${ROOT_DIR}/cart-service/target/cart-service-0.0.1-SNAPSHOT.jar" 8083
start_service "order-service" "${ROOT_DIR}/order-service/target/order-service-0.0.1-SNAPSHOT.jar" 8084
start_service "payment-service" "${ROOT_DIR}/payment-service/target/payment-service-0.0.1-SNAPSHOT.jar" 8085
start_service "notification-service" "${ROOT_DIR}/notification-service/target/notification-service-0.0.1-SNAPSHOT.jar" 8086

echo "All services are up. Logs: ${LOG_DIR}"
