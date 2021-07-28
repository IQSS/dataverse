=========
Debugging
=========

.. contents:: |toctitle|
	:local:

Logging
-------

By default, the app server logs at the "INFO" level but logging can be increased to "FINE" on the fly with (for example) ``./asadmin set-log-levels edu.harvard.iq.dataverse.api.Datasets=FINE``. Running ``./asadmin list-log-levels`` will show the current logging levels.

----

Previous: :doc:`documentation` | Next: :doc:`coding-style`
