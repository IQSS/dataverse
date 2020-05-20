=====================
Advanced Installation
=====================

Advanced installations are not officially supported but here we are at least documenting some tips and tricks that you might find helpful. You can find a diagram of an advanced installation in the :doc:`prep` section.

.. contents:: |toctitle|
	:local:

Multiple App Servers
--------------------

You should be conscious of the following when running multiple app servers.

- Only one app server can be the dedicated timer server, as explained in the :doc:`/admin/timers` section of the Admin Guide.
- When users upload a logo or footer for their dataverse using the "theme" feature described in the :doc:`/user/dataverse-management` section of the User Guide, these logos are stored only on the app server the user happend to be on when uploading the logo. By default these logos and footers are written to the directory ``/usr/local/payara5/glassfish/domains/domain1/docroot/logos``.
- When a sitemap is created by an app server it is written to the filesystem of just that app server. By default the sitemap is written to the directory ``/usr/local/payara5/glassfish/domains/domain1/docroot/sitemap``.
- If Make Data Count is used, its raw logs must be copied from each app server to single instance of Counter Processor. See also :ref:`:MDCLogPath` section in the Configuration section of this guide and the :doc:`/admin/make-data-count` section of the Admin Guide.
- Dataset draft version logging occurs separately on each app server. See :ref:`edit-draft-versions-logging` section in Monitoring of the Admin Guide for details.
- Password aliases (``db_password_alias``, etc.) are stored per app server.

Detecting Which App Server a User Is On
+++++++++++++++++++++++++++++++++++++++

If you have successfully installed multiple app servers behind a load balancer you might like to know which server a user has landed on. A straightforward solution is to place a file called ``host.txt`` in a directory that is served up by Apache such as ``/var/www/html`` and then configure Apache not to proxy requests to ``/host.txt`` to the app server. Here are some example commands on RHEL/CentOS 7 that accomplish this::

        [root@server1 ~]# vim /etc/httpd/conf.d/ssl.conf
        [root@server1 ~]# grep host.txt /etc/httpd/conf.d/ssl.conf
        ProxyPassMatch ^/host.txt !
        [root@server1 ~]# systemctl restart httpd.service
        [root@server1 ~]# echo $HOSTNAME > /var/www/html/host.txt
        [root@server1 ~]# curl https://dataverse.example.edu/host.txt
        server1.example.edu

You would repeat the steps above for all of your app servers. If users seem to be having a problem with a particular server, you can ask them to visit https://dataverse.example.edu/host.txt and let you know what they see there (e.g. "server1.example.edu") to help you know which server to troubleshoot.

Please note that :ref:`network-ports` under the Configuration section has more information on fronting your app server with Apache. The :doc:`shibboleth` section talks about the use of ``ProxyPassMatch``.
