Alternative Title is made repeatable. 
- One will need to update database with updated citation block.
`curl http://localhost:8080/api/admin/datasetfield/load -H "Content-type: text/tab-separated-values" -X POST --upload-file scripts/api/data/metadatablocks/citation.tsv`
- One will also need to update solr schema:
Change in "alternativeTitle" field  multiValued="true" in `/usr/local/solr/solr-8.11.1/server/solr/collection1/conf/schema.xml` 
Reload solr schema: `curl "http://localhost:8983/solr/admin/cores?action=RELOAD&core=collection1"`

Since Alternative Title is repeatable now, old json apis would not be compatable with a new version since value of alternative title has changed from simple string to an array.
For example, instead "value": "Alternative Title", the value canbe "value": ["Alternative Title1", "Alternative Title2"]  
