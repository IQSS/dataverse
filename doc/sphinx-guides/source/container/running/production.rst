Production (Future)
===================

.. contents:: |toctitle|
	:local:

Status
------

The images described in this guide are not yet recommended for production usage, but we think we are close. (Tagged releases are done; see the "supported image tags" section for :ref:`Application <app-image-supported-tags>` and :ref:`Config Baker <config-image-supported-tags>` images.) For now, please see :doc:`demo`.

We'd like to make the following improvements:

- More docs on setting up additional features

  - How to set up Rserve.

- Go through all the features in docs and check what needs to be done differently with containers

  - Check ports, for example.

To join the discussion on what else might be needed before declaring images ready for production, please comment on https://dataverse.zulipchat.com/#narrow/stream/375812-containers/topic/containers.20for.20production/near/434979159

You are also very welcome to join our meetings. See "how to help" below.

Limitations
-----------

- Multiple apps servers are not supported. See :ref:`multiple-app-servers` for more on this topic.

How to Help
-----------

You can help the effort to support these images in production by trying them out (see :doc:`demo`) and giving feedback (see :ref:`helping-containers`).

Alternatives
------------

Until the images are ready for production, please use the traditional installation method described in the :doc:`/installation/index`.
