Application Base Image
======================

The base image contains Payara and other dependencies that the Dataverse software runs on. It is the foundation for the :doc:`app-image`. Note that some dependencies, such as PostgreSQL and Solr, run in their own containers and are not part of the base image.

.. contents:: |toctitle|
    :local:

A "base image" offers you a pre-installed and pre-tuned application server to deploy Dataverse software to.
Adding basic functionality like executing scripts at container boot, monitoring, memory tweaks etc. is all done
at this layer, to make the application image focus on the app itself.

**NOTE: The base image does not contain the Dataverse application itself.**

Within the main repository, you may find the base image's files at ``<git root>/modules/container-base``.
This Maven module uses the `Maven Docker Plugin <https://dmp.fabric8.io>`_ to build and ship the image.
You may use, extend, or alter this image to your liking and/or host in some different registry if you want to.

**NOTE: This image is created, maintained and supported by the Dataverse community on a best-effort basis.**
IQSS will not offer you support how to deploy or run it, please reach out to the community (:ref:`support`) for help on using it.
You might be interested in taking a look at :doc:`../developers/containers`, linking you to some (community-based)
efforts.

.. _base-supported-image-tags:

Supported Image Tags
++++++++++++++++++++

This image is sourced from the main upstream code `repository of the Dataverse software <https://github.com/IQSS/dataverse>`_.
Development and maintenance of the `image's code <https://github.com/IQSS/dataverse/tree/develop/modules/container-base>`_
happens there (again, by the community).

All supported images are signed up for scheduled maintenance, executed every Sunday.
New revisions are kept to a minimum, usually created when some dependency needs (security) updates.
(Examples: JRE patch releases, ImageMagick fixes, etc.)

Our tagging is inspired by `Bitnami <https://docs.vmware.com/en/VMware-Tanzu-Application-Catalog/services/tutorials/GUID-understand-rolling-tags-containers-index.html>`_ and we offer two categories of tags:

- rolling: images change over time
- immutable: images are fixed and never change

In the tags below you'll see the term "flavor". This refers to flavor of Linux the container is built on. We use Ubuntu as the basis for our images and, for the time being, the only operating system flavors we use and support are ``noble`` (6.4+) and ``jammy`` (pre-6.4).

You can find all the tags at https://hub.docker.com/r/gdcc/base/tags

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

All of the tags below are strongly recommended for development purposes only due to their fast changing nature.
In addition to updates due to PR merges, the most recent are undergoing scheduled maintenance to ensure timely security fixes.
When a development cycle of the Dataverse project finishes, maintenance ceases for any tags carrying version numbers.
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
- | **Flexible Stack**
  | Definition: ``<dv-major>.<dv-minor-next>-<flavor>-p<payara.version>-j<java.version>``
  | Example: :substitution-code:`|nextVersion|-noble-p6.2025.3-j17`
  | Summary: Rolling tag during a development cycle of the Dataverse software (`Dockerfile <https://github.com/IQSS/dataverse/tree/develop/modules/container-base/src/main/docker/Dockerfile>`__).

**NOTE**: In these tags for development usage, the version number will always be 1 minor version ahead of existing Dataverse releases.
Example: Assume Dataverse ``6.x`` is released, ``6.(x+1)`` is underway.
The rolling tag in use during the cycle will be ``6.(x+1)-FFF`` and ``6.(x+1)-FFF-p6.202P.P-jJJ``.
See also: :doc:`/developers/making-releases`.

Image Contents
++++++++++++++

The base image provides:

- `Eclipse Temurin JRE using Java 17 <https://adoptium.net/temurin/releases?version=17>`_
- `Payara Community Application Server <https://docs.payara.fish/community>`_
- CLI tools necessary to run Dataverse (i. e. ``curl`` or ``jq`` - see also :doc:`../installation/prerequisites` in Installation Guide)
- Linux tools for analysis, monitoring and so on
- `Jattach <https://github.com/apangin/jattach>`__ (attach to running JVM)
- `wait4x <https://github.com/atkrad/wait4x>`__ (tool to "wait for" a service to be available)
- `dumb-init <https://github.com/Yelp/dumb-init>`__ (see :ref:`below <base-entrypoint>` for details)

This image is created as a "multi-arch image", see :ref:`below <base-multiarch>`.

It inherits (is built on) an Ubuntu environment from the upstream
`base image of Eclipse Temurin <https://hub.docker.com/_/eclipse-temurin>`_.
You are free to change the JRE/JDK image to your liking (see below).



Build Instructions
++++++++++++++++++

Assuming you have `Docker <https://docs.docker.com/engine/install/>`_, `Docker Desktop <https://www.docker.com/products/docker-desktop/>`_,
`Moby <https://mobyproject.org/>`_ or some remote Docker host configured, up and running from here on.

Simply execute the Maven modules packaging target with activated "container" profile. Either from the projects Git root:

``mvn -Pct -f modules/container-base install``

Or move to the module and execute:

``cd modules/container-base && mvn -Pct install``

Some additional notes, using Maven parameters to change the build and use ...:

- | ... a different tag only: add ``-Dbase.image.tag=tag``.
  | *Note:* default is ``unstable``
- | ... a different image name and tag: add ``-Dbase.image=name:tag``.
  | *Note:* default is ``gdcc/base:${base.image.tag}``
- ... a different image registry than Docker Hub: add ``-Ddocker.registry=registry.example.org`` (see also
  `DMP docs on registries <https://dmp.fabric8.io/#registry>`__)
- ... a different Payara version: add ``-Dpayara.version=V.YYYY.R``.
- | ... a different Temurin JRE version ``A``: add ``-Dtarget.java.version=A`` (i.e. ``11``, ``17``, ...).
  | *Note:* must resolve to an available image tag ``A-jre`` of Eclipse Temurin!
    (See also `Docker Hub search example <https://hub.docker.com/_/eclipse-temurin/tags?page=1&name=11-jre>`_)
- ... a different Java Distribution: add ``-Djava.image="name:tag"`` with precise reference to an
  image available local or remote.
- ... a different UID/GID for the ``payara`` user/group (default ``1000:1000``): add ``-Dbase.image.uid=1234`` (or ``.gid``)

Automated Builds & Publishing
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

To make reusing most simple, the image is built with a Github Action within the IQSS repository and then pushed
to `Docker Hub gdcc/base repository <https://hub.docker.com/r/gdcc/base>`_. It is built and pushed on every edit to
its sources plus uncached scheduled nightly builds to make sure security updates are finding their way in.

*Note:* For the Github Action to be able to push to Docker Hub, two repository secrets
(DOCKERHUB_USERNAME, DOCKERHUB_TOKEN) have been added by IQSS admins to their repository.

.. _base-multiarch:

Processor Architecture and Multiarch
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

This image is created as a "multi-arch image", supporting the most common architectures Dataverse usually runs on:
AMD64 (Windows/Linux/...) and ARM64 (Apple M1/M2), by using `Maven Docker Plugin's BuildX mode <https://dmp.fabric8.io/#build-buildx>`_.

Building the image via ``mvn -Pct package`` or ``mvn -Pct install`` as above will only build for the architecture of
the Docker machine's CPU.

Only ``mvn -Pct deploy`` will trigger building on all enabled architectures (and will try to push the images to a
registry, which is Docker Hub by default).

You can specify which architectures you would like to build for and include by them as a comma separated list:
``mvn -Pct deploy -Ddocker.platforms="linux/amd64,linux/arm64"``. The shown configuration is the default and may be omitted.

Yet, to enable building with non-native code on your build machine, you will need to setup a cross-platform builder!

On Linux, you should install `qemu-user-static <https://github.com/multiarch/qemu-user-static>`__ (preferably via
your package management) on the host and run ``docker run --rm --privileged multiarch/qemu-user-static --reset -p yes``
to enable that builder. The Docker plugin will setup everything else for you.

The upstream CI workflows publish images supporting AMD64 and ARM64 (see e.g. tag details on Docker Hub)

.. _base-tunables:

Tunables
++++++++

The base image provides a Payara domain suited for production use, but can also be used during development.
Many settings have been carefully selected for best performance and stability of the Dataverse application.

As with any service, you should always monitor any metrics and make use of the tuning capabilities the base image
provides. These are mostly based on environment variables (very common with containers) and provide sane defaults.

.. list-table::
    :align: left
    :width: 100
    :widths: 10 10 10 50
    :header-rows: 1

    * - Env. variable
      - Default
      - Type
      - Description
    * - ``DEPLOY_PROPS``
      - (empty)
      - String
      - Set to add arguments to generated `asadmin deploy` commands.
    * - ``PREBOOT_COMMANDS``
      - [preboot]_
      - Abs. path
      - Provide path to file with ``asadmin`` commands to run **before** boot of application server.
        See also `Pre/postboot script docs`_. Must be writeable by Payara Linux user!
    * - ``POSTBOOT_COMMANDS``
      - [postboot]_
      - Abs. path
      - Provide path to file with ``asadmin`` commands to run **after** boot of application server.
        See also `Pre/postboot script docs`_. Must be writeable by Payara Linux user!
    * - ``JVM_ARGS``
      - (empty)
      - String
      - Additional arguments to pass to application server's JVM on start.
    * - ``MEM_MAX_RAM_PERCENTAGE``
      - ``70.0``
      - Percentage
      - Maximum amount of container's allocated RAM to be used as heap space.
        Make sure to leave some room for native memory, OS overhead etc!
    * - ``MEM_XSS``
      - ``512k``
      - Size
      - Tune the maximum JVM stack size.
    * - ``MEM_MIN_HEAP_FREE_RATIO``
      - ``20``
      - Integer
      - Make the heap shrink aggressively and grow conservatively. See also `run-java-sh recommendations`_.
    * - ``MEM_MAX_HEAP_FREE_RATIO``
      - ``40``
      - Integer
      - Make the heap shrink aggressively and grow conservatively. See also `run-java-sh recommendations`_.
    * - ``MEM_MAX_GC_PAUSE_MILLIS``
      - ``500``
      - Milliseconds
      - Shorter pause times might result in lots of collections causing overhead without much gain.
        This needs monitoring and tuning. It's a complex matter.
    * - ``MEM_METASPACE_SIZE``
      - ``256m``
      - Size
      - Initial size of memory reserved for class metadata, also used as trigger to run a garbage collection
        once passing this size.
    * - ``MEM_MAX_METASPACE_SIZE``
      - ``2g``
      - Size
      - The metaspace's size will not outgrow this limit.
    * - ``ENABLE_DUMPS``
      - ``0``
      - Bool, ``0|1``
      - If enabled, the argument(s) given in ``JVM_DUMP_ARG`` will be added to the JVM starting up.
        This means it will enable dumping the heap to ``${DUMPS_DIR}`` (see below) in "out of memory" cases.
        (You should back this location with disk space / ramdisk, so it does not write into an overlay filesystem!)
    * - ``JVM_DUMPS_ARG``
      - [dump-option]_
      - String
      - Can be fine tuned for more grained controls of dumping behaviour.
    * - ``ENABLE_JMX``
      - ``0``
      - Bool, ``0|1``
      - Allow insecure JMX connections, enable AMX and tune all JMX monitoring levels to ``HIGH``.
        See also `Payara Docs - Basic Monitoring <https://docs.payara.fish/community/docs/Technical%20Documentation/Payara%20Server%20Documentation/Logging%20and%20Monitoring/Monitoring%20Service/Basic%20Monitoring%20Configuration.html>`_.
        A basic JMX service is enabled by default in Payara, exposing basic JVM MBeans, but especially no Payara MBeans.
    * - ``ENABLE_JDWP``
      - ``0``
      - Bool, ``0|1``
      - Enable the "Java Debug Wire Protocol" to attach a remote debugger to the JVM in this container.
        Listens on port 9009 when enabled. Search the internet for numerous tutorials to use it.
    * - ``ENABLE_RELOAD``
      - ``0``
      - Bool, ``0|1``
      - Enable the dynamic "hot" reloads of files when changed in a deployment. Useful for development,
        when new artifacts are copied into the running domain. Also, export Dataverse specific environment variables
        ``DATAVERSE_JSF_PROJECT_STAGE=Development`` and ``DATAVERSE_JSF_REFRESH_PERIOD=0`` to enable dynamic JSF page
        reloads.
    * - ``SKIP_DEPLOY``
      - ``0``
      - Bool, ``0|1`` or ``false|true``
      - When active, do not deploy applications from ``DEPLOY_DIR`` (see below), just start the application server.
        Will still execute any provided init scripts and only skip deployments within the default init scripts.
    * - ``DATAVERSE_HTTP_TIMEOUT``
      - ``900``
      - Seconds
      - See :ref:`:ApplicationServerSettings` ``http.request-timeout-seconds``.

        *Note:* can also be set using any other `MicroProfile Config Sources`_ available via ``dataverse.http.timeout``.
    * - ``PAYARA_ADMIN_PASSWORD``
      - ``admin``
      - String
      - Set to secret string to change `Payara Admin Console`_ Adminstrator User ("admin") password.
    * - ``LINUX_PASSWORD``
      - ``payara``
      - String
      - Set to secret string to change the Payara Linux User ("payara", default UID=1000) password.
    * - ``DOMAIN_PASSWORD``
      - ``changeit``
      - String
      - Set to secret string to change the `Domain Master Password`_.


.. [preboot] ``${CONFIG_DIR}/pre-boot-commands.asadmin``
.. [postboot] ``${CONFIG_DIR}/post-boot-commands.asadmin``
.. [dump-option] ``-XX:+HeapDumpOnOutOfMemoryError``


.. _base-locations:

Locations
+++++++++

This environment variables represent certain locations and might be reused in your scripts etc.
All of these variables aren't meant to be reconfigurable and reflect state in the filesystem layout!

**Writeable at build time:**

The overlay filesystem of Docker and other container technologies is not meant to be used for any performance IO.
You should avoid *writing* data anywhere in the file tree at runtime, except for well known locations with mounted
volumes backing them (see below).

The locations below are meant to be written to when you build a container image, either this base or anything
building upon it. You can also use these for references in scripts, etc.

.. list-table::
    :align: left
    :width: 100
    :widths: 10 10 50
    :header-rows: 1

    * - Env. variable
      - Value
      - Description
    * - ``HOME_DIR``
      - ``/opt/payara``
      - Home base to Payara and the application
    * - ``PAYARA_DIR``
      - ``${HOME_DIR}/appserver``
      - Installation directory of Payara server
    * - ``SCRIPT_DIR``
      - ``${HOME_DIR}/scripts``
      - Any scripts like the container entrypoint, init scripts, etc
    * - ``CONFIG_DIR``
      - ``${HOME_DIR}/config``
      - Payara Server configurations like pre/postboot command files go here
        (Might be reused for Dataverse one day)
    * - ``DEPLOY_DIR``
      - ``${HOME_DIR}/deployments``
      - Any EAR or WAR file, exploded WAR directory etc are autodeployed on start.
        See also ``SKIP_DEPLOY`` above.
    * - ``DOMAIN_DIR``
      - ``${PAYARA_DIR}/glassfish`` ``/domains/${DOMAIN_NAME}``
      - Path to root of the Payara domain applications will be deployed into. Usually ``${DOMAIN_NAME}`` will be ``domain1``.


**Writeable at runtime:**

The locations below are defined as `Docker volumes <https://docs.docker.com/storage/volumes/>`_ by the base image.
They will by default get backed by an "anonymous volume", but you can (and should) bind-mount a host directory or
named Docker volume in these places to avoid data loss, gain performance and/or use a network file system.

**Notes:**
1. On Kubernetes you still need to provide volume definitions for these places in your deployment objects!
2. You should not write data into these locations at build time - it will be shadowed by the mounted volumes!

.. list-table::
    :align: left
    :width: 100
    :widths: 10 10 50
    :header-rows: 1

    * - Env. variable
      - Value
      - Description
    * - ``STORAGE_DIR``
      - ``/dv``
      - This place is writeable by the Payara user, making it usable as a place to store research data, customizations or other.
        Images inheriting the base image should create distinct folders here, backed by different mounted volumes.
        Enforce correct filesystem permissions on the mounted volume using ``fix-fs-perms.sh`` from :doc:`configbaker-image` or similar scripts.
    * - ``SECRETS_DIR``
      - ``/secrets``
      - Mount secrets or other here, being picked up automatically by
        `Directory Config Source <https://docs.payara.fish/community/docs/Technical%20Documentation/MicroProfile/Config/Directory.html>`_.
        See also various :doc:`../installation/config` options involving secrets.
    * - ``DUMPS_DIR``
      - ``/dumps``
      - Default location where heap dumps will be stored (see above).
        You should mount some storage here (disk or ephemeral).


.. _base-exposed-ports:

Exposed Ports
+++++++++++++

The default ports that are exposed by this image are:

- 8080 - HTTP listener
- 4848 - Admin Service HTTPS listener
- 8686 - JMX listener
- 9009 - "Java Debug Wire Protocol" port (when ``ENABLE_JDWP=1``)

The HTTPS listener (on port 8181) becomes deactivated during the build, as we will always need to reverse-proxy the
application server and handle SSL/TLS termination at this point. Save the memory and some CPU cycles!



.. _base-entrypoint:

Entry & Extension Points
++++++++++++++++++++++++

The entrypoint shell script provided by this base image will by default ensure to:

- Run any scripts named ``${SCRIPT_DIR}/init_*`` or in ``${SCRIPT_DIR}/init.d/*`` directory for initialization
  **before** the application server starts.
- Run an executable script ``${SCRIPT_DIR}/startInBackground.sh`` in the background - if present.
- Run the application server startup scripting in foreground (``${SCRIPT_DIR}/startInForeground.sh``).

If you need to create some scripting that runs in parallel under supervision of `dumb-init <https://github.com/Yelp/dumb-init>`_,
e.g. to wait for the application to deploy before executing something, this is your point of extension: simply provide
the ``${SCRIPT_DIR}/startInBackground.sh`` executable script with your application image.



Other Hints
+++++++++++

By default, ``domain1`` is enabled to use the ``G1GC`` garbage collector.

To access the Payara Admin Console or use the ``asadmin`` command, use username ``admin`` and password ``admin``.

For running a Java application within a Linux based container, the support for CGroups is essential. It has been
included and activated by default since Java 8u192, Java 11 LTS and later. If you are interested in more details,
you can read about those in a few places like https://developers.redhat.com/articles/2022/04/19/java-17-whats-new-openjdks-container-awareness,
https://www.eclipse.org/openj9/docs/xxusecontainersupport, etc. The other memory defaults are inspired
from `run-java-sh recommendations`_.



.. _Pre/postboot script docs: https://docs.payara.fish/community/docs/Technical%20Documentation/Payara%20Micro%20Documentation/Payara%20Micro%20Configuration%20and%20Management/Micro%20Management/Asadmin%20Commands/Pre%20and%20Post%20Boot%20Commands.html
.. _MicroProfile Config Sources: https://docs.payara.fish/community/docs/Technical%20Documentation/MicroProfile/Config/Overview.html
.. _run-java-sh recommendations: https://github.com/fabric8io-images/run-java-sh/blob/master/TUNING.md#recommandations
.. _Domain Master Password: https://docs.payara.fish/community/docs/Technical%20Documentation/Payara%20Server%20Documentation/Security%20Guide/Administering%20System%20Security.html#to-change-the-master-password
.. _Payara Admin Console: https://docs.payara.fish/community/docs/Technical%20Documentation/Payara%20Server%20Documentation/General%20Administration/Overview.html#administration-console
