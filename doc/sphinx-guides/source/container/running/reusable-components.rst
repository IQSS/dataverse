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

The first component shipped this way is the React file uploader (DVWebloader
v2), gated by the :ref:`dataverse.feature.react-uploader` feature flag. The
second is the React lazy file tree on the dataset Files tab, gated by
:ref:`dataverse.feature.react-tree-view` and tracked in
`#6691 <https://github.com/IQSS/dataverse/issues/6691>`_. Both bundles are
emitted by the same frontend build and share React, i18n, and vendor chunks.

For the frontend-side contract — the config interface, build pipeline, CSS
isolation, and how to make a new SPA component reusable — see
``docs/reusable-components.md`` in the
`dataverse-frontend <https://github.com/IQSS/dataverse-frontend>`_ repo.

How It Works
------------

Each reusable component is published from ``dataverse-frontend`` as part of
an npm package. The package contains a self-contained ESM bundle plus
shared chunks (React, i18n, vendor, design system) and locale files. A
JSF page loads the bundle with a single ``<script type="module">`` tag and
mounts the React tree on a named ``<div>``:

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

Where to Get the Bundle
-----------------------

The npm package is published two places:

- **GitHub Packages** for prereleases (per pull request and merge to develop).
  Versions look like ``1.4.0-pr898.<sha>``.
- **npmjs.org** for tagged releases. Versions are plain semver, e.g.
  ``1.4.0``.

Operators have three reasonable ways to make the bundle reachable from a
browser:

1. **Run the** ``gdcc/dataverse-reusable-components`` **container image**
   alongside Dataverse (recommended for institutions that already use Docker
   Compose). The image is a small nginx that serves the contents of the
   matching npm package version. This mirrors the pattern used for the
   :ref:`file-previewers-ct` (``previewers-provider``) sidecar.
2. **Pull the npm package and serve it from your existing nginx / proxy**
   under any URL of your choosing.
3. **Reference a CDN URL** that mirrors the npm package, for example
   ``https://cdn.jsdelivr.net/npm/@iqss/dataverse-reusable-components@<version>/``.
   This is the lightest option but requires outbound connectivity from the
   user's browser.

Whichever you pick, point Dataverse at it via
:ref:`dataverse.reusable-components.base-url`.

.. _reusable-components-dev-compose:

Sample Compose Service
----------------------

For development, a sidecar service can be added to ``docker-compose-dev.yml``
following the same shape as the previewers provider:

.. code-block:: yaml

  reusable_components:
    container_name: dev_reusable_components
    hostname: reusable-components
    image: gdcc/dataverse-reusable-components:unstable
    networks:
      - dataverse
    ports:
      - "9090:80"

The ``unstable`` tag tracks the latest develop build of the bundle. For
reproducible runs, pin a specific version (e.g.
``gdcc/dataverse-reusable-components:1.4.0``).

The Dataverse container then needs the JVM setting:

.. code-block:: text

  -Ddataverse.reusable-components.base-url=http://reusable-components

If the setting is not provided, Dataverse falls back to ``/dvwebloader`` —
the default same-origin path used by the
``dataverse-frontend`` development environment.

Configuration
-------------

The relevant settings are documented in the Installation Guide:

- :ref:`dataverse.feature.react-uploader` — turn on the React uploader for
  the JSF dataset edit page.
- :ref:`dataverse.feature.api-session-auth` — required so the bundle can
  call the API using the user's session cookie.
- :ref:`dataverse.reusable-components.base-url` — where the JSF page should
  load the bundle from.

Versioning
----------

The npm package, the Docker image, and the URL used by Dataverse are all
pinned by the same semver string. Bumping is a one-line change in
``docker-compose-dev.yml`` (``image:`` tag) plus a Dataverse restart in
production. There is no Java/npm bridge in the Dataverse build itself —
the bundle is hosted, not bundled.

Cross-references
----------------

- :ref:`feature-flags`
- :ref:`file-previewers-ct` — companion pattern for static-content sidecars.
- The component contract on the frontend side:
  https://github.com/IQSS/dataverse-frontend/blob/develop/docs/reusable-components.md
