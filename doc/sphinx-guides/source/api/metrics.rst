Metrics API
===========

.. contents:: |toctitle|
    :local:

.. note:: |CORS| The Metrics API can be used from scripts running in web browsers, as it allows cross-origin resource sharing (CORS).

.. note:: For all metrics `besides` Past Days Count (``/pastDays/$days``), Database setting ``MetricsCacheTimeoutMinutes`` defines how long the cached value will be returned by subsequent queries.

.. _CORS: https://www.w3.org/TR/cors/

Counts
------

For all count metrics, ``$type`` can be set to ``dataverses``, ``datasets``, ``files`` or ``downloads``.


All-Time
~~~~~~~~

Returns a count of various objects in dataverse over all-time::

    GET https://$SERVER/api/info/metrics/$type

Example: ``curl https://demo.dataverse.org/api/info/metrics/downloads``

To-Month Counts
~~~~~~~~~~~~~~~

Returns a count of various objects in dataverse up to a specified month ``$YYYY-DD`` in YYYY-MM format (i.e. ``2018-01``)::

    GET https://$SERVER/api/info/metrics/$type/toMonth/$YYYY-DD

Example: ``curl https://demo.dataverse.org/api/info/metrics/dataverses/toMonth/2018-01``


Past Days Counts
~~~~~~~~~~~~~~~~

Returns a count of various objects in dataverse for the past ``$days`` (i.e. ``30``):: 

    GET https://$SERVER/api/info/metrics/$type/pastDays/$days

Example: ``curl https://demo.dataverse.org/api/info/metrics/datasets/pastDays/30``

Other
-----

By Subject
~~~~~~~~~~~~~~~

Returns the number of various objects by each subject. ``$type`` can be set to ``dataverses`` or ``datasets``::

    GET https://$SERVER/api/info/metrics/$type/bySubject

Example: ``curl https://demo.dataverse.org/api/info/metrics/datasets/bySubject``


By Category
~~~~~~~~~~~~~~~~~~~~~~

Returns the number an object by each category. ``$type`` can only be set to ``dataverses`` at this time::

    GET https://$SERVER/api/info/metrics/dataverses/byCategory

Example: ``curl https://demo.dataverse.org/api/info/metrics/dataverses/byCategory``

.. |CORS| raw:: html

      <span class="label label-success pull-right">
        CORS
      </span>