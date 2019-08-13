Upgrade instructions:

A new version of file type detection software, Jhove, is added in this release. It requires an update of its configuration file: ``jhove.conf``. Download the new configuration file from the Dataverse release page on GitHub, or from the source tree at https://github.com/IQSS/dataverse/blob/master/conf/jhove/jhove.conf, and place it in ``<GLASSFISH_DOMAIN_DIRECTORY>/config/``. For example: ``/usr/local/glassfish4/glassfish/domains/domain1/config/jhove.conf``. 

**Important:** If your Glassfish installation directory is different from ``/usr/local/glassfish4``, make sure to edit the header of the config file, to reflect the correct location. 
