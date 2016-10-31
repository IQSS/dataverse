=======
SELinux
=======

.. contents:: :local:

Introduction
------------

The ``shibboleth.te`` file below that is mentioned in the :doc:`/installation/shibboleth` section of the Installation Guide was created on CentOS 6 as part of https://github.com/IQSS/dataverse/issues/3406 but may need to be revised for future versions of RHEL/CentOS. The file is versioned with the docs and can be found in the following location:

``doc/sphinx-guides/source/_static/installation/files/etc/selinux/targeted/src/policy/domains/misc/shibboleth.te``

.. literalinclude:: ../_static/installation/files/etc/selinux/targeted/src/policy/domains/misc/shibboleth.te
   :language: text

This document is something of a survival guide for anyone who is tasked with updating this file.

Development Environment
-----------------------

In order to work on the ``shibboleth.te`` file you need to ``ssh`` into a RHEL or CentOS box running Shibboleth (instructions are in the :doc:`/installation/shibboleth` section of the Installation Guide) such as https://beta.dataverse.org or https://demo.dataverse.org that has all the commands below installed. As of this writing, the ``policycoreutils-python`` RPM was required.

Recreating the shibboleth.te File
---------------------------------

If you're reading this page because someone has reported that Shibboleth doesn't work with SELinux anymore (due to an operating system upgrade, perhaps) you *could* start with the existing ``shibboleth.te`` file, but it is recommended that you create a new one instead to ensure that extra lines aren't included that are no longer necessary.

The file you're recreating is called a Type Enforcement (TE) file, and you can read more about it at https://access.redhat.com/documentation/en-US/Red_Hat_Enterprise_Linux/6/html/Security-Enhanced_Linux/chap-Security-Enhanced_Linux-SELinux_Contexts.html

The following doc may or may not be helpful to orient you: https://access.redhat.com/documentation/en-US/Red_Hat_Enterprise_Linux/6/html/Security-Enhanced_Linux/sect-Security-Enhanced_Linux-Fixing_Problems-Allowing_Access_audit2allow.html

Ensure that SELinux is Enforcing
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

If ``getenforce`` returns anything other than ``Enforcing``, run ``setenforce Enforcing`` or otherwise configure SELinux by editing ``/etc/selinux/config`` and rebooting until SELinux is enforcing.

Removing the Existing shibboleth.te Rules
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Use ``semodule -l | grep shibboleth`` to see if the ``shibboleth.te`` rules are already installed. Run ``semodule -r shibboleth`` to remove the module, if necessary. Now we're at square one (no custom rules) and ready to generate a new ``shibboleth.te`` file.

Exercising SELinux denials
~~~~~~~~~~~~~~~~~~~~~~~~~~

As of this writing, there are two optional components of Dataverse that are known not to work with SELinux out of the box with SELinux: Shibboleth and rApache.

We will be exercising SELinux denials with Shibboleth, and the SELinux-related issues are expected out the box:

- Problems with the dropdown of institutions being created on the Login Page ("Internal Error - Failed to download metadata from /Shibboleth.sso/DiscoFeed.").
- Problems with the return trip after you've logged into HarvardKey or whatever ("shibsp::ListenerException" and "Cannot connect to shibd process, a site adminstrator should be notified.").

In short, all you need to do is try to log in with Shibboleth and you'll see problems associated with SELinux being enabled.

Stub out the new shibboleth.te file
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Iterate on the new ``shibboleth.te`` file wherever you like, such as the root user's home directory in the example below. Start by adding a ``module`` line like this:

``echo 'module shibboleth 1.0;' > /root/shibboleth.te``

Note that a version is required and perhaps it should be changed, but we'll stick with ``1.0`` for now. The point is that the ``shibboleth.te`` file must begin with that "module" line or else the ``checkmodule`` command you'll need to run later will fail. Your file should look like this:

.. code-block:: text

        module shibboleth 1.0;
        # require lines go here
        # allow lines go here

Iteratively Use audit2allow to Add Rules and Test Your Change
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Now that ``shibboleth.te`` has been stubbed out, we will iteratively add lines to it from the output of piping SELinux Access Vector Cache (AVC) denial messages to ``audit2allow -r``. These errors are found in ``/var/log/audit/audit.log`` so tail the file as you attempt to log in to Shibboleth.

``# tail -f /var/log/audit/audit.log | fgrep type=AVC``

You should see messages that look something like this:

``type=AVC msg=audit(1476728970.378:271405): avc:  denied  { write } for  pid=28548 comm="httpd" name="shibd.sock" dev=dm-2 ino=393300 scontext=unconfined_u:system_r:httpd_t:s0 tcontext=unconfined_u:object_r:var_run_t:s0 tclass=sock_file``

Next, pipe these message to ``audit2allow -r`` like this:

``echo 'type=AVC msg=audit(1476728970.378:271405): avc:  denied  { write } for  pid=28548 comm="httpd" name="shibd.sock" dev=dm-2 ino=393300 scontext=unconfined_u:system_r:httpd_t:s0 tcontext=unconfined_u:object_r:var_run_t:s0 tclass=sock_file' | audit2allow -r``

This will produce output like this:

.. code-block:: text

        require {
                type var_run_t;
                type httpd_t;
                class sock_file write;
        }

        #============= httpd_t ==============
        allow httpd_t var_run_t:sock_file write;

Copy and paste this output into the ``shibboleth.te`` file you stubbed out above. Then, use the same ``checkmodule``, ``semodule_package``, and ``semodule`` commands documented in the :doc:`/installation/shibboleth` section of the Installation Guide on your file to activate the SELinux rules you're constructing.

Once your updated SELinux rules are in place, try logging in with Shibboleth again. You should see a different AVC error. Pipe that error into ``audit2allow -r`` as well and put the resulting content into the ``shibboleth.te`` file you're constructing. As you do this, manually reformat the file using the following rules:

- Put the ``require`` block at the top.
- Within the require block, sort the lines.
- Put the ``allow`` lines at the bottom and sort them.
- Where possible, avoid duplicate lines by combining operations such as ``open`` and ``read`` into ``{open read}``.
- Remove all comment lines.

Keep iterating until it works and then create a pull request based on your updated file. Good luck!

Many thanks to Bill Horka from IQSS for his assistance in explaining how to construct a SELinux Type Enforcement (TE) file!
