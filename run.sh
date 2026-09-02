#!/usr/bin/env bash
# Start (or stop/inspect) the whole project: Docker (MySQL + Redis), backend (Spring Boot),
# and frontend (Angular). Safe to re-run - already-running pieces are left alone.
#
# Usage:
#   ./run.sh            # start everything that isn't already running
#   ./run.sh start      # same as above
#   ./run.sh stop       # stop backend + frontend (docker is left running - see stop())
#   ./run.sh status      # show what's up
#   ./run.sh logs [backend|frontend]   # tail logs (both by default)

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_DIR="$ROOT_DIR/frontend"
RUN_DIR="$ROOT_DIR/.run"
LOG_DIR="$RUN_DIR/logs"
PID_DIR="$RUN_DIR/pids"

BACKEND_PORT=8080
FRONTEND_PORT=4200
ADMINER_PORT=8081

# ---------- helpers ----------

is_port_open() {
  nc -z localhost "$1" >/dev/null 2>&1
}

pid_is_running() {
  local pid_file="$1"
  [ -f "$pid_file" ] && kill -0 "$(cat "$pid_file")" 2>/dev/null
}

wait_for_port() {
  local port="$1" label="$2" timeout="$3" waited=0
  printf "  waiting for %s on port %s" "$label" "$port"
  while ! is_port_open "$port"; do
    if [ "$waited" -ge "$timeout" ]; then
      echo " - still not up after ${timeout}s, check the log"
      return 1
    fi
    printf "."
    sleep 2
    waited=$((waited + 2))
  done
  echo " - up (${waited}s)"
}

require_tool() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required tool: $1" >&2
    exit 1
  fi
}

# ---------- actions ----------

start_docker() {
  echo "== Docker (MySQL + Redis) =="
  require_tool docker
  # Checked by port, not by `docker compose ps` for this project specifically: MySQL/Adminer
  # may already be reachable under a differently-named container/project (e.g. left over from
  # before this repo was renamed) - as long as something is actually listening on 3306/6379,
  # there's no need to start (or risk conflicting with) another container for it.
  if is_port_open 3306 && is_port_open 6379; then
    echo "  MySQL + Redis already reachable, skipping."
  else
    echo "  starting docker compose services..."
    (cd "$BACKEND_DIR" && docker compose up -d)
  fi
}

start_backend() {
  echo "== Backend (Spring Boot, port $BACKEND_PORT) =="
  require_tool mvn
  mkdir -p "$LOG_DIR" "$PID_DIR"
  if is_port_open "$BACKEND_PORT"; then
    echo "  already running on port $BACKEND_PORT, skipping."
    return
  fi
  if pid_is_running "$PID_DIR/backend.pid"; then
    echo "  already tracked as running (pid $(cat "$PID_DIR/backend.pid")), skipping."
    return
  fi
  echo "  starting, logging to $LOG_DIR/backend.log ..."
  # fork=false keeps the app in Maven's own JVM (no detached child process), so the pid we
  # capture here is the one that actually needs killing later - no orphaned forked JVM.
  (cd "$BACKEND_DIR" && nohup mvn -q spring-boot:run -Dspring-boot.run.fork=false \
    > "$LOG_DIR/backend.log" 2>&1 & echo $! > "$PID_DIR/backend.pid")
  wait_for_port "$BACKEND_PORT" "backend" 90 || echo "  see: $LOG_DIR/backend.log"
}

start_frontend() {
  echo "== Frontend (Angular, port $FRONTEND_PORT) =="
  require_tool npm
  mkdir -p "$LOG_DIR" "$PID_DIR"
  if is_port_open "$FRONTEND_PORT"; then
    echo "  already running on port $FRONTEND_PORT, skipping."
    return
  fi
  if pid_is_running "$PID_DIR/frontend.pid"; then
    echo "  already tracked as running (pid $(cat "$PID_DIR/frontend.pid")), skipping."
    return
  fi
  echo "  starting, logging to $LOG_DIR/frontend.log ..."
  (cd "$FRONTEND_DIR" && nohup npm start \
    > "$LOG_DIR/frontend.log" 2>&1 & echo $! > "$PID_DIR/frontend.pid")
  wait_for_port "$FRONTEND_PORT" "frontend" 90 || echo "  see: $LOG_DIR/frontend.log"
}

start_all() {
  start_docker
  start_backend
  start_frontend
  echo
  echo "Frontend: http://localhost:$FRONTEND_PORT"
  echo "Backend:  http://localhost:$BACKEND_PORT/api"
  echo "Adminer:  http://localhost:$ADMINER_PORT"
  echo "Logs:     $LOG_DIR"
  echo "Stop with: $0 stop"
}

stop_one() {
  local name="$1" port="$2"
  local pid_file="$PID_DIR/$name.pid"
  if pid_is_running "$pid_file"; then
    local pid
    pid="$(cat "$pid_file")"
    echo "  stopping $name (pid $pid)..."
    # Also stop the direct child (e.g. npm's ng process) - the parent alone often leaves it running.
    pkill -P "$pid" 2>/dev/null || true
    kill "$pid" 2>/dev/null || true
    sleep 1
    kill -9 "$pid" 2>/dev/null || true
  elif is_port_open "$port"; then
    echo "  $name isn't tracked by this script but port $port is in use - leaving it alone."
  else
    echo "  $name not running."
  fi
  rm -f "$pid_file"
}

stop_all() {
  echo "== Stopping backend + frontend =="
  stop_one backend "$BACKEND_PORT"
  stop_one frontend "$FRONTEND_PORT"
  echo
  echo "Docker (MySQL/Redis) left running - it's meant to stay up between sessions."
  echo "To stop it too: (cd backend && docker compose down)"
}

status_all() {
  echo "== Docker =="
  echo -n "  MySQL (3306): "; is_port_open 3306 && echo "up" || echo "down"
  echo -n "  Redis (6379): "; is_port_open 6379 && echo "up" || echo "down"
  echo
  echo "== Backend (port $BACKEND_PORT) =="
  is_port_open "$BACKEND_PORT" && echo "  up" || echo "  down"
  echo "== Frontend (port $FRONTEND_PORT) =="
  is_port_open "$FRONTEND_PORT" && echo "  up" || echo "  down"
}

logs_all() {
  local which="${1:-}"
  case "$which" in
    backend) tail -f "$LOG_DIR/backend.log" ;;
    frontend) tail -f "$LOG_DIR/frontend.log" ;;
    *) tail -f "$LOG_DIR/backend.log" "$LOG_DIR/frontend.log" ;;
  esac
}

# ---------- entrypoint ----------

case "${1:-start}" in
  start) start_all ;;
  stop) stop_all ;;
  status) status_all ;;
  logs) logs_all "${2:-}" ;;
  *)
    echo "Usage: $0 [start|stop|status|logs [backend|frontend]]" >&2
    exit 1
    ;;
esac
