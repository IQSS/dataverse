## Easier Configuration of Database Connections

Dataverse now being able to use up-to-date Java technologies, transforms
the way how to configure the connection to your PostgreSQL database.

In the past, the configuration of the connection has been quite static
and not very easy to update. This has been an issue especially for cloud
and container usage.

Using MicroProfile Config API, you can much more easily deposit configuration
details. For an overview of supported options, please see the
[installation guide](https://guides.dataverse.org/en/5.2/installation/config.html#jvm-options).

## Update to Payara Platform 5.2020.5

In contrast to good old Glassfish 4, the Payara application server
is a maintained product, receiving both feature and - more important -
security and dependent technology updates. You should update the
application server platform on a regular basis, as it is a key component
of a running Dataverse deployment.

🚨 THIS VERSION OF DATAVERSE **REQUIRES** UPGRADING TO PAYARA 5.2020.5. 🚨

Some bugs related to the new database connection configuration via MicroProfile Config
have been fixed. You will run into troubles when not upgrading.

Instructions how to update can be found in the
[Payara documentation](https://docs.payara.fish/community/docs/5.2020.5/documentation/user-guides/upgrade-payara.html)

## Upgrading from earlier releases

ℹ️ You need to update the Payara Application Server before continuing here. See above.

1. Undeploy the previous version.
```
<payara install path>/asadmin list-applications
<payara install path>/asadmin undeploy dataverse-<version>
```

(where `<payara install path>` is where Payara 5 is installed, for example: `/usr/local/payara5`)

2. Update your database connection before updating.

Please configure your connection details, replacing all the `${DB_...}`.
(If you are using a PostgreSQL server on `localhost:5432`, you can omit `dataverse.db.host` and `dataverse.db.port`.)

```
<payara install path>/asadmin create-system-properties "dataverse.db.user=${DB_USER}"
<payara install path>/asadmin create-system-properties "dataverse.db.host=${DB_HOST}"
<payara install path>/asadmin create-system-properties "dataverse.db.port=${DB_PORT}"
<payara install path>/asadmin create-system-properties "dataverse.db.name=${DB_NAME}"

echo "AS_ADMIN_ALIASPASSWORD=${DB_PASS}" > /tmp/password.txt
<payara install path>/asadmin create-password-alias --passwordfile /tmp/password.txt dataverse.db.password
rm /tmp/password.txt
```

After switching EJB timers to the new data source, you are safe to delete the old alias and DB pool:
```
<payara install path>/asadmin set configs.config.server-config.ejb-container.ejb-timer-service.timer-datasource=java:global/jdbc/dataverse
<payara install path>/asadmin delete-jdbc-connection-pool --cascade=true dvnDbPool
<payara install path>/asadmin delete-password-alias db_password_alias
```

3. Stop payara and remove the generated directory, start.
```
service payara stop
# remove the generated directory:
rm -rf <payara install path>/payara/domains/domain1/generated
service payara start
```

3. Deploy this version.
```
<payara install path>/bin/asadmin deploy dataverse-5.2.war
```

4. Restart Payara
```
service payara stop
service payara start
```
