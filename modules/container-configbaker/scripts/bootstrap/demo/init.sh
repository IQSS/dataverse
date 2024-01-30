#!/bin/bash

set -euo pipefail

# Set some defaults as documented
DATAVERSE_URL=${DATAVERSE_URL:-"http://dataverse:8080"}
export DATAVERSE_URL

echo "Running base setup-all.sh..."
"${BOOTSTRAP_DIR}"/base/setup-all.sh -p=admin1 | tee /tmp/setup-all.sh.out

echo ""
echo "Done, your instance has been configured for demo or eval. Have a nice day!"
