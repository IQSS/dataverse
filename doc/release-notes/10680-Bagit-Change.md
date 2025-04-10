### Archival Bag Configuration Change

Archival Bags now exclusively use the JVM option `dataverse.bagit.sourceorg.name` in generating the bag.info file's "Internal-Sender-Identifier" rather than pulling the value from a deprecated `bagit.SourceOrganization` entry in Bundle.properties (appending " Catalog" in both cases). Sites using archival bags would not see a change if these settings were already using the same value. See #10680 and #11416.

## Upgrade Instructions

If you are using archival bags, be sure that the `dataverse.bagit.sourceorg.name` JVM option is set.


