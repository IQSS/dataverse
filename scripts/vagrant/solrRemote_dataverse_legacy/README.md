Dataverse + Solr multi-server platform
===============================================

This test environment establishes the following VMs on the **192.168.10.x** local subnet.

+ *solr ([192.168.10.10:8983](http://192.168.10.10:8983/solr))*
+ *dataverse ([192.168.10.20:8080](http://192.168.10.20:8080))*

The contents of this directory contain documentation and platform configuration management files 
(e.g. Vagrantfile) that describe/implement a test environment for integrating Dataverse with a 
remote Solr server using a legacy method of installing Solr. This legacy method uses an 'example' 
core pre-configured in the solr version 4.x installation packages.

This configuration is appropriate for use with all Solr version 4.x packages. While these utilites
may work with newer versions of Solr, it is far preferable to use the solrService_dataverse 
environment with solr 5.x and newer.


