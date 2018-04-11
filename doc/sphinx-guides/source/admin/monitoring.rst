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

Assuming you are fronting Glassfish with Apache, prevent Apache from proxying "/munin" traffic to Glassfish by adding the following line to your Apache config:

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

HTTP traffic for web clients that have cookies enabled (most browsers) can be tracked by Google Analytics and Piwik (renamed to "Matomo") as explained in the :doc:`/installation/config` section of the Installation Guide under ``:GoogleAnalyticsCode`` and ``:PiwikAnalyticsId``, respectively. You could also embed additional client side monitoring solutions by using a custom footer (``:FooterCustomizationFile``), which is described on the same page.

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

Please note that load balancers (such as Amazon's ELB) might interfer with the ``LogFormat`` mentioned above.  To start troubleshooting errors such as ``AWStats did not find any valid log lines that match your LogFormat parameter``, you might need to bump up the value of ``NbOfLinesForCorruptedLog`` in the config files above and re-try while you interate on your Apache and AWStats config.

Please note that the Dataverse team has attempted to parse Glassfish logs using AWStats but it didn't seem to just work and posts have been made at https://stackoverflow.com/questions/49134154/what-logformat-definition-does-awstats-require-to-parse-glassfish-http-access-logs and https://sourceforge.net/p/awstats/discussion/43428/thread/9b1befda/ that can be followed up on some day.

Database Connection Pool used by Glassfish
------------------------------------------

https://github.com/IQSS/dataverse/issues/2595 contains some information on enabling monitoring of Glassfish, which is disabled by default. It's a TODO to document what to do here if there is sufficient interest.


actionlogrecord
---------------

There is a database table called ``actionlogrecord`` that captures events that may be of interest. See https://github.com/IQSS/dataverse/issues/2729 for more discussion around this table.

EJB Timers
----------

Should you be interested in monitoring the EJB timers, this script may be used as an example:

.. literalinclude:: ../_static/util/check_timer.bash
