#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/../.."

# Verify dev environment is running
if ! docker ps --filter "name=dev_dataverse" --filter "status=running" -q | grep -q .; then
    echo "Error: dev_dataverse container not running." >&2
    echo "Run './scripts/dev/dev-start-frd.sh' first to set up the environment." >&2
    exit 1
fi

echo "Compiling Dataverse sources..."
mvn -T 1C -DskipTests -DskipUnitTests -DskipIntegrationTests compile >/dev/null

if [ ! -d "target/classes" ]; then
    echo "ERROR: target/classes missing after compile." >&2
    exit 1
fi

echo "Syncing compiled classes..."
rsync -a --delete --exclude 'META-INF/persistence.xml' \
    target/classes/ target/dataverse/WEB-INF/classes/

if [ -d "src/main/webapp" ]; then
    echo "Syncing webapp resources..."
    rsync -a --delete \
        --exclude 'WEB-INF/classes' --exclude 'WEB-INF/classes/**' \
        --exclude 'WEB-INF/lib' --exclude 'WEB-INF/lib/**' \
        src/main/webapp/ target/dataverse/
fi

echo "Redeploying to Payara..."
docker exec dev_dataverse /bin/bash -lc '
    printf "AS_ADMIN_PASSWORD=%s\n" admin > /tmp/pwdfile;
    asadmin --user admin --passwordfile /tmp/pwdfile \
        deploy --force --upload=false /opt/payara/deployments/dataverse 2>&1 \
        | grep -v "PER01001\|PER01003\|Command deploy completed with warnings";
    rm /tmp/pwdfile'

echo ""
echo "✓ Fast redeploy complete (~12s)"
echo "  Test your changes at http://localhost:8080"
