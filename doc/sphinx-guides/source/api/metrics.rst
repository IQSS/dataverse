Metrics API
===========

.. contents:: |toctitle|
    :local:

.. note:: |CORS| The Metrics API can be used from scripts running in web browsers, as it allows cross-origin resource sharing (CORS).

.. note:: For all metrics `besides` Past Days Count (``/pastDays/$days``), Database setting ``MetricsCacheTimeoutMinutes`` defines how long the cached value will be returned by subsequent queries.

.. _CORS: https://www.w3.org/TR/cors/

Total
-----

Returns a count of various objects in dataverse over all-time::

    GET https://$SERVER/api/info/metrics/$type

``$type`` can be set to ``dataverses``, ``datasets``, ``files`` or ``downloads``.

Example: ``curl https://demo.dataverse.org/api/info/metrics/downloads``

To-Month
--------

Returns a count of various objects in dataverse up to a specified month ``$YYYY-DD`` in YYYY-MM format (e.g. ``2018-01``)::

    GET https://$SERVER/api/info/metrics/$type/toMonth/$YYYY-DD

``$type`` can be set to ``dataverses``, ``datasets``, ``files`` or ``downloads``.

Example: ``curl https://demo.dataverse.org/api/info/metrics/dataverses/toMonth/2018-01``


Past Days
---------

Returns a count of various objects in dataverse for the past ``$days`` (e.g. ``30``):: 

    GET https://$SERVER/api/info/metrics/$type/pastDays/$days

``$type`` can be set to ``dataverses``, ``datasets``, ``files`` or ``downloads``.

Example: ``curl https://demo.dataverse.org/api/info/metrics/datasets/pastDays/30``


Dataverse Specific Commands
---------------------------

By Subject
~~~~~~~~~~~~~~~

Returns the number of dataverses by each subject::

    GET https://$SERVER/api/info/metrics/dataverses/bySubject


By Category
~~~~~~~~~~~~~~~~~~~~~~

Returns the number of dataverses by each category::

    GET https://$SERVER/api/info/metrics/dataverses/byCategory


Dataset Specific Commands
-------------------------

By Subject
~~~~~~~~~~~~~~~

Returns the number of datasets by each subject::

    GET https://$SERVER/api/info/metrics/datasets/bySubject


By Subject, and to Month
~~~~~~~~~~~~~~~~~~~~~~~~

Returns the number of datasets by each subject, and up to a specified month ``$YYYY-DD`` in YYYY-MM format (e.g. ``2018-01``)::

    GET https://$SERVER/api/info/metrics/datasets/bySubject/toMonth/$YYYY-DD

Example: ``curl https://demo.dataverse.org/api/info/metrics/datasets/bySubject/toMonth/2018-01``

.. |CORS| raw:: html

      <span class="label label-success pull-right">
        CORS
      </span>