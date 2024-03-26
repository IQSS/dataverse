## Simplified SMTP configuration

With this release, we deprecate the usage of `asadmin create-javamail-resource` to configure Dataverse to send mail using your SMTP server and provide a simplified, standard alternative using JVM options or MicroProfile Config.

At this point, no action is required if you want to keep your current configuration.
Warnings will show in your server logs to inform and remind you about the deprecation.
A future major release of Dataverse may remove this way of configuration.

Please do take the opportunity to update your SMTP configuration. Details can be found in section of the Installation Guide starting with the [dataverse.mail.system-email](https://guides.dataverse.org/en/6.2/installation/config.html#dataverse-mail-system-email) section of the Installation Guide.

Once reconfiguration is complete, you should remove legacy, unused config. First, run `asadmin delete-javamail-resource mail/notifyMailSession` as described in the [6.1 guides](https://guides.dataverse.org/en/6.1/installation/installation-main.html#mail-host-configuration-authentication). Then run `curl -X DELETE http://localhost:8080/api/admin/settings/:SystemEmail` as this database setting has been replace with `dataverse.mail.system-email` as described below.

Please note: as there have been problems with email delivered to SPAM folders when the "From" within mail envelope and the mail session configuration didn't match (#4210), as of this version the sole source for the "From" address is the setting `dataverse.mail.system-email` once you migrate to the new way of configuration. 

List of options added:
- dataverse.mail.system-email
- dataverse.mail.mta.host
- dataverse.mail.mta.port
- dataverse.mail.mta.ssl.enable
- dataverse.mail.mta.auth
- dataverse.mail.mta.user
- dataverse.mail.mta.password
- dataverse.mail.mta.allow-utf8-addresses
- Plus many more for advanced usage and special provider requirements. See [configuration guide for a full list](https://guides.dataverse.org/en/6.2/installation/config.html#dataverse-mail-mta).