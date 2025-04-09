### Archival Bag Configuration Change

Archival Bags now use the JVMSetting BAGIT_SOURCE_ORG_NAME in generating the bag.info file's "Internal-Sender-Identifier" rather than pulling the value from a deprecated bagit.SourceOrganization Bundle.properties entry (appending " Catalog" in both cases). Sites using archival bags would not see a change if these settings were already using the same value.


