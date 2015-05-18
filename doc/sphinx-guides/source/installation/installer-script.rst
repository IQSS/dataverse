====================================
Dataverse Application Installer
====================================

**A scripted, interactive installer is provided. This script will configure your glassfish environment, create the database, set some required options and start the application. Some configuration tasks will still be required after you run the installer! So make sure to consult the next section. 
At this point the installer only runs on RedHat 6.* derivatives.** 

Download and run the  installer
-------------------------------

Download the installer package (``dvnstall.zip``). Unpack the zip file - this will create the directory ``dvinstall``.
Execuite the installer script (``install``):

``cd dvinstall``

``./install``

The script will prompt you for some configuration values. If this is a test/evaluation installation, it should be safe to accept nthe defaults for most of the settiongs. For for a developer's installation we recommend that you choose ``localhost`` for the host name.

The script is to a large degree a derivative of the old installer from DVN 3.x. It is written in Perl. 

All the Glassfish configuration tasks performed by the installer are isolated in the shell script ``scripts/install/glassfish-setup.sh`` (as ``asadmin`` commands). 

