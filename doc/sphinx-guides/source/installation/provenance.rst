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

Create ``cplservice`` User
~~~~~~~~~~~~~~~~~~~~~~~~~~

Our goal is to run a REST service on a high (unprivileged) port, so we create a Linux user that will run the process. Below we are calling the user ``cplservice`` but you can use whatever name you want.

``sudo useradd cplservice``

TODO: Consider creating a user in an RPM.

Install Python Bindings for CPL
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

First, install dependencies for building the Python bindings for CPL:

``sudo yum install redhat-lsb-core swig``

Then, become the ``cplservice`` user and build the Python bindings for CPL:

.. code-block:: bash

    [root@standalone ~]# su - cplservice
    [cplservice@standalone ~]$ git clone https://github.com/ProvTools/prov-cpl.git
    [cplservice@standalone ~]$ cd prov-cpl/bindings/python
    [cplservice@standalone python]$ make release
      SWIG    bindings/python/CPLDirect/cpl.i
    ../../../include/cplxx.h:356: Error: Syntax error in input(1).
    make[2]: *** [build/release/CPLDirect.py] Error 1
    make[1]: *** [all] Error 1
    make: *** [release] Error 2
    [cplservice@standalone python]$
    [cplservice@standalone python]$ git log --oneline | head -1
    b944eb6 addition to docs

FIXME: ``make release`` as described above works on Ubuntu (see the :download:`script <../_static/developers/prov/install/vagrant.sh>` used in the Ubuntu Vagrant environment developers use) but not CentOS. The error above seems to point to https://github.com/ProvTools/prov-cpl/blob/b944eb66137b1cd8e69bb01ab0169006aa88e214/include/cplxx.h#L356

Create PostgreSQL Database
~~~~~~~~~~~~~~~~~~~~~~~~~~

``sudo psql -U postgres postgres < /prov-cpl/scripts/postgresql-setup-default.sql``

TODO: Document this. Use the :download:`Ubuntu script <../_static/developers/prov/install/vagrant.sh>` as a starting point.

Start Provenance Service
~~~~~~~~~~~~~~~~~~~~~~~~

TODO: Document this. Use the :download:`Ubuntu script <../_static/developers/prov/install/vagrant.sh>` as a starting point.

Configure Dataverse to Use the Provenance Service URL
-----------------------------------------------------

Use the curl command below, substituting URL on which you installed the provenance service.

``curl -X PUT -d 'http://localhost:5000' http://localhost:8080/api/admin/settings/:ProvServiceUrl``
