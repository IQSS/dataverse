Metrics API
===========

.. contents:: |toctitle|
    :local:

.. note:: |CORS| The Metrics API can be used from scripts running in web browsers, as it allows cross-origin resource sharing (CORS).

.. _CORS: https://www.w3.org/TR/cors/

dataverses
----------------------

Returns an all-time count of the dataverses. ``MetricsCacheTimeoutMinutes`` sets how long the cached value will be returned by subsequent queries.

``curl https://demo.dataverse.org/api/info/metrics/dataverses``

datasets
----------------------

Returns an all-time count of the datasets. ``MetricsCacheTimeoutMinutes`` sets how long the cached value will be returned by subsequent queries.

``curl https://demo.dataverse.org/api/info/metrics/datasets``

files
----------------------

Returns an all-time count of the files. ``MetricsCacheTimeoutMinutes`` sets how long the cached value will be returned by subsequent queries.

``curl https://demo.dataverse.org/api/info/metrics/files``

downloads
----------------------

Returns a count of all the downloads. ``MetricsCacheTimeoutMinutes`` sets how long the cached value will be returned by subsequent queries.

``curl https://demo.dataverse.org/api/info/metrics/downloads``

dataverses/toMonth/$month
----------------------

Returns a count up to the specified month in YYYY-MM format (i.e. ``/2018-01``).

``curl https://demo.dataverse.org/api/info/metrics/dataverses/toMonth/YYYY-DD``

datasets/toMonth/$month
------------------------

Returns a count up to the specified month in YYYY-MM format (i.e. ``/2018-01``).

``curl https://demo.dataverse.org/api/info/metrics/datasets/toMonth/YYYY-DD``

files/toMonth/$month
------------------------

Returns a count up to the specified month in YYYY-MM format (i.e. ``/2018-01``).

``curl https://demo.dataverse.org/api/info/metrics/files/toMonth/YYYY-DD``

downloads/toMonth/$month
------------------------

Returns a count up to the specified month in YYYY-MM format (i.e. ``/2018-01``).

``curl https://demo.dataverse.org/api/info/metrics/downloads/toMonth/YYYY-DD``

dataverses/pastDays/$days
----------------------

Returns a count back to the specified day (i.e. ``/30``). The number returned is cached by the system until the next day.

``curl https://demo.dataverse.org/api/info/metrics/dataverses/pastDays/#ofdays``

datasets/pastDays/$days
----------------------

Returns a count back to the specified day (i.e. ``/30``). The number returned is cached by the system until the next day.

``curl https://demo.dataverse.org/api/info/metrics/datasets/pastDays/#ofdays``

files/pastDays/$days
----------------------

Returns a count back to the specified day (i.e. ``/30``). The number returned is cached by the system until the next day.

``curl https://demo.dataverse.org/api/info/metrics/files/pastDays/#ofdays``

downloads/pastDays/$days
----------------------

Returns a count back to the specified day (i.e. ``/30``). The number returned is cached by the system until the next day.

``curl https://demo.dataverse.org/api/info/metrics/downloads/pastDays/#ofdays``

dataverses/byCategory
------------------------

Returns the number of dataverses by each category. ``MetricsCacheTimeoutMinutes`` sets how long the cached value will be returned by subsequent queries.

``curl https://demo.dataverse.org/api/info/metrics/dataverses/byCategory``

datasets/bySubject
------------------------

Returns the number of datasets by each subject. ``MetricsCacheTimeoutMinutes`` sets how long the cached value will be returned by subsequent queries.

``curl https://demo.dataverse.org/api/info/metrics/datasets/bySubject``

.. |CORS| raw:: html

      <span class="label label-success pull-right">
        CORS
      </span>
