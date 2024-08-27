#!/bin/bash

# This script will check Java *.properties files within the src dir for duplicates
# and print logs with file annotations about it.

set -euo pipefail

FAIL=0

while IFS= read -r -d '' FILE; do

    # Scan the whole file for duplicates
    FILTER=$(grep -a -v -E "^(#.*|\s*$)" "$FILE" | cut -d"=" -f1 | sort | uniq -c | tr -s " " | { grep -vs "^ 1 " || true; })

    # If there are any duplicates present, analyse further to point people to the source
    if [ -n "$FILTER" ]; then
        FAIL=1

        echo "::group::$FILE"
        for KEY in $(echo "$FILTER" | cut -d" " -f3); do
            # Find duplicate lines' numbers by grepping for the KEY and cutting the number from the output
            DUPLICATE_LINES=$(grep -n -E -e "^$KEY=" "$FILE" | cut -d":" -f1)
            # Join the found line numbers for better error log
            DUPLICATE_NUMBERS=$(echo "$DUPLICATE_LINES" | paste -sd ',')

            # This form will make Github annotate the lines in the PR that changes the properties file
            for LINE_NUMBER in $DUPLICATE_LINES; do
                echo "::error file=$FILE,line=$LINE_NUMBER::Found duplicate for key '$KEY' in lines $DUPLICATE_NUMBERS"
            done
        done
        echo "::endgroup::"
    fi
done < <( find "$(git rev-parse --show-toplevel)" -wholename "*/src/*.properties" -print0 )

if [ "$FAIL" -eq 1 ]; then
    exit 1
fi
