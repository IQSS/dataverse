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

``mvn -Pct clean package docker:build``

Run Instructions
++++++++++++++++

First, start all the containers.

``mvn -Pct docker:run``

Once all containers have been started, run the script :download:`dev-rebuild-docker.sh <../../../../scripts/dev/dev-rebuild-docker.sh>` script below. This is a simplified version of the script described in :ref:`rebuilding-dev-environment`.

``scripts/dev/dev-rebuild-docker.sh``

.. literalinclude:: ../../../../scripts/dev/dev-rebuild-docker.sh

Check that you can log in to http://localhost:8080

Tunables
++++++++

Hints
+++++
