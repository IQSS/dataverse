## When harvesting, Dataverse can now use the identifier from the OAI-PMH record header as the persistent id for the harvested dataset.

This will allow harvesting from sources that do not include a persistent id in their oai_dc metadata records, but use valid dois or handles as the OAI-PMH record header identifiers. 

It is also possible to optionally configure a harvesting client to use this OAI-PMH identifier as the **preferred** choice for the persistent id. See the [Harvesting Clients API](https://guides.dataverse.org/en/6.5/api/native-api.html#create-a-harvesting-client) section of the Guides, #11049 and #10982 for more information.