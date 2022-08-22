Application Base Image
======================

Within the main repository, you may find the base image's files at ``<git root>/modules/container-base``.
This Maven module uses the `Maven Docker Plugin <https://dmp.fabric8.io>`_ to build and ship the image.

Contents
++++++++

The base image provides:

- `Eclipse Temurin JRE using Java 11 <https://adoptium.net/temurin/releases?version=11>`_
- `Payara Community Application Server <https://docs.payara.fish/community>`_
- CLI tools necessary to run Dataverse (i. e. ``curl`` or ``jq`` - see also :doc:`../installation/prerequisites` in Installation Guide)
- Linux tools for analysis, monitoring and so on
- `Jattach <https://github.com/apangin/jattach>`_

This image is created as a "multi-arch image", supporting the most common architectures Dataverse usually runs on:
AMD64 (Windows/Linux/...) and ARM64 (Apple M1/M2).

It inherits being built on an Ubuntu environment from the upstream
`base image of Eclipse Temurin <https://hub.docker.com/_/eclipse-temurin>`_.
You are free to change the JRE/JDK image to your liking (see below).



Build Instructions
++++++++++++++++++

Assuming you have `Docker <https://docs.docker.com/engine/install/>`_, `Docker Desktop <https://www.docker.com/products/docker-desktop/>`_,
`Moby <https://mobyproject.org/>`_ or some remote Docker host configured, up and running from here on.

Simply execute the Maven modules packaging target with activated "container profile. Either from the projects Git root:

``mvn -Pct -f modules/container-base package``

Or move to the module and execute:

``cd modules/container-base && mvn -Pct package``

Some additional notes, using Maven parameters to change the build and use ...:

- ... a different Payara version: add ``-Dpayara.version=V.YYYY.R``.
- | ... a different Temurin JRE version ``A``: add ``-Dtarget.java.version=A`` (i.e. ``11``, ``17``, ...).
  | *Note:* must resolve to an available Docker tag ``A-jre`` of Eclipse Temurin!
- ... a different Java Distribution: add ``-Ddocker.buildArg.BASE_IMAGE="name:tag"`` with precise reference to an
  image available from local or remote (e. g. Docker Hub).



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
        See also `Pre/postboot script docs`_.
    * - ``POSTBOOT_COMMANDS``
      - [postboot]_
      - Abs. path
      - Provide path to file with ``asadmin`` commands to run **after** boot of application server.
        See also `Pre/postboot script docs`_.
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
      - ``1``
      - Bool, ``0|1``
      - Enable JMX - Payara enables this by default, hard to deactivate.
    * - ``ENABLE_JDWP``
      - ``0``
      - Bool, ``0|1``
      - Enable the "Java Debug Wire Protocol" to attach a remote debugger to the JVM in this container.
        Listens on port 9009 when enabled. Search the internet for numerous tutorials to use it.
    * - ``DATAVERSE_HTTP_TIMEOUT``
      - ``900``
      - Seconds
      - See :ref:`:ApplicationServerSettings` ``http.request-timeout-seconds``.

        *Note:* can also be set using any other `MicroProfile Config Sources`_ available via ``dataverse.http.timeout``.


.. [preboot] ``${CONFIG_DIR}/pre-boot-commands.asadmin``
.. [postboot] ``${CONFIG_DIR}/post-boot-commands.asadmin``
.. [dump-option] ``-XX:+HeapDumpOnOutOfMemoryError``



Locations
+++++++++

This environment variables represent certain locations and might be reused in your scripts etc.
These variables aren't meant to be reconfigurable and reflect state in the filesystem layout!

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
      - Any EAR or WAR file, exploded WAR directory etc are autodeployed on start
    * - ``DOCROOT_DIR``
      - ``/docroot``
      - Mount a volume here to store i18n language bundle files, sitemaps, images for Dataverse collections, logos,
        custom themes and stylesheets, etc here. You might need to replicate this data or place on shared file storage.
    * - ``SECRETS_DIR``
      - ``/secrets``
      - Mount secrets or other here, being picked up automatically by
        `Directory Config Source <https://docs.payara.fish/community/docs/Technical%20Documentation/MicroProfile/Config/Directory.html>`_.
        See also various :doc:`../installation/config` options involving secrets.
    * - ``DUMPS_DIR``
      - ``/dumps``
      - Default location where heap dumps will be stored (see above).
        You should mount some storage here (disk or ephemeral).
    * - ``DOMAIN_DIR``
      - ``${PAYARA_DIR}/glassfish`` ``/domains/${DOMAIN_NAME}``
      - Path to root of the Payara domain applications will be deployed into. Usually ``${DOMAIN_NAME}`` will be ``domain1``.



Exposed Ports
+++++++++++++

The default ports that are exposed by this image are:

- 8080 - HTTP listener
- 8181 - HTTPS listener
- 4848 - Admin Service HTTPS listener
- 8686 - JMX listener
- 9009 - "Java Debug Wire Protocol" port (when ``ENABLE_JDWP=1``)



Hints
+++++

By default, ``domain1`` is enabled to use the ``G1GC`` garbage collector.

For running a Java application within a Linux based container, the support for CGroups is essential. It has been
included and activated by default since Java 8u192, Java 11 LTS and later. If you are interested in more details,
you can read about those in a few places like https://developers.redhat.com/articles/2022/04/19/java-17-whats-new-openjdks-container-awareness,
https://www.eclipse.org/openj9/docs/xxusecontainersupport, etc. The other memory defaults are inspired
from `run-java-sh recommendations`_.


.. _Pre/postboot script docs: https://docs.payara.fish/community/docs/Technical%20Documentation/Payara%20Micro%20Documentation/Payara%20Micro%20Configuration%20and%20Management/Micro%20Management/Asadmin%20Commands/Pre%20and%20Post%20Boot%20Commands.html
.. _MicroProfile Config Sources: https://docs.payara.fish/community/docs/Technical%20Documentation/MicroProfile/Config/Overview.html
.. _run-java-sh recommendations: https://github.com/fabric8io-images/run-java-sh/blob/master/TUNING.md#recommandations