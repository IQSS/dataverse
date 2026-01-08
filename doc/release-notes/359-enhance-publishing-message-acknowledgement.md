## Publishing Enhancement ##

Before a Dataset can be published the user must acknowledge acceptance of the disclaimer if it is required.

The setting "PublishDatasetDisclaimer", when set, will prevent a draft dataset from being published without the user acknowledging the disclaimer.
The approved disclaimer text is `"By publishing this dataset, I fully accept all legal responsibility for ensuring that the deposited content is: anonymized, free of copyright violations, and contains data that is computationally reusable. I understand and agree that any violation of these conditions may result in the immediate removal of the dataset by the repository without prior notice."`

To enable/disable the acknowledgement requirement an Admin can set/delete the setting using the following APIs:

`curl -X PUT -d "By publishing this dataset, I fully accept all legal responsibility for ensuring that the deposited content is: anonymized, free of copyright violations, and contains data that is computationally reusable. I understand and agree that any violation of these conditions may result in the immediate removal of the dataset by the repository without prior notice." http://localhost:8080/api/admin/settings/:PublishDatasetDisclaimer` 

`curl -X DELETE http://localhost:8080/api/admin/settings/:PublishDatasetDisclaimer`

The UI will prevent the user from publishing a Dataset unless the disclaimer is acknowledged.

The APIs will continue to publish without the acknowledgement for now.
