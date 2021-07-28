OAuth Login Options
===================

.. contents:: |toctitle|
	:local:

Introduction
------------

As explained under "Auth Modes" in the :doc:`config` section, OAuth2 is one of the ways that you can have end users log in to your Dataverse installation.

`OAuth2 <https://oauth.net/2/>`_ is an authentication protocol that allows systems to share user data, while letting the users control what data is being shared. When you see buttons stating "login with Google" or "login through Facebook", OAuth2 is probably involved. For the purposes of this section, we will shorten "OAuth2" to just "OAuth." OAuth can be compared and contrasted with :doc:`shibboleth`.

The Dataverse Software supports four OAuth providers: `ORCID <http://orcid.org>`_, `Microsoft Azure Active Directory (AD) <https://docs.microsoft.com/azure/active-directory/>`_, `GitHub <https://github.com>`_, and `Google <https://console.developers.google.com>`_.

In addition :doc:`oidc` are supported, using a standard based on OAuth2.

Setup
-----

Setting up an OAuth identity provider to work with your Dataverse installation requires setup in two places: the provider, and the Dataverse installation.

Identity Provider Side
~~~~~~~~~~~~~~~~~~~~~~

Obtain Client ID and Client Secret 
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Before OAuth providers will release information about their users (first name, last name, etc.) to your Dataverse installation, you must request a "Client ID" and "Client Secret" from them. In many cases you can use providers' automated system to request these credentials, but if not, contact the provider for assistance.

URLs to help you request a Client ID and Client Secret from the providers supported by the Dataverse Software are provided below.  For all of these providers, it's a good idea to request the Client ID and Client secret using a generic account, perhaps the one that's associated with the ``:SystemEmail`` you've configured for your Dataverse installation, rather than your own personal Microsoft Azure AD, ORCID, GitHub, or Google account:

- ORCID: https://orcid.org/content/register-client-application-0
- Microsoft: https://docs.microsoft.com/en-us/azure/active-directory/develop/v1-protocols-oauth-code
- GitHub: https://github.com/settings/applications/new via https://developer.github.com/v3/oauth/
- Google: https://console.developers.google.com/projectselector/apis/credentials via https://developers.google.com/identity/protocols/OAuth2WebServer (pick "OAuth client ID")

Each of these providers will require the following information from you:

- Basic information about your Dataverse installation such as a name, description, URL, logo, privacy policy, etc.
- OAuth2 Redirect URI (ORCID) or Redirect URI (Microsoft Azure AD) or Authorization Callback URL (GitHub) or Authorized Redirect URIs (Google): This is the URL on the Dataverse installation side to which the user will be sent after successfully authenticating with the identity provider. This should be the advertised URL of your Dataverse installation (the protocol, fully qualified domain name, and optional port configured via the ``dataverse.siteUrl`` JVM option mentioned in the :doc:`config` section) appended with ``/oauth2/callback.xhtml`` such as ``https://dataverse.example.edu/oauth2/callback.xhtml``.

When you are finished you should have a Client ID and Client Secret from the provider. Keep them safe and secret.

Dataverse Installation Side
~~~~~~~~~~~~~~~~~~~~~~~~~~~

As explained under "Auth Modes" in the :doc:`config` section, available authentication providers are stored in the ``authenticationproviderrow`` database table and can be listed with this command:

``curl http://localhost:8080/api/admin/authenticationProviders``

We will ``POST`` a JSON file containing the Client ID and Client Secret to this ``authenticationProviders`` API endpoint to add another authentication provider. As a starting point, you'll want to download the JSON template file matching the provider you're setting up:

- :download:`orcid-public.json <../_static/installation/files/root/auth-providers/orcid-public.json>`
- :download:`orcid-member.json <../_static/installation/files/root/auth-providers/orcid-member.json>`
- :download:`github.json <../_static/installation/files/root/auth-providers/github.json>`
- :download:`google.json <../_static/installation/files/root/auth-providers/google.json>`
- :download:`microsoft.json <../_static/installation/files/root/auth-providers/microsoft.json>`

Here's how the JSON template for GitHub looks, for example:

.. literalinclude:: ../_static/installation/files/root/auth-providers/github.json
   :language: json

Edit the JSON template and replace the two "FIXME" values with the Client ID and Client Secret you obtained earlier. Then use curl to ``POST`` the JSON to the Dataverse installation:

``curl -X POST -H 'Content-type: application/json' --upload-file github.json http://localhost:8080/api/admin/authenticationProviders``

After restarting your app server you should see the new provider under "Other options" on the Log In page, as described in the :doc:`/user/account` section of the User Guide.

By default, the Log In page will show the "builtin" provider, but you can adjust this via the ``:DefaultAuthProvider`` configuration option. For details, see :doc:`config`.

ORCID Sandbox
^^^^^^^^^^^^^

ORCID provides a sandbox registry, which may be useful for staging, or for development installations.
This template can be used for configuring this setting (**this is not something you should use in a production environment**):

- :download:`orcid-sandbox.json <../_static/installation/files/root/auth-providers/orcid-sandbox.json>`

Converting Local Users to OAuth
-------------------------------

Once you have enabled at least one OAuth provider, existing users might want to change their login method from local to OAuth to avoid having a Dataverse installation-specific password. This is documented from the end user perspective in the :doc:`/user/account` section of the User Guide. Users will be prompted to create a new account but can choose to convert an existing local account after confirming their password.

.. _converting-oauth-users-to-local:

Converting OAuth Users to Local
-------------------------------

Whereas users convert their own accounts from local to OAuth as described above, conversion in the opposite direction is performed by a sysadmin. A common scenario may be as follows:

- A user emails Support saying, "Rather than logging in with Google, I want to log in with ORCID (or a local password). What should I do?"
- Support replies asking the user for a new email address to associate with their Dataverse installation account.
- The user replies with a new email address to associate with their Dataverse installation account.
- Support runs the curl command below, supplying the database id of the user to convert and the new email address and notes the username returned.
- Support emails the user and indicates that they should use the password reset feature to set a new password and to make sure to take note of their username under Account Information (or the password reset confirmation email) since the user never had a username before.
- The user resets password and is able to log in with their local account. All permissions have been preserved. The user can continue to log in with this Dataverse installation-specific password or they can convert to an identity provider, if available.

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
