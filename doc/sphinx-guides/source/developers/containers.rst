=================================
Docker, Kubernetes, and OpenShift
=================================

Dataverse is exploring the use of Docker, Kubernetes, OpenShift and other container-related technologies.

.. contents:: |toctitle|
	:local:

OpenShift
---------

From the Dataverse perspective, we are in the business of providing a "template" for OpenShift that describes how the various components we build our application on (Glassfish, PostgreSQL, Solr, the Dataverse war file itself, etc.) work together. We publish Docker images to DockerHub at https://hub.docker.com/u/iqss/ that are used in this OpenShift template.

Dataverse's (light) use of Docker is documented below in a separate section. We actually started with Docker in the context of OpenShift, which is why OpenShift is listed first but we can imagine rearranging this in the future.

The OpenShift template for Dataverse can be found at ``conf/openshift/openshift.json`` and if you need to hack on the template or related files under ``conf/docker`` it is recommended that you iterate on them using Minishift.

The instructions below will walk you through spinning up Dataverse within Minishift. It is recommended that you do this on the "develop" branch to make sure everything is working before changing anything.

Install Minishift
~~~~~~~~~~~~~~~~~

Minishift requires a hypervisor and since we already use VirtualBox for Vagrant, you should install VirtualBox from http://virtualbox.org .

Download the Minishift tarball from https://docs.openshift.org/latest/minishift/getting-started/installing.html and put the ``minishift`` binary in ``/usr/local/bin`` or somewhere in your ``$PATH``. This assumes Mac or Linux. These instructions were last tested on version ``v1.14.0+1ec5877`` of Minishift.

At this point, you might want to consider going through the Minishift quickstart to get oriented: https://docs.openshift.org/latest/minishift/getting-started/quickstart.html

Start Minishift
~~~~~~~~~~~~~~~

``minishift start --vm-driver=virtualbox --memory=4GB``

Make the OpenShift Client Binary (oc) Executable
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

``eval $(minishift oc-env)``

Log in to Minishift from the Command Line
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Note that if you just installed and started Minishift, you are probably logged in already. This ``oc login`` step is included in case you aren't logged in anymore.

``oc login --username developer --password=whatever``

Use "developer" as the username and a couple characters as the password.

Create a Minishift Project
~~~~~~~~~~~~~~~~~~~~~~~~~~

Calling the project "project1" is fairly arbitrary. We'll probably want to revisit this name in the future. A project is necessary in order to create an OpenShift app.

``oc new-project project1``

Note that ``oc projects`` will return a list of projects.

Create a Dataverse App within the Minishift Project
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The following command operates on the ``conf/openshift/openshift.json`` file that resides in the main Dataverse git repo. It will download images from Docker Hub and use them to spin up Dataverse within Minishift/OpenShift. Later we will cover how to make changes to the images on Docker Hub.

``oc new-app conf/openshift/openshift.json``

Log into Minishift and Visit Dataverse in your Browser
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

After running the ``oc new-app`` command above, deployment of Dataverse within Minishift/OpenShift will begin. You should log into the OpenShift web interface to check on the status of the deployment. If you just created the Minishift VM with the ``minishift start`` command above, the ``oc new-app`` step is expected to take a while because the images need to be downloaded from Docker Hub. Also, the installation of Dataverse takes a while.

Typing ``minishift console`` should open the OpenShift web interface in your browser. The IP address might not be "192.168.99.100" but it's used below as an example.

- https://192.168.99.100:8443 (or URL from ``minishift console``)
- username: developer
- password: <any password>

In the OpenShift web interface you should see a link that looks something like http://dataverse-project1.192.168.99.100.nip.io but the IP address will vary and will match the output of ``minishift ip``. Eventually, after deployment is complete, the Dataverse web interface will appear at this URL and you will be able to log in with the username "dataverseAdmin" and the password "admin".

Another way to verify that Dataverse has been succesfully deployed is to make sure that the Dataverse "info" API endpoint returns a version (note that ``minishift ip`` is used because the IP address will vary):

``curl http://dataverse-project1.`minishift ip`.nip.io/api/info/version``

From the perspective of OpenShift and the ``openshift.json`` config file, the HTTP link to Dataverse in called a route. See also documentation for ``oc expose``.

Troubleshooting
~~~~~~~~~~~~~~~

Here are some tips on troubleshooting your deployment of Dataverse to Minishift.

Check Status of Dataverse Deployment to Minishift
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

``oc status``

Once images have been downloaded from Docker Hub, the output below will change from ``Pulling`` to ``Pulled``.

``oc get events | grep Pull``

This is a deep dive:

``oc get all``

Review Logs of Dataverse Deployment to Minishift
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Logs are provided in the web interface to each of the deployment configurations. The URLs should be something like this (but the IP address) will vary and you should click "View Log". The installation of Dataverse is done within the one Glassfish deployment configuration:

- https://192.168.99.100:8443/console/project/project1/browse/dc/dataverse-glassfish
- https://192.168.99.100:8443/console/project/project1/browse/dc/dataverse-postgresql
- https://192.168.99.100:8443/console/project/project1/browse/dc/dataverse-solr

You can also see logs from each of the components (Glassfish, PostgreSQL, and Solr) from the command line with ``oc logs`` like this (just change the ``grep`` at the end):

``oc logs $(oc get po -o json | jq '.items[] | select(.kind=="Pod").metadata.name' -r | grep glassfish)``

Get a Shell (ssh/rsh) on Containers Deployed to Minishift
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

You can get a shell on any of the containers for each of the components (Glassfish, PostgreSQL, and Solr) with ``oc rc`` (just change the ``grep`` at the end):

``oc rsh $(oc get po -o json | jq '.items[] | select(.kind=="Pod").metadata.name' -r | grep glassfish)``

From the ``rsh`` prompt of the Glassfish container you could run something like the following to make sure that Dataverse is running on port 8080:

``curl http://localhost:8080/api/info/version``

Cleaning up
~~~~~~~~~~~

If you simply wanted to try out Dataverse on Minishift and want to clean up, you can run ``oc delete project project1`` to delete the project or ``minishift stop`` and ``minishift delete`` to delete the entire Minishift VM and all the Docker containers inside it.

Making Changes
~~~~~~~~~~~~~~

Making Changes to Docker Images
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If you're interested in using Minishift for development and want to change the Dataverse code, you will need to get set up to create Docker images based on your changes and push them to a Docker registry such as Docker Hub (or Minishift's internal registry, if you can get that working, mentioned below). See the section below on Docker for details.

Using Minishift for day to day Dataverse development might be something we want to investigate in the future. These blog posts talk about developing Java applications using Minishift/OpenShift:

- https://blog.openshift.com/fast-iterative-java-development-on-openshift-kubernetes-using-rsync/
- https://blog.openshift.com/debugging-java-applications-on-openshift-kubernetes/

Making Changes to the OpenShift Config
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If you are interested in changing the OpenShift config file for Dataverse at ``conf/openshift/openshift.json`` note that in many cases once you have Dataverse running in Minishift you can use ``oc process`` and ``oc apply`` like this (but please note that some errors and warnings are expected):

``oc process -f conf/openshift/openshift.json | oc apply -f -``

The slower way to iterate on the ``openshift.json`` file is to delete the project and re-create it.

Running Containers to Run as Root in Minishift
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

It is **not** recommended to run containers as root in Minishift because for security reasons OpenShift doesn't support running containers as root. However, it's good to know how to allow containers to run as root in case you need to work on a Docker image to make it run as non-root.

For more information on improving Docker images to run as non-root, see "Support Arbitrary User IDs" at https://docs.openshift.org/latest/creating_images/guidelines.html#openshift-origin-specific-guidelines

Let's say you have a container that you suspect works fine when it runs as root. You want to see it working as-is before you start hacking on the Dockerfile and entrypoint file. You can configure Minishift to allow containers to run as root with this command:

``oc adm policy add-scc-to-user anyuid -z default --as system:admin``

Once you are done testing you can revert Minishift back to not allowing containers to run as root with this command:

``oc adm policy remove-scc-from-user anyuid -z default --as system:admin``

Minishift Resources
~~~~~~~~~~~~~~~~~~~

The following resources might be helpful.

- https://blog.openshift.com/part-1-from-app-to-openshift-runtimes-and-templates/
- https://blog.openshift.com/part-2-creating-a-template-a-technical-walkthrough/
- https://docs.openshift.com/enterprise/3.0/architecture/core_concepts/templates.html

Docker
------

From the Dataverse perspective, Docker is important for a few reasons:

- We are thankful that NDS Labs did the initial work to containerize Dataverse and include it in the "workbench" we mention in the :doc:`/installation/prep` section of the Installation Guide. The workbench allows people to kick the tires on Dataverse.
- There is interest from the community in running Dataverse on OpenShift and some initial work has been done to get Dataverse running on Minishift in Docker containers. Minishift makes use of Docker images on Docker Hub. To build new Docker images and push them to Docker Hub, you'll need to install Docker. The main issue to follow is https://github.com/IQSS/dataverse/issues/4040 .
- Docker may aid in testing efforts if we can easily spin up Docker images based on code in pull requests and run the full integration suite against those images. See the :doc:`testing` section for more information on integration tests.

Installing Docker
~~~~~~~~~~~~~~~~~

On Linux, you can probably get Docker from your package manager.

On Mac, download the ``.dmg`` from https://www.docker.com and install it. As of this writing is it known as Docker Community Edition for Mac.

On Windows, FIXME ("Docker Community Edition for Windows" maybe???).

As explained above, we use Docker images in two different contexts:

- Testing using an "all in one" Docker image (ephemeral, unpublished)
- Future production use on Minishift/OpenShift/Kubernetes (published to Docker Hub)

All In One Docker Images for Testing
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The "all in one" Docker files are in ``conf/docker-aio`` and you should follow the readme in that directory for more information on how to use them.

Future production use on Minishift/OpenShift/Kubernetes
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

When working with Docker in the context of Minishift, follow the instructions above and make sure you get the Dataverse Docker images running in Minishift before you start messing with them.

As of this writing, the Dataverse Docker images we publish under https://hub.docker.com/u/iqss/ are highly experimental. They were originally tagged with branch names like ``kick-the-tires`` and as of this writing the ``latest`` tag should be considered highly experimental and not for production use. See https://github.com/IQSS/dataverse/issues/4040 for the latest status and please reach out if you'd like to help!

Change to the docker directory:

``cd conf/docker``

Edit one of the files:

``vim dataverse-glassfish/Dockerfile``

At this point you want to build the image and run it. We are assuming you want to run it in your Minishift environment. We will be building your image and pushing it to Docker Hub. Then you will be pulling the image down from Docker Hub to run in your Minishift installation. If this sounds inefficient, you're right, but we haven't been able to figure out how to make use of Minishift's built in registry (see below) so we're pushing to Docker Hub instead.

Log in to Docker Hub with an account that has access to push to the ``iqss`` organization:

``docker login``

(If you don't have access to push to the ``iqss`` organization, you can push elsewhere and adjust your ``openshift.json`` file accordingly.)

Build and push the images to Docker Hub:

``./build.sh``

Note that you will see output such as ``digest: sha256:213b6380e6ee92607db5d02c9e88d7591d81f4b6d713224d47003d5807b93d4b`` that should later be reflected in Minishift to indicate that you are using the latest image you just pushed to Docker Hub.

You can get a list of all repos under the ``iqss`` organization with this:

``curl https://hub.docker.com/v2/repositories/iqss/``

To see a specific repo:

``curl https://hub.docker.com/v2/repositories/iqss/dataverse-glassfish/``

Known Issues with Dataverse Images on Docker Hub
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Again, Dataverse Docker images on Docker Hub are highly experimental at this point. As of this writing, their purpose is primarily for kicking the tires on Dataverse. Here are some known issues:

- The Dataverse installer is run in the entrypoint script every time you run the image. Ideally, Dataverse would be installed in the Dockerfile instead. Dataverse is being installed in the entrypoint script because it needs PosgreSQL to be up already so that database tables can be created when the war file is deployed.
- The storage should be abstracted. Storage of data files and PostgreSQL data. Probably Solr data.
- Better tuning of memory by examining ``/sys/fs/cgroup/memory/memory.limit_in_bytes`` and incorporating this into the Dataverse installation script.
- Only a single Glassfish server can be used. See "Dedicated timer server in a Dataverse server cluster" in the :doc:`/admin/timers` section of the Installation Guide.
- Only a single PostgreSQL server can be used.
- Only a single Solr server can be used.

Get Set Up to Push Docker Images to Minishift Registry
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

FIXME https://docs.openshift.org/latest/minishift/openshift/openshift-docker-registry.html indicates that it should be possible to make use of the builtin registry in Minishift while iterating on Docker images but you may get "unauthorized: authentication required" when trying to push to it as reported at https://github.com/minishift/minishift/issues/817 so until we figure this out, you must push to Docker Hub instead. Run ``docker login`` and use the ``conf/docker/build.sh`` script to push Docker images you create to https://hub.docker.com/u/iqss/

If you want to troubleshoot this, take a close look at the ``docker login`` command you're using to make sure the OpenShift token is being sent.

An alternative to using the the Minishift Registry is to do a local build. This isn't documented but should work within Minishift because it's an all-in-one OpenShift environment. The steps at a high level are to ssh into the Minishift VM and then do a ``docker build``. For a stateful set, the image pull policy should be never.

----

Previous: :doc:`coding-style` | Next: :doc:`making-releases`
