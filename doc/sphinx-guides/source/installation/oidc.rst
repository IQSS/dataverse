OpenID Connect Login Options
============================

.. contents:: |toctitle|
	:local:

Introduction
------------

The `OpenID Connect <https://openid.net/connect/>`_ (or OIDC) standard support is closely related to our :doc:`oauth2`,
as it has been based on the `OAuth 2.0 <https://oauth.net/2/>`_ standard. Quick summary: OIDC is using OAuth 2.0, but
adds a standardized way how authentication is done, while this is up to providers when using OAuth 2.0 for authentication.

Being a standard, you can easily enable the use of any OpenID connect compliant provider out there for login into your Dataverse installation.

Some prominent provider examples:

- `Google <https://developers.google.com/identity/protocols/OpenIDConnect>`_
- `Microsoft Azure AD <https://docs.microsoft.com/de-de/azure/active-directory/develop/v2-protocols-oidc>`_
- `Yahoo <https://developer.yahoo.com/oauth2/guide/openid_connect>`_
- ORCID `announced support <https://orcid.org/blog/2019/04/17/orcid-openid-connect-and-implicit-authentication>`_

You can also either host an OpenID Connect identity management on your own or use a customizable hosted service:

- `Okta <https://developer.okta.com/docs/reference/api/oidc/>`_ is a hosted solution
- `Keycloak <https://www.keycloak.org>`_ is an open source solution for an IDM/IAM
- `Unity IDM <https://www.unity-idm.eu>`_ is another open source IDM/IAM solution

Other use cases and combinations
--------------------------------

- Using your custom identity management solution might be a workaround when you seek for LDAP support, but
  don't want to go for services like Microsoft Azure AD et al.
- You want to enable users to login in multiple different ways but appear as one account to the Dataverse installation. This is
  currently not possible within the Dataverse Software itself, but hosting an IDM and attaching the Dataverse installation solves it.
- You want to use the `eduGain Federation <https://edugain.org>`_ or other well known SAML federations, but don't want
  to deploy Shibboleth as your service provider. Using an IDM solution in front easily allows you to use them
  without hassle.
- There's also a `Shibboleth IdP (not SP!) extension <https://github.com/CSCfi/shibboleth-idp-oidc-extension>`_,
  so if you already have a Shibboleth identity provider at your institution, you can reuse it more easily with your Dataverse installation.
- In the future, OpenID Connect might become a successor to the large scale R&E SAML federations we have nowadays.
  See also `OpenID Connect Federation Standard <https://openid.net/specs/openid-connect-federation-1_0.html>`_ (in development)

How to use
----------

Just like with :doc:`oauth2` you need to obtain a *Client ID* and a *Client Secret* from your provider(s).

.. note::
  The Dataverse Software does not support `OpenID Connect Dynamic Registration <https://openid.net/specs/openid-connect-registration-1_0.html>`_.
  You need to apply for credentials out-of-band.

The Dataverse installation will discover all necessary metadata for a given provider on its own (this is `part of the standard
<http://openid.net/specs/openid-connect-discovery-1_0.html>`_).

To enable this, you need to specify an *Issuer URL* when creating the configuration for your provider (see below).

Finding the issuer URL is best done by searching for terms like "discovery" in the documentation of your provider.
The discovery document is always located at ``<issuer url>/.well-known/openid-configuration`` (standardized).
To be sure, you can always lookup the ``issuer`` value inside the live JSON-based discovery document.

Please create a my-oidc-provider.json file like this, replacing every ``<...>`` with your values:

.. code-block:: json

    {
        "id":"<a unique id>",
        "factoryAlias":"oidc",
        "title":"<a title - shown in UI>",
        "subtitle":"<a subtitle - currently unused in UI>",
        "factoryData":"type: oidc | issuer: <issuer url> | clientId: <client id> | clientSecret: <client secret>",
        "enabled":true
    }

Now load the configuration into your Dataverse installation using the same API as with :doc:`oauth2`:

``curl -X POST -H 'Content-type: application/json' --upload-file my-oidc-provider.json http://localhost:8080/api/admin/authenticationProviders``

The Dataverse installation will automatically try to load the provider and retrieve the metadata. Watch the app server log for errors.
You should see the new provider under "Other options" on the Log In page, as described in the :doc:`/user/account`
section of the User Guide.

By default, the Log In page will show the "builtin" provider, but you can adjust this via the ``:DefaultAuthProvider``
configuration option. For details, see :doc:`config`.

.. hint::
   In contrast to our :doc:`oauth2`, you can use multiple providers by creating distinct configurations enabled by
   the same technology and without modifying the Dataverse Software code base (standards for the win!).
