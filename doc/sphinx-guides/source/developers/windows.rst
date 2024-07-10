===================
Windows Development
===================

Historically, development on Windows is `not well supported <https://groups.google.com/d/msg/dataverse-community/Hs9j5rIxqPI/-q54751aAgAJ>`_ but as of 2023 a container-based approach is recommended.

.. contents:: |toctitle|
	:local:

Running Dataverse in Docker on Windows
--------------------------------------

See the `post <https://groups.google.com/g/dataverse-dev/c/utqkZ7gYsf4/m/4IDtsvKSAwAJ>`_ by Akio Sone for additional details, but please observe the following:

- You must have jq installed: https://jqlang.github.io/jq/download/
- In git, the line-ending setting should be set to always LF (line feed, ``core.autocrlf=input``). Update: This should have been fixed by https://github.com/IQSS/dataverse/pull/10092.

Once the above is all set you can move on to :doc:`/container/dev-usage` in the Container Guide.

Generally speaking, if you're having trouble running a Dataverse dev environment in Docker on Windows, you are highly encouraged to post about it in the #containers channel on Zulip (https://chat.dataverse.org) and join a Containerization Working Group meeting (https://ct.gdcc.io). See also :doc:`/container/intro` in the Container Guide.

Running Dataverse in Windows WSL
--------------------------------

It is possible to run Dataverse in Windows 10 and 11 through WSL (Windows Subsystem for Linux).

Please note: these instructions have not been extensively tested. If you find any problems, please open an issue at https://github.com/IQSS/dataverse/issues.

Install WSL
~~~~~~~~~~~
If you have Docker already installed, you should already have WSL installed. Otherwise open PowerShell and run:

.. code-block:: powershell
  
   wsl --install

If you already had WSL installed you can install a specific Linux distribution:

See the list of possible distributions:

.. code-block:: powershell

  wsl --list --online

Choose the distribution you would like. Then run the following command. These instructions were tested with Ubuntu.

.. code-block:: powershell

  wsl --install -d <Distribution Name>

You will be asked to create a Linux user.
After the installation of Linux is complete, check that you have an Internet connection:

.. code-block:: bash

  ping www.google.com

If you do not have an Internet connection, try adding it in ``/etc/wsl.conf``

.. code-block:: bash
  
  [network]
  generateResolvConf = false

Also in ``/etc/resolv.conf`` add

.. code-block:: bash

  nameserver 1.1.1.1

Now you can install all the tools one usually uses in Linux. For example, it is good idea to run an update:

.. code-block:: bash

   sudo apt update
   sudo apt full-upgrade -y

Install Dataverse
~~~~~~~~~~~~~~~~~

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
