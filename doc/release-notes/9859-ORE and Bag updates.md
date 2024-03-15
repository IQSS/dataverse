Dataverse's OAI_ORE Metadata Export format and archival BagIT exports 
(which include the OAI-ORE metadata export file) have been updated to include 
information about the dataset version state, e.g. RELEASED or DEACCESSIONED 
and to indicate which version of Dataverse was used to create the archival Bag.
As part of the latter, the current OAI_ORE Metadata format has been given a 1.0.0 
version designation and it is expected that any future changes to the OAI_ORE export
format will result in a version change and that tools such as DVUploader that can
recreate datasets from archival Bags will start indicating which version(s) of the 
OAI_ORE format they can read.

Dataverse installations that have been using archival Bags may wish to update any
existing archival Bags they have, e.g. by deleting existing Bags and using the Dataverse
[archival Bag export API](https://guides.dataverse.org/en/latest/installation/config.html#bagit-export-api-calls)
to generate updated versions.