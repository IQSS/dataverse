Monitoring
===========

In production you'll want to monitor the usual suspects such as CPU, memory, free disk space, etc.

.. contents:: Contents:
	:local:

HTTP Traffic
------------

Please note that HTTP traffic for web clients that have cookies enabled (most browsers) can be tracked by Google Analytics and Piwik as explained in the :doc:`/installation/config` section of the Installation Guide.

awstats
+++++++

awstats is a venerable tool for monitoring web traffic based on Apache access logs. On RHEL/CentOS 7, you can try the following steps.

Enable the EPEL yum repo:

``yum install https://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm``

Install awstats:

``yum install awstats``

Assuming you are using HTTPS rather than HTTP (and you should!), edit ``/etc/awstats/awstats.standalone.conf`` and change ``LogFile="/var/log/httpd/access_log"`` to ``LogFile="/var/log/httpd/ssl_access_log"``. In the same file, change ``LogFormat=1`` to ``LogFormat=4``. Make both of these changes (``LogFile`` and ``LogFormat`` in ``/etc/awstats/awstats.localhost.localdomain.conf`` as well.

Process the logs:

``/usr/share/awstats/tools/awstats_updateall.pl now``

If you get an error saying ``AWStats did not find any valid log lines that match your LogFormat parameter``, you might need to bump up the value of ``NbOfLinesForCorruptedLog`` in the config files above and re-try.

Glassfish
---------

https://github.com/IQSS/dataverse/issues/2595 contains some information on enabling monitoring of Glassfish, which is disabled by default.


actionlogrecord
---------------

There is a database table called ``actionlogrecord`` that captures events that may be of interest. See https://github.com/IQSS/dataverse/issues/2729 for more discussion around this table.

EJB Timers
----------

Should you be interested in monitoring the EJB timers, this script may be used as an example:

.. literalinclude:: ../_static/util/check_timer.bash
