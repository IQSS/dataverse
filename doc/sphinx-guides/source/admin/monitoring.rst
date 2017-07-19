Monitoring
===========

.. contents:: Contents:
	:local:

In production you'll want to monitor the usual suspects such as CPU, memory, free disk space, etc.

https://github.com/IQSS/dataverse/issues/2595 contains some information on enabling monitoring of Glassfish, which is disabled by default.

There is a database table called ``actionlogrecord`` that captures events that may be of interest. See https://github.com/IQSS/dataverse/issues/2729 for more discussion around this table.
