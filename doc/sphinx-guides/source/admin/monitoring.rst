Monitoring
===========

Once you're in production, you'll want to set up some monitoring. This page may serve as a starting point for you but you are encouraged to share your ideas with the Dataverse community!

.. contents:: Contents:
	:local:

Operating System Monitoring
---------------------------

In production you'll want to monitor the usual suspects such as CPU, memory, free disk space, etc. There are a variety of tools in this space but we'll highlight Munin below because it's relatively easy to set up.

Munin
+++++

http://munin-monitoring.org says, "A default installation provides a lot of graphs with almost no work." From RHEL or CentOS 7, you can try the following steps.

Enable the EPEL yum repo (if you haven't already):

``yum install https://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm``

Install Munin:

``yum install munin``

Start Munin:

``systemctl start munin-node.service``

Configure Munin to start at boot:

``systemctl enable munin-node.service``

Create a username/password (i.e. "admin" for both):

``htpasswd /etc/munin/munin-htpasswd admin``

Assuming you are fronting your app server with Apache, prevent Apache from proxying "/munin" traffic to the app server by adding the following line to your Apache config:

``ProxyPassMatch ^/munin !``

Then reload Apache to pick up the config change:

``systemctl reload httpd.service``

Test auth for the web interface:

``curl http://localhost/munin/ -u admin:admin``

At this point, graphs should start being generated for disk, network, processes, system, etc.

HTTP Traffic
------------

HTTP traffic can be monitored from the client side, the server side, or both.

Monitoring HTTP Traffic from the Client Side
++++++++++++++++++++++++++++++++++++++++++++

HTTP traffic for web clients that have cookies enabled (most browsers) can be tracked by Google Analytics (https://www.google.com/analytics/) and Matomo (formerly "Piwik"; https://matomo.org/) as explained in the :ref:`Web-Analytics-Code` section of the Installation Guide.

To track analytics beyond pageviews, style classes have been added for end user action buttons, which include:

``btn-compute``, ``btn-contact``, ``btn-download``, ``btn-explore``, ``btn-export``, ``btn-preview``, ``btn-request``, ``btn-share``

Monitoring HTTP Traffic from the Server Side
+++++++++++++++++++++++++++++++++++++++++++++

There are a wide variety of solutions available for monitoring HTTP traffic from the server side. The following are merely suggestions and a pull request against what is written here to add additional ideas is certainly welcome! Are you excited about the ELK stack (Elasticsearch, Logstash, and Kibana)? The TICK stack (Telegraph InfluxDB Chronograph and Kapacitor)? GoAccess? Prometheus? Graphite? Splunk? Please consider sharing your work with the Dataverse community!

AWStats
+++++++

AWStats is a venerable tool for monitoring web traffic based on Apache access logs. On RHEL/CentOS 7, you can try the following steps.

Enable the EPEL yum repo:

``yum install https://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm``

Install AWStats:

``yum install awstats``

Assuming you are using HTTPS rather than HTTP (and you should!), edit ``/etc/awstats/awstats.standalone.conf`` and change ``LogFile="/var/log/httpd/access_log"`` to ``LogFile="/var/log/httpd/ssl_access_log"``. In the same file, change ``LogFormat=1`` to ``LogFormat=4``. Make both of these changes (``LogFile`` and ``LogFormat`` in ``/etc/awstats/awstats.localhost.localdomain.conf`` as well.

Process the logs:

``/usr/share/awstats/tools/awstats_updateall.pl now``

Please note that load balancers (such as Amazon's ELB) might interfere with the ``LogFormat`` mentioned above.  To start troubleshooting errors such as ``AWStats did not find any valid log lines that match your LogFormat parameter``, you might need to bump up the value of ``NbOfLinesForCorruptedLog`` in the config files above and re-try while you interate on your Apache and AWStats config.

Please note that the Dataverse Project team has attempted to parse Glassfish/Payara logs using AWStats but it didn't seem to just work and posts have been made at https://stackoverflow.com/questions/49134154/what-logformat-definition-does-awstats-require-to-parse-glassfish-http-access-logs and https://sourceforge.net/p/awstats/discussion/43428/thread/9b1befda/ that can be followed up on some day.

Database Connection Pool Used by App Server
-------------------------------------------

https://github.com/IQSS/dataverse/issues/2595 contains some information on enabling monitoring of app servers, which is disabled by default. It's a TODO to document what to do here if there is sufficient interest.


actionlogrecord
---------------

There is a database table called ``actionlogrecord`` that captures events that may be of interest. See https://github.com/IQSS/dataverse/issues/2729 for more discussion around this table.

.. _edit-draft-versions-logging:

Edit Draft Versions Logging
---------------------------

Changes made to draft versions of datasets are logged in a folder called logs/edit-drafts. See https://github.com/IQSS/dataverse/issues/5145 for more information on this logging.

Solr Indexing Failures Logging
------------------------------

Failures occurring during the indexing of Dataverse collections and datasets are logged in a folder called logs/process-failures. This logging will include instructions for manually re-running the failed processes. It may be advantageous to set up a automatic job to monitor new entries into this log folder so that indexes could be re-run.

EJB Timers
----------

Should you be interested in monitoring the EJB timers, this script may be used as an example:

.. literalinclude:: ../_static/util/check_timer.bash

AWS RDS
-------

Some installations of Dataverse use AWS's "database as a service" offering called RDS (Relational Database Service) so it's worth mentioning some monitoring tips here.

There are two documents that are especially worth reviewing:

- `Monitoring an Amazon RDS DB instance <https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_Monitoring.html>`_: The official documentation.
- `Performance Monitoring Workshop for RDS PostgreSQL and Aurora PostgreSQL <https://rdspg-monitoring.workshop.aws/en/intro.html>`_: A workshop that steps through practical examples.

Tips:

- Enable **Performance Insights**. The `product page <https://aws.amazon.com/rds/performance-insights/>`_ includes a `video from 2017 <https://youtu.be/4462hcfkApM>`_ that is still compelling today. For example, the `Top SQL <https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/USER_PerfInsights.UsingDashboard.Components.AvgActiveSessions.TopLoadItemsTable.TopSQL.html>`_ tab shows the SQL queries that are contributing the most to database load. Performance Insights seems to only be available for `PostgreSQL 10 and higher <https://aws.amazon.com/about-aws/whats-new/2018/04/rds-performance-insights-on-rds-for-postgresql/>`_.
- Enable the **slow query log** and consider using pgbadger to analyze the log files. Set ``log_min_duration_statement`` to "5000" for 5 seconds. See `slides <https://rdspg-monitoring.workshop.aws/en/postgresql-logs/enable-slow-query-log.html>`_ from the workshop for details.
- Use **CloudWatch**. CloudWatch gathers metrics about CPU utilization from the hypervisor for a DB instance. It has a separate GUI to log into. See `CloudWatch docs <https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/monitoring-cloudwatch.html>`_.
- Use **Enhanced Monitoring**. Enhanced Monitoring gathers its metrics from an agent on the instance. See `Enhanced Monitoring docs <https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/USER_Monitoring.OS.html>`_.
- `dataverse-ansible <https://github.com/GlobalDataverseCommunityConsortium/dataverse-ansible>`_ has a ``use_rds`` flag.
