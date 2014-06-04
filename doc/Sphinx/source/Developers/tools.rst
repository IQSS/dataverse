=====
Tools
=====

PageKite
++++++++

PageKite is a fantastic service that can be used to share your
local development environment over the Internet on a public IP address.

With PageKite running on your laptop, the world can access a URL such as
http://pdurbin.pagekite.me to see what you see at http://localhost:8080

Sign up at https://pagekite.net and follow the installation instructions or simply download https://pagekite.net/pk/pagekite.py

The first time you run ``./pagekite.py`` a file at ``~/.pagekite.rc`` will be
created. You can edit this file to configure PageKite to serve up port 8080
(the default GlassFish HTTP port) or the port of your choosing.

According to https://pagekite.net/support/free-for-foss/ PageKite (very generously!) offers free accounts to developers writing software the meets http://opensource.org/docs/definition.php such as Dataverse.

Vagrant
+++++++

Vagrant allows you to spin up a virtual machine running Dataverse on
your development workstation.

From the root of the git repo, run ``vagrant up`` and eventually you
should be able to reach an installation of Dataverse at
http://localhost:8888 (or whatever forwarded_port indicates in the
Vagrantfile)
