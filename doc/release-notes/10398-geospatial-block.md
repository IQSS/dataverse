## New Expanded Geospatial Metadata Block

This release introduces a major enhancement to geospatial data support with the addition of an expanded Geospatial Metadata Block, designed to improve how researchers describe, share, and discover geospatial datasets.

This new expanded metadata block aligns with the international ISO 19115 standard for describing geographic data.

- Includes 22 new metadata fields to provide more detailed and standardized descriptions of geospatial data (e.g. vector, raster, mixed or multi-format geospatial collections)
- Incorporates and extends current metadata elements (Geographic Coverage and Geographic Bounding Box)
- Ensures backward compatibility while significantly improving metadata description capabilities.

See [the guides](https://dataverse-guide--11507.org.readthedocs.build/en/11507/user/appendix.html#supported-metadata), #10398, and #11507.

## Upgrade Instructions

### Update geospatial metadata block in existing installation (PR #11507)

.. code-block:: javascript
  
  curl http://localhost:8080/api/admin/datasetfield/load -H "Content-type: text/tab-separated-values" -X POST --upload-file geospatial.tsv
  curl "http://localhost:8080/api/admin/index/solr/schema" > new.xml
  ./dataverse/conf/solr/update-fields.sh /usr/local/solr/solr-9.8.0/server/solr/collection1/conf/schema.xml new.xml
  curl "http://localhost:8983/solr/admin/cores?action=RELOAD&core=collection1"
