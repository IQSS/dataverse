## Easier Configuration of Database Connections

Dataverse now being able to use up-to-date Java technologies, transforms
the way how to configure the connection to your PostgreSQL database.

In the past, the configuration of the connection has been quite static
and not very easy to update. This has been an issue especially for cloud
and container usage.

Using MicroProfile Config API, you can much more easily deposit configuration
details. For an overview of supported options, please see the
[installation guide](https://guides.dataverse.org/en/5.2/installation/config.html#jvm-options).

### Upgrading from earlier releases
If you are running a classic, non-container-based installation, you'll have
to set a few JVM options:

```
asadmin create-system-properties "dataverse.db.user=${DB_USER}"
asadmin create-system-properties "dataverse.db.host=${DB_HOST}"
asadmin create-system-properties "dataverse.db.port=${DB_PORT}"
asadmin create-system-properties "dataverse.db.name=${DB_NAME}"

echo "AS_ADMIN_ALIASPASSWORD=${DB_PASS}" > /tmp/password.txt
asadmin create-password-alias --passwordfile /tmp/password.txt dataverse.db.password
rm /tmp/password.txt
```

You are safe to delete the old alias and DB pool:
```
asadmin delete-jdbc-connection-pool dvnDBpool
asadmin delete-password-alias db_password_alias
```