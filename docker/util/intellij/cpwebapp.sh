#!/usr/bin/env bash
#
# cpwebapp <project dir> <file in webapp>
#

set -eu

PROJECT_DIR="$1"
FILE_TO_COPY="$2"
RELATIVE_PATH="${FILE_TO_COPY#"${PROJECT_DIR}/"}"

# Only act on files under src/main/webapp
if [[ "$RELATIVE_PATH" == "src/main/webapp"* ]]; then
  POM="$PROJECT_DIR/modules/dataverse-parent/pom.xml"

  # Extract <revision> in a portable way
  VERSION="$(awk -F'[<>]' '/<revision>/{print $3; exit}' "$POM")"

  if [[ -z "${VERSION:-}" ]]; then
    echo "Error: Could not extract <revision> from $POM" >&2
    exit 1
  fi

  CONTAINER="dev_dataverse"

  # Build target path
  RELATIVE_PATH_WITHOUT_WEBAPP="${RELATIVE_PATH#src/main/webapp/}"
  TARGET_PATH="/opt/payara/appserver/glassfish/domains/domain1/applications/dataverse-$VERSION/${RELATIVE_PATH_WITHOUT_WEBAPP}"

  # Copy file into container
  docker cp "$FILE_TO_COPY" "$CONTAINER:$TARGET_PATH"

  echo "Copied $FILE_TO_COPY → $CONTAINER:$TARGET_PATH"
fi
