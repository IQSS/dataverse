Developers can now test S3 locally by using the Dockerized development environment, which now includes both LocalStack and MinIO. API (end to end) tests are in S3AccessIT.

In addition, a new integration test class (not an API test, the new Testcontainers-based test launched with `mvn verify`) has been added at S3AccessIOLocalstackIT. It uses Testcontainers to spin up Localstack for S3 testing and does not require Dataverse to be running.
