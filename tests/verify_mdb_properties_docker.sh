#!/usr/bin/env bash

# Run verify_mdb_properties.sh in a Linux container with GraalVM native-image and JBang.

set -euo pipefail

if ! command -v docker > /dev/null 2>&1; then
  echo "Cannot find docker on path. Did you install Docker Desktop or another Docker runtime?" >&2
  exit 1
fi

REPO_ROOT=$(git rev-parse --show-toplevel)
IMAGE=${VERIFY_MDB_PROPERTIES_IMAGE:-dataverse-verify-mdb-properties}
DOCKERFILE="$REPO_ROOT/tests/Dockerfile.verify_mdb_properties"

docker build -f "$DOCKERFILE" -t "$IMAGE" "$REPO_ROOT"
docker run --rm \
  -v "$REPO_ROOT:/workspace" \
  -w /workspace \
  --entrypoint /bin/bash \
  "$IMAGE" \
  -lc 'git config --global --add safe.directory /workspace && tests/verify_mdb_properties.sh'
