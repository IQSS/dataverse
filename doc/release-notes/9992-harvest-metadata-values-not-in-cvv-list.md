The API endpoint `api/harvest/clients/{harvestingClientNickname}` has been extended to include the following fields:

- `allowHarvestingMissingCVV`: enable/disable allowing datasets to be harvested with Controlled Vocabulary Values that existed in the originating Dataverse Project but are not in the harvesting Dataverse Project. Default is false.
Note: This setting is only available to the API and not currently accessible/settable via the UI