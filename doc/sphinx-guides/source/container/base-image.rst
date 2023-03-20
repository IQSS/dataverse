Application Base Image
======================

.. contents:: |toctitle|
    :local:

A "base image" offers you a pre-installed and pre-tuned application server to deploy Dataverse software to.
Adding basic functionality like executing scripts at container boot, monitoring, memory tweaks etc is all done
at this layer, to make the application image focus on the app itself.

**NOTE: The base image does not contain the Dataverse application itself.**

Within the main repository, you may find the base image's files at `<git root>/modules/container-base`.
This Maven module uses the `Maven Docker Plugin <https://dmp.fabric8.io>`_ to build and ship the image.
You may use, extend, or alter this image to your liking and/or host in some different registry if you want to.

**NOTE: This image is created, maintained and supported by the Dataverse community on a best-effort basis.**
IQSS will not offer you support how to deploy or run it, please reach out to the community for help on using it.
You might be interested in taking a look at :doc:`../developers/containers`, linking you to some (community-based)
efforts.

Supported Image Tags
++++++++++++++++++++

This image is sourced from the main upstream code `repository of the Dataverse software <https://github.com/IQSS/dataverse>`_.
Development and maintenance of the `image's code <https://github.com/IQSS/dataverse/tree/develop/modules/container-base>`_
happens there (again, by the community). Community-supported image tags are based on the two most important
upstream branches:

- The `unstable` tag corresponds to the `develop` branch, where pull requests are merged.
  (`Dockerfile <https://github.com/IQSS/dataverse/tree/develop/modules/container-base/src/main/docker/Dockerfile>`__)
- The `stable` tag corresponds to the `master` branch, where releases are cut from.
  (`Dockerfile <https://github.com/IQSS/dataverse/tree/master/modules/container-base/src/main/docker/Dockerfile>`__)



Image Contents
++++++++++++++

The base image provides:

- `Eclipse Temurin JRE using Java 11 <https://adoptium.net/temurin/releases?version=11>`_
- `Payara Community Application Server <https://docs.payara.fish/community>`_
- CLI tools necessary to run Dataverse (i. e. `curl` or `jq` - see also :doc:`../installation/prerequisites` in Installation Guide)
- Linux tools for analysis, monitoring and so on
- `Jattach <https://github.com/apangin/jattach>`__ (attach to running JVM)
- `wait-for <https://github.com/eficode/wait-for>`__ (tool to "wait for" a service to be available)
- `dumb-init <https://github.com/Yelp/dumb-init>`__ (see :ref:`below <base-entrypoint>` for details)

This image is created as a "multi-arch image", see :ref:`below <base-multiarch>`.

It inherits (is built on) an Ubuntu environment from the upstream
`base image of Eclipse Temurin <https://hub.docker.com/_/eclipse-temurin>`_.
You are free to change the JRE/JDK image to your liking (see below).



Build Instructions
++++++++++++++++++

Assuming you have `Docker <https://docs.docker.com/engine/install/>`_, `Docker Desktop <https://www.docker.com/products/docker-desktop/>`_,
`Moby <https://mobyproject.org/>`_ or some remote Docker host configured, up and running from here on.

Simply execute the Maven modules packaging target with activated "container profile. Either from the projects Git root:

`mvn -Pct -f modules/container-base install`

Or move to the module and execute:

`cd modules/container-base && mvn -Pct install`

Some additional notes, using Maven parameters to change the build and use ...:

- | ... a different tag only: add `-Dbase.image.tag=tag`.
  | *Note:* default is `develop`
- | ... a different image name and tag: add `-Dbase.image=name:tag`.
  | *Note:* default is `gdcc/base:${base.image.tag}`
- ... a different image registry than Docker Hub: add `-Ddocker.registry=registry.example.org` (see also
  `DMP docs on registries <https://dmp.fabric8.io/#registry>`__)
- ... a different Payara version: add `-Dpayara.version=V.YYYY.R`.
- | ... a different Temurin JRE version `A`: add `-Dtarget.java.version=A` (i.e. `11`, `17`, ...).
  | *Note:* must resolve to an available image tag `A-jre` of Eclipse Temurin!
    (See also `Docker Hub search example <https://hub.docker.com/_/eclipse-temurin/tags?page=1&name=11-jre>`_)
- ... a different Java Distribution: add `-Djava.image="name:tag"` with precise reference to an
  image available local or remote.
- ... a different UID/GID for the `payara` user/group: add `-Dbase.image.uid=1234` (or `.gid`)

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
AMD64 (Windows/Linux/...) and ARM64 (Apple M1/M2), by using Maven Docker Plugin's *BuildX* mode.

Building the image via `mvn -Pct package` or `mvn -Pct install` as above will only build for the architecture of
the Docker maschine's CPU.

Only `mvn -Pct deploy` will trigger building on all enabled architectures.
Yet, to enable building with non-native code on your build machine, you will need to setup a cross-platform builder.

On Linux, you should install `qemu-user-static <https://github.com/multiarch/qemu-user-static>`__ (preferably via
your package management) on the host and run `docker run --rm --privileged multiarch/qemu-user-static --reset -p yes`
to enable that builder. The Docker plugin will setup everything else for you.



Tunables
++++++++

The base image provides a Payara domain suited for production use, but can also be used during development.
Many settings have been carefully selected for best performance and stability of the Dataverse application.

As with any service, you should always monitor any metrics and make use of the tuning capabilities the base image
provides. These are mostly based on environment variables (very common with containers) and provide sane defaults.

.. csv-table::
    :class: longtable
    :header-rows: 1
    :delim: tab
    :file: ../_static/container/tunables.tsv
    :widths: 35, 15, 15, 35

.. [preboot] `${CONFIG_DIR}/pre-boot-commands.asadmin`
.. [postboot] `${CONFIG_DIR}/post-boot-commands.asadmin`
.. [dump-option] `-XX:+HeapDumpOnOutOfMemoryError`

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
    :widths: 25 25 50
    :header-rows: 1

    * - Env. variable
      - Value
      - Description
    * - `HOME_DIR`
      - `/opt/payara`
      - Home base to Payara and the application
    * - `PAYARA_DIR`
      - `${HOME_DIR}/appserver`
      - Installation directory of Payara server
    * - `SCRIPT_DIR`
      - `${HOME_DIR}/scripts`
      - Any scripts like the container entrypoint, init scripts, etc
    * - `CONFIG_DIR`
      - `${HOME_DIR}/config`
      - Payara Server configurations like pre/postboot command files go here
        (Might be reused for Dataverse one day)
    * - `DEPLOY_DIR`
      - `${HOME_DIR}/deployments`
      - Any EAR or WAR file, exploded WAR directory etc are autodeployed on start
    * - `DOMAIN_DIR`
      - `${PAYARA_DIR}/glassfish` `/domains/${DOMAIN_NAME}`
      - Path to root of the Payara domain applications will be deployed into. Usually `${DOMAIN_NAME}` will be `domain1`.


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
    * - `STORAGE_DIR`
      - `/dv`
      - This place is writeable by the Payara user, making it usable as a place to store research data, customizations
        or other. Images inheriting the base image should create distinct folders here, backed by different
        mounted volumes.
    * - `SECRETS_DIR`
      - `/secrets`
      - Mount secrets or other here, being picked up automatically by
        `Directory Config Source <https://docs.payara.fish/community/docs/Technical%20Documentation/MicroProfile/Config/Directory.html>`_.
        See also various :doc:`../installation/config` options involving secrets.
    * - `DUMPS_DIR`
      - `/dumps`
      - Default location where heap dumps will be stored (see above).
        You should mount some storage here (disk or ephemeral).


Exposed Ports
+++++++++++++

The default ports that are exposed by this image are:

- 8080 - HTTP listener
- 4848 - Admin Service HTTPS listener
- 8686 - JMX listener
- 9009 - "Java Debug Wire Protocol" port (when `ENABLE_JDWP=1`)

The HTTPS listener (on port 8181) becomes deactivated during the build, as we will always need to reverse-proxy the
application server and handle SSL/TLS termination at this point. Save the memory and some CPU cycles!



.. _base-entrypoint:

Entry & Extension Points
++++++++++++++++++++++++

The entrypoint shell script provided by this base image will by default ensure to:

- Run any scripts named `${SCRIPT_DIR}/init_*` or in `${SCRIPT_DIR}/init.d/*` directory for initialization
  **before** the application server starts.
- Run an executable script `${SCRIPT_DIR}/startInBackground.sh` in the background - if present.
- Run the application server startup scripting in foreground (`${SCRIPT_DIR}/startInForeground.sh`).

If you need to create some scripting that runs in parallel under supervision of `dumb-init <https://github.com/Yelp/dumb-init>`_,
e.g. to wait for the application to deploy before executing something, this is your point of extension: simply provide
the `${SCRIPT_DIR}/startInBackground.sh` executable script with your application image.



Other Hints
+++++++++++

By default, `domain1` is enabled to use the `G1GC` garbage collector.

For running a Java application within a Linux based container, the support for CGroups is essential. It has been
included and activated by default since Java 8u192, Java 11 LTS and later. If you are interested in more details,
you can read about those in a few places like https://developers.redhat.com/articles/2022/04/19/java-17-whats-new-openjdks-container-awareness,
https://www.eclipse.org/openj9/docs/xxusecontainersupport, etc. The other memory defaults are inspired
from `run-java-sh recommendations`_.



.. _Pre/postboot script docs: https://docs.payara.fish/community/docs/Technical%20Documentation/Payara%20Micro%20Documentation/Payara%20Micro%20Configuration%20and%20Management/Micro%20Management/Asadmin%20Commands/Pre%20and%20Post%20Boot%20Commands.html
.. _MicroProfile Config Sources: https://docs.payara.fish/community/docs/Technical%20Documentation/MicroProfile/Config/Overview.html
.. _run-java-sh recommendations: https://github.com/fabric8io-images/run-java-sh/blob/master/TUNING.md#recommandations
