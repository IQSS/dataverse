### Initial Support for Dataset Types

Out of the box, all datasets have the type "dataset" but superusers can add additional types. At this time the type can only be set at creation time via API. The types "dataset", "software", and "workflow" will be sent to DataCite when the dataset is published.

For details see <https://dataverse-guide--10694.org.readthedocs.build/en/10694/user/dataset-management.html#dataset-types> and #10517. Please note that this feature is highly experimental and is expected to evolve.

Upgrade instructions
--------------------

Update your Solr schema.xml file to pick up the "datasetType" additions and do a full reindex.
