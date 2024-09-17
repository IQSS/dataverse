### Enhanced DataCite Metadata, Relation Type

A new field has been added to the citation metadatablock to allow entry of the "Relation Type" between a "Related Publication" and a dataset. The Relation Type is currently limited to the most common 6 values recommended by DataCite: isCitedBy, Cites, IsSupplementTo, IsSupplementedBy, IsReferencedBy, and References. For existing datasets where no "Relation Type" has been specified, "IsSupplementTo" is assumed.

Dataverse now supports the DataCite v4.5 schema. Additional metadata, including metadata about Related Publications, and files in the dataset are now being sent to DataCite and improvements to how PIDs (ORCID, ROR, DOIs, etc.), license/terms, geospatial, and other metadata is represented have been made. The enhanced metadata will automatically be sent when datasets are created and published and is available in the DataCite XML export after publication.

The additions are in rough alignment with the OpenAIRE XML export, but there are some minor differences in addition to the Relation Type addition, including an update to the DataCite 4.5 schema. For details see https://github.com/IQSS/dataverse/pull/10632 and https://github.com/IQSS/dataverse/pull/10615 and the [design document](https://docs.google.com/document/d/1JzDo9UOIy9dVvaHvtIbOI8tFU6bWdfDfuQvWWpC0tkA/edit?usp=sharing) referenced there.

Multiple backward incompatible changes and bug fixes have been made to API calls (3 of the four of which were not documented) related to updating PID target urls and metadata at the provider service:
- [Update Target URL for a Published Dataset at the PID provider](https://guides.dataverse.org/en/latest/admin/dataverses-datasets.html#update-target-url-for-a-published-dataset-at-the-pid-provider)
- [Update Target URL for all Published Datasets at the PID provider](https://guides.dataverse.org/en/latest/admin/dataverses-datasets.html#update-target-url-for-all-published-datasets-at-the-pid-provider)
- [Update Metadata for a Published Dataset at the PID provider](https://guides.dataverse.org/en/latest/admin/dataverses-datasets.html#update-metadata-for-a-published-dataset-at-the-pid-provider)
- [Update Metadata for all Published Datasets at the PID provider](https://guides.dataverse.org/en/latest/admin/dataverses-datasets.html#update-metadata-for-all-published-datasets-at-the-pid-provider)

Upgrade instructions
--------------------

The Solr schema has to be updated via the normal mechanism to add the new "relationType" field.

The citation metadatablock has to be reinstalled using the standard instructions.

With these two changes, the "Relation Type" fields will be available and creation/publication of datasets will result in the expanded XML being sent to DataCite.

To update existing datasets (and files using DataCite DOIs):

Exports can be updated by running `curl http://localhost:8080/api/admin/metadata/reExportAll`

Entries at DataCite for published datasets can be updated by a superuser using an API call (newly documented):

`curl  -X POST -H 'X-Dataverse-key:<key>' http://localhost:8080/api/datasets/modifyRegistrationPIDMetadataAll` 

This will loop through all published datasets (and released files with PIDs). As long as the loop completes, the call will return a 200/OK response. Any PIDs for which the update fails can be found using 

`grep 'Failure for id' server.log` 

Failures may occur if PIDs were never registered, or if they were never made findable. Any such cases can be fixed manually in DataCite Fabrica or using the [Reserve a PID](https://guides.dataverse.org/en/latest/api/native-api.html#reserve-a-pid) API call and the newly documented `/api/datasets/<id>/modifyRegistration` call respectively. See https://guides.dataverse.org/en/latest/admin/dataverses-datasets.html#send-dataset-metadata-to-pid-provider. Please reach out with any questions.

PIDs can also be updated by a superuser on a per-dataset basis using 

`curl -X POST -H 'X-Dataverse-key:<key>' http://localhost:8080/api/datasets/<id>/modifyRegistrationMetadata`

