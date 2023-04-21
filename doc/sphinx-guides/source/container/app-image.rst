Dataverse Application Image
===========================

The application image is a layer on top of the base image and contains the Dataverse software.

.. contents:: |toctitle|
    :local:

An "application image" offers you a deployment ready Dataverse application running on the underlying
application server, which is provided by the :doc:`base-image`. Its sole purpose is to bundle the application
and any additional material necessary to successfully jumpstart the application.

Until all :ref:`jvm-options` are *MicroProfile Config* enabled, it also adds the necessary scripting glue to
configure the applications domain during booting the application server. See :ref:`app-tunables`.

Within the main repository, you may find the application image's files at ``<git root>/src/main/docker``.
This is the same Maven module providing a Dataverse WAR file for classic installations, and uses the
`Maven Docker Plugin <https://dmp.fabric8.io>`_ to build and ship the image within a special Maven profile.

**NOTE: This image is created, maintained and supported by the Dataverse community on a best-effort basis.**
IQSS will not offer you support how to deploy or run it, please reach out to the community for help on using it.
You might be interested in taking a look at :doc:`../developers/containers`, linking you to some (community-based)
efforts.



Supported Image Tags
++++++++++++++++++++

This image is sourced from the main upstream code `repository of the Dataverse software <https://github.com/IQSS/dataverse>`_.
Development and maintenance of the `image's code <https://github.com/IQSS/dataverse/tree/develop>`_ happens there
(again, by the community).

.. note::
    Please note that this image is not (yet) available from Docker Hub. You need to build local to use
    (see below). Follow https://github.com/IQSS/dataverse/issues/9444 for new developments.



Image Contents
++++++++++++++

The application image builds by convention upon the :doc:`base image <base-image>` and provides:

- Dataverse class files
- Resource files
- Dependency JAR files
- `JHove <http://jhove.openpreservation.org>`_ configuration
- Script to configure the application server domain for :ref:`jvm-options` not yet *MicroProfile Config* enabled.

The image is provided as a multi-arch image to support the most common architectures Dataverse usually runs on:
AMD64 (Windows/Linux/...) and ARM64 (Apple M1/M2). (Easy to extend.)



Build Instructions
++++++++++++++++++

Assuming you have `Docker <https://docs.docker.com/engine/install/>`_, `Docker Desktop <https://www.docker.com/products/docker-desktop/>`_,
`Moby <https://mobyproject.org/>`_ or some remote Docker host configured, up and running from here on.

Simply execute the Maven modules packaging target with activated "container" profile from the projects Git root to
compile the Java code and build the image:

``mvn -Pct clean package``

Some additional notes, using Maven parameters to change the build and use ...:

- | ... a different tag only: add ``-Dapp.image.tag=tag``.
  | *Note:* default is ``unstable``
- | ... a different image name and tag: add ``-Dapp.image=name:tag``.
  | *Note:* default is ``gdcc/dataverse:${app.image.tag}``
- ... a different image registry than Docker Hub: add ``-Ddocker.registry=registry.example.org`` (see also
  `DMP docs on registries <https://dmp.fabric8.io/#registry>`__)
- | ... a different base image tag: add ``-Dbase.image.tag=tag``
  | *Note:* default is ``unstable``
- | ... a different base image: add ``-Dbase.image=name:tag``
  | *Note:* default is ``gdcc/base:${base.image.tag}``. See also :doc:`base-image` for more details on it.

Automated Builds & Publishing
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

See note above at "Supported Image Tags".

.. _app-multiarch:

Processor Architecture and Multiarch
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

This image is created as a "multi-arch image", supporting the most common architectures Dataverse usually runs on:
AMD64 (Windows/Linux/...) and ARM64 (Apple M1/M2), by using `Maven Docker Plugin's BuildX mode <https://dmp.fabric8.io/#build-buildx>`_.

Building the image via ``mvn -Pct package`` or ``mvn -Pct install`` as above will only build for the architecture of
the Docker machine's CPU.

Only ``mvn -Pct clean deploy -Ddocker.platforms=linux/amd64,linux/arm64`` will trigger building on all enabled architectures.
Yet, to enable building with non-native code on your build machine, you will need to setup a cross-platform builder.

On Linux, you should install `qemu-user-static <https://github.com/multiarch/qemu-user-static>`__ (preferably via
your package management) on the host and run ``docker run --rm --privileged multiarch/qemu-user-static --reset -p yes``
to enable that builder. The Docker plugin will setup everything else for you.



.. _app-tunables:

Tunables
++++++++

The :doc:`base-image` provides a long list of possible options to tune many aspects of the application server, and,
as the application image builds upon it, :ref:`Base Image Tunables <base-tunables>` apply to it as well.

In addition, the application image provides the following tunables:

.. list-table::
    :align: left
    :width: 100
    :widths: 10 10 10 50
    :header-rows: 1

    * - Env. variable
      - Default
      - Type
      - Description
    * - ``MP_CONFIG_PROFILE``
      - ``ct``
      - String
      - Set to switch the activated *MicroProfile Config Profile*. Note that certain defaults will not apply any longer.
        See :ref:`:ApplicationServerSettings` for details.
    * - ``dataverse_*`` and ``doi_*``
      - \-
      - String
      - Configure any :ref:`jvm-options` not yet *MicroProfile Config* enabled with this magic trick.

        1. Simply pick a JVM option from the list and replace any ``.`` with ``_``.
        2. Replace any ``-`` in the option name with ``__``.
    * - ``DATAVERSE_MAIL_HOST``
      - ``smtp``
      - String
      - A hostname (w/o port!) where to reach a Mail MTA on port 25.
    * - ``DATAVERSE_MAIL_USER``
      - ``dataversenotify``
      - String
      - A username to use with the Mail MTA
    * - ``DATAVERSE_MAIL_FROM``
      - ``dataverse@localhost``
      - Mail address
      - The "From" field for all outbound mail. Make sure to set :ref:`systemEmail` to the same value or no mail will
        be sent.


Note that the script ``init_2_configure.sh`` will apply a few very important defaults to enable quick usage
by a) activating the scheduled tasks timer, b) add local file storage if not disabled, and c) a sensible password
reset timeout:

.. code-block:: shell

    dataverse_auth_password__reset__timeout__in__minutes=60
    dataverse_timerServer=true
    dataverse_files_storage__driver__id=local

    if dataverse_files_storage__driver__id = "local" then
        dataverse_files_local_type=file
        dataverse_files_local_label=Local
        dataverse_files_local_directory=${STORAGE_DIR}/store



.. _app-locations:

Locations
+++++++++

There are only a few important additions to the list of `locations by the base image <base-locations>`_.
Please make sure to back these locations with volumes or tmpfs to avoid writing data into the overlay filesystem, which
will significantly hurt performance.

.. list-table::
    :align: left
    :width: 100
    :widths: 10 10 50
    :header-rows: 1

    * - Location
      - Value
      - Description
    * - ``${STORAGE_DIR}``
      - ``/dv``
      - Defined by base image. Either back this folder or, if suitable, the locations below it with volumes
        or tmpfs.
    * - ``${STORAGE_DIR}/uploads``
      - ``/dv/uploads``
      - See :ref:`dataverse.files.uploads` for a detailed description.
    * - ``${STORAGE_DIR}/temp``
      - ``/dv/temp``
      - See :ref:`dataverse.files.directory` for a detailed description.
    * - ``${STORAGE_DIR}/store``
      - ``/dv/store``
      - Important when using the default provided local storage option (see above and :ref:`storage-files-dir`)
    * - ``/tmp``
      - \-
      - Location for temporary files, see also :ref:`temporary-file-storage`



Exposed Ports
+++++++++++++

See base image :ref:`exposed port <base-exposed-ports>`.



Entry & Extension Points
++++++++++++++++++++++++

The application image makes use of the base image provided system to execute scripts on boot, see :ref:`base-entrypoint`.
See there for potential extension of this image in your own derivative.
