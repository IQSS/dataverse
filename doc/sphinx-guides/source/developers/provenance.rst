==========
Provenance
==========

.. contents:: |toctitle|
    :local:

Introduction
------------

Support for "provenance" is being added to Dataverse. Provenance is metadata that describes the history of a digital object: where it came from, how it came to be in its present state, who or what acted upon it, etc.

Running the Provenance System as Part of Your Development Environment
---------------------------------------------------------------------

See the :doc:`dev-environment` section for how to run the prov system as a developer. You will need this system running to work on features related to provenance in Dataverse.

Building the Provenance RPM
---------------------------

Dataverse maintains RPM spec files to create an RPM of provenance components for installation on RHEL/CentOS.

In the Dataverse source tree, the RPM spec file can be found at ``doc/sphinx-guides/source/_static/developers/prov/libcpl.spec`` or :download:`downloaded <../_static/developers/prov/libcpl.spec>`.

We are using Vagrant to build the RPM. Your starting point should be the ``Vagrantfile`` in the root of this git repo, the main repo for Dataverse. In that ``Vagrantfile``, temporarily comment out any shell provisioning lines such as ``config.vm.provision "shell", path: "scripts/vagrant/setup.sh"``. We don't need to install Dataverse to build the prov RPM. We just need a bare-bones CentOS environment onto which we will install the packages we need to build the RPM.

Once you have disabled the setup scripts, start up the VM:

``vagrant up``

Then, ssh into the VM:

``vagrant ssh``

The following steps are all done in the VM. Install the necessary RPMs (add the ``scl`` repo first and separately:

``sudo yum install -y centos-release-scl``

``sudo yum install -y redhat-lsb-core devtoolset-7 boost-devel unixODBC-devel rpm-build rpmdevtools``

Create directories used by rpmbuild:

``mkdir -p ~/rpmbuild/{BUILD,RPMS,SOURCES,SPECS,SRPMS}``

Change to the directory with the spec file:

``cd /dataverse/doc/sphinx-guides/source/_static/developers/prov``

Download the sources:

``spectool -g libcpl.spec -C ~/rpmbuild/SOURCES``

Build the RPM:

``rpmbuild -ba libcpl.spec``

Verify the files in the RPM you've built:

``rpm -qpl /home/vagrant/rpmbuild/RPMS/x86_64/libcpl-3.0-1.x86_64.rpm``

The output should include files like ``/usr/lib64/libcpl.so``.

If your task is to update the RPM, bump the version or release number in the spec and add a changelog entry to the end. The resulting RPM is so small (~200KB) that we host it right in the guides at ``doc/sphinx-guides/source/_static/installation/files/home/rpmbuild/rpmbuild/RPMS/x86_64``. From the Vagrant environment, you can copy over a new RPM like this:

``cp /home/vagrant/rpmbuild/RPMS/x86_64/libcpl-3*.rpm /dataverse/doc/sphinx-guides/source/_static/installation/files/home/rpmbuild/rpmbuild/RPMS/x86_64``


----

Previous: :doc:`big-data-support`
