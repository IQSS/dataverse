## Updates for Developers

The "Container Integration Tests Workflow" and "Dataverse JSF Frontend Tests Workflow" GitHub Actions have been combined into a single workflow called "Containerized Tests for Dataverse". It builds the containers once and then runs one job per browser (Chromium, Firefox and WebKit). Each job runs the API tests and that browser's JSF frontend tests at the same time, against the same Dataverse installation. JSF test results now arrive in about 27 minutes instead of about 50.

To see which API tests are failing on your pull request, look at any of the "Integration + JSF Tests" jobs of this workflow (each runs the full set). See [the guides](https://guides.dataverse.org/en/latest/developers/testing.html#continuous-integration) and #12782.

The container images each run was tested against are now kept for 7 days, so you can start the exact same build on your machine to investigate a failure. See [the guides](https://guides.dataverse.org/en/latest/developers/testing.html#reproducing-a-ci-failure-locally).
