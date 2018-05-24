Metrics API
===========

.. contents:: |toctitle|
    :local:

The Metrics API

.. note:: |CORS| The Metrics API can be used from scripts running in web browsers, as it allows cross-origin resource sharing (CORS).

.. _CORS: https://www.w3.org/TR/cors/

dataverses/toMonth
----------------------

Returns a count up to the current month or append a date in YYYY-MM format (i.e. ``/2018-01``) for a specific month.

``curl https://demo.dataverse.org/api/info/metrics/dataverses/toMonth``

datasets/toMonth
------------------------

Returns a count up to the current month or append a date in YYYY-MM format (i.e. ``/2018-01``) for a specific month.

``curl https://demo.dataverse.org/api/info/metrics/datasets/toMonth``

files/toMonth
------------------------

Returns a count up to the current month or append a date in YYYY-MM format (i.e. ``/2018-01``) for a specific month.

``curl https://demo.dataverse.org/api/info/metrics/files/toMonth``

downloads/toMonth
------------------------

Returns a count up to the current month or append a date in YYYY-MM format (i.e. ``/2018-01``) for a specific month.

``curl https://demo.dataverse.org/api/info/metrics/downloads/toMonth``

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
