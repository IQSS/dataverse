Metrics API
===========

The Metrics API provides counts of downloads, datasets created, files uploaded, user accounts created, and more, as described below. The Dataverse Software also includes aggregate counts of Make Data Count metrics (described in the :doc:`/admin/make-data-count` section of the Admin Guide and available per-Dataset through the :doc:`/api/native-api`). A table of all the endpoints is listed below.

.. contents:: |toctitle|
    :local:

.. note:: |CORS| The Metrics API can be used from scripts running in web browsers, as it allows cross-origin resource sharing (CORS).

.. note:: For all metrics `besides` Past Days Count (``/pastDays/$days``) - recalculated daily, and (``/toMonth/$month``) for prior months - never recalculated, the setting ``MetricsCacheTimeoutMinutes`` defines how long the cached value will be returned by subsequent queries.

.. _CORS: https://www.w3.org/TR/cors/

Categories
----------

The Metrics API includes several categories of endpoints that provide different ways of understanding the evolution of metrics over time:

* Total - an aggregate count over all-time:

  * Form: GET https://$SERVER/api/info/metrics/$type

  * where ``$type`` can be set, for example, to ``dataverses`` (Dataverse collections), ``datasets``, ``files``, ``downloads`` or ``accounts``.

  * Example: ``curl https://demo.dataverse.org/api/info/metrics/downloads``

  * Return: Most of these calls return a simple JSON object with a ``count`` whose value is the metric's total count. Some calls, such as ``filedownloads`` return aggregate metrics per item (e.g. per file or by subject) as a JSONArray or CSV (see Return Formats below)

* To-Month - a count of various objects in dataverse up to and including a specified month ``$YYYY-DD`` in YYYY-MM format (e.g. ``2018-01``):

  * Form: GET https://$SERVER/api/info/metrics/$type/toMonth/$YYYY-DD

  * where ``$type`` can be set, for example, to ``dataverses`` (Dataverse collections), ``datasets``, ``files``, ``downloads`` or ``accounts``.

  * Example: ``curl https://demo.dataverse.org/api/info/metrics/dataverses/toMonth/2018-01``
    
  * Return: Most of these calls return a simple JSON object with a ``count`` whose value is the metric's total count. One variant, ``/api/info/metrics/datasets/bySubject/toMonth`` return aggregate metrics per Dataset Subject as a JSONArray or CSV (see Return Formats below)

* Past Days - a count of various objects in a Dataverse installation for the past ``$days`` (e.g. ``30``):

  * Form: GET https://$SERVER/api/info/metrics/$type/pastDays/$days

  * where ``$type`` can be set, for example, to ``dataverses`` (Dataverse collections), ``datasets``, ``files``, ``downloads`` or ``accounts``.

  * Example: ``curl https://demo.dataverse.org/api/info/metrics/datasets/pastDays/30``

  * Return: A simple JSON object with a ``count`` whose value is the metric's total count.

* Monthly - a time series of the metric with aggregate values per month with counts up to and including the given month:

  * Form: GET https://$SERVER/api/info/metrics/$type/monthly

  * where ``$type`` can be set, for example, to ``dataverses`` (Dataverse collections), ``datasets``, ``files``, ``downloads`` or ``accounts``.

  * Example: ``curl https://demo.dataverse.org/api/info/metrics/downloads/monthly``

  * Return: A JSON Array or CSV file with an array of counts per month. Variants of this this category that provide a time series per object return that information in the same formats (JSON array, CSV) with one time-series column per item (see Return Formats below).

* Tree - endpoints that describe the structure of the tree of published Dataverses, as of now or as of the end of a specified month. The monthly version could be used to show growth of the Dataverse instance over time, but the initial use case for these endpoints was to provide a means to list the tree in a selection widget to scope the metrics displayed in the local Dataverse metrics page in the `dataverse-metrics app <https://github.com/IQSS/dataverse-metrics>`_.

  * Form: GET https://$SERVER/api/info/metrics/tree
  * or
  * Form: GET https://$SERVER/api/info/metrics/tree/toMonth/$YYYY-DD

  * Example: ``curl https://demo.dataverse.org/api/info/metrics/tree``

  * Return: A nested JSON array containing JSON objects for each Dataverse collection with key/values for id, ownerId, alias, depth, and name, and a JSON array containing analogous objects for Dataverse collections within the current one.

Return Formats
--------------

There are a number of API calls that provide time series, information reported per item (e.g. per dataset, per file, by subject, by category, and by file Mimetype), or both (time series per item). Because these calls all report more than a single number, the API provides two optional formats for the return that can be selected by specifying an HTTP Accept Header for the desired format:

* application/json - a JSON array of objects. For time-series, the objects include key/values for the ``date`` and ``count`` for that month. For per-item calls, the objects include the item (e.g. for a subject), or it's id/pid (for a dataset or datafile (which may/may not not have a PID)). For timeseries per-item, the objects also include a date. In all cases, the response is a single array.

  * Example: ``curl -H 'Accept:application/json' https://demo.dataverse.org/api/info/metrics/downloads/monthly``

* comma-separated-value (CSV) - a CSV file with rows corresponding to each JSON object in the application/json format. Column headers are included (e.g. ``date,count`` or ``subject,count`` or ``date,pid,id,count`` (for a time series per file)).

  * Example: ``curl -H 'Accept:text/csv' https://demo.dataverse.org/api/info/metrics/downloads/monthly``

  * The default format is CSV, so ``curl https://demo.dataverse.org/api/info/metrics/downloads/monthly``, or typing this URL into a browser return the CSV format.

.. |CORS| raw:: html

      <span class="label label-success pull-right">
        CORS
      </span>


Filtering with Query Parameters
-------------------------------

To further tailor your metric, query parameters can be provided. On relevant endpoints, these query parameters can be used together.

parentAlias
~~~~~~~~~~~

Specifies which Dataverse sub-collection the metric should be collected for. Not including this parameter gathers metrics for the entire instance.

Example: ``curl https://demo.dataverse.org/api/info/metrics/datasets/?parentAlias=abc`` would return the number of datasets in the Dataverse collection with alias 'abc' and in sub-collections within it.

dataLocation
~~~~~~~~~~~~

Specifies whether the metric should query ``local`` data, ``remote`` data (e.g. harvested), or ``all`` data when getting results. Only works for dataset metrics.

Example: ``curl https://demo.dataverse.org/api/info/metrics/datasets/?dataLocation=remote``

country
~~~~~~~

The Make Data Count endpoints are also able to filter results by Country (specified using the ISO 3166 Country codes)

Example: ``curl https://demo.dataverse.org/api/info/metrics/makeDataCount/viewsTotal?country=au``



Endpoint Table
--------------

The following table lists the available metrics endpoints (not including the Make Data Counts endpoints for a single dataset which are part of the :doc:`/api/native-api`) along with additional notes about them.


.. csv-table:: Metrics Endpoints
   :header: endpoint,variables,formats,scope,limits,cached,meaning,notes
   :widths: 100, 15, 10, 20, 20, 8, 30, 70

    /api/info/metrics/dataverses,count,json,collection subtree,published,y,as of now/total,collection subtree means you can get info for the instance or with ?parentAlias={alias} can optionally specify a dataverse which should be used to scope the query. 
    /api/info/metrics/dataverses/toMonth/{yyyy-MM},count,json,collection subtree,published,y,cumulative up to month specified,
    /api/info/metrics/dataverses/monthly,"date, count","json, csv",collection subtree,published,y,monthly cumulative  timeseries from first date of first entry to now,
    /api/info/metrics/dataverses/pastDays/{n},count,json,collection subtree,published,y,aggregate count for past n days,
    /api/info/metrics/dataverses/byCategory,"category, count","json, csv",collection subtree,published,y,total count per category,
    /api/info/metrics/dataverses/bySubject,"subject, count","json, csv",collection subtree,all,y,total count per subject,
    /api/info/metrics/datasets,count,json,collection subtree,"released, choice of all, local or remote (harvested)",y,as of now/total,released means only currently released dataset versions (not unpublished or DEACCESSIONED versions)
    /api/info/metrics/datasets/toMonth/{yyyy-MM},count,json,collection subtree,"released, choice of all, local or remote (harvested)",y,cumulative up to month specified,
    /api/info/metrics/datasets/monthly,"date, count","json, csv",collection subtree,"released, choice of all, local or remote (harvested)",y,monthly cumulative  timeseries from first date of first entry to now,released means only currently released dataset versions (not unpublished or DEACCESSIONED versions)
    /api/info/metrics/datasets/pastDays/{n},count,json,collection subtree,"released, choice of all, local or remote (harvested)",y,aggregate count for past n days,
    /api/info/metrics/datasets/bySubject,"subject, count","json, csv",collection subtree,"released, choice of all, local or remote (harvested)",y,total count per subject,
    /api/info/metrics/datasets/bySubject/toMonth/{yyyy-MM},"subject, count","json, csv",collection subtree,"released, choice of all, local or remote (harvested)",y,cumulative cont per subject up to month specified,
    /api/info/metrics/files,count,json,collection subtree,in released datasets,y,as of now/total,
    /api/info/metrics/files/toMonth/{yyyy-MM},count,json,collection subtree,in released datasets,y,cumulative up to month specified,
    /api/info/metrics/files/monthly,"date, count","json, csv",collection subtree,in released datasets,y,monthly cumulative  timeseries from first date of first entry to now,date is the month when the first version containing the file was released (or created for harvested versions)
    /api/info/metrics/files/pastDays/{n},count,json,collection subtree,in released datasets,y,aggregate count for past n days,
    /api/info/metrics/files/byType,"mimetype, count, size","json, csv",collection subtree,in released datasets,y,current totals,
    /api/info/metrics/files/byType/monthly,"date, mimetype, count, size","json, csv",collection subtree,in released datasets,y,monthly cumulative  timeseries from first date of first entry to now,data for a specific mimetype is only listed starting with the first month there are files of that type
    /api/info/metrics/downloads,count,json,collection subtree,published,y,as of now/total,"published for downloads means 'recorded in guestbookresponse' which occurs for any files that were ever in a published version, even if that version is now DEACCESSIONED, the file isn't in a current version, etc."
    /api/info/metrics/downloads/toMonth/{yyyy-MM},count,json,collection subtree,published,y,cumulative up to month specified,downloads from versions that do not have a releasetime (from older Dataverse versions) are included in this cumulative count and the total as of now (line above)
    /api/info/metrics/downloads/pastDays/{n},count,json,collection subtree,published,y,aggregate count for past n days,
    /api/info/metrics/downloads/monthly,"date, count","json, csv",collection subtree,published,y,monthly cumulative  timeseries from first date of first entry to now,counts from dataset versions with no releasetime (legacy from old Dataverse versions) are counted as occuring in the month prior to the first count that does have a date
    /api/info/metrics/filedownloads,"count by id, pid","json, csv",collection subtree,published,y,as of now/totals,download counts per file id. PIDs are also included in output if they exist
    /api/info/metrics/filedownloads/toMonth/{yyyy-MM},"count by id, pid","json, csv",collection subtree,published,y,cumulative up to month specified,download counts per file id to the specified month. PIDs are also included in output if they exist
    /api/info/metrics/filedownloads/monthly,"date, count, id, pid","json, csv",collection subtree,published,y,"monthly cumulative  timeseries by file id, pid from first date of first entry to now","unique downloads per month by file (id, pid) sorted in decreasing order of counts"
    /api/info/metrics/makeDataCount/{metric},count,json,"collection subtree, optionally also by {country}","published, MDC",y,count for specified {metric} as of now/total,"published means in the mdc logs which are not created for unpublished datasets, so this is filtered like downloads and includes counts from DEACCESSED, old versions. "
    /api/info/metrics/makeDataCount/{metric}/toMonth/{yyyy-MM},count,json,"collection subtree, optionally also by {country}","published, MDC",y,cumulative count for specified {metric} through specified month,These metrics are also limited by the MDC start date and by MDC filtering done by counter-processor
    /api/info/metrics/makeDataCount/{metric}/monthly,"date, count","json, csv","collection subtree, optionally also by {country}","published, MDC",y,monthly cumulative timeseries of counts for specified {metric},These metrics are also limited by the MDC start date and by MDC filtering done by counter-processor
    /api/info/metrics/uniquedownloads,"pid, count",json,collection subtree,published,y,total count of unique users who have downloaded from the datasets in scope,The use case for this metric (uniquedownloads) is to more fairly assess which datasets are getting downloaded/used by only counting each users who downloads any file from a dataset as one count (versus downloads of multiple files or repeat downloads counting as multiple counts which adds a bias for large datasets and/or use patterns where a file is accessed repeatedly for new analyses)
    /api/info/metrics/uniquedownloads/monthly,"date, pid, count","json, csv",collection subtree,published,y,monthly cumulative timeseries of unique user counts for datasets in the dataverse scope,
    /api/info/metrics/uniquedownloads/toMonth/{yyyy-MM},"pid, count",json,collection subtree,published,y,cumulative count of unique users who have downloaded from the datasets in scope through specified month,
    /api/info/metrics/uniquefiledownloads,"count by id, pid","json, csv",collection subtree,published,y,as of now/totals,unique download counts per file id. PIDs are also included in output if they exist
    /api/info/metrics/uniquefiledownloads/monthly,"date, count, id, pid","json, csv",collection subtree,published,y,"monthly cumulative  timeseries by file id, pid from first date of first entry to now","unique downloads per month by file (id, pid) sorted in decreasing order of counts"
    /api/info/metrics/uniquefiledownloads/toMonth/{yyyy-MM},"count by id, pid","json, csv",collection subtree,published,y,cumulative up to month specified,unique download counts per file id to the specified month. PIDs are also included in output if they exist
    /api/info/metrics/tree,"id, ownerId, alias, depth, name, children",json,collection subtree,published,y,"tree of dataverses starting at the root or a specified parentAlias with their id, owner id, alias, name, a computed depth, and array of children dataverses","underlying code can also include draft dataverses, this is not currently accessible via api, depth starts at 0"
    /api/info/metrics/tree/toMonth/{yyyy-MM},"id, ownerId, alias, depth, name, children",json,collection subtree,published,y,"tree of dataverses in existence as of specified date starting at the root or a specified parentAlias with their id, owner id, alias, name, a computed depth, and array of children dataverses","underlying code can also include draft dataverses, this is not currently accessible via api, depth starts at 0"
    /api/info/metrics/accounts,count,json,Dataverse installation,all,y,as of now/totals,
    /api/info/metrics/accounts/toMonth/{yyyy-MM},count,json,Dataverse installation,all,y,cumulative up to month specified,
    /api/info/metrics/accounts/pastDays/{n},count,json,Dataverse installation,all,y,aggregate count for past n days,
    /api/info/metrics/accounts/monthly,"date, count","json, csv",Dataverse installation,all,y,monthly cumulative timeseries from first date of first entry to now,

Related API Endpoints
---------------------

The following endpoints are not under the metrics namespace but also return counts:

- :ref:`file-download-count` 
