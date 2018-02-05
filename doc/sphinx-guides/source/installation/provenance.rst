Provenance
==========

.. contents:: |toctitle|
  :local:
  
Introduction
------------

Provenance is metadata that describes the history of a digital object: where it came from, how it came to be in its present state, who or what acted upon it, etc. Support for provenance within Dataverse is built on top of the "prov-cpl" project at https://github.com/ProvTools/prov-cpl

Installation
------------

To install the provenance system, you will need to install an RPM, create a PostgreSQL database, and install and configure a REST service.

The following instructions assume you are installing on RHEL/CentOS.

Install Provenance RPM
~~~~~~~~~~~~~~~~~~~~~~

Download this :download:`libcpl RPM <../_static/installation/files/home/rpmbuild/rpmbuild/RPMS/x86_64/libcpl-3.0-1.x86_64.rpm>` and install it with:

``sudo yum install libcpl-3*.x86_64.rpm``

Create PostgreSQL Database
~~~~~~~~~~~~~~~~~~~~~~~~~~

TODO: Document this. Use the :download:`Ubuntu script <../_static/developers/prov/install/vagrant.sh>` as a starting point.

Install and Configure REST Service
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

TODO: Document this. Use the :download:`Ubuntu script <../_static/developers/prov/install/vagrant.sh>` as a starting point.

Configure Dataverse to Use the Provenance Service URL
-----------------------------------------------------

Use the curl command below, substituting URL on which you installed the provenance service.

``curl -X PUT -d 'http://localhost:5000' http://localhost:8080/api/admin/settings/:ProvServiceUrl``
