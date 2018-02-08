Provenance
==========

Provenance is metadata that describes the history of a digital object: where it came from, how it came to be in its present state, who or what acted upon it, etc. See "Data Provenance" in the :doc:`/user/dataset-management.rst` section of the User Guide for more on this feature from the end user perspective.

.. contents:: |toctitle|
  :local:
  
Introduction
------------

Support for provenance within Dataverse is built on top of the "prov-cpl" project at https://github.com/ProvTools/prov-cpl and "CPL" stands for "Core Provenance Library."

Installation
------------

Here is an overview of the steps required to install the provenance service:

- install an RPM
- create, configure and secure a PostgreSQL database
- install, configure, and secure a REST service

Each of these steps is described in detail below.

Installation of the provenance service is only supported on RHEL/CentOS. CentOS 7 was used when writing these instructions.

Install ``dataverse-provenance`` RPM
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Download this :download:`dataverse-provenance RPM <../_static/installation/files/home/rpmbuild/rpmbuild/RPMS/x86_64/dataverse-provenance-0.1-1.x86_64.rpm>` and install it with:

``sudo yum install dataverse-provenance-0*.rpm``

(If you are interested in improving this RPM, see "Building the Provenance RPM" in the :doc:`/developers/provenance` section of the Developer Guide.)

Create ``cplservice`` Linux User
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Our goal is to run a REST service on a high (unprivileged) port, so we create a Linux user that will run the process. Below we are calling the user ``cplservice`` but you can use whatever name you want. Please note, however, that the name you choose must match the ``User`` in the ``odbc.ini`` file described below. Create the user with ``useradd`` like this:

``sudo useradd cplservice``

Configure Database and ODBC
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Download :download:`odbc.ini <../_static/installation/files/etc/odbc.ini>` and place it at ``/etc/odbc.ini``. (Please note that there is a related file at ``/etc/odbcinst.ini`` provided by the ``unixODBC`` RPM that you should not need to touch.) For the purpose of this documentation, we'll be using values for ``Database``, ``User``, and ``Password`` below, but you are welcome to use different values and encouraged to use a unique password. As mentioned above, the ``User`` value must match the Linux user you intend to run the service as, such as ``cplservice``.

.. literalinclude:: ../_static/installation/files/etc/odbc.ini

Download https://raw.githubusercontent.com/ProvTools/prov-cpl/8150ee315abc21712b49da2bf4cfdbf308eef1d7/scripts/postgresql-setup-conf.sql to your ``/tmp`` directory.

You are about to run a SQL script to create a PostgreSQL database on localhost, which may be the same server on which you run the database for Dataverse. You might want to review configuration options for ``pg_hba.conf`` described in the :doc:`prerequisites` section.

Run the ``psql`` command below, changing the ``db_name``, ``user_name``, and ``user_password`` arguments to match the ones you used in ``odbc.ini`` above:

``sudo psql -U postgres postgres -v db_name=cpl -v user_name=cplservice -v user_password=\'cplcplcpl\' < /tmp/postgresql-setup-conf.sql``

Start Provenance Service
~~~~~~~~~~~~~~~~~~~~~~~~

Before you can start the provenance REST service, you must install Flask:

``sudo yum install python-flask``

In addition, you must install the ``postgresql-odbc`` package (``/usr/lib64/psqlodbcw.so`` is referenced in ``/etc/odbcinst.ini``):

``sudo yum install postgresql-odbc``

Download :download:`dataverse-provenance.service  <../_static/installation/files/etc/systemd/system/dataverse-provenance.service>` and place it at ``/etc/systemd/system/dataverse-provenance.service``. Here are the default contents of that file but see below for adjustsments you might want to make:

.. literalinclude:: ../_static/installation/files/etc/systemd/system/dataverse-provenance.service

The argument ``--port=5000`` is used by default but you are welcome to change the port the service runs on.

If you are running all Dataverse service on a single server, you can leave ``--host=127.0.0.1`` as-is. If you are running the provenance service on a separate host you must adjust this argument to ``--host=0.0.0.0`` and adjust any firewalls to only allow traffic from your Glassfish server(s) to your provenance server. The REST interface to the provenance service has no authentication so it must be secured using firewalls. Running the service over HTTPS is an exercise for the reader.

Once you are happy with the configuration of your ``dataverse-provenance.service`` file, start the service with the following command:

``sudo systemctl start dataverse-provenance.service``

Test to make sure the service is running with the following curl command:

``curl http://127.0.0.1:5000/provapi/version``

This command should return a version string such as "3.0".

Configure Dataverse to Use the Provenance Service URL
-----------------------------------------------------

Use the curl command below, substituting URL on which you installed the provenance service.

``curl -X PUT -d 'http://localhost:5000' http://localhost:8080/api/admin/settings/:ProvServiceUrl``
