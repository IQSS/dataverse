Solr 9.4.1 is now the version recommended in our installation guides and used with automated testing. There is a known security issue in the previously recommended version 9.3.0: https://nvd.nist.gov/vuln/detail/CVE-2023-36478. While the risk of an exploit should not be significant unless the Solr instance is accessible from the outside networks (which we have always recommended against), existing Dataverse installations should consider upgrading.

For the upgrade instructions section:

[note that 6.3 will contain other solr-related changes, so the instructions may need to contain information merged from multiple release notes!]

If you are upgrading Solr:
 - Install solr-9.4.1 following the instructions from the Installation guide.
 - Run a full reindex to populate the search catalog.
 - Note that it may be possible to skip the reindexing step by simply moving the existing `.../server/solr/collection1/` under the new `solr-9.4.1` installation directory. This however has not been thoroughly tested and is not officially supported.




