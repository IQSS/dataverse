=======================
Development Environment
=======================

These instructions are oriented around Docker but the "classic" instructions we used for Dataverse 4 and 5 are still available at :doc:`classic-dev-env`.

.. contents:: |toctitle|
	:local:

.. _container-dev-quickstart:

Quickstart
----------

First, install Java 17, Maven, and Docker.

After cloning the `dataverse repo <https://github.com/IQSS/dataverse>`_, run this:

``mvn -Pct clean package docker:run``

After some time you should be able to log in:

- url: http://localhost:8080
- username: dataverseAdmin
- password: admin1

Detailed Steps
--------------

Install Java
~~~~~~~~~~~~

The Dataverse Software requires Java 17.

On Mac and Windows, we suggest downloading OpenJDK from https://adoptium.net (formerly `AdoptOpenJDK <https://adoptopenjdk.net>`_) or `SDKMAN <https://sdkman.io>`_.

On Linux, you are welcome to use the OpenJDK available from package managers.

Install Maven
~~~~~~~~~~~~~

Follow instructions at https://maven.apache.org

Install and Start Docker
~~~~~~~~~~~~~~~~~~~~~~~~

Follow instructions at https://www.docker.com

Be sure to start Docker.

Git Clone Repo
~~~~~~~~~~~~~~

Fork https://github.com/IQSS/dataverse and then clone your fork like this:

``git clone git@github.com:[YOUR GITHUB USERNAME]/dataverse.git``

Build and Run
~~~~~~~~~~~~~

Change into the ``dataverse`` directory you just cloned and run the following command:

``mvn -Pct clean package docker:run``

Verify 
~~~~~~

After some time you should be able to log in:

- url: http://localhost:8080
- username: dataverseAdmin
- password: admin1

Next Steps
----------

See the :doc:`/container/dev-usage` section of the Container Guide for tips on fast redeployment, viewing logs, and more.

Getting Help
------------

Please feel free to reach out at https://chat.dataverse.org or https://groups.google.com/g/dataverse-dev if you have any difficulty setting up a dev environment!
