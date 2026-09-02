#!/usr/bin/env bash
# Thin wrapper around the deploy.sh scripts that own the real deploy logic:
#   ../deployment/bookdb-api/deploy.sh
#   ../deployment/bookdb-ui/deploy.sh
# This file does NOT duplicate that logic - it just cd's over and calls it, so
# there is one source of truth for the (already-running) deployment.
#
# Usage:
#   ./server.sh build   [api|all]       # mvn package (api only - ui builds inside Docker)
#   ./server.sh push    [api|ui|all]    # bump version + build/push docker image
#   ./server.sh deploy  [api|ui|all]    # full pipeline: build -> push -> kubectl apply
#   ./server.sh apply   [api|ui|all]    # kubectl apply only, no rebuild

set -e

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOYMENT_DIR="$(cd "$ROOT_DIR/../deployment" && pwd)"

command="${1:-}"
target="${2:-all}"

run_api(){
  (cd "$DEPLOYMENT_DIR/bookdb-api" && APP_DIR="$ROOT_DIR/backend" ./deploy.sh "$command")
}

run_ui(){
  if [ "$command" = "build" ]; then
    echo "bookdb-ui: nothing to build locally - the Angular build happens inside the Docker image (see 'push'/'deploy')."
    return
  fi
  (cd "$DEPLOYMENT_DIR/bookdb-ui" && APP_DIR="$ROOT_DIR/frontend" ./deploy.sh "$command")
}

case "$command" in
  build|push|deploy|apply)
    case "$target" in
      api) run_api ;;
      ui) run_ui ;;
      all) run_api; run_ui ;;
      *) echo "Unknown target '$target'. Use api, ui, or all." >&2; exit 1 ;;
    esac
    ;;
  *)
    echo "Usage: $0 {build|push|deploy|apply} [api|ui|all]" >&2
    echo "Delegates to deploy.sh in $DEPLOYMENT_DIR/bookdb-api and $DEPLOYMENT_DIR/bookdb-ui." >&2
    exit 1
    ;;
esac
