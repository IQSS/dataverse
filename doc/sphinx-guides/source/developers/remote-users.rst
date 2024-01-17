==========================
Shibboleth, OAuth and OIDC
==========================

.. contents:: |toctitle|
	:local:

Shibboleth and OAuth
--------------------

If you are working on anything related to users, please keep in mind that your changes will likely affect Shibboleth and OAuth users. For some background on user accounts in the Dataverse Software, see :ref:`auth-modes` section of Configuration in the Installation Guide.

Rather than setting up Shibboleth on your laptop, developers are advised to add the Shibboleth auth provider (see "Add the Shibboleth Authentication Provider to Your Dataverse Installation" at :doc:`/installation/shibboleth`) and add a value to their database to enable Shibboleth "dev mode" like this:

``curl http://localhost:8080/api/admin/settings/:DebugShibAccountType -X PUT -d RANDOM``

For a list of possible values, please "find usages" on the settings key above and look at the enum.

Now when you go to http://localhost:8080/shib.xhtml you should be prompted to create a Shibboleth account.

OAuth is much more straightforward to get working on your laptop than Shibboleth. GitHub is a good identity provider to test with because you can easily request a Client ID and Client Secret that works against localhost. Follow the instructions in the :doc:`/installation/oauth2` section of the installation Guide and use "http://localhost:8080/oauth2/callback.xhtml" as the callback URL.

In addition to setting up OAuth on your laptop for real per above, you can also use a dev/debug mode:

``curl http://localhost:8080/api/admin/settings/:DebugOAuthAccountType -X PUT -d RANDOM_EMAIL2``

For a list of possible values, please "find usages" on the settings key above and look at the enum.

Now when you go to http://localhost:8080/oauth2/firstLogin.xhtml you should be prompted to create an OAuth account.

----

.. _oidc-dev:

OpenID Connect (OIDC)
---------------------

STOP! ``oidc-keycloak-auth-provider.json`` was changed from http://localhost:8090 to http://keycloak.mydomain.com:8090 to test :ref:`bearer-tokens`. In addition, ``docker-compose-dev.yml`` in the root of the repo was updated to start up Keycloak. To use these, you should add ``127.0.0.1 keycloak.mydomain.com`` to your ``/etc/hosts file``. If you'd like to use the docker compose as described below (``conf/keycloak/docker-compose.yml``), you should revert the change to ``oidc-keycloak-auth-provider.json``.

If you are working on the OpenID Connect (OIDC) user authentication flow, you do not need to connect to a remote provider (as explained in :doc:`/installation/oidc`) to test this feature. Instead, you can use the available configuration that allows you to run a test Keycloak OIDC identity management service locally through a Docker container.

(Please note! The client secret (``94XHrfNRwXsjqTqApRrwWmhDLDHpIYV8``) is hard-coded in ``test-realm.json`` and ``oidc-keycloak-auth-provider.json``. Do not use this config in production! This is only for developers.)

You can find this configuration in ``conf/keycloak``. There are two options available in this directory to run a Keycloak container: bash script or docker-compose.

To run the container via bash script, execute the following command (positioned in ``conf/keycloak``):

``./run-keycloak.sh``

The script will create a Keycloak container or restart it if the container was already created and stopped. Once the script is executed, Keycloak should be accessible from http://localhost:8090/

Now load the configuration defined in ``oidc-keycloak-auth-provider.json`` into your Dataverse installation to enable Keycloak as an authentication provider.

``curl -X POST -H 'Content-type: application/json' --upload-file oidc-keycloak-auth-provider.json http://localhost:8080/api/admin/authenticationProviders``

You should see the new provider, called "OIDC-Keycloak", under "Other options" on the Log In page.

You should be able to log into Keycloak with the one of the following credentials:

.. list-table::

  * - Username
    - Password
  * - admin
    - admin
  * - curator
    - curator
  * - user
    - user
  * - affiliate
    - affiliate

In case you want to stop and remove the Keycloak container, just run the other available bash script:

``./rm-keycloak.sh``

Note: the Keycloak admin to login at the admin console is ``kcadmin:kcpassword``

----

Previous: :doc:`unf/index` | Next: :doc:`geospatial`
