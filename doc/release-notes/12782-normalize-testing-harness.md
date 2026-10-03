## Updates for Developers

The "Container Integration Tests Workflow" and "Dataverse JSF Frontend Tests Workflow" GitHub Actions have been combined into a single workflow called "Containerized Tests for Dataverse". It builds the containers once and then runs the API tests and the JSF frontend tests in parallel, with one job per browser (Chromium, Firefox and WebKit). JSF test results now arrive in about 27 minutes instead of about 50.

To see which API tests are failing on your pull request, look at the "Integration Tests" job of this workflow. See [the guides](https://guides.dataverse.org/en/latest/developers/testing.html#continuous-integration) and #12782.
