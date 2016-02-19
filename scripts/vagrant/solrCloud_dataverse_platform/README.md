Dataverse + Solr multi-server platform
===============================================

This test environment establishes the following VMs on the **192.168.30.x** local subnet.

+ *solrCloud1 (192.168.30.11\[:2181\]\[[:8983](http://192.168.30.11:8983/solr)\])*
+ *solrCloud2 (192.168.30.12\[:2181\]\[[:8983](http://192.168.30.12:8983/solr)\])*
+ *solrCloud3 (192.168.30.13\[:2181\]\[[:8983](http://192.168.30.13:8983/solr)\])*
+ *dataverse ([192.168.30.20:8080](http://192.168.30.20:8080))*

The contents of this directory contain documentation and platform configuration management files 
(e.g. Vagrantfile) that describe/implement a test environment for integrating Dataverse with a 
trio of remote servers providing a solrCloud service. 

Each solrCloud server runs solr as a service (see [../solrService_dataverse_platform/README.md](../solrService_dataverse_platform/README.md)) 
Each solrCloud server is also a member of a common zookeeper ensemble.

This environments configuration management uses an installation script packaged with Solr versions 
5.0.0 and newer to install solr as a system service.

This configuration is appropriate for use with all Solr version 5.x and newer packages.

NOTE: Solr 5+ no longer supports a default core/collection. This means that all api URI's must 
reference the collection/core name (e.g. http://myhost:8983/solr/collection1/...)!
