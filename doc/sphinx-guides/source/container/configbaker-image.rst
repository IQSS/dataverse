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

.. _config-image-supported-tags:

Supported Image Tags
++++++++++++++++++++

This image is sourced from the main upstream code `repository of the Dataverse software <https://github.com/IQSS/dataverse>`_.
Development and maintenance of the `image's code <https://github.com/IQSS/dataverse/tree/develop/modules/container-configbaker>`_
happens there (again, by the community).

All supported images receive scheduled maintenance, executed every Sunday.
New revisions are kept to a minimum, usually created when some dependency needs (security) updates.
The `Trivy <https://trivy.dev>`_ scanner is used to check for fixed vulnerabilities and rebuilds are issued if such a fix is detected.

Our tagging is inspired by `Bitnami <https://docs.vmware.com/en/VMware-Tanzu-Application-Catalog/services/tutorials/GUID-understand-rolling-tags-containers-index.html>`_ and we offer two categories of tags:

- rolling: images change over time
- immutable: images are fixed and never change

In the tags below you'll see the term "flavor". This refers to flavor of Linux the container is built on. We use Ubuntu as the basis for our images and, for the time being, the only operating system flavors we use and support are ``noble`` (6.7+) and ``alpine`` (pre-6.7).

You can find all the tags at https://hub.docker.com/r/gdcc/configbaker/tags

Tags for Production Use
^^^^^^^^^^^^^^^^^^^^^^^

The images of the three latest releases of the Dataverse project will receive updates such as security patches for the underlying operating system.
Content will be fairly stable as disruptive changes like Payara or Java upgrades will be handled in a new major or minor upgrade to Dataverse (a new ``<dv-major>.<dv-minor>`` tag).
Expect disruptive changes in case of high risk security threats.

- | **Latest**
  | Definition: ``latest``
  | Summary: Rolling tag, always pointing to the latest revision of the most current Dataverse release.
- | **Rolling Production**
  | Definition: ``<dv-major>.<dv-minor>-<flavor>``
  | Example: :substitution-code:`|version|-noble`
  | Summary: Rolling tag, pointing to the latest revision of an immutable production image for released versions of Dataverse.
- | **Immutable Production**
  | Definition: ``<dv-major>.<dv-minor>-<flavor>-r<revision>``
  | Example: :substitution-code:`|version|-noble-r1`
  | Summary: An **immutable tag** where the revision is incremented for rebuilds of the image.
  | This image should be especially attractive if you want explict control over when your images are updated.

Tags for Development Use
^^^^^^^^^^^^^^^^^^^^^^^^

All of the tags below are strongly recommended only for development purposes due to their fast-changing nature.
In addition to updates due to PR merges, the most recent tags undergo scheduled maintenance to ensure timely security fixes.
When a development cycle of Dataverse finishes, maintenance ceases for any tags carrying version numbers.
For now, stale images will be kept on Docker Hub indefinitely.

- | **Unstable**
  | Definition: ``unstable``
  | Summary: Rolling tag, tracking the ``develop`` branch (see also :ref:`develop-branch`). (`Dockerfile <https://github.com/IQSS/dataverse/tree/develop/modules/container-base/src/main/docker/Dockerfile>`__)
  | Please expect abrupt changes like new Payara or Java versions as well as OS updates or flavor switches when using this tag.
- | **Upcoming**
  | Definition: ``<dv-major>.<dv-minor-next>-<flavor>``
  | Example: :substitution-code:`|nextVersion|-noble`
  | Summary: Rolling tag, equivalent to ``unstable`` for current development cycle.
    Will roll over to the rolling production tag after a Dataverse release.

**NOTE**: In these tags for development usage, the version number will always be 1 minor version ahead of existing Dataverse releases.
Example: Assume Dataverse ``6.x`` is released, ``6.(x+1)`` is underway.
The rolling tag in use during the cycle will be ``6.(x+1)-FFF`` and ``6.(x+1)-FFF-p6.202P.P-jJJ``.
See also: :doc:`/developers/making-releases`.


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
