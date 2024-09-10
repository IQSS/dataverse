### Enhanced DataCite Metadata

A new field has been added to the citation metadatablock to allow entry of the "Relation Type" between a "Related Publication" and a dataset. The Relation Type is currently limited to the most common 6 values recommended by DataCite: isCitedBy, Cites, IsSupplementTo, IsSupplementedBy, IsReferencedBy, and References. 

Additional metadata, including metadata about Related Publications is now being sent to DataCite when DOIs are registered and published and is available in the DataCite XML export. For existing datasets where no "Relation Type" has been specified, "IsSupplementTo" is assumed. The additions are in rough alignment with the OpenAire xml export, but there are some minor differences in addition to the Relation Type addition, including an update to the DataCite 4.5 schema. 

For details see https://github.com/IQSS/dataverse/pull/10632 and https://github.com/IQSS/dataverse/pull/10615 and the design document referenced there.

Upgrade instructions
--------------------

The solr schema has to be updated via the normal mechanism to add the new "relationType" field.

The citation metadatablock has to be reinstalled using the standard instructions.

With these two changes, the "Relation Type" fields will be available and creation/publication of datasets will result in the expanded XML being sent to DataCite.

To update existing datasets (and files using DataCite DOIs):

Exports can be updated by running curl http://localhost:8080/api/admin/metadata/reExportAll

Entries at DataCite for published datasets can be updated by a superuser on a per-dataset basis using 

`curl -X POST -H 'X-Dataverse-key:<key>' http://localhost:8080/api/datasets/<id>/modifyRegistrationMetadata`

(Installations can also try the newly documented 

`curl  -X POST -H 'X-Dataverse-key:<key>' http://localhost:8080/api/datasets/modifyRegistrationPIDMetadataAll` 

This will loop through all published datasets (and released files with PIDs). Any PIDs for which the update fails can be found using 

`grep 'Failure for id' server.log` 

Failures may occur if PIDs were never registered, or if they were never made findable. Any such cases can be fixed manually in DataCite Fabrica or using the [Reserve a PID](https://guides.dataverse.org/en/latest/api/native-api.html#reserve-a-pid) API call and the newly documented `/api/datasets/<id>/modifyRegistration` call respectively. See https://guides.dataverse.org/en/latest/admin/dataverses-datasets.html#send-dataset-metadata-to-pid-provider. Please reach out with any questions.)
