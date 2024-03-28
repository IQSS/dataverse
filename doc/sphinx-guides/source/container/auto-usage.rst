Automation Usage
================

With containers, a lot of automation becomes easier or is even enabled in the first place. Using containers
for Continuous Integration, Continuous Deployment or even fancy Continuous Benchmarking opens up a complete new
range of possibilities.

This section describes how the upstream project ships ready to use container images in an automated fashion
using Github Actions.

.. contents:: |toctitle|
    :local:

Continuous Deployment for Container Images
------------------------------------------

The following diagram highlights events on Github and how the CI pipeline delivers ready to use images.

Main Image Workflow
^^^^^^^^^^^^^^^^^^^

This workflow can be found in ``container_app_push.yml``. It provides a place to enable integration tests using
containers and can produce an :doc:`application image <app-image>` as well as a :doc:`configbaker image <configbaker-image>`.

It is triggered by a) pull requests with changes to the images source files or b) called from other workflows, namely
the workflows to produce the base image and run Maven unit tests.

The produces images are either meant to be pushed to Docker Hub for common use or to the Github Container Registry
for preview images of pull request code for testing and development purposes. For preview images, the job will leave
a comment on the pull request with details where to find and use them.

**Note 1:** Secrets are available only to jobs running with the organizational context, which means no pull request from a
fork can use this workflow to push images to some place. Any feature branch within the main repository has access to
these secrets. Accordingly, the deployment job needs to look closely when to run or not and how to tag images.

**Note 2:** The workflow file contains another job to sync image descriptions on Docker Hub on push events to the
``develop`` branch. It is left out of the diagram below to reduce complexity.

.. graphviz::

    digraph shipit {
        node [shape=rect]
        rankdir=TB
        splines=ortho

        trigger [label="CI\ntrigger", shape=circle, margin=0]
        trigger -> build [label="start"]

        subgraph build_test {
            cluster=true
            label="Job: Build & Test"
            rank=same

            build [label="Build app and app image\nw/ runner architecture"]
            testing [label="Testing (future)", style="dashed"]
        }

        build -> secretsCheck [label="on success"]

        subgraph secrets {
            cluster=true
            label="Job: Check Secrets"

            secretsCheck [label="Secrets available\nwithin jobs?", shape=diamond]
        }

        secretsCheck -> decideSkip [taillabel="yes"]
        secretsCheck -> end1 [taillabel="no"]
        end1 [label="End", shape=circle, margin=0]

        subgraph deploy {
            cluster=true
            label="Job: Deploy Images"

            decideSkip [label="Trigger was no push\nor on branches develop or master?", shape=diamond]
            decideSkip -> decideHubLogin [taillabel="yes"]
            decideSkip -> end2 [taillabel="no"]

            decideHubLogin [label="Trigger was PR?", shape=diamond]
            hubLogin [label="Login at Docker Hub"]
            ghcrLogin [label="Login at GHCR"]

            decideHubLogin -> hubLogin [taillabel="no"]
            decideHubLogin -> ghcrLogin [taillabel="yes"]
            hubLogin -> decideMaster
            ghcrLogin -> decideMaster

            decideMaster [label="On branch master?", shape=diamond]
            setTagsMaster [label="Use 'alpha' as tag"]
            decidePR [label="Trigger was PR?", shape=diamond]
            setForPR [label="Use branch name as tag,\nSet registry to GHCR"]

            decideMaster -> setTagsMaster [taillabel="yes"]
            decideMaster -> decidePR [taillabel="no"]
            setTagsMaster -> decidePR
            decidePR -> setForPR [taillabel="yes"]

            buildLocal [label="Build app and app image with runner architecture"]
            buildPush [label="Build and push multiarch images to registry"]
            comment [label="If PR, leave comment on whereabouts"]
            end2 [label="End", shape=circle, margin=0]

            setForPR -> buildLocal
            decidePR -> buildLocal [taillabel="no"]
            buildLocal -> buildPush
            buildPush -> comment
            comment -> end2
        }
    }
