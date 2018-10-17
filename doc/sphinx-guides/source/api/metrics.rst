Metrics API
===========

.. contents:: |toctitle|
    :local:

The Metrics API

.. note:: |CORS| The Metrics API can be used from scripts running in web browsers, as it allows cross-origin resource sharing (CORS).

.. _CORS: https://www.w3.org/TR/cors/

dataverses
----------------------

Returns an all-time count of the dataverses.

``curl https://demo.dataverse.org/api/info/metrics/dataverses``

datasets
----------------------

Returns an all-time count of the datasets.

``curl https://demo.dataverse.org/api/info/metrics/datasets``

files
----------------------

Returns an all-time count of the files.

``curl https://demo.dataverse.org/api/info/metrics/files``

downloads
----------------------

Returns a count of all the downloads.

``curl https://demo.dataverse.org/api/info/metrics/downloads``

dataverses/toMonth/$month
----------------------

Returns a count up to the specified month in YYYY-MM format (i.e. ``/2018-01``).

``curl https://demo.dataverse.org/api/info/metrics/dataverses/toMonth/YYYY-DD``

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

dataverses/byCategory
------------------------

``curl https://demo.dataverse.org/api/info/metrics/dataverses/byCategory``

datasets/bySubject
------------------------

``curl https://demo.dataverse.org/api/info/metrics/datasets/bySubject``

.. |CORS| raw:: html

      <span class="label label-success pull-right">
        CORS
      </span>
