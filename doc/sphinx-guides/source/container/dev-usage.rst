Development Usage
=================

Please note! This Docker setup is not for production!

.. contents:: |toctitle|
        :local:

Quickstart
----------

First, install Java 11 and Maven.

After cloning the repo, try this:

``mvn -Pct clean package docker:run``

After some time you should be able to log in:

- url: http://localhost:8080
- username: dataverseAdmin
- password: admin1

Intro
-----

Assuming you have `Docker <https://docs.docker.com/engine/install/>`_, `Docker Desktop <https://www.docker.com/products/docker-desktop/>`_,
`Moby <https://mobyproject.org/>`_ or some remote Docker host configured, up and running from here on. Also assuming
you have Java and Maven installed, as you are at least about to develop code changes.

To test drive these local changes to the Dataverse codebase in a containerized application server (and avoid the
setup described in :doc:`../developers/dev-environment`), you must a) build the application and b) run it in addition
to the necessary dependencies. (Which might involve building a new local version of the :doc:`configbaker-image`.)

.. _dev-build:

Building
--------

To build the :doc:`application <app-image>` and :doc:`config baker image <configbaker-image>`, run the following command:

``mvn -Pct clean package``

Once this is done, you will see images ``gdcc/dataverse:unstable`` and ``gdcc/configbaker:unstable`` available in your
Docker cache.

**Note:** This will skip any unit tests. If you have built the code before for testing, etc. you might omit the
``clean`` to avoid recompiling.

**Note:** Also we have a ``docker-compose-dev.yml`` file, it's currently not possible to build the images without
invoking Maven. This might change in the future.


.. _dev-run:

Running
-------

After building the app and config baker image containing your local changes to the Dataverse application, you want to
run it together with all dependencies. There are four ways to do this (commands executed at root of project directory):

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
at http://localhost:8080/api/info/version or watch the logs.

**Note:** To stop all containers you started in background, invoke ``mvn -Pct docker:stop`` or
``docker compose -f docker-compose-dev.yml down``.

Check that you can log in to http://localhost:8080 using user ``dataverseAdmin`` and password ``admin1``.

You can also access the Payara Admin Console if needed, which is available at http://localhost:4848. To log in, use
user ``admin`` and password ``admin``. As a reminder, the application container is for development use only, so we
are exposing the admin console for testing purposes. In a production environment, it may be more convenient to leave
this console unopened.

Note that data is persisted in ``./docker-dev-volumes`` in the root of the Git repo. For a clean start, you should
remove this directory before running the ``mvn`` commands above.


.. _dev-logs:

Viewing Logs
------------

In case you started containers in background mode (see :ref:`dev-run`), you can use the following commands to view and/or
watch logs from the containers.

The safe bet for any running container's logs is to lookup the container name via ``docker ps`` and use it in
``docker logs <name>``. You can tail logs by adding ``-n`` and follow them by adding ``-f`` (just like ``tail`` cmd).
See ``docker logs --help`` for more.

Alternatives:

- In case you used Maven for running, you may use ``mvn -Pct docker:logs -Ddocker.filter=<service name>``.
- If you used Docker Compose for running, you may use ``docker compose -f docker-compose-dev.yml logs <service name>``.
  Options are the same.


Re-Deploying
------------

Currently, the only safe and tested way to re-deploy the Dataverse application after you applied code changes is
by recreating the container(s). In the future, more options may be added here.

If you started your containers in foreground, just stop them and follow the steps for building and running again.
The same goes for using Maven to start the containers in the background.

In case of using Docker Compose and starting the containers in the background, you can use a workaround to only
restart the application container:

.. code-block::

  # First rebuild the container (will complain about an image still in use, this is fine.)
  mvn -Pct package
  # Then re-create the container (will automatically restart the container for you)
  docker compose -f docker-compose-dev.yml create dev_dataverse

Using ``docker container inspect dev_dataverse | grep Image`` you can verify the changed checksums.

Using A Debugger
----------------

The :doc:`base-image` enables usage of the `Java Debugging Wire Protocol <https://dzone.com/articles/remote-debugging-java-applications-with-jdwp>`_
for remote debugging if you set ``ENABLE_JDWP=1`` as environment variable for the application container.
The default configuration when executing containers with the commands listed at :ref:`dev-run` already enables this.

There are a lot of tutorials how to connect your IDE's debugger to a remote endpoint. Please use ``localhost:9009``
as the endpoint. Here are links to the most common IDEs docs on remote debugging:
`Eclipse <https://help.eclipse.org/latest/topic/org.eclipse.jdt.doc.user/concepts/cremdbug.htm?cp=1_2_12>`_,
`IntelliJ <https://www.jetbrains.com/help/idea/tutorial-remote-debug.html#debugger_rc>`_
