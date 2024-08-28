Development Usage
=================

Please note! This Docker setup is not for production!

.. contents:: |toctitle|
        :local:

Quickstart
----------

See :ref:`container-dev-quickstart`.

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


Redeploying
-----------

The safest and most reliable way to redeploy code is to stop the running containers (with Ctrl-c if you started them in the foreground) and then build and run them again with ``mvn -Pct clean package docker:run``.
Safe, but also slowing down the development cycle a lot.

Triggering redeployment of changes using an IDE can greatly improve your feedback loop when changing code.
You have at least two options:

#. Use builtin features of IDEs or `IDE plugins from Payara <https://docs.payara.fish/community/docs/documentation/ecosystem/ecosystem.html>`_.
#. Use a paid product like `JRebel <https://www.jrebel.com/>`_.

The main differences between the first and the second options are support for hot deploys of non-class files and limitations in what the JVM HotswapAgent can do for you.
Find more details in a `blog article by JRebel <https://www.jrebel.com/blog/java-hotswap-guide>`_.

.. _ide-trigger-code-deploy:

IDE Triggered Code Re-Deployments
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

To make use of builtin features or Payara IDE Tools (option 1), please follow steps below.
Note that using this method, you may redeploy a complete WAR or single methods.
Redeploying WARs supports swapping and adding classes and non-code materials, but is slower (still faster than rebuilding containers).
Hotswapping methods requires using JDWP (Debug Mode), but does not allow switching non-code material or adding classes.

#. | Download the version of Payara shown in :ref:`install-payara-dev` and unzip it to a reasonable location such as ``/usr/local/payara6``.
   | - Note that Payara can also be downloaded from `Maven Central <https://mvnrepository.com/artifact/fish.payara.distributions/payara>`_.
   | - Note that another way to check the expected version of Payara is to run this command:
   |   ``mvn help:evaluate -Dexpression=payara.version -q -DforceStdout``

#. Install Payara Tools plugin in your IDE:

   .. tabs::
     .. group-tab:: Netbeans

       This step is not necessary for Netbeans. The feature is builtin.

     .. group-tab:: IntelliJ

       **Requires IntelliJ Ultimate!**
       (Note that `free educational licenses <https://www.jetbrains.com/community/education/>`_ are available)

       .. image:: img/intellij-payara-plugin-install.png

#. Configure a connection to Payara:

   .. tabs::
     .. group-tab:: Netbeans

        Launch Netbeans and click "Tools" and then "Servers". Click "Add Server" and select "Payara Server" and set the installation location to ``/usr/local/payara6`` (or wherever you unzipped Payara). Choose "Remote Domain". Use the settings in the screenshot below. Most of the defaults are fine.

        Under "Common", the username and password should be "admin". Make sure "Enable Hot Deploy" is checked.

        .. image:: img/netbeans-servers-common.png

        Under "Java", change the debug port to 9009.

        .. image:: img/netbeans-servers-java.png

        Open the project properties (under "File"), navigate to "Compile" and make sure "Compile on Save" is checked.

        .. image:: img/netbeans-compile.png

        Under "Run", under "Server", select "Payara Server". Make sure "Deploy on Save" is checked.

        .. image:: img/netbeans-run.png

     .. group-tab:: IntelliJ
        Create a new running configuration with a "Remote Payara".
        (Open dialog by clicking "Run", then "Edit Configurations")

        .. image:: img/intellij-payara-add-new-config.png

        Click on "Configure" next to "Application Server".
        Add an application server and select unzipped local directory.

        .. image:: img/intellij-payara-config-add-server.png

        Add admin password "admin" and add "building artifact" before launch.
        Make sure to select the WAR, *not* exploded!

        .. image:: img/intellij-payara-config-server.png

        Go to "Deployment" tab and add the Dataverse WAR, *not* exploded!.

        .. image:: img/intellij-payara-config-deployment.png

        Go to "Startup/Connection" tab, select "Debug" and change port to ``9009``.

        .. image:: img/intellij-payara-config-startup.png

        You might want to tweak the hot deploy behavior in the "Server" tab now.
        "Update action" can be found in the run window (see below).
        "Frame deactivation" means switching from IntelliJ window to something else, e.g. your browser.
        *Note: static resources like properties, XHTML etc will only update when redeploying!*

        .. image:: img/intellij-payara-config-server-behaviour.png

#. Start all the containers, but take care to skip application deployment.

   .. tabs::
     .. group-tab:: Maven
        ``mvn -Pct docker:run -Dapp.skipDeploy``

        Run above command in your terminal to start containers in foreground and skip deployment.
        See cheat sheet above for more options.
        Note that this command either assumes you built the :doc:`app-image` first or will download it from Docker Hub.
     .. group-tab:: Compose
        ``SKIP_DEPLOY=1 docker compose -f docker-compose-dev.yml up``

        Run above command in your terminal to start containers in foreground and skip deployment.
        See cheat sheet above for more options.
        Note that this command either assumes you built the :doc:`app-image` first or will download it from Docker Hub.
     .. group-tab:: IntelliJ
        You can create a service configuration to automatically start services for you.

        **IMPORTANT**: This requires installation of the `Docker plugin <https://plugins.jetbrains.com/plugin/7724-docker>`_.

        **NOTE**: You might need to change the Docker Compose executable in your IDE settings to ``docker`` if you have no ``docker-compose`` binary. Start from the ``File`` menu if you are on Linux/Windows or ``IntelliJ IDEA`` on Mac and then go to Settings > Build > Docker > Tools.

        .. image:: img/intellij-compose-add-new-config.png

        Give your configuration a meaningful name, select the compose file to use (in this case the default one), add the environment variable ``SKIP_DEPLOY=1``, and optionally select the services to start.
        You might also want to change other options like attaching to containers to view the logs within the "Services" tab.

        .. image:: img/intellij-compose-setup.png

        Now run the configuration to prepare for deployment and watch it unfold in the "Services" tab.

        .. image:: img/intellij-compose-run.png
        .. image:: img/intellij-compose-services.png

   Note: the Admin Console can be reached at http://localhost:4848 or https://localhost:4949

#. To deploy the application to the running server, use the configured tools to deploy.
   Using the "Run" configuration only deploys and enables redeploys, while running "Debug" enables hot swapping of classes via JDWP.

   .. tabs::
     .. group-tab:: Netbeans

        Click "Debug" then "Debug Project". After some time, Dataverse will be deployed.

        Try making a code change, perhaps to ``Info.java``.

        Click "Debug" and then "Apply Code Changes". If the change was correctly applied, you should see output similar to this:

        .. code-block::

          Classes to reload:
          edu.harvard.iq.dataverse.api.Info

          Code updated

        Check to make sure the change is live by visiting, for example, http://localhost:8080/api/info/version

        See below for a `video <https://www.youtube.com/watch?v=yo3aKOg96f0>`_ demonstrating the steps above but please note that the ports used have changed and now that we have the concept of "skip deploy" the undeployment step shown is no longer necessary.

        .. raw:: html

          <iframe width="560" height="315" src="https://www.youtube.com/embed/yo3aKOg96f0?si=2OCDj-_fmQFBMOLc" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowfullscreen></iframe>

     .. group-tab:: IntelliJ
        Choose "Run" or "Debug" in the toolbar.

        .. image:: img/intellij-payara-run-toolbar.png

        Watch the WAR build and the deployment unfold.
        Note the "Update" action button (see config to change its behavior).

        .. image:: img/intellij-payara-run-output.png

        Manually hotswap classes in "Debug" mode via "Run" > "Debugging Actions" > "Reload Changed Classes".

        .. image:: img/intellij-payara-run-menu-reload.png

Note: in the background, the bootstrap job will wait for Dataverse to be deployed and responsive.
When your IDE automatically opens the URL a newly deployed, not bootstrapped Dataverse application, it might take some more time and page refreshes until the job finishes.

IDE Triggered Non-Code Re-Deployments
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Either redeploy the WAR (see above), use JRebel or look into copying files into the exploded WAR within the running container.
The steps below describe options to enable the later in different IDEs.

.. tabs::
  .. group-tab:: IntelliJ

    This imitates the Netbeans builtin function to copy changes to files under ``src/main/webapp`` into a destination folder.
    It is different in the way that it will copy the files into the running container deployment without using a bind mount.

    1. Install the `File Watchers plugin <https://plugins.jetbrains.com/plugin/7177-file-watchers>`_
    2. Import the :download:`watchers.xml <../../../../docker/util/intellij/watchers.xml>` file at *File > Settings > Tools > File Watchers*
    3. Once you have the deployment running (see above), editing files under ``src/main/webapp`` will be copied into the container after saving the edited file.
       Note: by default, IDE auto-saves will not trigger the copy.
    4. Changes are visible once you reload the browser window.

    **IMPORTANT**: This tool assumes you are using the :ref:`ide-trigger-code-deploy` method to run Dataverse.

    **IMPORTANT**: This tool uses a Bash shell script and is thus limited to Mac and Linux OS.

Exploring the Database
----------------------

See :ref:`db-name-creds` in the Developer Guide.

Using a Debugger
----------------

The :doc:`base-image` enables usage of the `Java Debugging Wire Protocol <https://dzone.com/articles/remote-debugging-java-applications-with-jdwp>`_
for remote debugging if you set ``ENABLE_JDWP=1`` as environment variable for the application container.
The default configuration when executing containers with the commands listed at :ref:`dev-run` already enables this.

There are a lot of tutorials how to connect your IDE's debugger to a remote endpoint. Please use ``localhost:9009``
as the endpoint. Here are links to the most common IDEs docs on remote debugging:
`Eclipse <https://help.eclipse.org/latest/topic/org.eclipse.jdt.doc.user/concepts/cremdbug.htm?cp=1_2_12>`_,
`IntelliJ <https://www.jetbrains.com/help/idea/tutorial-remote-debug.html#debugger_rc>`_

Building Your Own Base Image
----------------------------

If you find yourself tasked with upgrading Payara, you will need to create your own base image before running the :ref:`container-dev-quickstart`. For instructions, see :doc:`base-image`.
