Production (Future)
===================

.. contents:: |toctitle|
	:local:

Status
------

The images described in this guide are not yet recommended for production usage, but we think we are close. We'd like to make the following improvements:

- Tagged releases

  - Currently, you have the choice between "alpha" images that change under your feet every time a new version of Dataverse is released or "unstable" images that track the "develop" branch, which is updated frequently. Instead, we'd like to offer images like 6.4, 6.5, etc. We are tracking this work at https://github.com/IQSS/dataverse/issues/10478 and there is some preliminary code at https://github.com/IQSS/dataverse/tree/10478-version-base-img . You are welcome to join the following discussions:

    - https://dataverse.zulipchat.com/#narrow/stream/375812-containers/topic/change.20version.20scheme.20base.20image.3F/near/405636949
    - https://dataverse.zulipchat.com/#narrow/stream/375812-containers/topic/tagging.20images.20with.20versions/near/366600747 

- More docs on setting up additional features

  - How to set up previewers. See https://github.com/IQSS/dataverse/issues/10506
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
