# Test Automation

```{contents}
:depth: 3
```

The API test suite is added to and maintained by development. (See {doc}`/developers/testing` in the Developer Guide.) It is generally advisable for code contributors to add API tests when adding new functionality. The approach here is one of code coverage: exercise as much of the code base's code paths as possible, every time to catch bugs. 

This type of approach is often used to give contributing developers confidence that their code didn’t introduce any obvious, major issues and is run on each commit. Since it is a broad set of tests, it is not clear whether any specific, conceivable test is run but it does add a lot of confidence that the code base is functioning due to its reach and consistency.

## Building and Deploying a Pull Request from Jenkins to Dataverse-Internal


1. Log on to GitHub, go to projects, dataverse to see Kanban board, select a pull request to test from the QA queue. 

1. From the pull request page, click the copy icon next to the pull request branch name.

1. Log on to <https://jenkins.dataverse.org>, select the `IQSS_Dataverse_Internal` project, and configure the repository URL and branch specifier to match the ones from the pull request. For example:

    * 8372-gdcc-xoai-library has IQSS implied
        - **Repository URL:** https://github.com/IQSS/dataverse.git 
        - **Branch specifier:** */8372-gdcc-xoai-library
    * GlobalDataverseCommunityConsortium:GDCC/DC-3B
        - **Repository URL:** https://github.com/GlobalDataverseCommunityConsortium/dataverse.git 
        - **Branch specifier:** */GDCC/DC-3B. 

1. Click "Build Now" and note the build number in progress.

1. Once complete, go to <https://dataverse-internal.iq.harvard.edu> and check that the deployment succeeded, and that the homepage displays the latest build number.

1. If for some reason it didn’t deploy, check the server.log file. It may just be a caching issue so try un-deploying, deleting cache, restarting, and re-deploying on the server (`su - dataverse` then `/usr/local/payara5/bin/asadmin list-applications; /usr/local/payara5/bin/asadmin undeploy dataverse-5.11.1; /usr/local/payara5/bin/asadmin deploy /tmp/dataverse-5.11.1.war`)

1. If that didn't work, you may have run into a Flyway DB script collision error but that should be indicated by the server.log. See {doc}`/developers/sql-upgrade-scripts` in the Developer Guide.

1.	Assuming the above steps worked, and they should 99% of the time, test away! Note: be sure to `tail -F server.log` in a terminal window while you are doing any testing. This way you can spot problems that may not appear in the UI and have easier access to any stack traces for easier reporting.
