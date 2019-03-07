Workflows
================

Dataverse has a flexible workflow mechanism that can be used to trigger actions before and after Dataset publication.

.. contents:: |toctitle|
        :local:


Introduction
------------

Dataverse can perform two sequences of actions when datasets are published: one prior to publishing (marked by a ``PrePublishDataset`` trigger), and one after the publication has succeeded (``PostPublishDataset``). The pre-publish workflow is useful for having an external system prepare a dataset for being publicly accessed (a possibly lengthy activity that requires moving files around, uploading videos to a streaming server, etc.), or to start an approval process. A post-publish workflow might be used for sending notifications about the newly published dataset.

Workflow steps are created using *step providers*. Dataverse ships with an internal step provider that offers some basic functionality, and with the ability to load 3rd party step providers. This allows installations to implement functionality they need without changing the Dataverse source code.

Steps can be internal (say, writing some data to the log) or external. External steps involve Dataverse sending a request to an external system, and waiting for the system to reply. The wait period is arbitrary, and so allows the external system unbounded operation time. This is useful, e.g., for steps that require human intervension, such as manual approval of a dataset publication.

The external system reports the step result back to dataverse, by sending a HTTP ``POST`` command to ``api/workflows/{invocation-id}``. The body of the request is passed to the paused step for further processing.

If a step in a workflow fails, Dataverse make an effort to roll back all the steps that preceded it. Some actions, such as writing to the log, cannot be rolled back. If such an action has a public external effect (e.g. send an EMail to a mailing list) it is advisable to put it in the post-release workflow.

.. tip::
  For invoking external systems using a REST api, Dataverse's internal step
  provider offers a step for sending and receiving customizable HTTP requests.
  It's called *http/sr*, and is detailed below.

Administration
~~~~~~~~~~~~~~

A Dataverse instance stores a set of workflows in its database. Workflows can be managed using the ``api/admin/workflows/`` endpoints of the :doc:`/api/native-api`. Sample workflow files are available in ``scripts/api/data/workflows``.

At the moment, defining a workflow for each trigger is done for the entire instance, using the endpoint ``api/admin/workflows/default/«trigger type»``.

In order to prevent unauthorized resuming of workflows, Dataverse maintains a "white list" of IP addresses from which resume requests are honored. This list is maintained using the ``/api/admin/workflows/ip-whitelist`` endpoint of the :doc:`/api/native-api`. By default, Dataverse honors resume requests from localhost only (``127.0.0.1;::1``), so set-ups that use a single server work with no additional configuration.


Available Steps
~~~~~~~~~~~~~~~

Dataverse has an internal step provider, whose id is ``:internal``. It offers the following steps:

log
+++

A step that writes data about the current workflow invocation to the instance log. It also writes the messages in its ``parameters`` map.

.. code:: json

  {
     "provider":":internal",
     "stepType":"log",
     "parameters": {
         "aMessage": "message content",
         "anotherMessage": "message content, too"
     }
  }


pause
+++++

A step that pauses the workflow. The workflow is paused until a POST request is sent to ``/api/workflows/{invocation-id}``.

.. code:: json

  {
      "provider":":internal",
      "stepType":"pause"
  }


http/sr
+++++++

A step that sends a HTTP request to an external system, and then waits for a response. The response has to match a regular expression specified in the step parameters. The url, content type, and message body can use data from the workflow context, using a simple markup language. This step has specific parameters for rollback.

.. code:: json

  {
    "provider":":internal",
    "stepType":"http/sr",
    "parameters": {
        "url":"http://localhost:5050/dump/${invocationId}",
        "method":"POST",
        "contentType":"text/plain",
        "body":"START RELEASE ${dataset.id} as ${dataset.displayName}",
        "expectedResponse":"OK.*",
        "rollbackUrl":"http://localhost:5050/dump/${invocationId}",
        "rollbackMethod":"DELETE ${dataset.id}"
    }
  }

Available variables are:

* ``invocationId``
* ``dataset.id``
* ``dataset.identifier``
* ``dataset.globalId``
* ``dataset.displayName``
* ``dataset.citation``
* ``minorVersion``
* ``majorVersion``
* ``releaseStatus``

archiver
++++++++

A step that sends an archival copy of a Dataset Version to a configured archiver, e.g. the DuraCloud interface of Chronopolis. See the `DuraCloud/Chronopolis Integration documentation <http://guides.dataverse.org/en/latest/admin/integrations.html#id15>`_ for further detail.

Note - the example step includes two settings required for any archiver and three (DuraCloud*) that are specific to DuraCloud.

.. code:: json


  {
    "provider":":internal",
    "stepType":"archiver",
    "parameters": {
      "stepName":"archive submission"
    },
    "requiredSettings": {
      ":ArchiverClassName": "string",
      ":ArchiverSettings": "string",
      ":DuraCloudHost":"string",
      ":DuraCloudPort":"string",
      ":DuraCloudContext":"string" 
    }
  }

