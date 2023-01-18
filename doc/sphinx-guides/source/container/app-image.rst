Dataverse Application Image
===========================

Within the main repository, you may find the application image's files at ``<git root>/src/main/docker``.
This Maven module, which also build the Dataverse WAR, uses the `Maven Docker Plugin <https://dmp.fabric8.io>`_
to build and ship the image within a special Maven profile.

Contents
++++++++

The application image builds by convention upon the :doc:`base image <base-image>` and provides:

- Dataverse class files
- Resource files
- Scripts and associated data necessary for bootstrapping the application

The image is provided as a multi-arch image to support the most common architectures Dataverse usually runs on:
AMD64 (Windows/Linux/...) and ARM64 (Apple M1/M2). (Easy to extend.)

Build Instructions
++++++++++++++++++

Tunables
++++++++



Hints
+++++

