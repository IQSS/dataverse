#!/usr/bin/env bash
set -euo pipefail

# Ensure we're in project root
cd "$(dirname "${BASH_SOURCE[0]}")/../.."

echo "Building Dataverse WAR for fast redeploy..."
mvn -T 1C -DskipTests -DskipUnitTests -DskipIntegrationTests clean package

echo "Extracting WAR into target/dataverse/..."
mkdir -p target/dataverse
unzip -oq target/dataverse-*.war -d target/dataverse/

# Check if database is already initialized (before creating directories)
# If postgres has initialized, the data dir will have restrictive permissions (0700)
# On first run, the directory either doesn't exist or has default permissions
DB_INITIALIZED=false
if [ -d "docker-dev-volumes/postgresql/data" ]; then
    # Try to list the directory - if permission denied, it means postgres owns it (initialized)
    if ! ls docker-dev-volumes/postgresql/data >/dev/null 2>&1; then
        DB_INITIALIZED=true
    fi
fi

echo "Ensuring docker volume mount points exist..."
mkdir -p docker-dev-volumes/app/data
mkdir -p docker-dev-volumes/app/secrets
mkdir -p docker-dev-volumes/postgresql/data
mkdir -p docker-dev-volumes/solr/data
mkdir -p docker-dev-volumes/solr/conf
mkdir -p docker-dev-volumes/minio_storage

# Only disable DDL generation if database is already initialized
# (on first run, we need create-tables to bootstrap the schema)
if [ "$DB_INITIALIZED" = true ]; then
    echo "Detected existing database - disabling DDL generation to preserve schema..."
    sed -i.bak 's/\(eclipselink.ddl-generation" value="\)create-tables/\1none/' \
        target/dataverse/WEB-INF/classes/META-INF/persistence.xml
    rm -f target/dataverse/WEB-INF/classes/META-INF/persistence.xml.bak
else
    echo "First-time setup detected - keeping DDL generation enabled for schema creation..."
fi

echo "Starting dev stack (SKIP_DEPLOY=1)..."
export SKIP_DEPLOY=1
# Use override file if it exists (for local customizations like memory limits)
if [ -f docker-compose.override.yml ]; then
    docker compose -f docker-compose-dev.yml -f docker-compose.override.yml up -d
else
    docker compose -f docker-compose-dev.yml up -d
fi

echo "Waiting for Payara to be ready..."
until curl -sf http://localhost:8080/ >/dev/null 2>&1; do
    sleep 2
done

echo "Deploying exploded WAR..."
docker exec dev_dataverse /bin/bash -lc '
    printf "AS_ADMIN_PASSWORD=%s\n" admin > /tmp/pwdfile;
    asadmin --user admin --passwordfile /tmp/pwdfile \
        deploy --upload=false /opt/payara/deployments/dataverse 2>&1 \
        | grep -v "PER01001\|PER01003\|Command deploy completed with warnings";
    rm /tmp/pwdfile'

echo ""
echo "✓ Fast redeploy environment ready!"
echo "  Next: Make code changes, then run './scripts/dev/dev-frd.sh' to redeploy (~12s)"
