Demo or Evaluation
==================

If you would like to demo or evaluate Dataverse running in containers, you're in the right place. Your feedback is extremely valuable to us! To let us know what you think, please see :ref:`helping-containers`.

.. contents:: |toctitle|
	:local:

Quickstart
----------

- Download :download:`compose.yml <../../../../../docker/compose/demo/compose.yml>`
- Run ``docker compose up`` in the directory where you put ``compose.yml``
- Visit http://localhost:8080 and try logging in:

  - username: dataverseAdmin
  - password: admin1

Hardware and Software Requirements
-----------------------------------

- 8 GB RAM (if not much else is running)
- Mac, Linux, or Windows (experimental)
- Docker

Windows support is experimental but we are very interested in supporting Windows better. Please report bugs (see :ref:`helping-containers`).

Tags and Versions
-----------------

The compose file references a tag called "alpha", which corresponds to the latest released version of Dataverse. This means that if a release of Dataverse comes out while you are demo'ing or evaluating, the version of Dataverse you are using could change. We are aware that there is a desire for tags that correspond to versions to ensure consistency. You are welcome to join `the discussion <https://dataverse.zulipchat.com/#narrow/stream/375812-containers/topic/tagging.20images.20with.20versions/near/366600747>`_ and otherwise get in touch (see :ref:`helping-containers`). For more on tags, see :ref:`supported-image-tags-app`.

Once Dataverse is running, you can check which version you have through the normal methods:

- Check the bottom right in a web browser.
- Check http://localhost:8080/api/info/version via API.

About the Containers
--------------------

If you run ``docker ps``, you'll see that multiple containers are spun up in a demo or evaluation. Here are the most important ones:

- dataverse
- postgres
- solr
- smtp
- bootstrap

Most are self-explanatory, and correspond to components listed under :doc:`/installation/prerequisites` in the (traditional) Installation Guide, but "bootstrap" refers to :doc:`../configbaker-image`.

Additional containers are used in development (see :doc:`../dev-usage`), but for the purposes of a demo or evaluation, fewer moving (sometimes pointy) parts are included.

Security
--------

Please be aware that for now, the "dev" persona is used to bootstrap Dataverse, which means that admin APIs are wide open (to allow developers to test them; see :ref:`securing-your-installation` for more on API blocking), the "create user" key is set to a default value, etc. You can inspect the dev person `on GitHub <https://github.com/IQSS/dataverse/blob/master/modules/container-configbaker/scripts/bootstrap/dev/init.sh>`_ (look for ``--insecure``).

We plan to ship a "demo" persona but it is not ready yet. See also :ref:`configbaker-personas`.

Common Operations
-----------------

Starting the Containers
+++++++++++++++++++++++

First, download :download:`compose.yml <../../../../../docker/compose/demo/compose.yml>` and place it somewhere you'll remember.

Then, run ``docker compose up`` in the directory where you put ``compose.yml``

Starting the containers for the first time involves a bootstrap process. You should see "have a nice day" output at the end.

Stopping the Containers
+++++++++++++++++++++++

You might want to stop the containers if you aren't using them. Hit ``Ctrl-c`` (hold down the ``Ctrl`` key and then hit the ``c`` key).

You data is still intact and you can start the containers again with ``docker compose up``.

Deleting the Containers
+++++++++++++++++++++++

If you no longer need the containers because your demo or evaluation is finished and you want to reclaim disk space, run ``docker compose down`` in the directory where you put ``compose.yml``.

Deleting the Data Directory
+++++++++++++++++++++++++++

Data related to the Dataverse containers is placed in a directory called ``data`` next to the ``compose.yml`` file. If you are finished with your demo or evaluation or you want to start fresh, simply delete this directory.

Configuration
-------------

Configuration is described in greater detail under :doc:`/installation/config` in the Installation Guide, but there are some specifics to running in containers you should know about.

.. _configbaker-personas:

Personas
++++++++

When the containers are bootstrapped, the "dev" persona is used. In the future we plan to add a "demo" persona that is more suited to demo and evaluation use cases.

Database Settings
+++++++++++++++++

Updating database settings is the same as described under :ref:`database-settings` in the Installation Guide.

MPCONFIG Options
++++++++++++++++

The compose file contains an ``environment`` section with various MicroProfile Config (MPCONFIG) options. You can experiment with this by adding ``DATAVERSE_VERSION: foobar`` to change the (displayed) version of Dataverse to "foobar".

JVM Options
+++++++++++

JVM options are not especially easy to change in the container. The general process is to get a shell on the "dataverse" container, change the settings, and then stop and start the containers. See :ref:`jvm-options` for more.

Troubleshooting
---------------

Bootstrapping Did Not Complete
++++++++++++++++++++++++++++++

In the compose file, try increasing the timeout in the bootstrap container by adding something like this:

.. code-block:: bash

   environment:
     - TIMEOUT=10m

Getting Help
------------

Please do not be shy about reaching out for help. We very much want you to have a pleasant demo or evaluation experience. For ways to contact us, please see See :ref:`getting-help-containers`.
