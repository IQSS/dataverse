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
