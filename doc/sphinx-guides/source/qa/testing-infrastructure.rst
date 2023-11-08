Infrastructure for Testing
==========================

.. contents:: |toctitle|
    :local:


Dataverse Internal
-------------------
To build and test a PR, we use a build named IQSS_Dataverse_Internal on jenkins.dataverse.org, which deploys the .war file to an AWS instance named dataverse-internal.iq.harvard.edu.
Login to Jenkins requires a username and password. Check with Don Sizemore. Login to the dataverse-internal server requires a key, see Leonid. 

Guides Server
-------------
There is also a guides build project named guides.dataverse.org. Any test builds of guides are deployed to a named directory** on guides.dataverse.org and can be found and tested by going to the existing guides, removing the part of the URL that contains the version, and browsing the resulting directory listing for the latest change. 
Login to the guides server requires a key, see Don Sizemore.  
