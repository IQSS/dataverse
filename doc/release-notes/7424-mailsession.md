## New way to configure mail transfer agent

With this release, we deprecate the usage of `asadmin create-javamail-resource` to configure your MTA.
Instead, we provide the ability to configure your SMTP mail host using JVM options with the flexibility of MicroProfile Config.

At this point, no action is required if you want to keep your current configuration.
Warnings will show in your server logs to inform and remind you about the deprecation.
A future major release of Dataverse may remove this way of configuration.
