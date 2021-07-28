====================
Shibboleth and OAuth
====================

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

Now when you go to http://localhost:8080/oauth2/firstLogin.xhtml you should be prompted to create a Shibboleth account.

----

Previous: :doc:`unf/index` | Next: :doc:`geospatial`
