# Broader MicroProfile Config Support for Developers

As of this release, many [JVM options](https://guides.dataverse.org/en/latest/installation/config.html#jvm-options)
can be set using any [MicroProfile Config Source](https://docs.payara.fish/community/docs/Technical%20Documentation/MicroProfile/Config/Overview.html#config-sources).

Currently this change is only relevant to developers but as settings are migrated to the new "lookup" pattern documented in the [Consuming Configuration](https://guides.dataverse.org/en/latest/developers/configuration.html) section of the Developer Guide, anyone installing the Dataverse software will have much greater flexibility when configuring those settings, especially within containers. These changes will be announced in future releases.

Please note that an upgrade to Payara 5.2021.8 or higher is required to make use of this. Payara 5.2021.5 threw exceptions, as explained in PR #8823.

Some options have been renamed for better consistency. Docs have been changed and enhanced accordingly.
**Important** to know: any old settings remain to work as-is, but you'll find deprecation hints in the logs.

Future enhancements to this may include:
- Validation of JVM options on start
- Interoperability with database settings
- Single file configurability for your installation (sth. like /etc/dataverse.conf) and hot-reload

**Persistent Identifier Configuration**

- Settings for DOI provider username and password have been splitted and scoped for EZID and DataCite in advance for
  future additions of other providers or multi-provider scenarios.
- The documentation has been enhanced with many explanations and details about the settings.
- The API settings for DataCite have been renamed to be more comprehensible and inline with DataCite documentation.
