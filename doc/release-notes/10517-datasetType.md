### Initial Support for Dataset Types (Dataset, Software, Workflow)

Datasets now have types. By default the dataset type will be "dataset", but out of the box datasets can have a type of "software" or "workflow" as well. For more details see <https://dataverse-guide--10694.org.readthedocs.build/en/10694/user/dataset-management.html#dataset-types> and #10517. Please note that this feature is highly experimental and is expected to evolve.

Upgrade instructions
--------------------

Add the following line to your Solr schema.xml file and do a full reindex:

```
<field name="datasetType" type="string" stored="true" indexed="true" multiValued="false"/>
```
