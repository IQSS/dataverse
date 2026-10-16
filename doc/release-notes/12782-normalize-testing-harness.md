## Updates for Developers

The "Container Integration Tests Workflow" and "Dataverse JSF Frontend Tests Workflow" GitHub Actions have been combined into a single workflow called "Dataverse Tests". It builds the containers once and hands them to separate jobs for the API tests and for the JSF frontend tests in each browser (Chromium, Firefox and WebKit), each with its own fresh Dataverse installation. The API tests and the Chromium tests run at the same time; Firefox and WebKit follow in parallel once Chromium has finished. The workflow runs as part of "Maven Tests", after the unit tests. The JSF tests now use the Java version of the suite in the `java` branch of [gdcc/dataverse-jsf-tests](https://github.com/gdcc/dataverse-jsf-tests/tree/java), which runs with Maven and doesn't need Node.js.

To see which API tests are failing on your pull request, look at the "Dataverse API Tests" job of this workflow, under Maven Tests. See [the guides](https://guides.dataverse.org/en/latest/developers/testing.html#continuous-integration) and #12782.

The code coverage reported to Coveralls and SonarCloud now also includes the server-side coverage of the API and JSF tests, merged with the unit and integration tests.

The container images each run was tested against are now kept for 7 days, so you can start the exact same build on your machine to investigate a failure. See [the guides](https://guides.dataverse.org/en/latest/developers/testing.html#reproducing-a-ci-failure-locally).
