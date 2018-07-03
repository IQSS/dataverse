===================
Windows Development
===================

Development on Windows is not well supported, unfortunately. You will have a much easier time if you develop on Mac or Linux as described under :doc:`dev-environment` section.

If you want to try using Windows for Dataverse development, your best best is to use Vagrant, as described below. Minishift is also an option. These instructions were tested on Windows 10.

.. contents:: |toctitle|
	:local:

Running Dataverse in Vagrant
----------------------------

Install Vagrant
~~~~~~~~~~~~~~~

Download and install Vagrant from https://www.vagrantup.com

Vagrant advises you to reboot but let's install VirtualBox first.

Install VirtualBox
~~~~~~~~~~~~~~~~~~

Download and install VirtualBox from https://www.virtualbox.org

Note that we saw an error saying "Oracle VM VirtualBox 5.2.8 Setup Wizard ended prematurely" but then we re-ran the installer and it seemed to work.

Reboot
~~~~~~

Again, Vagrant asks you to reboot, so go ahead.

Install Git
~~~~~~~~~~~

Download and install Git from https://git-scm.com

Configure Git to use Unix Line Endings
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Launch Git Bash and run the following commands:

``git config --global core.autocrlf input``

Pro tip: Use Shift-Insert to paste into Git Bash.

See also https://help.github.com/articles/dealing-with-line-endings/

If you skip this step you are likely to see the following error when you run ``vagrant up``.

``/tmp/vagrant-shell: ./install: /usr/bin/perl^M: bad interpreter: No such file or directory``

Clone Git Repo
~~~~~~~~~~~~~~

From Git Bash, run the following command:

``git clone https://github.com/IQSS/dataverse.git``

vagrant up
~~~~~~~~~~

From Git Bash, run the following commands:

``cd dataverse``

The ``dataverse`` directory you changed is the one you just cloned. Vagrant will operate on a file called ``Vagrantfile``.

``vagrant up``

After a long while you hopefully will have Dataverse installed at http://localhost:8888

Running Dataverse in Minishift
------------------------------

Minishift is a dev environment for OpenShift, which is Red Hat's distribution of Kubernetes.  The :doc:`containers` section contains much more detail but the essential steps for using Minishift on Windows are described below.

Install VirtualBox
~~~~~~~~~~~~~~~~~~

Download and install VirtualBox from https://www.virtualbox.org

Install Git
~~~~~~~~~~~

Download and install Git from https://git-scm.com

Install Minishift
~~~~~~~~~~~~~~~~~

Download Minishift from https://docs.openshift.org/latest/minishift/getting-started/installing.html . It should be a zip file.

From Git Bash:

``cd ~/Downloads``

``unzip minishift*.zip``

``mkdir ~/bin``

``cp minishift*/minishift.exe ~/bin``

Clone Git Repo
~~~~~~~~~~~~~~

From Git Bash, run the following commands:

``git config --global core.autocrlf input``

``git clone https://github.com/IQSS/dataverse.git``

Start Minishift VM and Run Dataverse
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

``minishift start --vm-driver=virtualbox --memory=8GB``

``eval $(minishift oc-env)``

``oc new-project project1``

``cd ~/dataverse``

``oc new-app conf/openshift/openshift.json``

``minishift console``

This should open a web browser. In Microsoft Edge we saw ``INET_E_RESOURCE_NOT_FOUND`` so if you see that, try Chrome instead. A cert error is expected. Log in with the username "developer" and any password such as "asdf".

Under "Overview" you should see a URL that has "dataverse-project1" in it. You should be able to click it and log into Dataverse with the username "dataverseAdmin" and the password "admin".

Improving Windows Support
-------------------------

Windows Subsystem for Linux
~~~~~~~~~~~~~~~~~~~~~~~~~~~

We have been unable to get Windows Subsystem for Linux (WSL) to work. We tried following the steps at https://docs.microsoft.com/en-us/windows/wsl/install-win10 but the "Get" button was greyed out when we went to download Ubuntu.

Discussion and Feedback
~~~~~~~~~~~~~~~~~~~~~~~

For more discussion of Windows support for Dataverse development see our community list thread `"Do you want to develop on Windows?" <https://groups.google.com/d/msg/dataverse-community/Hs9j5rIxqPI/-q54751aAgAJ>`_ We would be happy to inconrporate feedback from Windows developers into this page. The :doc:`documentation` section describes how.
