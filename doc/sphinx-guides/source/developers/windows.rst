===================
Windows Development
===================



Running Dataverse in Windows WSL
--------------------------------

The simplest method to run Dataverse in Windows 10 and 11 is using Docker and WSL (Windows Subsystem for Linux).
Once Docker and WSL are installed, you can follow the :ref:`Quickstart instructions <container-dev-quickstart>`.

Please note: these instructions have not been extensively tested. They do work with the Ubuntu-24.04 distribution for WSL. If you find any problems, please open an issue at https://github.com/IQSS/dataverse/issues.

Install WSL
~~~~~~~~~~~
If you have Docker already installed, you should already have WSL installed. Otherwise open PowerShell and run:

.. code-block:: powershell
  
   wsl --install

For use with Docker, you should use WSL v2 - run:

.. code-block:: powershell
  
   wsl  --set-default-version 2

If you already had WSL installed you can install a specific Linux distribution:

See the list of possible distributions:

.. code-block:: powershell

  wsl --list --online

Choose the distribution you would like. Then run the following command. These instructions were tested with Ubuntu.

.. code-block:: powershell

  wsl --install -d <Distribution Name>



You will be asked to create a Linux user.

.. note::
   Using wsl --set-version to upgrade an existing distribution from WSL 1 to WSL 2 may not work.

Install Docker Desktop
~~~~~~~~~~~~~~~~~~~~~~

Now you can install Dataverse in WSL following the instructions for :doc:`classic-dev-env`
At the end, check that you have ``-Ddataverse.pid.default-provider=fake`` in jvm-options.

Now you can access Dataverse in your Windows browser (Edge, Chrome, etc.):

- http://localhost:8080
- username: dataverseAdmin
- password: admin

IDE for Dataverse in Windows
~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Files in WSL are accessible from Windows for editing using ``\\wsl.localhost`` or ``\\wsl$`` path. Windows files are accessible under Linux in the ``/mnt/c/`` directory. Therefore one can use one's favorite editor or IDE to edit Dataverse project files. Then one can build using ``mvn`` in WSL and deploy manually in WSL using ``asadmin``.

It is still though possible to use a full-strength IDE. The following instructions are for IntelliJ users.

- Install Intelij in Windows.

You can open the project through ``\\wsl.localhost`` and navigate to the Dataverse project.
You can try to build the project in IntelliJ. You may get a message ``Cannot establish network connection from WSL to Windows host (could be blocked by the firewall).`` In that case you can try
to disable WSL Hyperviser from the firewall.
After that you should be able to build the project in IntelliJ.
It seems that at present it is impossible to deploy the Glassfish application in IntelliJ. You can try to add a Glassfish plugin through Settings->Plugins and in Run->Edit Configurations configure Application Server from WSL ``/usr/localhost/payara6`` with URL http://localhost:8080 and Server Domain as domain1, but it may fail since IntelliJ confuses the Windows and Linux paths.

To use the full strength of Intelij with build, deployment and debugging, one will need to use Intelij ``Remote development``. Close all the projects in IntelliJ and go to ``Remote development->WSL`` and press ``New Project``. In WSL instance choose your Linux distribution and press ``Next``. In ``Project Directory`` navigate to WSL Dataverse project. Then press ``Download IDE and Connect``. This will install IntelliJ in WSL in ``~/.cache/JetBrains/``. Now in IntelliJ you should see your project opened in a new IntelliJ window. After adding the Glassfish plugin and editing your configuration you should be able to build the project and run the project.

pgAdmin in Windows for Dataverse
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You can access the Dataverse database from Windows.

Install pgAdmin from https://www.pgadmin.org/download/pgadmin-4-windows/

In pgAdmin, register a server using 127.0.0.1 with port 5432, database dvndb and dvnapp as username with secret password. Now you will be able to access and update the Dataverse database.

