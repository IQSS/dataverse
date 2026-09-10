Reusable Frontend Components
============================

.. contents:: |toctitle|
	:local:

Intro
-----

Some Dataverse features can be served by React components built in
https://github.com/IQSS/dataverse-frontend and embedded directly into the
classic JSF UI. This lets institutions that have not migrated to the
single-page application (SPA) still benefit from new frontend work,
component by component, without replacing the whole UI.

The first components shipped this way are the React file uploader
(DVWebloader v2), gated by the :ref:`dataverse.feature.react-uploader`
feature flag, and the React lazy file tree on the dataset Files tab, gated
by :ref:`dataverse.feature.react-tree-view` and tracked in
`#6691 <https://github.com/IQSS/dataverse/issues/6691>`_. Both bundles are
emitted by the same frontend build and share React, i18n, and vendor chunks.

For the frontend-side contract (the config interface, build pipeline, CSS
isolation, and how to make a new SPA component reusable) see
``docs/reusable-components.md`` in the
`dataverse-frontend <https://github.com/IQSS/dataverse-frontend>`_ repo.

How It Works
------------

Each reusable component is a self-contained ESM bundle plus shared chunks
(React, i18n, vendor, design system) and locale files. A JSF page loads
the bundle with a single ``<script type="module">`` tag and mounts the
React tree on a named ``<div>``:

.. code-block:: html

  <div id="dv-uploader"></div>
  <script>
    window.dvUploaderConfig = {
      siteUrl:    "https://your-dataverse.edu",
      datasetPid: "doi:10.5072/FK2/...",
      locale:     "en"
    };
  </script>
  <script type="module"
    src=".../reusable-components/dv-uploader.js"></script>

Authentication is via session cookie (JSESSIONID). The
:ref:`dataverse.feature.api-session-auth` feature flag must be enabled.
For production deployments, also enable session-cookie API hardening (see
the security notice next to ``dataverse.feature.api-session-auth``).

Hosting the Bundle
------------------

The bundles are not shipped in the Dataverse WAR. Build them from
`dataverse-frontend <https://github.com/IQSS/dataverse-frontend>`_, serve the
build output as static content, and point
:ref:`dataverse.reusable-components.base-url` at it. Until that setting is in
place the feature flags above do nothing and the components are not rendered.

Serving the files from the same origin as Dataverse, for example as a path on
the web server already in front of it, keeps the setup simple and avoids
cross-origin questions for the static assets. It is not required for
authentication: the components call the API from the Dataverse page, so those
requests carry the page's origin no matter where the bundle itself came from.

The build output must keep its internal layout. The entry points
``dv-uploader.js`` and ``dv-tree-view.js`` sit at the root of the served
location, with ``chunks/`` and ``locales/`` beside them. Translations are
fetched at runtime from ``<base-url>/locales/<lang>/<namespace>.json``.

Cache headers are the operator's responsibility. Dataverse does not add a
version parameter to the script URLs, so a deployment that replaces the
bundle in place should serve the entry points with a short max-age, or set a
new base URL per build. The hashed files under ``chunks/`` can be cached
indefinitely.

Configuration
-------------

The relevant settings are documented in the Installation Guide:

- :ref:`dataverse.feature.react-uploader` - turn on the React uploader for
  the JSF dataset edit page.
- :ref:`dataverse.feature.react-tree-view` - turn on the React tree view
  on the JSF dataset Files tab.
- :ref:`dataverse.feature.api-session-auth` - required so the bundle can
  call the API using the user's session cookie.
- :ref:`dataverse.reusable-components.base-url` - where the bundles are
  hosted. Required; the flags above are inert without it.

Cross-references
----------------

- :ref:`feature-flags`
- The component contract on the frontend side:
  https://github.com/IQSS/dataverse-frontend/blob/develop/docs/reusable-components.md
