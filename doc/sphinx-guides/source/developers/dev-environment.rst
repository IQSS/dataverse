=======================
Development Environment
=======================

These instructions are oriented around Docker but the "classic" instructions we used for Dataverse 4 and 5 are still available at :doc:`classic-dev-env`.

.. contents:: |toctitle|
	:local:

.. _container-dev-quickstart:

Quickstart
----------

First, install Java 21, Maven, and Docker or Podman.

After cloning the `dataverse repo <https://github.com/IQSS/dataverse>`_, run this:

``mvn -Pct clean package docker:run``

(Note that if you are Windows, you must run the command above in `WSL <https://learn.microsoft.com/windows/wsl>`_ rather than cmd.exe. See :doc:`windows`.)

If using Podman, assuming ``podman.sock`` is located at ``/run/user/1000/podman/podman.sock``
and a symlink ``docker`` pointing to ``podman`` executable exists, run this:

``DOCKER_HOST=unix:///run/user/1000/podman/podman.sock mvn -Pct clean package docker:run``

After some time you should be able to log in:

- url: http://localhost:8080
- username: dataverseAdmin
- password: admin1

Detailed Steps
--------------

Install Java
~~~~~~~~~~~~

The recommended version is Java 21 because it's the version we test with. See https://github.com/IQSS/dataverse/pull/12043.

On Mac and Windows, we suggest using `SDKMAN <https://sdkman.io>`_ to install Temurin (Eclipe's name for its OpenJDK distribution). Type ``sdk install java 21`` and then hit the "tab" key until you get to a version that ends with ``-tem`` and then hit enter. If you don't set this version as the default, you will need to type ``sdk use java 21`` and then hit the tab to autocomplete the version.

Alternatively you can download Temurin from https://adoptium.net (formerly `AdoptOpenJDK <https://adoptopenjdk.net>`_).

On Linux, you are welcome to use the OpenJDK available from package managers.

Install Maven
~~~~~~~~~~~~~

If you are using SKDMAN, run this command:

``sdk install maven``

Otherwise, follow instructions at https://maven.apache.org.

Install and Start Docker/Podman
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Follow instructions at https://www.docker.com

Be sure to start Docker.

For Podman, follow instructions at https://podman.io/docs/installation. Once Podman is
installed, ensure to start Podman socket service. This can be verified by running
``podman info`` command in the output look for the following output:

.. code-block:: yaml

	remoteSocket:
	  exists: true
	  path: /run/user/1000/podman/podman.sock

In case if exists is ``false``, start the socket using command ``systemctl --user start podman.socket``.
To ensure the socket service starts automatically on system boot, run ``systemctl --user enable podman.socket``.
Finally, create a symlink named ``docker`` pointing to ``podman`` executable as ``ln -s /usr/bin/podman /usr/bin/docker``. 

Git Clone Repo
~~~~~~~~~~~~~~

Fork https://github.com/IQSS/dataverse and then clone your fork like this:

``git clone git@github.com:[YOUR GITHUB USERNAME]/dataverse.git``

Build and Run
~~~~~~~~~~~~~

Change into the ``dataverse`` directory you just cloned and run the following command:

``mvn -Pct clean package docker:run``

In the case of Podman, if ``podman.sock`` is running at ``/run/user/1000/podman/podman.sock``, the
command becomes:

``DOCKER_HOST=unix:///run/user/1000/podman/podman.sock mvn -Pct clean package docker:run``

Verify 
~~~~~~

After some time you should be able to log in:

- url: http://localhost:8080
- username: dataverseAdmin
- password: admin1

Next Steps
----------

See the :doc:`/container/dev-usage` section of the Container Guide for tips on fast redeployment, viewing logs, and more.

Getting Help
------------

Please feel free to reach out at https://chat.dataverse.org or https://groups.google.com/g/dataverse-dev if you have any difficulty setting up a dev environment!
