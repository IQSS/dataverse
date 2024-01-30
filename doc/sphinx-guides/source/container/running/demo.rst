Demo or Evaluation
==================

In the following tutorial we'll walk through spinning up Dataverse in containers for demo or evaluation purposes.

.. contents:: |toctitle|
	:local:

Quickstart
----------

First, let's confirm that we can get Dataverse running on your system.

- Download :download:`compose.yml <../../../../../docker/compose/demo/compose.yml>`
- Run ``docker compose up`` in the directory where you put ``compose.yml``
- Visit http://localhost:8080 and try logging in:

  - username: dataverseAdmin
  - password: admin1

If you can log in, great! Please continue through the tutorial. If you have any trouble, please consult the sections below on troubleshooting and getting help.

Stopping and Starting the Containers
------------------------------------

Let's practice stopping the containers and starting them up again. Your data, stored in a directory called ``data``, will remain intact

To stop the containers hit ``Ctrl-c`` (hold down the ``Ctrl`` key and then hit the ``c`` key).

To start the containers, run ``docker compose up``.

Deleting Data and Starting Over
-------------------------------

Again, data related to your Dataverse installation such as the database is stored in a directory called ``data`` that gets created in the directory where you ran ``docker compose`` commands.

You may reach a point during your demo or evaluation that you'd like to start over with a fresh database. Simply make sure the containers are not running and then remove the ``data`` directory. Now, as before, you can run ``docker compose up`` to spin up the containers.

Configuring Dataverse
---------------------

Now that you are familiar with the basics of running Dataverse in containers, let's move on to configuration.

Start Fresh
+++++++++++

For this configuration exercise, please start fresh by stopping all containers and removing the ``data`` directory.

Change the Site URL
+++++++++++++++++++

Edit ``compose.yml`` and change ``_CT_DATAVERSE_SITEURL`` to the URL you plan to use for your installation.

(You can read more about this setting at :ref:`dataverse.siteUrl`.)

This is an example of setting an environment variable to configure Dataverse.

Create and Run a Demo Persona
+++++++++++++++++++++++++++++

Previously we used the "dev" persona to bootstrap Dataverse, but for security reasons, we should create a persona more suited to demos and evaluations.

Edit the ``compose.yml`` file and look for the following section.

.. code-block:: bash

  bootstrap:
    container_name: "bootstrap"
    image: gdcc/configbaker:alpha
    restart: "no"
    command:
      - bootstrap.sh
      - dev
      #- demo
    #volumes:
    #  - ./demo:/scripts/bootstrap/demo
    networks:
      - dataverse

Comment out "dev" and uncomment "demo".

Uncomment the "volumes" section.

Create a directory called "demo" and copy :download:`init.sh <../../../../../modules/container-configbaker/scripts/bootstrap/demo/init.sh>` into it. You are welcome to edit this demo init script, customizing the final message, for example.

Now run ``docker compose up``. The "bootstrap" container should exit with the message from the init script and Dataverse should be running on http://localhost:8080 as before during the quickstart exercise.

One of the main differences between the "dev" persona and our new "demo" persona is that we are now running the setup-all script without the ``--insecure`` flag. This makes our installation more secure, though it does block "admin" APIs that are useful for configuration. 

Set DOI Provider to FAKE
++++++++++++++++++++++++

For the purposes of a demo, we'll use the "FAKE" DOI provider. (For more on this and related settings, see :ref:`pids-configuration` in the Installation Guide.) Without this step, you won't be able to create or publish datasets.

Run the following command. (In this context, "dataverse" is the name of the running container.)

``docker exec -it dataverse curl http://localhost:8080/api/admin/settings/:DoiProvider -X PUT -d FAKE``

This is an example of configuring a database setting, which you can read more about at :ref:`database-settings` in the Installation Guide.

Smoke Test
----------

At this point, please try some basic operations within your installation, such as:

- logging in as dataverseAdmin
- publishing the "root" collection (dataverse)
- creating a collection
- creating a dataset
- uploading a data file
- publishing the dataset

About the Containers
--------------------

Container List
++++++++++++++

If you run ``docker ps``, you'll see that multiple containers are spun up in a demo or evaluation. Here are the most important ones:

- dataverse
- postgres
- solr
- smtp
- bootstrap

Most are self-explanatory, and correspond to components listed under :doc:`/installation/prerequisites` in the (traditional) Installation Guide, but "bootstrap" refers to :doc:`../configbaker-image`.

Additional containers are used in development (see :doc:`../dev-usage`), but for the purposes of a demo or evaluation, fewer moving (sometimes pointy) parts are included.

Tags and Versions
+++++++++++++++++

The compose file references a tag called "alpha", which corresponds to the latest released version of Dataverse. This means that if a release of Dataverse comes out while you are demo'ing or evaluating, the version of Dataverse you are using could change if you do a ``docker pull``. We are aware that there is a desire for tags that correspond to versions to ensure consistency. You are welcome to join `the discussion <https://dataverse.zulipchat.com/#narrow/stream/375812-containers/topic/tagging.20images.20with.20versions/near/366600747>`_ and otherwise get in touch (see :ref:`helping-containers`). For more on tags, see :ref:`supported-image-tags-app`.

Once Dataverse is running, you can check which version you have through the normal methods:

- Check the bottom right in a web browser.
- Check http://localhost:8080/api/info/version via API.

Troubleshooting
---------------

Hardware and Software Requirements
++++++++++++++++++++++++++++++++++

- 8 GB RAM (if not much else is running)
- Mac, Linux, or Windows (experimental)
- Docker

Windows support is experimental but we are very interested in supporting Windows better. Please report bugs (see :ref:`helping-containers`).

Bootstrapping Did Not Complete
++++++++++++++++++++++++++++++

In the compose file, try increasing the timeout in the bootstrap container by adding something like this:

.. code-block:: bash

   environment:
     - TIMEOUT=10m

Wrapping Up
-----------

Deleting the Containers and Data
++++++++++++++++++++++++++++++++

If you no longer need the containers because your demo or evaluation is finished and you want to reclaim disk space, run ``docker compose down`` in the directory where you put ``compose.yml``.

You might also want to delete the ``data`` directory, as described above.

Giving Feedback
---------------

Your feedback is extremely valuable to us! To let us know what you think, please see :ref:`helping-containers`.

Getting Help
------------

Please do not be shy about reaching out for help. We very much want you to have a pleasant demo or evaluation experience. For ways to contact us, please see See :ref:`getting-help-containers`.
