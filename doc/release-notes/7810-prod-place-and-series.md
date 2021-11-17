## Upgrade Instructions

X\. Reload Citation Metadata Block:

   `wget https://github.com/IQSS/dataverse/releases/download/v5.X/citation.tsv`
   `curl http://localhost:8080/api/admin/datasetfield/load -X POST --data-binary @citation.tsv -H "Content-type: text/tab-separated-values"`

X\. Update Solr schema.xml.

`/usr/local/solr/solr-8.8.1/server/solr/collection1/conf` is used in the examples below as the location of your Solr schema. Please adapt it to the correct location, if different in your installation. Use `find / -name schema.xml` if in doubt.

X\. Replace `schema.xml` with the base version included in this release.

```
   wget https://github.com/IQSS/dataverse/releases/download/v5.X/schema.xml
   cp schema.xml /usr/local/solr/solr-8.8.1/server/solr/collection1/conf
```

X\. Run a ['Reindex in Place'](https://guides.dataverse.org/en/5.X/admin/solr-search-index.html#reindex-in-place) as described in the Dataverse Guides.