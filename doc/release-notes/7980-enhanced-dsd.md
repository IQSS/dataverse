### Default Values for Database Connections Fixed

Introduced in Dataverse release 5.3 a regression might have hit you:
the announced default values for the database connection never actually worked.

With the update to Payara 5.2022.3 it was possible to introduce working
defaults. The documentation has been changed accordingly.

Together with this change, you can now enable advanced connection pool
configurations useful for debugging and monitoring. Of particular interest may be `sslmode=require`. See the docs for details.
