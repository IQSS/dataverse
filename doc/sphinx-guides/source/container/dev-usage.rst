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

Building and Running
--------------------

To build the application image, run the following command, as described in :doc:`app-image`:

``mvn -Pct clean package``

Now, start all the containers with a single command:

``mvn -Pct docker:run``

(You could also concatenate both commands into one.)

Once all containers have been started, you can check if the application was deployed correctly by checking the version
at http://localhost:8080/api/info/version.

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
