## Upgrade to AWS SDK v2 (for S3), v1 EOL in December 2025

To support S3 storage, Dataverse uses the AWS SDK. We have upgraded to v2 of this SDK because v1 reaches End Of Life (EOL) in [December 2025](https://aws.amazon.com/fr/blogs/developer/announcing-end-of-support-for-aws-sdk-for-java-v1-x-on-december-31-2025/).

As part of the upgrade, the payload-signing setting for S3 stores (`dataverse.files.<id>.payload-signing`) has been removed because it is no longer necessary. With the updated SDK, a payload signature will automatically be sent when required (and not sent when not required).

Dataverse developers should note that LocalStack is used to test S3 and older versions appear to be incompatible. The development environment has been upgraded to LocalStack v2.3.2 to v4.2.0, which seems to work fine.

See also #11073 and #11360.

### Settings Removed

- `dataverse.files.<id>.payload-signing`
