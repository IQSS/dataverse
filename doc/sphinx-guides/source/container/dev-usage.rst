Development Usage
=================

Please note! This Docker setup is not for production!

.. contents:: |toctitle|
        :local:

Intro
-----

Assuming you have `Docker <https://docs.docker.com/engine/install/>`_, `Docker Desktop <https://www.docker.com/products/docker-desktop/>`_,
`Moby <https://mobyproject.org/>`_ or some remote Docker host configured, up and running from here on. Also assuming
you have Java and Maven installed, as you are at least about to develop code changes.

To test drive these local changes to the Dataverse codebase in a containerized application server (and avoid the
setup described in :doc:`../developers/dev-environment`), you must a) build the application container and b)
run it in addition to the necessary dependencies.

Building
--------

To build the application image, run the following command, as described in :doc:`app-image`:

``mvn -Pct clean package``

Once this is done, you will see an image ``gdcc/dataverse:unstable`` available in your Docker cache.

**Note:** This will skip any unit tests. If you have built the code before for testing, etc. you might omit the ``clean`` to
avoid recompiling.

**Note:** Also we have a ``docker-compose-dev.yml`` file, it's currently not possible to build the images without
invoking Maven. This might change in the future.

Running
-------

After building the app image containing your local changes to the Dataverse application, you want to run it together
with all dependencies. There are four ways to do this (commands executed at root of project directory):

.. list-table:: Cheatsheet: Running Containers
   :widths: 15 40 45
   :header-rows: 1
   :stub-columns: 1
   :align: left

   * - \
     - Using Maven
     - Using Compose
   * - In foreground
     - ``mvn -Pct docker:run``
     - ``docker compose -f docker-compose-dev.yml up``
   * - In background
     - ``mvn -Pct docker:start``
     - ``docker compose -f docker-compose-dev.yml up -d``

Both ways have their pros and cons:

.. list-table:: Decision Helper: Fore- or Background?
   :widths: 15 40 45
   :header-rows: 1
   :stub-columns: 1
   :align: left

   * - \
     - Pros
     - Cons
   * - Foreground
     - | Logs scroll by when interacting with API / UI
       | To stop all containers simply hit ``Ctrl+C``
     - | Lots and lots of logs scrolling by
       | Must stop all containers to restart
   * - Background
     - | No logs scrolling by
       | Easy to replace single containers
     - | No logs scrolling by
       | Stopping containers needs an extra command

In case you want to concatenate building and running, here's a cheatsheet for you:

.. list-table:: Cheatsheet: Building and Running Containers
   :widths: 15 40 45
   :header-rows: 1
   :stub-columns: 1
   :align: left

   * - \
     - Using Maven
     - Using Compose
   * - In foreground
     - ``mvn -Pct package docker:run``
     - ``mvn -Pct package && docker compose -f docker-compose-dev.yml up``
   * - In background
     - ``mvn -Pct package docker:start``
     - ``mvn -Pct package && docker compose -f docker-compose-dev.yml up -d``

Once all containers have been started, you can check if the application was deployed correctly by checking the version
at http://localhost:8080/api/info/version. or watch the logs.

**Note:** To stop all containers you started in background, invoke ``mvn -Pct docker:stop`` or
``docker compose -f docker-compose-dev.yml down``.

Bootstrapping New Instance
--------------------------

If all looks good, run the :download:`docker-final-setup.sh <../../../../scripts/dev/docker-final-setup.sh>` script below.
(This is a simplified version of the script described in :ref:`rebuilding-dev-environment`.)
In the future, we are planning on running this script within a container as part of https://github.com/IQSS/dataverse/issues/9443

.. literalinclude:: ../../../../scripts/dev/docker-final-setup.sh
  :language: shell
  :encoding: utf-8
  :caption: ``scripts/dev/docker-final-setup.sh``
  :name: docker-final-setup

Check that you can log in to http://localhost:8080 using user ``dataverseAdmin`` and password ``admin1``.

You can also access the Payara Admin Console if needed, which is available at http://localhost:4848. To log in, use user ``admin`` and password ``admin``. As a reminder, the application container is for development use only, so we are exposing the admin console for testing purposes. In a production environment, it may be more convenient to leave this console unopened.

Note that data is persisted in ``./docker-dev-volumes`` in the root of the Git repo. For a clean start, you should
remove this directory before running the ``mvn`` commands above.

Viewing Logs
------------

TODO

Re-Deploying
------------

TODO

Using A Debugger
----------------

TODO
