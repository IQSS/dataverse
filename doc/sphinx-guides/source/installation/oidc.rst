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
- `Microsoft Azure AD <https://learn.microsoft.com/en-us/azure/active-directory/develop/v2-protocols-oidc>`_
- `Yahoo <https://developer.yahoo.com/oauth2/guide/openid_connect>`_
- ORCID `announced support <https://orcid.org/blog/2019/04/17/orcid-openid-connect-and-implicit-authentication>`_

You can also either host an OpenID Connect identity management on your own or use a customizable hosted service:

- `Okta <https://developer.okta.com/docs/reference/api/oidc/>`_ is a hosted solution
- `Keycloak <https://www.keycloak.org>`_ is an open source solution for an IDM/IAM
- `Unity IDM <https://www.unity-idm.eu>`_ is another open source IDM/IAM solution

Other Use Cases and Combinations
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

How to Use
----------

Just like with :doc:`oauth2` you need to obtain a *Client ID* and a *Client Secret* from your provider(s).

.. note::
  The Dataverse Software does not support `OpenID Connect Dynamic Registration <https://openid.net/specs/openid-connect-registration-1_0.html>`_.
  You need to apply for credentials out-of-band.

The Dataverse installation will discover all necessary metadata for a given provider on its own (this is `part of the standard
<https://openid.net/specs/openid-connect-discovery-1_0.html>`_).

To enable this, you need to specify an *Issuer URL* when creating the configuration for your provider (see below).

Finding the issuer URL is best done by searching for terms like "discovery" in the documentation of your provider.
The discovery document is always located at ``<issuer url>/.well-known/openid-configuration`` (standardized).
To be sure, you can always lookup the ``issuer`` value inside the live JSON-based discovery document.

Note if you work with Keycloak, make sure the base URL is in the following format: ``https://host:port/realms/{realm}``
where ``{realm}`` has to be replaced by the name of the Keycloak realm.

After adding a provider, the Log In page will by default show the "builtin" provider, but you can adjust this via the
``:DefaultAuthProvider`` configuration option. For details, see :doc:`config`.

.. hint::
   In contrast to our :doc:`oauth2`, you can use multiple providers by creating distinct configurations enabled by
   the same technology and without modifying the Dataverse Software code base (standards for the win!).


.. _oidc-pkce:

Enabling PKCE Security
^^^^^^^^^^^^^^^^^^^^^^

Many providers these days support or even require the usage of `PKCE <https://oauth.net/2/pkce/>`_ to safeguard against
some attacks and enable public clients that cannot have a secure secret to still use OpenID Connect (or OAuth2).

The Dataverse-built OIDC client can be configured to use PKCE and the method to use when creating the code challenge can be specified.
See also `this explanation of the flow <https://auth0.com/docs/get-started/authentication-and-authorization-flow/authorization-code-flow-with-proof-key-for-code-exchange-pkce>`_
for details on how this works.

As we are using the `Nimbus SDK <https://connect2id.com/products/nimbus-oauth-openid-connect-sdk>`_ as our client
library, we support the standard ``PLAIN`` and ``S256`` (SHA-256) code challenge methods. "SHA-256 method" is the default
as recommend in `RFC7636 <https://datatracker.ietf.org/doc/html/rfc7636#section-4.2>`_. If your provider needs some
other method, please open an issue.

The provisioning sections below contain in the example the parameters you may use to configure PKCE.

Provision a Provider
--------------------

Depending on your use case, you can choose different ways to setup one or multiple OIDC identity providers.

Using :ref:`jvm-options` has the advantage of being consistent and does not require additional calls to the API.
It can only configure one provider though, yet you can mix with other provider definitions via API.

Using the REST API has the advantage of provisioning multiple, different OIDC providers.
Depending on your use case, it has the drawback of needing additional API calls.

If you only need one single provider in your installation and it is using OIDC, use the JVM options, as it
requires fewer extra steps and allows you to keep more configuration in a single source.

Provision via REST API
^^^^^^^^^^^^^^^^^^^^^^

Note: you may omit the PKCE related settings from ``factoryData`` below if you don't plan on using PKCE - default is
disabled.

Please create a :download:`my-oidc-provider.json <../_static/installation/files/root/auth-providers/oidc.json>` file, replacing every ``<...>`` with your values:

.. literalinclude:: /_static/installation/files/root/auth-providers/oidc.json
    :name: oidc-provider-example
    :language: json

Now load the configuration into your Dataverse installation using the same API as with :doc:`oauth2`:

``curl -X POST -H 'Content-type: application/json' --upload-file my-oidc-provider.json http://localhost:8080/api/admin/authenticationProviders``

The Dataverse installation will automatically try to load the provider and retrieve the metadata. Watch the app server log for errors.
You should see the new provider under "Other options" on the Log In page, as described in the :doc:`/user/account`
section of the User Guide.

.. _oidc-mpconfig:

Provision via JVM Options
^^^^^^^^^^^^^^^^^^^^^^^^^

A single provider may be provisioned using :ref:`jvm-options`.
It may be accompanied by more providers configured via REST API.
Note that this provider will only be deployed at startup time and (currently) cannot be reconfigured without a restart.

All options below may be set via *MicroProfile Config API* sources. Examples: use environment variable
``DATAVERSE_AUTH_OIDC_ENABLED`` for the ``dataverse.auth.oidc.enabled`` option or ``DATAVERSE_AUTH_OIDC_CLIENT_ID``
for the ``dataverse.auth.oidc.client-id`` option.

The following options are available:

.. list-table::
  :widths: 25 55 10 10
  :header-rows: 1
  :align: left

  * - Option
    - Description
    - Mandatory
    - Default
  * - ``dataverse.auth.oidc.enabled``
    - Enable or disable provisioning the provider via MicroProfile.
    - N
    - ``false``
  * - ``dataverse.auth.oidc.hidden-jsf``
    - Show or Hide the provider from the JSF UI via MicroProfile.
    - N
    - ``false``
  * - ``dataverse.auth.oidc.client-id``
    - The client-id of the application to identify it at your provider.
    - Y
    - \-
  * - ``dataverse.auth.oidc.client-secret``
    - A confidential secret to authorize application requests to the provider as legit.
    - N
    - \-
  * - ``dataverse.auth.oidc.auth-server-url``
    - The base URL of the OpenID Connect (OIDC) server as explained above.
    - Y
    - \-
  * - ``dataverse.auth.oidc.pkce.enabled``
    - Set to ``true`` to enable :ref:`PKCE <oidc-pkce>` in auth flow.
    - N
    - ``false``
  * - ``dataverse.auth.oidc.pkce.method``
    - Set code challenge method. The default value is the current best practice in the literature.
    - N
    - ``S256``
  * - ``dataverse.auth.oidc.title``
    - The UI visible name for this provider in login options.
    - N
    - ``OpenID Connect``
  * - ``dataverse.auth.oidc.subtitle``
    - A subtitle, currently not displayed by the UI.
    - N
    - ``OpenID Connect``
  * - ``dataverse.auth.oidc.pkce.max-cache-size``
    - Tune the maximum size of all OIDC providers' verifier cache (the number of outstanding PKCE-enabled auth responses).
    - N
    - 10000
  * - ``dataverse.auth.oidc.pkce.max-cache-age``
    - Tune the maximum age, in seconds, of all OIDC providers' verifier cache entries. Default is 5 minutes, equivalent to lifetime
      of many OIDC access tokens.
    - N
    - 300

.. _oidc-group-sync:

Synchronizing Groups From the Provider
--------------------------------------

Dataverse can keep a user's authorizations in step with the groups they hold on the OIDC
provider, so that the provider stays the single source of truth for who may do what. The
synchronization runs on every login, before the user is placed in the session, so a change
of role is already in force for the login that follows it.

This feature expects the provider's group tree to be laid out as follows, where
``<parent>``, ``<tenants>`` and the role names are configurable:

.. code-block:: none

  /<parent>/admins                            -> Dataverse superuser
  /<parent>/<tenants>/<tenant>/admins         -> "admin" role on the tenant's collection
  /<parent>/<tenants>/<tenant>/curators       -> "curator" role on the tenant's collection
  /<parent>/<tenants>/<tenant>/users          -> "member" role on the tenant's collection

Each ``<tenant>`` group must carry an attribute (``dataverse-alias`` by default) holding the
alias of the Dataverse collection it maps to. Groups that do not fit this shape are ignored.

Rather than granting roles to users one by one, Dataverse mirrors each tenant role into an
explicit group owned by the target collection, and grants the role to that group once. A
login then only adds or removes the user from those groups, which needs no permission
reindex and takes effect on the user's next request. Groups and role assignments created by
hand are never touched: only groups carrying the configured prefix are managed.

Requirements on the provider:

- A **group membership mapper** on the Dataverse client, emitting full group paths into a
  claim (``groups`` by default). Dataverse reads the userinfo endpoint, so the mapper's
  **Add to userinfo** setting must be on; adding it to the tokens alone is not enough.
- A **service account client** (``client_credentials`` grant) that Dataverse uses to read
  group attributes, which the provider does not put into claims. On Keycloak the account
  needs the ``view-users`` and ``query-groups`` realm-management roles, and nothing more.

If the provider cannot be reached, or the group claim is absent, nothing is changed and a
warning is logged. A missing claim is never treated as "member of no group", so a
misconfigured mapper cannot silently strip everyone's permissions.

.. list-table::
  :widths: 25 55 10 10
  :header-rows: 1
  :align: left

  * - Option
    - Description
    - Mandatory
    - Default
  * - ``dataverse.auth.oidc.sync.enabled``
    - Enable synchronizing Dataverse authorizations from the provider's groups.
    - N
    - ``false``
  * - ``dataverse.auth.oidc.sync.client-id``
    - Client id of the service account used to read group attributes.
    - Y
    - \-
  * - ``dataverse.auth.oidc.sync.client-secret``
    - Client secret of that service account.
    - Y
    - \-
  * - ``dataverse.auth.oidc.sync.server-url``
    - Base URL of the provider, without the realm. Derived from ``auth-server-url`` when omitted.
    - N
    - \-
  * - ``dataverse.auth.oidc.sync.realm``
    - Realm name. Derived from ``auth-server-url`` when omitted.
    - N
    - \-
  * - ``dataverse.auth.oidc.sync.groups-claim``
    - Name of the claim carrying the full group paths.
    - N
    - ``groups``
  * - ``dataverse.auth.oidc.sync.parent-group``
    - Name of the top-level group holding the platform's groups.
    - N
    - ``platica``
  * - ``dataverse.auth.oidc.sync.tenants-group``
    - Name of the group, under the parent, holding one subgroup per tenant.
    - N
    - ``tenant-users``
  * - ``dataverse.auth.oidc.sync.superuser-group``
    - Name of the group, under the parent, whose members become Dataverse superusers.
    - N
    - ``admins``
  * - ``dataverse.auth.oidc.sync.alias-attribute``
    - Tenant group attribute holding the alias of the Dataverse collection it maps to.
    - N
    - ``dataverse-alias``
  * - ``dataverse.auth.oidc.sync.group-prefix``
    - Prefix of the explicit groups Dataverse manages. Groups without it are never modified.
    - N
    - ``kc``
  * - ``dataverse.auth.oidc.sync.role-admin``
    - Alias of the Dataverse role granted to a tenant's ``admins``.
    - N
    - ``admin``
  * - ``dataverse.auth.oidc.sync.role-curator``
    - Alias of the Dataverse role granted to a tenant's ``curators``.
    - N
    - ``curator``
  * - ``dataverse.auth.oidc.sync.role-user``
    - Alias of the Dataverse role granted to a tenant's ``users``.
    - N
    - ``member``
  * - ``dataverse.auth.oidc.sync.protected-users``
    - Comma-separated user identifiers that never lose superuser status, whatever the provider says.
    - N
    - ``dataverseAdmin``
  * - ``dataverse.auth.oidc.sync.cache-max-age``
    - Maximum age, in seconds, of cached group attributes.
    - N
    - 300
  * - ``dataverse.auth.oidc.sync.interval-minutes``
    - How often the reconciliation sweep runs. Set to ``0`` to sync at login only.
    - N
    - 15
  * - ``dataverse.auth.oidc.sync.provider-id``
    - Id of the authentication provider whose accounts the sweep matches against.
    - N
    - ``oidc``
  * - ``dataverse.auth.oidc.sync.min-removals``
    - A sweep may always remove at least this many memberships, whatever the ratio says.
    - N
    - 5
  * - ``dataverse.auth.oidc.sync.max-removal-ratio``
    - Fraction of existing memberships a single sweep may remove before it aborts.
    - N
    - 0.2

Reconciliation Sweep
~~~~~~~~~~~~~~~~~~~~

Synchronizing at login only ever sees the person walking through the door. A user demoted on
the provider who never logs in again would keep their authorizations indefinitely, so a timer
periodically reconciles every tenant. Its interval is the installation's worst-case delay for
a permission change to take effect.

The sweep reads everything from the provider before writing anything, and skips -- leaving
membership untouched -- any group it could not read. A failed read must never look like an
empty group.

It also refuses to write when the change looks like a wipe: if a single sweep would remove
more memberships than ``max-removal-ratio`` of the current total (with ``min-removals`` as an
absolute floor), it logs the intended change at ``SEVERE`` and writes nothing. This is what
stops a renamed group or a mistyped path from stripping everyone's permissions in one pass.
Raising the thresholds is the deliberate way to push a large legitimate change through.

Each group is written in its own transaction, so one unreachable tenant leaves the others
reconciled rather than aborting the whole sweep.

In a cluster the sweep only runs on the node started with ``-Ddataverse.timerServer=true``,
the same rule the harvesting and saved-search timers follow.

To run it immediately -- when testing a configuration change, or to force a revocation through
without waiting out the interval:

.. code-block:: bash

  curl -X POST -H "X-Dataverse-key:$API_TOKEN" "$SERVER_URL/api/admin/oidc/sync"

Users who exist on the provider but have never logged into Dataverse have no account yet, so
the sweep cannot add them to anything. Their first login handles them.

.. warning::

  Revoking superuser status takes effect immediately only for the user logging in. Someone
  who is already logged in keeps the flag until their session ends, because it is read from
  the user object held in the session. Group-based roles do not have this problem: they are
  resolved per request.
