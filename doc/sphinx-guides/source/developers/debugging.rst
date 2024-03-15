=========
Debugging
=========

.. contents:: |toctitle|
	:local:

Logging
-------

By default, the app server logs at the "INFO" level but logging can be increased to "FINE" on the fly with (for example) ``./asadmin set-log-levels edu.harvard.iq.dataverse.api.Datasets=FINE``. Running ``./asadmin list-log-levels`` will show the current logging levels.

.. _jsf-config:

Java Server Faces (JSF) Configuration Options
---------------------------------------------

Some JSF options can be easily changed via MicroProfile Config (using environment variables, system properties, etc.)
during development without recompiling. Changing the options will require at least a redeployment, obviously depending
how you get these options in. (Variable substitution only happens during deployment and when using system properties
or environment variables, you'll need to pass these into the domain, which usually will require an app server restart.)

Please note you can use
`MicroProfile Config <https://download.eclipse.org/microprofile/microprofile-config-2.0/microprofile-config-spec-2.0.html#configprofile>`_
to maintain your settings more easily for different environments.

.. list-table::
   :widths: 15 15 60 10
   :header-rows: 1
   :align: left

   * - JSF Option
     - MPCONFIG Key
     - Description
     - Default
   * - javax.faces.PROJECT_STAGE
     - dataverse.jsf.project-stage
     - Switch to different levels to make JSF more verbose, disable caches etc.
       Read more `at <https://www.ibm.com/support/pages/changes-xhtml-and-java-sources-jsf-20-web-project-not-refreshed-publish-was-v8-server>`_
       `various <https://docs.oracle.com/javaee/6/tutorial/doc/bnaxj.html#giqxl>`_ `places <https://javaee.github.io/tutorial/jsf-facelets003.html>`_.
     - ``Production``
   * - javax.faces.INTERPRET_EMPTY
       _STRING_SUBMITTED_VALUES_AS_NULL
     - dataverse.jsf.empty-string-null
     - See `Jakarta Server Faces 3.0 Spec`_
     - ``true``
   * - javax.faces.FACELETS_SKIP_COMMENTS
     - dataverse.jsf.skip-comments
     - See `Jakarta Server Faces 3.0 Spec`_
     - ``true``
   * - javax.faces.FACELETS_BUFFER_SIZE
     - dataverse.jsf.buffer-size
     - See `Jakarta Server Faces 3.0 Spec`_
     - ``102400`` (100 KB)
   * - javax.faces.FACELETS_REFRESH_PERIOD
     - dataverse.jsf.refresh-period
     - See `Jakarta Server Faces 3.0 Spec`_
     - ``-1``
   * - primefaces.THEME
     - dataverse.jsf.primefaces.theme
     - See `PrimeFaces Configuration Docs`_
     - ``bootstrap``

.. _Jakarta Server Faces 3.0 Spec: https://jakarta.ee/specifications/faces/3.0/jakarta-faces-3.0.html#a6088
.. _PrimeFaces Configuration Docs: https://primefaces.github.io/primefaces/11_0_0/#/gettingstarted/configuration

----

Previous: :doc:`documentation` | Next: :doc:`coding-style`
