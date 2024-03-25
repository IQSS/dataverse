## New way to configure mail transfer agent

With this release, we deprecate the usage of `asadmin create-javamail-resource` to configure your MTA.
Instead, we provide the ability to configure your SMTP mail host using JVM options only, with the flexibility of MicroProfile Config.

At this point, no action is required if you want to keep your current configuration.
Warnings will show in your server logs to inform and remind you about the deprecation.
A future major release of Dataverse may remove this way of configuration.

For more details on how to configure the connection to your mail provider, please find updated details within the Installation Guide's main installation and configuration section.

Please note: as there have been problems with email delivered to SPAM folders when the "From" within mail envelope and the mail session configuration didn't match (#4210), as of this version the sole source for the "From" address is the setting `dataverse.mail.system-email` once you migrate to the new way of configuration. 