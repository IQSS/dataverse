Developers can now test S3 locally by using the Dockerized development environment, which now includes both LocalStack and MinIO. See S3AccessIT which executes API (end to end) tests.

In addition, a new integration test test class (not an API test, the new kind launched with `mvn verify`) has been added at S3AccessIOLocalstackIT. It uses Testcontainers to spin up Localstack for S3 testing and does not require Dataverse to be running.
