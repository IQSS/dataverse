ConfigBaker Image
=================

The ConfigBaker image is designed to run shortly after the Dataverse software has been installed and configures it.

.. contents:: |toctitle|
    :local:

Supported Image Tags
++++++++++++++++++++

This image is sourced from the main upstream code `repository of the Dataverse software <https://github.com/IQSS/dataverse>`_.
Development and maintenance of the `image's code <https://github.com/IQSS/dataverse/tree/develop/modules/container-configbaker>`_
happens there (again, by the community). Community-supported image tags are based on the two most important
upstream branches:

- The ``unstable`` tag corresponds to the ``develop`` branch, where pull requests are merged.
  (`Dockerfile <https://github.com/IQSS/dataverse/tree/develop/modules/container-configbaker/src/main/docker/Dockerfile>`__)
- The ``alpha`` tag corresponds to the ``master`` branch, where releases are cut from.
  (`Dockerfile <https://github.com/IQSS/dataverse/tree/master/modules/container-configbaker/src/main/docker/Dockerfile>`__)

Image Contents
++++++++++++++

- Scripts for bootstrapping Dataverse
