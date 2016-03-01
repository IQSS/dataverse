Dataverse + Solr single-server-service platform
===============================================

The contents of this directory contain documentation and platform configuration management files 
(e.g. Vagrantfile) that describe/implement a test environment for integrating Dataverse with a 
remote server running a solr core as a system service. 

This test environment establishes the following VMs on the **192.168.20.x** local subnet.

+ *solr ([192.168.20.10:8983](http://192.168.20.10:8983/solr))*<BR>
  <sup>with ENABLE_TLS_ON_SOLR=1 (https://192.168.20.10\[[:8983](https://192.168.20.10:8983/solr)\])</sup>
+ *dataverse ([192.168.20.20:8080](http://192.168.20.20:8080))*

  <BR>
TLS/SSL
-------
To enable Transport Layer Security from the solrCore node use: 
> ENABLE_TLS_ON_SOLR=1 vagrant up

<BR>
Platform Notes
--------------
This environments configuration management uses an installation script packaged with Solr versions 
5.0.0 and newer to install solr as a system service.

NOTE: Solr 5+ no longer supports a default core/collection. This means that all api URI's must 
reference the collection/core name (e.g. http://myhost:8983/solr/collection1/...)!

