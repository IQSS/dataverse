Production
==========

.. contents:: |toctitle|
	:local:

Status
------

As of Dataverse 6.8, when we introduced image tagging per version (see the :ref:`app-image-supported-tags` section for the :ref:`application image <app-image-supported-tags>`), we feel that the images described in this guide are ready for production use. Enjoy!

The images and the documentation is not perfect, of course.

For now, we recommend following the :doc:`demo` tutorial. It will help you learn how to configure and secure your installation. Not that instead of "latest" you might want to select a specific version. Again see :ref:`app-image-supported-tags`.

The Dataverse guides were originally written with a non-Docker installation in mind so we'd like rewrite them with both Docker and non-Docker in mind. This is a big job, obviously. 😅 We know we'd like to write more about ports. We'd like to explain `how to set up Rserve <https://github.com/IQSS/dataverse/issues/11731>`_. Etc., etc.

To talk about your ideas for making the images and docs better for production, please feel free to join the `containers for production <https://dataverse.zulipchat.com/#narrow/channel/375812-containers/topic/containers.20for.20production/with/451611258>`_ topic or join a working group meeting (see :ref:`helping-containers`).

Limitations
-----------

- Multiple apps servers are not supported. See :ref:`multiple-app-servers` for more on this topic.

How to Help
-----------

Please try the images (see :doc:`demo`) and give feedback (see :ref:`helping-containers`)! ❤️

Alternatives
------------

The traditional (non-Docker) installation method is described in the :doc:`/installation/index`.