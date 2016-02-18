Dataverse + Solr multi-server platform
===============================================

This test environment establishes the following VMs on the **192.168.20.x** local subnet.

+ *solr ([192.168.20.10:8983](http://192.168.20.10:8983/solr))*
+ *dataverse ([192.168.20.20:8080](http://192.168.20.20:8080))*

The contents of this directory contain documentation and platform configuration management files 
(e.g. Vagrantfile) that describe/implement a test environment for integrating Dataverse with a 
remote server running a Solr core as a service. This environments configuration management uses an
installation script packaged Solr versions 5+.

This configuration is appropriate for use with all Solr version 5.x and newer packages.

NOTE: Solr 5+ no longer supports a default core/collection. This means that all api URI's must 
reference the collection/core name (e.g. http://myhost:8983/solr/collection1/...)!
