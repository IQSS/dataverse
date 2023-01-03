Container Guide
===============

Running the Dataverse software in containers is quite different than in a :doc:`standard installation <../installation/prep>`.

Both approaches have pros and cons. These days, containers are very often used for development and testing,
but there is an ever rising move toward running applications in the cloud using container technology.

**NOTE:**
**As the Institute for Quantitative Social Sciences (IQSS) at Harvard is running a standard, non-containerized installation,
container support described in this guide is mostly created and maintained by the Dataverse community on a best-effort
basis.**

This guide is *not* about installation on technology like Docker Swarm, Kubernetes, Rancher or other
solutions to run containers in production. There is the `Dataverse on K8s project <https://k8s-docs.gdcc.io>`_ for this
purpose, as mentioned in the :doc:`/developers/containers` section of the Developer Guide.

This guide focuses on describing the container images managed from the main Dataverse repository (again: by the
community, not IQSS), their features and limitations. Instructions on how to build the images yourself and how to
develop and extend them further are provided.

**Contents:**

.. toctree::

  base-image

