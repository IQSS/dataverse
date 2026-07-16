#!/usr/bin/env bash
set -euo pipefail

# Ensure we're in project root
cd "$(dirname "${BASH_SOURCE[0]}")/../.."

echo "Stopping and removing dev containers..."

# Use override file if it exists (for local customizations like memory limits)
if [ -f docker-compose.override.yml ]; then
    docker compose -f docker-compose-dev.yml -f docker-compose.override.yml down
else
    docker compose -f docker-compose-dev.yml down
fi

echo ""
echo "✓ Dev environment stopped"
echo "  To restart: ./scripts/dev/dev-start-frd.sh"
echo "  To clean volumes: sudo rm -rf docker-dev-volumes/"
