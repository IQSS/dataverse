ORCiD and Other OAuth2 Identity Providers
=========================================

.. contents:: :local:

Introduction
------------
`OAuth2 <https://oauth.net/2/>`_ is an authentication protocol that allows systems to
share user data, while letting the users control what data is being shared. When you
see buttons stating "login with Google" or "login through Facebook", OAuth2 is probably
involved.

Dataverse supports three OAuth2 providers: `ORCiD <http://orcid.org>`_, `GitHub <github.com>`_,
and `Google <https://console.developers.google.com>`_. Additional providers can be added
with relatively small effort, as long as they play according to the OAuth2 rules. Please send us a pull request if you add one!

Setup
-----

Setting up an OAuth2 identity provider to work with Dataverse requires setup in two places:
the provider, and the Dataverse installation.

Identity Provider Side
~~~~~~~~~~~~~~~~~~~~~~

In this stage, you need to register the Dataverse installation at the provider. Large
providers offer a UI for doing this. Smaller providers, like ORCiD, allow you to
set up the application by email. The provider will require the following items:

- Data about the application, such as name, URL, privacy policy link etc.
- Callback URL. This is where the provider sends the user to after the authentication is complete. For Dataverse, the value should be ``https://{path-to-dataverse-installation}/oauth2/callback.xhtml``. Normally, the provider will allow multiple urls, to allow test sites and local development alongside a production system.
- Scopes, which is a list of strings specifying the data Dataverse will request from the
  provider. These strings are provider-specific:

  - ORCiD
        ``/read-limited``
  - GitHub
        (no scope required)
  - Google
        ``https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email``

At the end of this stage, the provider will give you two values: a *Client ID* and a *Client Secret*.
Keep the secret safe and hidden - in particular, don't push it into a publicly visible code repository, such as GitHub or BitBucket.

Dataverse Side
~~~~~~~~~~~~~~
Adding an OAuth provider to Dataverse is done the usual way, by ``POST``-ing a description of the provider,
in the form of a .json file, to the providers API endpoint. Sample provider files can be found at ``/scripts/api/data/authentication-providers/``.

Registering a new provider, assuming installation is at ``localhost:8080`` and the provider
file is called ``orcid.json``::

   curl -X POST -H"Content-type: application/json"  -d@orcid.json http://localhost:8080/api/admin/authenticationProviders/

When an OAuth2 provider is enabled, Dataverse's Log In screen will list it as a login option below the usual "username/password" panel.

.. warning:: When using OAuth2 in production environments, Dataverse requires the production server's callback URL to be stored in the settings table, under the key ``OAuth2CallbackUrl``. This can be done like so::

      curl -X PUT -dhttp://dataverse.institute.edu/oauth2/callback.xhtml  localhost:8080/api/admin/settings/OAuth2CallbackUrl

  The callback URL has to be identical to one of the callbacks listed at the provider during the previous stage.

Converting Local Users to OAuth
-------------------------------

Once you have enabled at least one OAuth provider, existing users might want to change their login method from local to OAuth to avoid having a Dataverse-specific password. This is documented from the end user perspective in the :doc:`/user/account` section of the User Guide. Users will be prompted to create a new account but can choose to convert an existing local account after confirming their password.

Converting OAuth Users to Local
-------------------------------

Whereas users convert their own accounts from local to OAuth as described above, conversion in the opposite direction is performed by a sysadmin. A common scenario may be as follows:

- A user emails Support saying, "Rather logging in with Google, I want to log in with ORCID (or a local password). What should I do?"
- Support replies asking the user for a new email address to associate with their Dataverse account.
- The user replies with a new email address to associate with their Dataverse account.
- Support runs the curl command below, supplying the database id of the user to convert and the new email address and notes the username returned.
- Support emails the user and indicates that that they should use the password reset feature to set a new password and to make sure to take note of their username under Account Information (or the password reset confirmation email) since the user never had a username before.
- The user resets password and is able to log in with their local account. All permissions have been preserved. The user can continue to log in with this Dataverse-specific password or they can convert to a identity provider, if available.

In the example below, the user has indicated that the new email address they'd like to have associated with their account is "former.oauth.user@mailinator.com" and their user id from the ``authenticateduser`` database table is "42". The API token must belong to a superuser (probably the sysadmin executing the command).

``curl -H "X-Dataverse-key: $API_TOKEN" -X PUT -d "former.oauth.user@mailinator.com" http://localhost:8080/api/admin/authenticatedUsers/id/42/convertRemoteToBuiltIn``

The expected output is something like this::

    {
      "status": "OK",
      "data": {
        "email": "former.oauth.user@mailinator.com",
        "username": "jdoe"
      }
    }

Rather than looking up the user's id in the ``authenticateduser`` database table, you can issue this command to get a listing of all users:

``curl -H "X-Dataverse-key: $API_TOKEN" http://localhost:8080/api/admin/authenticatedUsers``

Per above, you now need to tell the user to use the password reset feature to set a password for their local account.
