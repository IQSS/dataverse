## Faster Deployments and a Fast Redeploy Workflow for Container-Based Development

### Tables Are Now Only Created When Missing (Faster Deployments for Everyone)

The application no longer runs EclipseLink DDL generation (`eclipselink.ddl-generation=create-tables`) on every
deployment. Instead, a quick check at startup detects whether any entity tables are missing from the database
(first boot on an empty database, or newly added entities) and only then creates them - using the same EclipseLink
schema framework as before, so the semantics are unchanged. On all other (re)deployments, table creation is skipped
entirely, which noticeably speeds up deployment - in containers, classic installations and development environments
alike. Incremental schema changes continue to be handled by Flyway migrations on every startup, as before.

### Fast Redeploy for Container-Based Development

Container-based development gains a portable, Maven-based fast-redeploy workflow that works on any
platform and with any editor or IDE:

```bash
# Start the dev environment the usual way
mvn -Pct clean package docker:run

# Make code changes, then hot-redeploy them into the running container in ~10-15 seconds
mvn -Pfrd package

# Repeat as needed; when finished, stop the environment the usual way
mvn -Pct docker:stop
```

`mvn -Pfrd package` incrementally compiles your changes, refreshes the exploded WAR at `target/dataverse` (bind
mounted into the application container) and makes Payara hot-redeploy it - no container restarts, no image rebuilds.
Flyway migrations run on every redeploy, and tables for newly added entities are created automatically (see above).

### Metadata Blocks and Solr Schema Updates in the Dev Environment

A new one-shot service `dev_metadata_update` in `docker-compose-dev.yml` keeps a running dev instance in sync with
the metadata block TSV files in your working tree: on every start of the stack (and on demand via
`docker start -a dev_metadata_update`) it reloads the standard metadata blocks and
updates the Solr schema of an already bootstrapped instance. Previously, TSV changes were only picked up when
bootstrapping a fresh database. The `dev_bootstrap` service now also uses the TSV files from your working tree
instead of the ones baked into the config baker image. This is backed by a new `update-metadata.sh` script in the
config baker image.

### Memory Configuration of the Dev Environment

The application container in `docker-compose-dev.yml` now runs with a 6 GiB memory limit (previously 2.5 GiB).
The old limit could not support the hot-redeploy workflow: each redeploy retains some memory in the running server
(roughly 150-200 MiB, mostly classloader leftovers), and measurements showed the container being OOM-killed by the
kernel after only about 3 redeploys at 2.5 GiB. At 6 GiB, well over 20 consecutive redeploys have been verified.
Since a limit is not a reservation (an idle instance uses about 1.5 GiB), this does not increase the baseline
footprint. The `docker-compose.override.yml` file name is gitignored and documented as the place for
personal local overrides.

### Documentation

See the [Fast Redeploy (Command-Line)](https://guides.dataverse.org/en/latest/container/dev-usage.html#dev-fast-redeploy) section in the Container Guide for complete usage instructions and limitations.

See also #10156 and #11961.
