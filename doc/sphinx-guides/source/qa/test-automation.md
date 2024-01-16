# Test Automation
```{contents} Contents:
:local: 
:depth: 3
```

## Introduction

Jenkins is our primary tool for knowing if our API tests are passing. (Unit tests are executed locally by developers.)

You can find our Jenkins installation at <https://jenkins.dataverse.org>.

Please note that while it has been open to the public in the past, it is currently firewalled off. We can poke a hole in the firewall for your IP address if necessary. Please get in touch. (You might also be interested in <https://github.com/IQSS/dataverse/issues/9916> which is about restoring the ability of contributors to see if their pull requests are passing API tests or not.)

## Jobs

Jenkins is organized into jobs. We'll highlight a few.

### IQSS-dataverse-develop

<https://jenkins.dataverse.org/job/IQSS-dataverse-develop>, which we will refer to as the "develop" job runs after pull requests are merged. It is crucial that this job stays green (passing) because we always want to stay in a "release ready" state. If you notice that this job is failing, make noise about it!

You can get to this job from the README at <https://github.com/IQSS/dataverse>.

### IQSS-Dataverse-Develop-PR

<https://jenkins.dataverse.org/job/IQSS-Dataverse-Develop-PR/> can be thought of as "PR jobs". It's a collection of jobs run on pull requests. Typically, you will navigate directly into the job (and it's particular build number) from a pull request. For example, from <https://github.com/IQSS/dataverse/pull/10044>, look for a check called "continuous-integration/jenkins/pr-merge". Clicking it will bring you to a particular build like <https://jenkins.dataverse.org/blue/organizations/jenkins/IQSS-Dataverse-Develop-PR/detail/PR-10044/10/pipeline> (build #10).

### guides.dataverse.org

<https://jenkins.dataverse.org/job/guides.dataverse.org/> is what we use to build guides. See {doc}`/developers/making-releases` in the Developer Guide.

### Building and Deploying a Pull Request from Jenkins to Dataverse-Internal


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



## Checking if API Tests are Passing

If API tests are failing, you should not merge the pull request.

How can you know if API tests are passing? Here are the steps, by way of example.

- From the pull request, navigate to the build. For example from <https://github.com/IQSS/dataverse/pull/10044>, look for a check called "continuous-integration/jenkins/pr-merge". Clicking it will bring you to a particular build like <https://jenkins.dataverse.org/blue/organizations/jenkins/IQSS-Dataverse-Develop-PR/detail/PR-10044/10/pipeline> (build #10).
- You are now on the new "blue" interface for Jenkins. Click the button with an arrow on the right side of the header called "go to classic" which should take you to (for example) <https://jenkins.dataverse.org/job/IQSS-Dataverse-Develop-PR/job/PR-10044/10/>.
- Click "Test Result".
- Under "All Tests", look at the duration for "edu.harvard.iq.dataverse.api". It should be ten minutes or higher. If it was only a few seconds, tests did not run.
- Assuming tests ran, if there were failures, they should appear at the top under "All Failed Tests". Inform the author of the pull request about the error.

## Diagnosing Failures

API test failures can have multiple causes. As described above, from the "Test Result" page, you might see the failure under "All Failed Tests". However, the test could have failed because of some underlying system issue.

If you have determined that the API tests have not run at all, your next step should be to click on "Console Output". For example, <https://jenkins.dataverse.org/job/IQSS-Dataverse-Develop-PR/job/PR-10109/26/console>. Click "Full log" to see the full log in the browser or navigate to <https://jenkins.dataverse.org/job/IQSS-Dataverse-Develop-PR/job/PR-10109/26/consoleText> (for example) to get a plain text version.

Go to the end of the log and then scroll up, looking for the failure. A failed Ansible task can look like this:

```
TASK [dataverse : download payara zip] *****************************************
fatal: [localhost]: FAILED! => {"changed": false, "dest": "/tmp/payara.zip", "elapsed": 10, "msg": "Request failed: <urlopen error timed out>", "url": "https://nexus.payara.fish/repository/payara-community/fish/payara/distributions/payara/6.2023.8/payara-6.2023.8.zip"}
```

In the example above, if Payara can't be downloaded, we're obviously going to have problems deploying Dataverse to it!
