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

``minishift start --vm-driver=virtualbox --memory=8GB``

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

If you're interested in using Minishift for development and want to change the Dataverse code, you will need to get set up to create Docker images based on your changes and make them available within Minishift.

It is recommended to add experimental images to Minishift's internal registry. Note that despite what https://docs.openshift.org/latest/minishift/openshift/openshift-docker-registry.html says you will not use ``docker push`` because we have seen "unauthorized: authentication required” when trying to push to it as reported at https://github.com/minishift/minishift/issues/817 . Rather you will run ``docker build`` and run ``docker images`` to see that your newly build images are listed in Minishift's internal registry.

First, set the Docker environment variables so that ``docker build`` and ``docker images`` refer to the internal Minishift registry rather than your normal Docker setup:

``eval $(minishift docker-env)``

When you're ready to build, change to the right directory:

``cd conf/docker``

And then run the build script in "internal" mode:

``./build.sh internal``

Note that ``conf/openshift/openshift.json`` must not have ``imagePullPolicy`` set to ``Always`` or it will pull from "iqss" on Docker Hub. Changing it to ``IfNotPresent`` allow Minishift to use the images shown from ``docker images`` rather than the ones on Docker Hub.

Using Minishift for day to day Dataverse development might be something we want to investigate in the future. These blog posts talk about developing Java applications using Minishift/OpenShift:

- https://blog.openshift.com/fast-iterative-java-development-on-openshift-kubernetes-using-rsync/
- https://blog.openshift.com/debugging-java-applications-on-openshift-kubernetes/

Making Changes to the OpenShift Config
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If you are interested in changing the OpenShift config file for Dataverse at ``conf/openshift/openshift.json`` note that in many cases once you have Dataverse running in Minishift you can use ``oc process`` and ``oc apply`` like this (but please note that some errors and warnings are expected):

``oc process -f conf/openshift/openshift.json | oc apply -f -``

The slower way to iterate on the ``openshift.json`` file is to delete the project and re-create it.

Making Changes to the PostgreSQL Database from the Glassfish Pod
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

You can access and modify the PostgreSQL database via an interactive terminal called psql.

To log in to psql from the command line of the Glassfish pod, type the following command:

``PGPASSWORD=$POSTGRES_PASSWORD; export PGPASSWORD; /usr/bin/psql -h $POSTGRES_SERVER.$POSTGRES_SERVICE_HOST -U $POSTGRES_USER -d $POSTGRES_DATABASE``

To log in as an admin, type this command instead:

``PGPASSWORD=$POSTGRESQL_ADMIN_PASSWORD; export PGPASSWORD; /usr/bin/psql -h $POSTGRES_SERVER.$POSTGRES_SERVICE_HOST -U postgres -d $POSTGRES_DATABASE``

Scaling Dataverse by Increasing Replicas in a StatefulSet
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Glassfish, Solr and PostgreSQL Pods are in a "StatefulSet" which is a concept from OpenShift and Kubernetes that you can read about at https://kubernetes.io/docs/concepts/workloads/controllers/statefulset/

As of this writing, the ``openshift.json`` file has a single "replica" for each of these two stateful sets. It's possible to increase the number of replicas from 1 to 3, for example, with this command:

``oc scale statefulset/dataverse-glassfish --replicas=3``

The command above should result in two additional Glassfish pods being spun up. The name of the pods is significant and there is special logic in the "zeroth" pod ("dataverse-glassfish-0" and "dataverse-postgresql-0"). For example, only "dataverse-glassfish-0" makes itself the dedicated timer server as explained in :doc:`/admin/timers` section of the Admin Guide. "dataverse-glassfish-1" and other higher number pods will not be configured as a timer server.

Once you have multiple Glassfish servers you may notice bugs that will require additional configuration to fix. One such bug has to do with Dataverse logos which are stored at ``/usr/local/glassfish4/glassfish/domains/domain1/docroot/logos`` on each of the Glassfish servers. This means that the logo will look fine when you just uploaded it because you're on the server with the logo on the local file system but when you visit that dataverse in the future and you're on a differernt Glassfish server, you will see a broken image. (You can find some discussion of this logo bug at https://github.com/IQSS/dataverse-aws/issues/10 and http://irclog.iq.harvard.edu/dataverse/2016-10-21 .) This is all "advanced" installation territory (see the :doc:`/installation/advanced` section of the Installation Guide) and OpenShift might be a good environment in which to work on some of these bugs.

Multiple PostgreSQL servers are possible within the OpenShift environment as well and have been set up with some amount of replication. "dataverse-postgresql-0" is the master and non-zero pods are the slaves. We have just scratched the surface of this configuration but replication from master to slave seems to we working. Future work could include failover and making Dataverse smarter about utilizing multiple PostgreSQL servers for reads. Right now we assume Dataverse is only being used with a single PostgreSQL server and that it's the master.

Solr supports index distribution and replication for scaling. For OpenShift use, we choose replication. It's possible to scale up Solr using the method method similar to Glassfish, as mentioned aboved
In OpenShift, the first Solr pod, dataverse-solr-0, will be the master node, and the rest will be slave nodes


Configuring Persistent Volumes and Solr master node recovery 
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Solr requires backing up the search index to persistent storage. For our proof of concept, we configure a hostPath, which allows Solr containers to access the hosts' file system, for our Solr containers backups. To read more about OpenShift/Kubernetes' persistent volumes, please visit: https://kubernetes.io/docs/concepts/storage/persistent-volumes

To allow containers to use a host's storage, we need to allow access to that directory first. In this example, we expose /tmp/share to the containers::

# mkdir /tmp/share            
# chcon -R -t svirt_sandbox_file_t 
# chgrp root -R /tmp/share 
# oc login -u system:admin 
# oc edit scc restricted     # Update allowHostDirVolumePlugin to true and runAsUser type to RunAsAny


To add a persistent volume and persistent volume claim, in conf/docker/openshift/openshift.json, add the following to objects in openshift.json.
Here, we are using hostPath for development purposes. Since OpenShift supports many types of cluster storages, 
if the administrator wishes to use any cluster storage like EBS, Google Cloud Storage, etc, they would have to use a different type of Persistent Storage::

    {
      "kind" : "PersistentVolume",
      "apiVersion" : "v1",
      "metadata":{
        "name" : "solr-index-backup",
        "labels":{
          "name" : "solr-index-backup",
          "type" : "local"
        }
      },
      "spec":{
        "capacity":{
          "storage" : "8Gi"
        },
        "accessModes":[
          "ReadWriteMany", "ReadWriteOnce",  "ReadOnlyMany"
        ],
        "hostPath": {
          "path" : "/tmp/share"
        }
      }
    },
    {
      "kind" : "PersistentVolumeClaim",
      "apiVersion": "v1",
      "metadata": {
        "name": "solr-claim"
      },
      "spec": {
        "accessModes": [
          "ReadWriteMany", "ReadWriteOnce",  "ReadOnlyMany"
        ],
        "resources": {
          "requests": {
            "storage": "3Gi"
          }
        },
        "selector":{
          "matchLabels":{
            "name" : "solr-index-backup",
            "type" : "local"
            }
          }
        }
      }


To make solr container mount the hostPath, add the following part under .spec.spec (for Solr StatefulSet)::

    {
      "kind": "StatefulSet",
      "apiVersion": "apps/v1beta1",
	  "metadata": {
        "name": "dataverse-solr",
	  ....

      "spec": {
        "serviceName" : "dataverse-solr-service",
		.....

          "spec": {
            "volumes": [
              {
                "name": "solr-index-backup",
                "persistentVolumeClaim": {
                  "claimName": "solr-claim"
                }
              }
            ],

			"containers": [
              ....

                "volumeMounts":[
                  {
                    "mountPath" : "/var/share",
                    "name" : "solr-index-backup"
                  }  



Solr is now ready for backup and recovery. In order to backup::

  oc rsh dataverse-solr-0
  curl 'http://localhost:8983/solr/collection1/replication?command=backup&location=/var/share'  


In solr entrypoint.sh, it's configured so that if dataverse-solr-0 failed, it will get the latest version of the index in the backup and restore. All backups are stored in /tmp/share in the host, or /home/share in solr containers.

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

On Windows, we have heard reports of success using Docker on a Linux VM running in VirtualBox or similar. There's something called "Docker Community Edition for Windows" but we haven't tried it. See also the :doc:`windows` section.

As explained above, we use Docker images in two different contexts:

- Testing using an "all in one" Docker image (ephemeral, unpublished)
- Future production use on Minishift/OpenShift/Kubernetes (published to Docker Hub)

All In One Docker Images for Testing
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The "all in one" Docker files are in ``conf/docker-aio`` and you should follow the readme in that directory for more information on how to use them.

Future production use on Minishift/OpenShift/Kubernetes
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

FIXME: rewrite this section to talk about only pushing stable images to Docker Hub.

When working with Docker in the context of Minishift, follow the instructions above and make sure you get the Dataverse Docker images running in Minishift before you start messing with them.

As of this writing, the Dataverse Docker images we publish under https://hub.docker.com/u/iqss/ are highly experimental. They were originally tagged with branch names like ``kick-the-tires`` and as of this writing the ``latest`` tag should be considered highly experimental and not for production use. See https://github.com/IQSS/dataverse/issues/4040 for the latest status and please reach out if you'd like to help!

Change to the docker directory:

``cd conf/docker``

Edit one of the files:

``vim dataverse-glassfish/Dockerfile``

At this point you want to build the image and run it. We are assuming you want to run it in your Minishift environment. We will be building your image and pushing it to Docker Hub.

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

----

Previous: :doc:`deployment` | Next: :doc:`making-releases`
