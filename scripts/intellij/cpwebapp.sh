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

PROJECT_DIR=$1
FILE_TO_COPY=$2
RELATIVE_PATH="${FILE_TO_COPY#$PROJECT_DIR/}"

# Check if RELATIVE_PATH starts with 'src/main/webapp', otherwise ignore
if [[ $RELATIVE_PATH == src/main/webapp* ]]; then
    # Get current version. Any other way to do this? A simple VERSION file would help.
    VERSION=`perl -ne 'print $1 if /<revision>(.*?)<\/revision>/' ./modules/dataverse-parent/pom.xml`
    RELATIVE_PATH_WITHOUT_WEBAPP="${RELATIVE_PATH#src/main/webapp/}"
    TARGET_DIR=./docker-dev-volumes/glassfish/applications/dataverse-$VERSION
    TARGET_PATH="${TARGET_DIR}/${RELATIVE_PATH_WITHOUT_WEBAPP}"

    mkdir -p "$(dirname "$TARGET_PATH")"
    cp "$FILE_TO_COPY" "$TARGET_PATH"

    echo "File $FILE_TO_COPY copied to $TARGET_PATH"
fi
