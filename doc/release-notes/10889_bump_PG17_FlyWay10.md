This release bumps both the Postgres JDBC driver and Flyway versions. This should better support Postgres version 17, and as of version 10 Flyway no longer requires a paid subscription to support older versions of Postgres.

While we don't encourage the use of older Postgres versions, this flexibility may benefit some of our long-standing installations in their upgrade paths. Postgres 13 remains the version used with automated testing.

As part of this update, the containerized development environment now uses Postgres 17 instead of 16. Worst case, developers can start with a fresh database, if necessary.

The Docker compose file used for [evaluations or demos](https://dataverse-guide--10912.org.readthedocs.build/en/10912/container/running/demo.html) has been upgraded from Postgres 13 to 17.
