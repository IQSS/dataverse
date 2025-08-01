===================
Windows Development
===================

.. contents:: |toctitle|
       :local:

Running Dataverse in Windows WSL
--------------------------------

The simplest method to run Dataverse in Windows 10 and 11 is using Docker and Windows Subsystem for Linux (WSL) - specifically WSL 2. 
Once Docker and WSL are installed, you can follow the :ref:`quickstart instructions <container-dev-quickstart>`.

Please note: these instructions have not been extensively tested. They have been found to work with the Ubuntu-24.04 distribution for WSL. If you find any problems, please open an issue at https://github.com/IQSS/dataverse/issues and/or submit a PR to update this guide.

Install Docker Desktop
~~~~~~~~~~~~~~~~~~~~~~

Follow the directions at https://www.docker.com to install Docker Desktop on Windows. If prompted, turn on WSL 2 during installation.

Settings you may need in Docker Desktop:

* **General/Expose daemon on tcp://localhost:2375 without TLS**: true
* **General/Use the WSL 2 based engine**: true
* **General/Add the \*.docker.internal names to the host's /etc/hosts file (Requires password)**: true
* **Resources/WSL Integration/Enable integration with my default WSL distro**: true
* **Resources/WSL Integration/Enable integration with additional distros**: select any you run Dataverse in

Install WSL
~~~~~~~~~~~
If you install Docker Desktop, you should already have WSL installed. If not, or if you wish to add an additional Linux distribution, open PowerShell.

If WSL itself is not installed run:
 
.. code-block:: powershell
  
   wsl --install

For use with Docker, you should use WSL v2 - run:

.. code-block:: powershell
  
   wsl  --set-default-version 2

Install a specific Linux distribution. To see the list of possible distributions:

.. code-block:: powershell

  wsl --list --online

Choose the distribution you would like. Then run the following command. These instructions were tested with ``Ubuntu 24.04 LTS``.

.. code-block:: powershell

  wsl --install -d <Distribution Name>

You will be asked to create an initial Linux user.

.. note::
   Using wsl --set-version to upgrade an existing distribution from WSL 1 to WSL 2 may not work - installing a new distribution using WSL 2 is recommended.

Prepare WSL
~~~~~~~~~~~

Once that you have WSL installed, You will need Java and MVN working inside WSL, how you go about this will depend on the Linux distribution you installed in WSL.

Here is an example using SDKMAN, which is not required, but it is recommended for managing Java and other SDKs.

.. code-block:: bash

   sudo apt update
   sudo apt install zip

.. code-block:: bash

   sudo apt update
   sudo apt install unzip

.. code-block:: bash

   curl -s "https://get.sdkman.io" | bash
   source "$HOME/.sdkman/bin/sdkman-init.sh"

.. code-block:: bash

   sdk install java 17.0.7-tem

.. code-block:: bash

   sdk install maven

Install Dataverse
~~~~~~~~~~~~~~~~~

Open a Linux terminal (e.g. use Windows Terminal and open a tab for the Linux distribution you selected). Then install Dataverse in WSL following the :ref:`quickstart instructions <container-dev-quickstart>`. You should then have a working Dataverse instance.

We strongly recommend that you clone the Dataverse repository from WSL, not from Windows. This will ensure that builds are much faster.

IDEs for Dataverse in Windows
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You can use your favorite editor or IDE to edit Dataverse project files. Files in WSL are accessible from Windows for editing using the path ``\\wsl.localhost``. Your Linux distribution files should also be visible in File Explorer under the This PC/Linux entry.

.. note:: FYI: For the best performance, it is recommended, with WSL 2, to store Dataverse files in the WSL/Linux file system and to access them from there with your Windows-based IDE (versus storing Dataverse files in your Windows file system and trying to run maven and build from Linux - access to /mnt/c files using WSL 2 is slow).

pgAdmin in Windows for Dataverse
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You can access the Dataverse database from Windows.

Install pgAdmin from https://www.pgadmin.org/download/pgadmin-4-windows/

In pgAdmin, register a server using ``127.0.0.1`` with port ``5432``. For the database name, username, and password, see :ref:`db-name-creds`. Now you will be able to access, monitor, and update the Dataverse database. 
