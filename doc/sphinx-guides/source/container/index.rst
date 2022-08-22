Container Guide
===============

**Contents:**

.. toctree::

  base-image
  app-image

Running Dataverse software in containers is quite different than in a :doc:`classic installation <../installation/prep>`.

Both approaches have pros and cons. These days (2022) containers are very often used for development and testing,
but there is an ever rising move for running applications in the cloud using container technology.

**NOTE:**
**As the "Institute for Quantitative Social Sciences" at Harvard is running their installations in the classic
deployment way, the container support is mostly created and maintained by the Dataverse community.**

This guide is *not* about installation on technology like Docker Swarm, Kubernetes, Rancher or other
solutions to run containers in production. There is the `Dataverse on K8s project <https://k8s-docs.gdcc.io>`_ for this
purpose.

This guide focuses on describing the container images managed from the main Dataverse repository (again: by the
community, not IQSS), their features and limitations. Instructions on how to build the images yourself, how to
extend them and how to use them for development purposes may be found in respective subpages.