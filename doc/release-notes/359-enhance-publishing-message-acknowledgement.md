## Publishing Enhancement ##

Before a Dataset can be published the user must acknowledge acceptance of the legal disclaimer if it is required.

The setting "DatasetPublishLegalDisclaimerAcknowledgementRequired", when set to `true`, will prevent a draft dataset from being published without the user acknowledging the legal disclaimer.

APIs: If the setting is set to true and the dataset version is in draft the GET Dataset and GET Dataset Version APIs will include the legal disclaimer text in the Json response. The publish Dataset API will require a query parameter `legalDisclaimerAcknowledged=true` to publish this Dataset. 

The UI will prevent the user from publishing a Dataset unless the legal disclaimer is acknowledged.

