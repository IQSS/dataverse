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

Install ``dataverse-provenance`` RPM
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Download this :download:`dataverse-provenance RPM <../_static/installation/files/home/rpmbuild/rpmbuild/RPMS/x86_64/dataverse-provenance-0.1-1.x86_64.rpm>` and install it with:

``sudo yum install dataverse-provenance-0*.rpm``

Create ``cplservice`` Linux User
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Our goal is to run a REST service on a high (unprivileged) port, so we create a Linux user that will run the process. Below we are calling the user ``cplservice`` but you can use whatever name you want.

``sudo useradd cplservice``

TODO: Consider creating a user in the RPM. See "Building the Provenance RPM" in the :doc:`/developers/provenance` section of the Developer Guide.

Configure Database and ODBC
~~~~~~~~~~~~~~~~~~~~~~~~~~~

TODO: Change this to the ``postgresql-setup-conf.sql`` version first used in this commit: https://github.com/IQSS/dataverse/commit/ea3fa5cace275c39faf2e7b8cf20299aedebf94b

``sudo psql -U postgres postgres < /prov-cpl/scripts/postgresql-setup-default.sql``

TODO: Document this. Use the Vagrant script in the commit above as a starting point.

Start Provenance Service
~~~~~~~~~~~~~~~~~~~~~~~~

Before you can start the provenance REST service, you must install Flask:

``sudo yum install python-flask``

First, as root, become the ``cplservice`` user.

``su - cplservice``

Then, as the ``cplservice user``, start the REST service:

``LD_LIBRARY_PATH=/usr/local/lib python /usr/local/dataverse-provenance/cpl-rest.py --host=0.0.0.0 &``

TODO: It would be nice to have an init script instead.

Configure Dataverse to Use the Provenance Service URL
-----------------------------------------------------

Use the curl command below, substituting URL on which you installed the provenance service.

``curl -X PUT -d 'http://localhost:5000' http://localhost:8080/api/admin/settings/:ProvServiceUrl``
