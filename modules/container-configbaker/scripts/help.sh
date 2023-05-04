#!/bin/bash

set -euo pipefail

echo -e '          ╓mαo\n         ╫   jh\n         `%╥æ╨\n           ╫µ\n          ╓@M%╗,\n         ▓`    ╫U\n         ▓²    ╫╛\n          ▓M#M╝"\n  ┌æM╝╝%φ╫┘\n┌╫"      "╫┐\n▓          ▓\n▓          ▓\n`╫µ      ¿╫"\n  "╜%%MM╜`'
echo ""
echo "Hello!"
echo ""
echo "I'm ConfigBaker, a container image with lots of tooling to 'bake' a containerized Dataverse instance!"
echo "I can cook up an instance (initial config), put icing on your Solr search index configuration, and more!"
echo ""
echo "Here's a list of things I can do for you:"

ls -1 ${SCRIPT_DIR}
