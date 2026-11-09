Dataverse now sends ORCID and ROR identifiers to DataCite and includes them in the DataCite XML metadata export when they are used for contacts, contributors, funders, etc.

An entry is the social sciences metadatablock for the dataCollector field will now be included in the DataCite XML list of contributors.

New FeatuerFlag:
dataverse.feature.treat-sandbox-orcids-as-pids
It is now possible to test using the ORCID sandbox and verify that ORCID identifiers found/used through Dataverse's external vocabulary scripts are correctly sent to DataCite and included in the DataCite XML export. Set `dataverse.feature.treat-sandbox-orcids-as-pids` to true to enable this feature. (This setting should not be set to true in production. The default is false.)