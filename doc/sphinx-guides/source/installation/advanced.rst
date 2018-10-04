=====================
Advanced Installation
=====================

Advanced installations are not officially supported but here we are at least documenting some tips and tricks that you might find helpful. You can find a diagram of an advanced installation in the :doc:`prep` section.

.. contents:: |toctitle|
	:local:

Multiple Glassfish Servers
--------------------------

You should be conscious of the following when running multiple Glassfish servers.

- Only one Glassfish server can be the dedicated timer server, as explained in the :doc:`/admin/timers` section of the Admin Guide.
- When users upload a logo for their dataverse using the "theme" feature described in the :doc:`/user/dataverse-management` section of the User Guide, these logos are stored only on the Glassfish server the user happend to be on when uploading the logo. By default these logos are written to the directory ``/usr/local/glassfish4/glassfish/domains/domain1/docroot/logos``.
- When a sitemp is created by a Glassfish server it is written to the filesystem of just that Glassfish server. By default the sitemap is written to the directory ``/usr/local/glassfish4/glassfish/domains/domain1/docroot/sitemap``.

Detecting Which Glassfish Server a User Is On
+++++++++++++++++++++++++++++++++++++++++++++

If you have successfully installed multiple Glassfish servers behind a load balancer you might like to know which server a user has landed on. A straightforward solution is to place a file called ``host.txt`` in a directory that is served up by Apache such as ``/var/www/html`` and then configure Apache not to proxy requests to ``/host.txt`` to Glassfish. Here are some example commands on RHEL/CentOS 7 that accomplish this::

        [root@server1 ~]# vim /etc/httpd/conf.d/ssl.conf
        [root@server1 ~]# grep host.txt /etc/httpd/conf.d/ssl.conf
        ProxyPassMatch ^/host.txt !
        [root@server1 ~]# systemctl restart httpd.service
        [root@server1 ~]# echo $HOSTNAME > /var/www/html/host.txt
        [root@server1 ~]# curl https://dataverse.example.edu/host.txt
        server1.example.edu

You would repeat the steps above for all of your Glassfish servers. If users seem to be having a problem with a particular server, you can ask them to visit https://dataverse.example.edu/host.txt and let you know what they see there (e.g. "server1.example.edu") to help you know which server to troubleshoot.

Please note that "Network Ports" under the :doc:`config` section has more information on fronting Glassfish with Apache. The :doc:`shibboleth` section talks about the use of ``ProxyPassMatch``.
