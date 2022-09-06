# Broader MicroProfile Config Support for Developers

As of this release, many [JVM options](https://guides.dataverse.org/en/latest/installation/config.html#jvm-options)
can be set using any [MicroProfile Config Source](https://docs.payara.fish/community/docs/Technical%20Documentation/MicroProfile/Config/Overview.html#config-sources).

Currently this change is only relevant to developers but as settings are migrated to the new "lookup" pattern documented in the [Consuming Configuration](https://guides.dataverse.org/en/latest/developers/configuration.html) section of the Developer Guide, anyone installing the Dataverse software will have much greater flexibility when configuring those settings, especially within containers. These changes will be announced in future releases.
