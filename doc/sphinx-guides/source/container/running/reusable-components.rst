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

For the frontend-side contract — the config interface, build pipeline, CSS
isolation, and how to make a new SPA component reusable — see
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

The pre-built bundle ships inside the Dataverse WAR at
``webapp/dvwebloader/reusable-components/`` and is served same-origin via
the default :ref:`dataverse.reusable-components.base-url` of
``/dvwebloader``. Out of the box no extra setup is required to enable the
feature flags above.

Operators who prefer to host the bundle off the WAR — for example behind
a separate static-file server, an existing nginx, or a CDN — can copy the
contents of ``webapp/dvwebloader/reusable-components/`` to that location
and override :ref:`dataverse.reusable-components.base-url` to point at it.
There is no published artifact (npm package, Docker image, etc.) for the
bundle today; rehosting it is the operator's responsibility.

Configuration
-------------

The relevant settings are documented in the Installation Guide:

- :ref:`dataverse.feature.react-uploader` — turn on the React uploader for
  the JSF dataset edit page.
- :ref:`dataverse.feature.react-tree-view` — turn on the React tree view
  on the JSF dataset Files tab.
- :ref:`dataverse.feature.api-session-auth` — required so the bundle can
  call the API using the user's session cookie.
- :ref:`dataverse.reusable-components.base-url` — where the JSF page should
  load the bundle from.

Cross-references
----------------

- :ref:`feature-flags`
- The component contract on the frontend side:
  https://github.com/IQSS/dataverse-frontend/blob/develop/docs/reusable-components.md
