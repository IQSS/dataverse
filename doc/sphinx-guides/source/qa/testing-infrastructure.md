# Infrastructure for Testing

```{contents} Contents:
:local: 
:depth: 3
```

## Dataverse Internal

To build and test a PR, we use a build named `IQSS_Dataverse_Internal` on <https://jenkins.dataverse.org>, which deploys the .war file to an AWS instance named <https://dataverse-internal.iq.harvard.edu>.

## Guides Server

There is also a guides build project named `guides.dataverse.org`. Any test builds of guides are deployed to a named directory on guides.dataverse.org and can be found and tested by going to the existing guides, removing the part of the URL that contains the version, and browsing the resulting directory listing for the latest change. 

Note that changes to guides can also be previewed on Read the Docs. In the pull request, look for a link like <https://dataverse-guide--10103.org.readthedocs.build/en/10103/qa/index.html>. This Read the Docs preview is also mentioned under also {doc}`/developers/documentation`.

## Other Servers

We can spin up additional AWS EC2 instances as needed. See {doc}`/developers/deployment` in the Developer Guide for the scripts we use.
