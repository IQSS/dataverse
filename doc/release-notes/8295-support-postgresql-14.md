This release upgrades the bundled PostgreSQL JDBC driver to support major version 14.

Note that the newer PostgreSQL driver required a Flyway version bump, which entails positive and negative consequences:

- The newer version of Flyway supports PostgreSQL 14 and includes a number of security fixes.
- As of version 8.0 the Flyway Community Edition dropped support for PostgreSQL 9.6 and older.

Upgrade instructions may be found under “PostgreSQL Update” in the 5.10 release notes: https://github.com/IQSS/dataverse/releases/tag/v5.10
