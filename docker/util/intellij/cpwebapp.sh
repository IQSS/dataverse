#!/usr/bin/env bash
#
# cpwebapp <project dir> <file in webapp>
#
# Usage:
#
# Add a File watcher by importing watchers.xml into IntelliJ IDEA, and let it do the copying whenever you save a
# file under webapp.
#
#     https://www.jetbrains.com/help/idea/settings-tools-file-watchers.html
#
# Alternatively, you can add an External tool and trigger via menu or shortcut to do the copying manually:
#
#     https://www.jetbrains.com/help/idea/configuring-third-party-tools.html
#

set -eu

PROJECT_DIR="$1"
FILE_TO_COPY="$2"
RELATIVE_PATH="${FILE_TO_COPY#$PROJECT_DIR/}"

# Check if RELATIVE_PATH starts with 'src/main/webapp', otherwise ignore
if [[ "$RELATIVE_PATH" == "src/main/webapp"* ]]; then
    # Extract version from POM, so we don't need to have Maven on the PATH
    VERSION=$(grep -oPm1 "(?<=<revision>)[^<]+" "$PROJECT_DIR/modules/dataverse-parent/pom.xml")

    # Construct the target path by cutting off the local prefix and prepend with in-container path
    RELATIVE_PATH_WITHOUT_WEBAPP="${RELATIVE_PATH#src/main/webapp/}"
    TARGET_PATH="/opt/payara/appserver/glassfish/domains/domain1/applications/dataverse-$VERSION/${RELATIVE_PATH_WITHOUT_WEBAPP}"

    # Copy file to container
    docker cp "$FILE_TO_COPY" "dev_dataverse:$TARGET_PATH"
fi
