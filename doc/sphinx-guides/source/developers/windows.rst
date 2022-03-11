===================
Windows Development
===================

Development on Windows is not well supported, unfortunately. You will have a much easier time if you develop on Mac or Linux as described under :doc:`dev-environment` section.

Vagrant commands appear below and were tested on Windows 10 but the Vagrant environment is currently broken. Please see https://github.com/IQSS/dataverse/issues/6849

.. contents:: |toctitle|
	:local:

Running the Dataverse Software in Vagrant
-----------------------------------------

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

After a long while you hopefully will have a Dataverse installation available at http://localhost:8888

Improving Windows Support
-------------------------

Windows Subsystem for Linux
~~~~~~~~~~~~~~~~~~~~~~~~~~~

We have been unable to get Windows Subsystem for Linux (WSL) to work. We tried following the steps at https://docs.microsoft.com/en-us/windows/wsl/install-win10 but the "Get" button was greyed out when we went to download Ubuntu.

Discussion and Feedback
~~~~~~~~~~~~~~~~~~~~~~~

For more discussion of Windows support for Dataverse Software development see our community list thread `"Do you want to develop on Windows?" <https://groups.google.com/d/msg/dataverse-community/Hs9j5rIxqPI/-q54751aAgAJ>`_ We would be happy to incorporate feedback from Windows developers into this page. The :doc:`documentation` section describes how.
