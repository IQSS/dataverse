#!/bin/bash

set -euo pipefail

# [INFO]: This script.

# This is the Dataverse logo in ASCII
# shellcheck disable=SC2016
echo -e '          ╓mαo\n         ╫   jh\n         `%╥æ╨\n           ╫µ\n          ╓@M%╗,\n         ▓`    ╫U\n         ▓²    ╫╛\n          ▓M#M╝"\n  ┌æM╝╝%φ╫┘\n┌╫"      "╫┐\n▓          ▓\n▓          ▓\n`╫µ      ¿╫"\n  "╜%%MM╜`'
echo ""
echo "Hello!"
echo ""
echo "My name is Config Baker. I'm a container image with lots of tooling to 'bake' a containerized Dataverse instance!"
echo "I can cook up an instance (initial config), put icing on your Solr search index configuration, and more!"
echo ""
echo "Here's a list of things I can do for you:"

# Get the longest name length
LENGTH=1
for SCRIPT in "${SCRIPT_DIR}"/*.sh; do
  L="$(basename "$SCRIPT" | wc -m)"
  if [ "$L" -gt "$LENGTH" ]; then
    LENGTH="$L"
  fi
done

# Print script names and info, but formatted
for SCRIPT in "${SCRIPT_DIR}"/*.sh; do
  printf "%${LENGTH}s - " "$(basename "$SCRIPT")"
  grep "# \[INFO\]: " "$SCRIPT" | sed -e "s|# \[INFO\]: ||"
done

echo ""
echo "Simply execute this container with the script name (and potentially arguments) as 'command'."
