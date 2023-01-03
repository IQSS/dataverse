#!/bin/bash

# Remove expired certs from a keystore
# ------------------------------------
# This script was copied from https://gist.github.com/damkh/a4a0d74891f92b0285a3853418357c1e (thanks @damkh)
# and slightly modified to be used within our scenario and comply with shellcheck good practices.

set -euo pipefail

KEYSTORE="${DOMAIN_DIR}/config/cacerts.jks"
keytool -list -v -keystore "${KEYSTORE}" -storepass changeit 2>/dev/null | \
    grep -i 'alias\|until' > aliases.txt

i=1
# Split dates and aliases to different arrays
while read -r p; do
    # uneven lines are dates, evens are aliases
    if ! ((i % 2)); then
        arr_date+=("$p")
    else
        arr_cn+=("$p")
    fi
    i=$((i+1))
done < aliases.txt
i=0

# Parse until-dates ->
# convert until-dates to "seconds from 01-01-1970"-format ->
# compare until-dates with today-date ->
# delete expired aliases
for date_idx in $(seq 0 $((${#arr_date[*]}-1)));
do
    a_date=$(echo "${arr_date[$date_idx]}" | awk -F"until: " '{print $2}')
    if [ "$(date +%s --date="$a_date")" -lt "$(date +%s)" ];
    then
        echo "removing ${arr_cn[$i]} expired: $a_date"
        alias_name=$(echo "${arr_cn[$i]}" | awk -F"name: " '{print $2}')
        keytool -delete -alias "$alias_name" -keystore "${KEYSTORE}" -storepass changeit
    fi
    i=$((i+1))
done
echo "Done."