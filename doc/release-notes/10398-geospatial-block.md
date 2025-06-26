### Update geospatial metadata block in existing installation (PR #11507)

.. code-block:: javascript
  
  curl http://localhost:8080/api/admin/datasetfield/load -H "Content-type: text/tab-separated-values" -X POST --upload-file geospatial.tsv
  curl "http://localhost:8080/api/admin/index/solr/schema" > new.xml
  ./dataverse/conf/solr/update-fields.sh /usr/local/solr/solr-9.8.0/server/solr/collection1/conf/schema.xml new.xml
  curl "http://localhost:8983/solr/admin/cores?action=RELOAD&core=collection1"
