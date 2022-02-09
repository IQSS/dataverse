## Notes for Dataverse Installation Administrators

### PostgreSQL Version 10+ Required

If you are still using PostgreSQL 9.X, now is the time to upgrade. PostgreSQL is now EOL (no longer supported, as of January 2022), and the Flyway library in the next release of the Dataverse Software will no longer work with versions prior to 10. 

The Dataverse Software has been tested with PostgreSQL versions up to 13. The current stable version 13.5 is recommended. If that's not an option for reasons specific to your installation (for example, if PostgreSQL 13.5 is not available for the OS distribution you are using), any 10+ version should work.

See the upgrade section for more information. 


### PostgreSQL Upgrade (for the "Upgrade Instructions" section)

The tested and recommended way of upgrading an existing database is as follows:

- Export your current database with ``pg_dumpall``;
- Install the new version of PostgreSQL; (make sure it's running on the same port, so that no changes are needed in the Payara configuration)
- Re-import the database with ``psql``, as the user ``postgres``.

It is strongly recommended to use the versions of the ``pg_dumpall`` and ``psql`` from the old and new versions of PostgreSQL, respectively. For example, the commands below were used to migrate a database running under PostgreSQL 9.6 to 13.5. Adjust the versions and the path names to match your environment. 

Back up/export:

``/usr/pgsql-9.6/bin/pg_dumpall -U postgres > /tmp/backup.sql``

Restore/import:

``/usr/pgsql-13/bin/psql -U postgres -f /tmp/backup.sql``

When upgrading the production database here at Harvard IQSS we were able to go from version 9.6 all the way to 13.3 without any issues.

You may want to try these backup and restore steps on a test server, to get an accurate estimate of how much downtime to expect with the final production upgrade. That of course will depend on the size of your database. 

Consult the PostgreSQL upgrade documentation for more information, for example <https://www.postgresql.org/docs/13/upgrading.html#UPGRADING-VIA-PGDUMPALL>.



