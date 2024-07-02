NOTE that this release note supercedes the 10464-add-name-harvesting-client-facet.md note from the PR 10464.

An option has been added to index the name of the Harvesting Client as the "Metadata Source" of harvested datasets and files; if enabled, the Metadata Source facet will be showing separate entries for the content harvested from different sources, instead of the current, default behavior where there is one "Harvested" facet for all such content.


TODO: for the v6.3 release note:
If you choose to enable the extended "Metadata Souce" facet for harvested content, set the optional feature flage (jvm option) `dataverse.feature.index-harvested-metadata-source=true` before reindexing.

[Please note that the upgrade instruction in 6.3 will contain a suggestion to run full reindex, as part of the Solr upgrade, so the sentence above will need to be added to that section]

