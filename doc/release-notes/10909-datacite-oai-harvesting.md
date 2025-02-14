### OAI Harvesting from DataCite

DataCite maintains an OAI server (https://oai.datacite.org/oai) that serves records for every DOI they have registered. There's been a lot of interest in the community in being able to harvest from them. This way, it will be possible to harvest metadata from institution X even if the institution X does not maintain an OAI server of their own, if they happen to register their DOIs with DataCite. One extra element of this harvesting model that makes it especially powerful and flexible is the DataCite's concept of a "dynamic OAI set": a harvester is not limited to harvesting the pre-defined set of ALL the records registered by the Institution X, but can instead harvest virtually any arbitrary subset thereof; any query that the DataCite search API understands can be used as an OAI set (!).

A few technical issues had to be resolved in the process of adding this functionality so, as of this release it is being offered as somewhat experimental. Its beta version is nevertheless already in use at IQSS with seemingly satisfactory results.

For various reasons, in order to take advantage of this feature harvesting clients must be created and edited via the `/api/harvest/clients` API. Once configured however, harvests can be run from the Harvesting Clients control panel in the UI.

Please see the entry in the ... [ harvesting guide ] ... for more information.

