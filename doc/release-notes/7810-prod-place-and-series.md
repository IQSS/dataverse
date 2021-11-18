## Upgrade Instructions

X\. Reload Citation Metadata Block:

   `wget https://github.com/IQSS/dataverse/releases/download/v5.X/citation.tsv`
   `curl http://localhost:8080/api/admin/datasetfield/load -X POST --data-binary @citation.tsv -H "Content-type: text/tab-separated-values"`

X\. Update Solr schema.xml.

`/usr/local/solr/solr-8.8.1/server/solr/collection1/conf` is used in the examples below as the location of your Solr schema. Please adapt it to the correct location, if different in your installation. Use `find / -name schema.xml` if in doubt.

Xa\. Replace `schema.xml` with the base version included in this release.

```
   wget https://github.com/IQSS/dataverse/releases/download/v5.X/schema.xml
   cp schema.xml /usr/local/solr/solr-8.8.1/server/solr/collection1/conf
```

For installations that are not using any Custom Metadata Blocks, **you can skip the next step**.

Xb\. For installations with Custom Metadata Blocks

Use the script provided in the release to add the custom fields to the base `schema.xml` installed in the previous step.

```
   wget https://github.com/IQSS/dataverse/releases/download/v5.X/update-fields.sh
   chmod +x update-fields.sh
   curl "http://localhost:8080/api/admin/index/solr/schema" | ./update-fields.sh /usr/local/solr/solr-8.8.1/server/solr/collection1/conf/schema.xml
```

(Note that the curl command above calls the admin api on `localhost` to obtain the list of the custom fields. In the unlikely case that you are running the main Dataverse Application and Solr on different servers, generate the `schema.xml` on the application node, then copy it onto the Solr server)

X\. Run a ['Reindex in Place'](https://guides.dataverse.org/en/5.X/admin/solr-search-index.html#reindex-in-place) as described in the Dataverse Guides.