Dataverse Application Image
===========================

.. contents:: |toctitle|
    :local:

Within the main repository, you may find the application image's files at ``<git root>/src/main/docker``.
This Maven module, which also build the Dataverse WAR, uses the `Maven Docker Plugin <https://dmp.fabric8.io>`_
to build and ship the image within a special Maven profile.

Introduction
++++++++++++

The application image builds by convention upon the :doc:`base image <base-image>` and provides:

- Dataverse class files
- Resource files
- Scripts and associated data necessary for bootstrapping the application

The image is provided as a multi-arch image to support the most common architectures Dataverse usually runs on:
AMD64 (Windows/Linux/...) and ARM64 (Apple M1/M2). (Easy to extend.)

Build Instructions
++++++++++++++++++

``mvn -Pct clean package``

Run Instructions
++++++++++++++++

First, start all the containers.

``mvn -Pct docker:run``

Once all containers have been started, you can check if the WAR file was deployed correctly by checking the version at http://localhost:8080/api/info/version

If all looks good, run the :download:`docker-final-setup.sh <../../../../scripts/dev/docker-final-setup.sh>` script below. (This is a simplified version of the script described in :ref:`rebuilding-dev-environment`.)

``scripts/dev/docker-final-setup.sh``

.. literalinclude:: ../../../../scripts/dev/docker-final-setup.sh

Check that you can log in to http://localhost:8080

Tunables
++++++++

Hints
+++++
