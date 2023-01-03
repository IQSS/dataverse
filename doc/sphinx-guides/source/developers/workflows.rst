Workflows
=========

The Dataverse Software has a flexible workflow mechanism that can be used to trigger actions before and after Dataset publication.

.. contents:: |toctitle|
        :local:


Introduction
------------

The Dataverse Software can perform two sequences of actions when datasets are published: one prior to publishing (marked by a ``PrePublishDataset`` trigger), and one after the publication has succeeded (``PostPublishDataset``). The pre-publish workflow is useful for having an external system prepare a dataset for being publicly accessed (a possibly lengthy activity that requires moving files around, uploading videos to a streaming server, etc.), or to start an approval process. A post-publish workflow might be used for sending notifications about the newly published dataset.

Workflow steps are created using *step providers*. The Dataverse Software ships with an internal step provider that offers some basic functionality, and with the ability to load 3rd party step providers (currently disabled). This allows installations to implement functionality they need without changing the Dataverse Software source code.

Steps can be internal (say, writing some data to the log) or external. External steps involve the Dataverse Software sending a request to an external system, and waiting for the system to reply. The wait period is arbitrary, and so allows the external system unbounded operation time. This is useful, e.g., for steps that require human intervention, such as manual approval of a dataset publication.

The external system reports the step result back to the Dataverse installation, by sending a HTTP ``POST`` command to ``api/workflows/{invocation-id}`` with Content-Type: text/plain. The body of the request is passed to the paused step for further processing.

Steps can define messages to send to the log and to users. If defined, the message to users is sent as a user notification (creating an email and showing in the user notification tab) and will show once for the given user if/when they view the relevant dataset page. The latter provides a means for the asynchronous workflow execution to report success or failure analogous to the way the publication and other processes report on the page.

If a step in a workflow fails, the Dataverse installation makes an effort to roll back all the steps that preceded it. Some actions, such as writing to the log, cannot be rolled back. If such an action has a public external effect (e.g. send an EMail to a mailing list) it is advisable to put it in the post-release workflow.

.. tip::
  For invoking external systems using a REST api, the Dataverse Software's internal step
  provider offers two steps for sending and receiving customizable HTTP requests.
  *http/sr* and *http/authExt*, detailed below, with the latter able to use the API to make changes to the dataset being processed. (Both lock the dataset to prevent other processes from changing the dataset between the time the step is launched to when the external process responds to the Dataverse instance.)

Administration
~~~~~~~~~~~~~~

A Dataverse installation stores a set of workflows in its database. Workflows can be managed using the ``api/admin/workflows/`` endpoints of the :doc:`/api/native-api`. Sample workflow files are available in ``scripts/api/data/workflows``.

At the moment, defining a workflow for each trigger is done for the entire instance, using the endpoint ``api/admin/workflows/default/«trigger type»``.

In order to prevent unauthorized resuming of workflows, the Dataverse installation maintains a "white list" of IP addresses from which resume requests are honored. This list is maintained using the ``/api/admin/workflows/ip-whitelist`` endpoint of the :doc:`/api/native-api`. By default, the Dataverse installation honors resume requests from localhost only (``127.0.0.1;::1``), so set-ups that use a single server work with no additional configuration.


Available Steps
~~~~~~~~~~~~~~~

The Dataverse Software has an internal step provider, whose id is ``:internal``. It offers the following steps:

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

A step that pauses the workflow. The workflow is paused until a POST request is sent to ``/api/workflows/{invocation-id}``. Sending 'fail' in the POST body (Content-type:text/plain) will trigger a failure and workflow rollback. All other responses are considered as successes.
The pause step is intended for testing - the invocationId required to end the pause is only available in the log (and database). Adding a parameter (see log step) with the key/value "authorized":"true" will allow the invocationId to be used as a credential as with the http/authext step below. 

.. code:: json

  {
      "provider":":internal",
      "stepType":"pause"
  }

pause/message
+++++++++++++

A variant of the  pause step that pauses the workflow and allows the external process to send a success/failure message. The workflow is paused until a POST request is sent to ``/api/workflows/{invocation-id}``. 
The response in the POST body (Content-type:application/json) should be a json object (the same as for the http/extauth step) containing:
- "status" - can be "success" or "failure"
- "reason" - a message that will be logged
- "message" - a message to send to the user that will be sent as a notification and as a banner on the relevant dataset page.
An unparsable reponse will be considered a Failure that will be logged with no user message. (See the http/authext step for an example POST call)

.. code:: json

  {
      "provider":":internal",
      "stepType":"pause/message"
  }


http/sr
+++++++

A step that sends a HTTP request to an external system, and then waits for a response. The response has to match a regular expression specified in the step parameters. The url, content type, and message body can use data from the workflow context, using a simple markup language. This step has specific parameters for rollback.
The workflow is restarted when the external system replies with a POST request  to ``/api/workflows/{invocation-id}``. Responses starting with "OK" (Content-type:text/plain) are considered successes. Other responses will be considered failures and trigger workflow rollback.

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

http/authext
++++++++++++

Similar to the *http/sr* step. A step that sends a HTTP request to an external system, and then waits for a response. The receiver can use the invocationId of the workflow in lieu of an api key to perform work on behalf of the user launching the workflow. 
The invocationId must be sent as an 'X-Dataverse-invocationId' HTTP Header or as an ?invocationId= query parameter. *Note that any external process started using this step then has the ability to access a Dataverse instance via the API as the user.*
Once this step completes and responds, the invocationId is invalidated and will not allow further access.

The url, content type, and message body can use data from the workflow context, using a simple markup language. This step has specific parameters for rollback.
The workflow is restarted when the external system replies with a POST request  to ``/api/workflows/{invocation-id}`` (Content-Type: application/json).

The response has is expected to be a json object with three keys:
- "status" - can be "success" or "failure"
- "reason" - a message that will be logged
- "message" - a message to send to the user that will be sent as a notification and as a banner on the relevant dataset page.

.. code-block:: bash

  export INVOCATION_ID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export MESSAGE={"status":"success", "reason":"Workflow completed in 10 seconds", "message":"An external workflow to virus check your data was successfully run prior to publication of your data"}
 
  curl -H 'Content-Type:application/json' -X POST -d $MESSAGE "$SERVER_URL/api/workflows/$INVOCATION_ID"

.. code:: json

  {
    "provider":":internal",
    "stepType":"http/authext",
    "parameters": {
        "url":"http://localhost:5050/dump/${invocationId}",
        "method":"POST",
        "contentType":"text/plain",
        "body":"START RELEASE ${dataset.id} as ${dataset.displayName}",
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

A step that sends an archival copy of a Dataset Version to a configured archiver, e.g. the DuraCloud interface of Chronopolis. See :ref:`rda-bagit-archiving` for further detail.

Note - the example step includes two settings required for any archiver, three (DuraCloud*) that are specific to DuraCloud, and the optional BagGeneratorThreads setting that controls parallelism when creating the Bag.

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
      ":DuraCloudContext":"string",
      ":BagGeneratorThreads":"string"
    }
  }


ldnannounce
+++++++++++

An experimental step that sends a Linked Data Notification (LDN) message to a specific LDN Inbox announcing the publication/availability of a dataset meeting certain criteria. 

The two parameters are
* ``:LDNAnnounceRequiredFields`` - a list of metadata fields that must exist to trigger the message. Currently, the message also includes the values for these fields but future versions may only send the dataset's persistent identifier (making the receiver responsible for making a call-back to get any metadata).
* ``:LDNTarget`` - a JSON object containing an ``inbox`` key whose value is the URL of the target LDN inbox to which messages should be sent, e.g. ``{"id": "https://dashv7-dev.lib.harvard.edu","inbox": "https://dashv7-api-dev.lib.harvard.edu/server/ldn/inbox","type": "Service"}`` ).

The supported message format is desribed by `our preliminary specification <https://docs.google.com/document/d/1dqj8_vEcIBeyDIZCaPQvp0FM1eSGO_5CSNCdXOpoUz0/edit?usp=sharing>`_. The format is expected to change in the near future to match the standard for relationship announcements being developed as part of `the COAR Notify Project <https://notify.coar-repositories.org/>`_. 


.. code:: json


  {
    "provider":":internal",
    "stepType":"ldnannounce",
    "parameters": {
      "stepName":"LDN Announce"
    },
    "requiredSettings": {
      ":LDNAnnounceRequiredFields": "string",
      ":LDNTarget": "string"
    }
  }

