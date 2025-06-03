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

.. _starting-over:

Deleting Data and Starting Over
-------------------------------

Again, data related to your Dataverse installation such as the database is stored in a directory called ``data`` that gets created in the directory where you ran ``docker compose`` commands.

You may reach a point during your demo or evaluation that you'd like to start over with a fresh database. Simply make sure the containers are not running and then remove the ``data`` directory. Now, as before, you can run ``docker compose up`` to spin up the containers.

Setting Up for a Demo
---------------------

Now that you are familiar with the basics of running Dataverse in containers, let's move on to a better setup for a demo or evaluation.

Starting Fresh
++++++++++++++

For this exercise, please start fresh by stopping all containers and removing the ``data`` directory.

.. _demo-persona:

Creating and Running a Demo Persona
+++++++++++++++++++++++++++++++++++

Previously we used the "dev" persona to bootstrap Dataverse, but for security reasons, we should create a persona more suited to demos and evaluations.

Edit the ``compose.yml`` file and look for the following section.

.. code-block:: bash

  bootstrap:
    container_name: "bootstrap"
    image: gdcc/configbaker:alpha
    restart: "no"
    environment:
      - TIMEOUT=3m
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

Note that the init script contains a key for using the admin API once it is blocked. You should change it in the script from "unblockme" to something only you know.

Now run ``docker compose up``. The "bootstrap" container should exit with the message from the init script and Dataverse should be running on http://localhost:8080 as before during the quickstart exercise.

One of the main differences between the "dev" persona and our new "demo" persona is that we are now running the setup-all script without the ``--insecure`` flag. This makes our installation more secure, though it does block "admin" APIs that are useful for configuration. 

Smoke Testing
-------------

At this point, please try the following basic operations within your installation:

- logging in as dataverseAdmin (password "admin1")
- publishing the "root" collection (dataverse)
- creating a collection
- creating a dataset
- uploading a data file
- publishing the dataset

If anything isn't working, please see the sections below on troubleshooting, giving feedback, and getting help.

Further Configuration
---------------------

Now that we've verified through a smoke test that basic operations are working, let's configure our installation of Dataverse.

Please refer to the :doc:`/installation/config` section of the Installation Guide for various configuration options.

Below we'll explain some specifics for configuration in containers.

JVM Options/MicroProfile Config
+++++++++++++++++++++++++++++++

:ref:`jvm-options` can be configured under ``JVM_ARGS`` in the ``compose.yml`` file. Here's an example:

.. code-block:: bash

    environment:
      JVM_ARGS: -Ddataverse.files.storage-driver-id=file1

Some JVM options can be configured as environment variables. For example, you can configure the database host like this:

.. code-block:: bash

    environment:
      DATAVERSE_DB_HOST: postgres

We are in the process of making more JVM options configurable as environment variables. Look for the term "MicroProfile Config" in under :doc:`/installation/config` in the Installation Guide to know if you can use them this way.

There is a final way to configure JVM options that we plan to deprecate once all JVM options have been converted to MicroProfile Config. Look for "magic trick" under "tunables" at :doc:`../app-image` for more information.

Database Settings
+++++++++++++++++

Generally, you should be able to look at the list of :ref:`database-settings` and configure them but the "demo" persona above secured your installation to the point that you'll need an "unblock key" to access the "admin" API and change database settings.

In the example below of configuring :ref:`:FooterCopyright` we use the default unblock key of "unblockme" but you should use the key you set above.

``curl -X PUT -d ", My Org" "http://localhost:8080/api/admin/settings/:FooterCopyright?unblock-key=unblockme"``

One you make this change it should be visible in the copyright in the bottom left of every page.

Root Collection Customization (Alias, Name, etc.)
+++++++++++++++++++++++++++++++++++++++++++++++++

Before running ``docker compose up`` for the first time, you can customize the root collection by placing a JSON file in the right place.

First, in the "demo" directory you created (see :ref:`demo-persona`), create a subdirectory called "config":

``mkdir demo/config``

Next, download :download:`dataverse-complete.json <../../_static/api/dataverse-complete.json>` and put it in the "config" directory you just created. The contents of your "demo" directory should look something like this:

.. code-block:: bash

        % find demo
        demo
        demo/config
        demo/config/dataverse-complete.json
        demo/init.sh

Edit ``dataverse-complete.json`` to have the values you want. You'll want to refer to :ref:`update-dataverse-api` in the API Guide to understand the format. In that documentation you can find optional parameters as well.

To test your JSON file, run ``docker compose up``. Again, this only works when you are running ``docker compose up`` for the first time. (You can always start over. See :ref:`starting-over`.)

Multiple Languages
++++++++++++++++++

Generally speaking, you'll want to follow :ref:`i18n` in the Installation Guide to set up multiple languages. (You need to create your own "languages.zip" file, for example.) Here will give you guidance specific to this demo tutorial. We'll be setting up a toggle between English and French.

First, edit the ``compose.yml`` file and uncomment the following line:

.. code-block:: text

        #-Ddataverse.lang.directory=/dv/lang

Next, upload "languages.zip" to the "loadpropertyfiles" API endpoint as shown below. This will place files ending in ".properties" into the ``/dv/lang`` directory configured above.

Please note that we are using a slight variation on the command in the instructions above, adding the unblock key we created above:

``curl "http://localhost:8080/api/admin/datasetfield/loadpropertyfiles?unblock-key=unblockme" -X POST --upload-file /tmp/languages/languages.zip -H "Content-Type: application/zip"``

Next, set up the UI toggle between English and French, again using the unblock key:

``curl "http://localhost:8080/api/admin/settings/:Languages?unblock-key=unblockme" -X PUT -d '[{"locale":"en","title":"English"},{"locale":"fr","title":"Français"}]'``

Stop and start the Dataverse container in order for the language toggle to work.

PID Providers
+++++++++++++

Dataverse supports multiple Persistent ID (PID) providers. The ``compose.yml`` file uses the Permalink PID provider. Follow :ref:`pids-configuration` to reconfigure as needed.

File Previewers
+++++++++++++++

By default, all available file previewers are enabled (see :ref:`file-previews` in the User Guide for details). Specifically, we enable all the previewers that are available in the `trivadis/dataverse-previewers-provider <https://hub.docker.com/r/trivadis/dataverse-previewers-provider>`_ image (`code <https://github.com/TrivadisPF/docker-register-dataverse-previewers>`_). You can run the following command to see a list of available previewers:

``docker run --rm trivadis/dataverse-deploy-previewers:latest previewers``

You should expect to see output like this:

.. code-block:: text

        name     description
        ----------------------------
        text     Read the text file.
        html     View the html file.
        ...

If you want to specify fewer previewers, you can edit the ``compose.yml`` file. Uncomment "INCLUDE_PREVIEWERS" and list the previewers you want, separated by commas, like this:

``INCLUDE_PREVIEWERS=text,html,pdf,csv``


.. _additional-metadata-blocks:

Additional Metadata Blocks
++++++++++++++++++++++++++

Metadata fields such as "Title" are part of a metadata block such as "Citation". See :ref:`metadata-references` in the User Guide for the metadata blocks that ship with Dataverse.

At a high level, we will be loading a metadata block and then adjusting our Solr config to know about it.

Care should be taken when adding additional metadata blocks. There is no way to `preview <https://github.com/IQSS/dataverse/issues/2551>`_ or `delete <https://github.com/IQSS/dataverse/issues/9628>`_ a metadata block so please use a throwaway environment.

:ref:`metadata-references` lists some experimental metadata blocks. In the example below, we'll use the CodeMeta block.

First, download a metadata block or create one by following :doc:`/admin/metadatacustomization` in the Admin Guide.

Load the metadata block like this:

``curl http://localhost:8080/api/admin/datasetfield/load -H "Content-type: text/tab-separated-values" -X POST --upload-file codemeta.tsv``

Next, reconfigure Solr to know about the new metadata block.

You can back up your existing Solr schema like this:

``cp docker-dev-volumes/solr/data/data/collection1/conf/schema.xml docker-dev-volumes/solr/data/data/collection1/conf/schema.xml.orig``

You can see the existing fields Solr knows about like this:

``curl http://localhost:8983/solr/collection1/schema/fields``

Update your Solr schema with the following command:

``curl http://localhost:8080/api/admin/index/solr/schema | docker run -i --rm -v ./docker-dev-volumes/solr/data:/var/solr gdcc/configbaker:unstable update-fields.sh /var/solr/data/collection1/conf/schema.xml``

Then, reload Solr:

``curl "http://localhost:8983/solr/admin/cores?action=RELOAD&core=collection1"``

You can get a diff of your old and new Solr schema like this:

``diff docker-dev-volumes/solr/data/data/collection1/conf/schema.xml.orig docker-dev-volumes/solr/data/data/collection1/conf/schema.xml``

You should be able to see the new fields from the metadata block you added in the following output:

``curl http://localhost:8983/solr/collection1/schema/fields``

At this point you can proceed with testing the metadata block in the Dataverse UI. First you'll need to enable it for a collection (see :ref:`general-information` in the User Guide section about collection). Afterwards, create a new dataset, save it, and then edit the metadata for that dataset. Your metadata block should appear.

Next Steps
----------

From here, you are encouraged to continue poking around, configuring, and testing. You probably spend a lot of time reading the :doc:`/installation/config` section of the Installation Guide.

Please consider giving feedback using the methods described below. Good luck with your demo!

About the Containers
--------------------

Now that you've gone through the tutorial, you might be interested in the various containers you've spun up and what they do.

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

The compose file references a tag called "alpha", which corresponds to the latest released version of Dataverse. This means that if a release of Dataverse comes out while you are demo'ing or evaluating, the version of Dataverse you are using could change if you do a ``docker pull``. We are aware that there is a desire for tags that correspond to versions to ensure consistency. You are welcome to join `the discussion <https://dataverse.zulipchat.com/#narrow/stream/375812-containers/topic/tagging.20images.20with.20versions/near/366600747>`_ and otherwise get in touch (see :ref:`helping-containers`). For more on tags, see :ref:`app-image-supported-tags`.

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

In the compose file, try increasing the timeout for the bootstrap container:

.. code-block:: bash

   environment:
     - TIMEOUT=10m

As described above, you'll want to stop containers, delete data, and start over with ``docker compose up``. To make sure the increased timeout is in effect, you can run ``docker logs bootstrap`` and look for the new value in the output:

``Waiting for http://dataverse:8080 to become ready in max 10m.``

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

Please do not be shy about reaching out for help. We very much want you to have a pleasant demo or evaluation experience. For ways to contact us, please see :ref:`getting-help-containers`.
