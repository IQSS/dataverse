## Notes for Dataverse Administrators 

Up until this release our installation guide "strongly recommended" to install PostgreSQL v. 9.6. While that version is known to be very stable, it is nearing its end-of-life (in Nov. 2021). Dataverse Software has now been tested with versions up to 13. If you decide to upgrade PostgreSQL, the tested and recommended way of doing that is as follows:

- Export your current database with ``pg_dumpall``;
- Install the new version of PostgreSQL; (make sure it's running on the same port, etc. so that no changes are needed in the Payara configuration)
- Re-import the database with ``psql``, as the postgres user.

Consult the PostgreSQL upgrade documentation for more information, for example https://www.postgresql.org/docs/13/upgrading.html#UPGRADING-VIA-PGDUMPALL. 
