#!/bin/bash

# This script will check our metadata block files and scan if the properties files contain all the matching keys.

set -euo pipefail

if ! which jbang > /dev/null 2>&1; then
  echo "Cannot find jbang on path. Did you install it?" >&2
  exit 1
fi
if ! which native-image > /dev/null 2>&1; then
  echo "Cannot find GraalVM native-image on path. Did you install it?" >&2
  exit 1
fi

FAIL=0

# We need a small Java app here, replacing spaces, converting to lower case but especially to replace UTF-8 chars with nearest ascii / strip accents because of
# https://github.com/IQSS/dataverse/blob/dddcf29188a5c35174f3c94ffc1c4cb1d7fc0552/src/main/java/edu/harvard/iq/dataverse/ControlledVocabularyValue.java#L139-L140
# This cannot be replaced by another tool, as it behaves rather individually.
DIR=$(mktemp -d)
SOURCE="$DIR/stripaccents.java"
STRIP_BIN="$(dirname "$0")/stripaccents"
cat > "$SOURCE" << EOF
///usr/bin/env jbang "\$0" "\$@" ; exit \$?
//JAVA 11+
//DEPS org.apache.commons:commons-lang3:3.12.0
import org.apache.commons.lang3.StringUtils;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
class stripaccents {
    public static void main(String[] args) throws IOException {
        String input = new String(System.in.readAllBytes(), StandardCharsets.UTF_8).toLowerCase().replace(" ", "_");
        System.out.println(StringUtils.stripAccents(input));
    }
}
EOF
jbang export native --force --fresh -O "$STRIP_BIN" "$SOURCE"

while IFS= read -r -d '' MDB; do

    echo "::group::$MDB"
    BLOCK_NAME=$(sed -n "2p" "$MDB" | cut -f2)
    BLOCK_DISPLAY_NAME=$(sed -n "2p" "$MDB" | cut -f4)
    PROPERTIES_FILE="$(git rev-parse --show-toplevel)/src/main/java/propertyFiles/$BLOCK_NAME.properties"

    # Check correct file exists
    if [ ! -r "$PROPERTIES_FILE" ]; then
        echo "::error::Missing properties file for metadata block '$BLOCK_NAME', expected at '$PROPERTIES_FILE'"
        FAIL=1
        continue
    fi

    # Check metadata block properties exist and are equal to TSV source
    if ! grep -a -q -e "^metadatablock.name=$BLOCK_NAME$" "$PROPERTIES_FILE"; then
        echo "::error::Missing 'metadatablock.name=$BLOCK_NAME' or different from TSV source in $PROPERTIES_FILE"
        FAIL=1
    fi
    if ! grep -a -q -e "^metadatablock.displayName=$BLOCK_DISPLAY_NAME$" "$PROPERTIES_FILE"; then
        echo "::error::Missing 'metadatablock.displayName=$BLOCK_DISPLAY_NAME' or different from TSV source in $PROPERTIES_FILE"
        FAIL=1
    fi
    if ! grep -a -q -e "^metadatablock.displayFacet=" "$PROPERTIES_FILE"; then
        echo "::error::Missing 'metadatablock.displayFacet=...' in $PROPERTIES_FILE"
        FAIL=1
    fi

    # Check dataset fields
    for FIELD in $(grep -a -A1000 "^#datasetField" "$MDB" | tail -n+2 | grep -a -B1000 "^#controlledVocabulary" | head -n-1 | cut -f2); do
        for ENTRY in title description watermark; do
            if ! grep -a -q -e "^datasetfieldtype.$FIELD.$ENTRY=" "$PROPERTIES_FILE"; then
                echo "::error::Missing key 'datasetfieldtype.$FIELD.$ENTRY=...' in $PROPERTIES_FILE"
                FAIL=1
            fi
        done
    done

    # Check CV entries
    while read -r LINE; do
        FIELD_NAME=$(echo "$LINE" | cut -f1)
        # See https://github.com/IQSS/dataverse/blob/dddcf29188a5c35174f3c94ffc1c4cb1d7fc0552/src/main/java/edu/harvard/iq/dataverse/ControlledVocabularyValue.java#L139-L140
        # Square brackets are special in grep with expressions activated, so escape them if present!
        FIELD_VALUE=$(echo "$LINE" | cut -f2 | "$STRIP_BIN" | sed -e 's/\([][]\)/\\\1/g' )

        if ! grep -q -a -e "^controlledvocabulary.$FIELD_NAME.$FIELD_VALUE=" "$PROPERTIES_FILE"; then
            echo "::error::Missing key 'controlledvocabulary.$FIELD_NAME.$FIELD_VALUE=...' in $PROPERTIES_FILE"
            FAIL=1
        fi
    done < <(grep -a -A1000 "^#controlledVocabulary" "$MDB" | tail -n+2)

    echo "::endgroup::"

done < <( find "$(git rev-parse --show-toplevel)/scripts/api/data/metadatablocks" -name '*.tsv' -print0 )

rm "$SOURCE" "$STRIP_BIN"

if [ "$FAIL" -eq 1 ]; then
    exit 1
fi
