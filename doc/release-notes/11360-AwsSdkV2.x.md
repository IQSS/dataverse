## Update to AWS SDK v2.x for S3
The v1 AWS SDK Dataverse used for S3 is in maintenance mode and will reach it's end of life in Dec. 2025.
With this release, Dataverse has switched to v2.x of the SDK. Older versions of localstack, e.g. v2.3.2 appear to be incompatible and users of localstack should update (v.4.2.0 was tested).
As part of the update the payload-signing setting for S3 stores has been removed. With the new toolkit a payload signature will automatically be sent when required (and not sent when not required).
 