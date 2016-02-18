Dataverse + Solr multi-server platform
===============================================

The contents of this directory contain documentation and platform configuration management files 
(e.g. Vagrantfile) that describe/implement a test environment for integrating Dataverse with a 
remote server running a Solr core as a service. This environments configuration management uses an
installation script packaged Solr versions 5+.

This configuration is appropriate for use with all Solr version 5.x and newer packages.

NOTE: Solr 5+ no longer supports a default core/collection. This means that all api URI's must 
reference the collection/core name (e.g. http://myhost:8983/solr/collection1/...)!
