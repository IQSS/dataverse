# Release Highlights

### Dataset Migration API (Experimental)

Datasets can now imported/updated following the format of an OAI-ORE export (RDA-conformant Bags), allowing for not only easier migration from one Dataverse installation to another, but also for better support of import from other systems. This experimental endpoint also allows keeping the existing persistent identifier (where the authority and shoulder match those for which the software is configured) and publication dates. This endpoint also allows for the update of terms metadata (#5899).

This development was supported by the [Research Data Alliance](https://rd-alliance.org) and follows the recommendations from the [Research Data Repository Interoperability Working Group](http://dx.doi.org/10.15497/RDA00025).

### Additional Upgrade Steps

Update Solr Schema

- copy schema_dv_mdb_fields.xml and schema_dv_mdb_copies.xml to solr server, for example into /usr/local/solr/solr-8.8.1/server/solr/collection1/conf/ directory

- Restart Solr, or tell Solr to reload its configuration:

   `curl "http://localhost:8983/solr/admin/cores?action=RELOAD&core=collection1"`
