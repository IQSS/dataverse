### OAI Harvesting from DataCite

DataCite maintains an OAI server (https://oai.datacite.org/oai) that serves records for every DOI they have registered. There's been a lot of interest in the community in being able to harvest from them. This way, it will be possible to harvest metadata from institution X even if the institution X does not maintain an OAI server of their own, if they happen to register their DOIs with DataCite. One extra element of this harvesting model that makes it especially powerful and flexible is the DataCite's concept of a "dynamic OAI set": a harvester is not limited to harvesting the pre-defined set of ALL the records registered by the Institution X, but can instead harvest virtually any arbitrary subset thereof; any query that the DataCite search API understands can be used as an OAI set (!). The feature is already in use at IQSS, as a beta version patch. 

For various reasons, in order to take advantage of this feature harvesting clients must be created using the `/api/harvest/clients` API. Once configured however, harvests can be run from the Harvesting Clients control panel in the UI.

DataCite-harvesting clients must be configured with 2 new feature flags, `useListRecords` and `useOaiIdentifiersAsPids` (added in v6.5). Note that these features may be of use when harvesting from other sources, not just from DataCite.

See "Harvesting from DataCite" under https://guides.dataverse.org/en/latest/api/native-api.html#managing-harvesting-clients for more information.

