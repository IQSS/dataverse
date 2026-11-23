## Updates for Developers

The Java JSF frontend test suite has moved from [gdcc/dataverse-jsf-tests](https://github.com/gdcc/dataverse-jsf-tests) into this repository, at `modules/dataverse-jsf-tests`, with its full git history. A pull request can now change the JSF frontend and its tests together, and the "Dataverse Tests" workflow runs the suite from that directory instead of checking out a separate repository.

The suite is a standalone Maven project and is not part of the main build. To run it, start a Dataverse instance and run `mvn -f modules/dataverse-jsf-tests verify` from the root of the repo. See [the guides](https://guides.dataverse.org/en/latest/developers/testing.html#browser-based-testing-of-jsf) and #12797.
