# Introducing broader MicroProfile Config Support

As of this release, many [JVM options](https://guides.dataverse.org/en/latest/installation/config.html#jvm-options)
support to be set using any [MicroProfile Config Source](https://docs.payara.fish/community/docs/Technical%20Documentation/MicroProfile/Config/Overview.html#config-sources).

This allows for much greater flexibility when configuring your Dataverse installation, especially when making use
of containers, setting up development environments or firing up ephemeral environments for CI etc.

Some options have been renamed for better consistency. Docs have been changed and enhanced accordingly.
**Important** to know: any old settings remain to work as-is, but you'll find deprecation hints in the logs.

Future enhancements to this may include:
- Validation of JVM options on start
- Interoperability with database settings
- Single file configurability for your installation (sth. like /etc/dataverse.conf) and hot-reload
