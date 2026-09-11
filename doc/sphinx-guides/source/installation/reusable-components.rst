Reusable Frontend Components
============================

.. contents:: |toctitle|
	:local:

Intro
-----

Some Dataverse features can be served by React components built in
https://github.com/IQSS/dataverse-frontend and embedded directly into the
classic JSF UI. This lets installations that have not migrated to the
single-page application (SPA) benefit from new frontend work, component by
component, without replacing the whole UI.

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
(React, i18n, vendor, design system) and locale files. A JSF page loads the
bundle with a single ``<script type="module">`` tag and mounts the React tree
on a named ``<div>``:

.. code-block:: html

  <div id="dv-uploader"></div>
  <script>
    window.dvUploaderConfig = {
      siteUrl:    "https://your-dataverse.edu",
      datasetPid: "doi:10.5072/FK2/...",
      locale:     "en"
    };
  </script>
  <script type="module" src="/reusable-components/dv-uploader.js"></script>

Authentication is via session cookie (JSESSIONID). The
:ref:`dataverse.feature.api-session-auth` feature flag must be enabled. For
production deployments, also enable session-cookie API hardening (see the
security notice next to ``dataverse.feature.api-session-auth``).

The bundles are not shipped in the Dataverse WAR. The SPA is not shipped there
either, and these components are optional and disabled by default, so
installations that do not want them carry nothing extra. Until you deploy the
bundles and set :ref:`dataverse.reusable-components.base-url`, the feature flags
above do nothing and the components are not rendered.

Building the Bundles
--------------------

Build them from a checkout of
`dataverse-frontend <https://github.com/IQSS/dataverse-frontend>`_:

.. code-block:: bash

  npm install
  npm run build --workspace packages/design-system
  npm run build-reusable-components

The result is a self-contained directory:

.. code-block:: text

  dist-reusable-components/reusable-components/
    dv-tree-view.js
    dv-uploader.js
    chunks/          shared React, i18n, vendor and design-system code
    locales/         translations, fetched at runtime

Keep this layout intact. The entry points must sit at the top of whatever
location you serve, with ``chunks/`` and ``locales/`` beside them; the bundles
locate their own chunks and translations relative to themselves.

Deploying the Bundles
---------------------

Serve the bundles from the same origin as Dataverse. Every installation already
runs a web server in front of Dataverse, and putting the files behind it means
the component scripts, their translations, and the API calls they make all share
one origin and port. There is nothing to configure for cross-origin access.

Either of the following works. Both start from the build above.

Option 1: Static Files Behind Your Web Server
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Copy ``dist-reusable-components/reusable-components/`` to a directory your web
server serves, and add a location for it alongside the proxy to Dataverse. With
nginx:

.. code-block:: nginx

  location /reusable-components/ {
      alias /var/www/dataverse/reusable-components/;
  }

  location / {
      proxy_pass http://127.0.0.1:8080;
  }

The Apache equivalent is an ``Alias`` directive placed before the proxy rules.
Set :ref:`dataverse.reusable-components.base-url` to ``/reusable-components``.

Option 2: A WAR Deployed to Payara
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

If you would rather deploy the bundles the same way you deploy Dataverse
itself, build ``reusable-components.war`` from the same checkout and deploy it
next to ``dataverse.war``:

.. code-block:: bash

  cd deployment/reusable-components
  mvn package -Dversion=<version>

The WAR contains only the static files -- no Java, no dependencies -- and
Payara serves it at ``/reusable-components``, the context root matching its
name. Because your web server already proxies Dataverse, that path is
same-origin with the JSF pages without any further configuration. The same
WAR works on every installation; nothing in it is specific to yours.

Set :ref:`dataverse.reusable-components.base-url` to ``/reusable-components``,
as above. Nothing else differs between the two options.

Caching
-------

Cache headers are your responsibility. Dataverse does not add a version
parameter to the script URLs, so an installation that replaces the bundles in
place should serve the two entry points with a short ``max-age``. Everything
under ``chunks/`` is content-hashed and can be cached indefinitely.

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
  served from. Required; the flags above are inert without it.

Cross-references
----------------

- :ref:`feature-flags`
- The component contract on the frontend side:
  https://github.com/IQSS/dataverse-frontend/blob/develop/docs/reusable-components.md
