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
- When users upload a logo or footer for their Dataverse collection using the "theme" feature described in the :doc:`/user/dataverse-management` section of the User Guide, these logos are stored only on the app server the user happened to be on when uploading the logo. By default these logos and footers are written to the directory ``/usr/local/payara5/glassfish/domains/domain1/docroot/logos``.
- When a sitemap is created by an app server it is written to the filesystem of just that app server. By default the sitemap is written to the directory ``/usr/local/payara5/glassfish/domains/domain1/docroot/sitemap``.
- If Make Data Count is used, its raw logs must be copied from each app server to single instance of Counter Processor. See also :ref:`:MDCLogPath` section in the Configuration section of this guide and the :doc:`/admin/make-data-count` section of the Admin Guide.
- Dataset draft version logging occurs separately on each app server. See :ref:`edit-draft-versions-logging` section in Monitoring of the Admin Guide for details.
- Password aliases (``dataverse.db.password``, etc.) are stored per app server.

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

Optional Components
-------------------

Standalone "Zipper" Service Tool
++++++++++++++++++++++++++++++++

As of Dataverse Software 5.0 we offer an experimental optimization for the multi-file, download-as-zip functionality. If this option
(``:CustomZipDownloadServiceUrl``) is enabled, instead of enforcing
the size limit on multi-file zipped downloads (as normally specified
by the option ``:ZipDownloadLimit``), we attempt to serve all the
files that the user requested (that they are authorized to download),
but the request is redirected to a standalone zipper service running
as a cgi-bin executable under Apache. Thus moving these potentially
long-running jobs completely outside the Application Server (Payara);
and preventing worker threads from becoming locked serving them. Since
zipping is also a CPU-intensive task, it is possible to have this
service running on a different host system, freeing the cycles on the
main Application Server. (The system running the service needs to have
access to the database as well as to the storage filesystem, and/or S3
bucket).

Please consult the scripts/zipdownload/README.md in the Dataverse Software 5.0+ source tree for more information. 

To install: You can follow the instructions in the file above to build
``ZipDownloadService-v1.0.0.jar``. It will also be available, pre-built as part of the Dataverse Software 5.0 release on GitHub. Copy it, together with the shell
script scripts/zipdownload/cgi-bin/zipdownload to the cgi-bin
directory of the chosen Apache server (/var/www/cgi-bin standard). 

Make sure the shell script (zipdownload) is executable, and edit it to configure the
database access credentials. Do note that the executable does not need
access to the entire Dataverse installation database. A security-conscious admin
can create a dedicated database user with access to just one table:
``CUSTOMZIPSERVICEREQUEST``.

You may need to make extra Apache configuration changes to make sure /cgi-bin/zipdownload is accessible from the outside.
For example, if this is the same Apache that's in front of your Dataverse installation Payara instance, you will need to add another pass through statement to your configuration:

``ProxyPassMatch ^/cgi-bin/zipdownload !``

Test this by accessing it directly at ``<SERVER URL>/cgi-bin/download``. You should get a ``404 No such download job!``. If instead you are getting an "internal server error", this may be an SELinux issue; try ``setenforce Permissive``. If you are getting a generic Dataverse collection "not found" page, review the ``ProxyPassMatch`` rule you have added. 

To activate in your Dataverse installation::

   curl -X PUT -d '/cgi-bin/zipdownload' http://localhost:8080/api/admin/settings/:CustomZipDownloadServiceUrl

