Config Baker Image
==================

The config baker container may be used to execute all sorts of tasks around setting up, preparing and finalizing
an instance of the Dataverse software. Its focus is bootstrapping non-initialized installations.

.. contents:: |toctitle|
    :local:

Quickstart
++++++++++

To see the Config Baker help screen:

``docker run -it --rm gdcc/configbaker:unstable``

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

This image contains some crucial parts to make a freshly baked Dataverse installation usable.

Scripts
^^^^^^^

.. list-table::
  :align: left
  :widths: 20 80
  :header-rows: 1

  * - Script
    - Description
  * - ``bootstrap.sh``
    - Run an initialization script contained in a persona. See ``bootstrap.sh -h`` for usage details.
      For development purposes, use ``bootstrap.sh dev`` or provide your own.
  * - ``fix-fs-perms.sh``
    - Fixes filesystem permissions. App and Solr container run as non-privileged users and might need adjusted
      filesystem permissions on mounted volumes to be able to write data. Run without parameters to see usage details.
  * - ``help.sh``
    - Default script when running container without parameters. Lists available scripts and details about them.
  * - ``update-fields.sh``
    - Update a Solr ``schema.xml`` with a given list of metadata fields. See ``update-fields.sh -h`` for usage details
      and example use cases at :ref:`update-solr-schema` and :ref:`update-solr-schema-dev`.

Solr Template
^^^^^^^^^^^^^

In addition, at ``/template`` a `Solr Configset <https://solr.apache.org/guide/solr/latest/configuration-guide/config-sets.html>`_
is available, ready for Dataverse usage with a tuned core config and schema.

Providing this template to a vanilla Solr image and using `solr-precreate <https://solr.apache.org/guide/solr/latest/deployment-guide/solr-in-docker.html#using-solr-precreate-command>`_
with it will create the necessary Solr search index.

The ``solrconfig.xml`` and ``schema.xml`` are included from the upstream project ``conf/solr/...`` folder. You are
obviously free to provide such a template in some other way, maybe tuned for your purposes.
As a start, the contained script ``update-fields.sh`` may be used to edit the field definitions.



Build Instructions
++++++++++++++++++

Assuming you have `Docker <https://docs.docker.com/engine/install/>`_, `Docker Desktop <https://www.docker.com/products/docker-desktop/>`_,
`Moby <https://mobyproject.org/>`_ or some remote Docker host configured, up and running from here on.
Note: You need to use Maven when building this image, as we collate selective files from different places of the upstream
repository. (Building with pure Docker Compose does not support this kind of selection.)

By default, when building the application image, it will also create a new config baker image. Simply execute the
Maven modules packaging target with activated "container" profile from the projects Git root to build the image:

``mvn -Pct package``

If you specifically want to build a config baker image *only*, try

``mvn -Pct docker:build -Ddocker.filter=dev_bootstrap``

The build of config baker involves copying Solr configset files. The Solr version used is inherited from Maven,
acting as the single source of truth. Also, the tag of the image should correspond the application image, as
their usage is intertwined.

Some additional notes, using Maven parameters to change the build and use ...:

- | ... a different tag only: add ``-Dconf.image.tag=tag``.
  | *Note:* default is ``${app.image.tag}``, which defaults to ``unstable``
- | ... a different image name and tag: add ``-Dconf.image=name:tag``.
  | *Note:* default is ``gdcc/configbaker:${conf.image.tag}``
- ... a different image registry than Docker Hub: add ``-Ddocker.registry=registry.example.org`` (see also
  `DMP docs on registries <https://dmp.fabric8.io/#registry>`__)
- ... a different Solr version: use ``-Dsolr.version=x.y.z``

Processor Architecture and Multiarch
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

This image is published as a "multi-arch image", supporting the most common architectures Dataverse usually runs on:
AMD64 (Windows/Linux/...) and ARM64 (Apple M1/M2), by using `Maven Docker Plugin's BuildX mode <https://dmp.fabric8.io/#build-buildx>`_.

Building the image via ``mvn -Pct package``, etc. will only build for the architecture of the Docker machine's CPU.

Only ``mvn -Pct deploy -Ddocker.platforms=linux/amd64,linux/arm64`` will trigger building on all enabled architectures.
Yet, to enable building with non-native code on your build machine, you will need to setup a cross-platform builder.

On Linux, you should install `qemu-user-static <https://github.com/multiarch/qemu-user-static>`__ (preferably via
your package management) on the host and run ``docker run --rm --privileged multiarch/qemu-user-static --reset -p yes``
to enable that builder. The Docker plugin will setup everything else for you.



Tunables
++++++++

This image has no tunable runtime parameters yet.



Locations
+++++++++

.. list-table::
    :align: left
    :width: 100
    :widths: 10 10 50
    :header-rows: 1

    * - Location
      - Value
      - Description
    * - ``${SCRIPT_DIR}``
      - ``/scripts``
      - Place to store the scripts. Part of ``$PATH``.
    * - ``${SOLR_TEMPLATE}``
      - ``/template``
      - Place where the Solr Configset resides to create an index core from it.
    * - ``${BOOTSTRAP_DIR}``
      - ``/scripts/bootstrap``
      - Stores the bootstrapping personas in sub-folders.
    * - ``${BOOTSTRAP_DIR}/base``
      - ``/scripts/bootstrap/base``
      - Minimal set of scripts and data from upstream ``scripts/api`` folder, just enough for the most basic setup.
        The idea is that other personas may reuse it within their own ``init.sh``, avoiding (some) code duplication.
        See ``dev`` persona for an example.



Exposed Ports
+++++++++++++

This image contains no runnable services yet, so no ports exposed.



Entry & Extension Points
++++++++++++++++++++++++

The entrypoint of this image is pinned to ``dumb-init`` to safeguard signal handling. You may feed any script or
executable to it as command.

By using our released images as base image to add your own scripting, personas, Solr configset and so on, simply
adapt and alter any aspect you need changed.



Examples
++++++++

Docker Compose snippet to wait for Dataverse deployment and execute bootstrapping using a custom persona you added
by bind mounting (as an alternative to extending the image):

.. code-block:: yaml

  bootstrap:
    image: gdcc/configbaker:unstable
    restart: "no"
    command:
      - bootstrap.sh
      - mypersona
    volumes:
      - ./mypersona:/scripts/bootstrap/mypersona
    networks:
      - dataverse

Docker Compose snippet to prepare execution of Solr and copy your custom configset you added by bind mounting
(instead of an extension). Note that ``solr-precreate`` will not overwrite an already existing core! To update
the config of an existing core, you need to mount the right volume with the stateful data!

.. code-block:: yaml

  solr_initializer:
    container_name: solr_initializer
    image: gdcc/configbaker:unstable
    restart: "no"
    command:
      - sh
      - -c
      - "fix-fs-perms.sh solr && cp -a /template/* /solr-template"
    volumes:
      - ./volumes/solr/data:/var/solr
      - ./volumes/solr/conf:/solr-template
      - /tmp/my-generated-configset:/template

  solr:
    container_name: solr
    hostname: solr
    image: solr:${SOLR_VERSION}
    depends_on:
      - dev_solr_initializer
    restart: on-failure
    ports:
      - "8983:8983"
    networks:
      - dataverse
    command:
      - "solr-precreate"
      - "collection1"
      - "/template"
    volumes:
      - ./volumes/solr/data:/var/solr
      - ./volumes/solr/conf:/template
