# Infrastructure for Testing

```{contents} Contents:
:local: 
:depth: 3
```

## Dataverse Internal

To build and test a PR, we use a job called `IQSS_Dataverse_Internal` on <https://jenkins.dataverse.org> (see {doc}`test-automation`), which deploys the .war file to an AWS instance named <https://dataverse-internal.iq.harvard.edu>.

(deploy-to-internal)=
### Building and Deploying a Pull Request from Jenkins to Dataverse-Internal

1. Go to the QA column on our [project board](https://github.com/orgs/IQSS/projects/34), and select a pull request to test.

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

1. If for some reason it didn't deploy, check the server.log file. It may just be a caching issue so try un-deploying, deleting cache, restarting, and re-deploying on the server (`su - dataverse` then `/usr/local/payara6/bin/asadmin list-applications; /usr/local/payara6/bin/asadmin undeploy dataverse-6.1; /usr/local/payara6/bin/asadmin deploy /tmp/dataverse-6.1.war`)

1. When a Jenkins job fails after a release, it might be due to the version number in the `pom.xml` file not being updated in the pull request (PR). To verify this, open the relevant GitHub issue, navigate to the PR branch, and go to `dataverse > modules > dataverse-parent > pom.xml`. Look for the version number, typically shown as `<revision>6.3</revision>`, and ensure it matches the current Dataverse build version. If it doesn't match, ask the developer to update the branch with the latest from the "develop" branch.

1. If that didn't work, you may have run into a Flyway DB script collision error but that should be indicated by the server.log. See {doc}`/developers/sql-upgrade-scripts` in the Developer Guide. In the case of a collision, ask the developer to rename the script.

1.	Assuming the above steps worked, and they should 99% of the time, test away! Note: be sure to `tail -F server.log` in a terminal window while you are doing any testing. This way you can spot problems that may not appear in the UI and have easier access to any stack traces for easier reporting.

## Guides Server

There is also a guides job called `guides.dataverse.org` (see {doc}`test-automation`). Any test builds of guides are deployed to a named directory on guides.dataverse.org and can be found and tested by going to the existing guides, removing the part of the URL that contains the version, and browsing the resulting directory listing for the latest change. 

Note that changes to guides can also be previewed on Read the Docs. In the pull request, look for a link like <https://dataverse-guide--10103.org.readthedocs.build/en/10103/qa/index.html>. This Read the Docs preview is also mentioned under also {doc}`/contributor/documentation`.

## Other Servers

We can spin up additional AWS EC2 instances as needed. See {doc}`/developers/deployment` in the Developer Guide for the scripts we use.
