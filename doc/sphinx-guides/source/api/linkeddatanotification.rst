Linked Data Notification API
============================

Dataverse has an API implementing a Linked Data Notification (LDN) inbox allowing it to receive messages implementing the `COAR Notify Relationship Announcement <https://coar-notify.net/catalogue/workflows/repository-relationship-repository/>`_ indicating a link between an external resource and a Dataverse dataset.

Dataverse has a related capability to send COAR Notify Relationship Announcement messages, automatically upon publication or manually. See the :doc:`/developers/workflows` section of the Guides.

The motivating use case is to support a use case where Dataverse administrators may wish to create back-links to the remote resource (e.g. as a Related Publication, Related Material, etc.).

Upon receipt of a relevant message, Dataverse will create Announcement Received notifications for users who can edit the dataset involved. Notifications can be restricted to superusers who can publish the dataset as described below. (In the motivating use case, these superusers may then add an appropriate relationship and use the Update Curent Version publishing option to add it to the most recently published version of the dataset.)

The ``dataverse.ldn.allowed-hosts`` JVM option is a comma-separated list of hosts from which Dataverse will accept and process messages. By default, no hosts are allowed. ``*`` can be used in testing to indicate all hosts are allowed.

The ``dataverse.ldn.coar-noptify.relationship-announcement.notify-superusers-only`` JVM option can be set to ``true`` to restrict notifications to superusers only (those who can publish the dataset). The default is to notify all users who can publish the dataset.

Messages can be sent via POST, using the application/ld+json ContentType:

.. code-block:: bash

  export SERVER_URL=https://demo.dataverse.org
  
  curl -X POST -H 'ContentType:application/ld+json' $SERVER_URL/api/inbox --upload-file message.jsonld
  

The supported message format is described by `the COAR Notify Relationship Announcement specification <https://coar-notify.net/catalogue/workflows/repository-relationship-repository/2/>`_. 

An example message is shown below. It indicates that a resource in the "Harvard DASH" test server has, as a "supplement", the dataset with DOI doi:10.5074/FKNOAHNQ.
If this dataset is managed in the receiving Dataverse, a notification will be sent to user with the relevant permissions (as described above).

.. code:: json

  {
    "@context": [
      "https://www.w3.org/ns/activitystreams",
      "https://purl.org/coar/notify"
    ],
    "actor": {
      "id": "https://harvard-dash.staging.4science.cloud",
      "name": "Harvard DASH",
      "type": "Service"
    },
    "context": {
      "id": "https://harvard-dash.staging.4science.cloud/handle/1/42718322",
      "ietf:cite-as": "https://harvard-dash.staging.4science.cloud/handle/1/42718322",
      "ietf:item": {
        "id": "https://harvard-dash.staging.4science.cloud/bitstreams/e2ae80a1-35e5-411b-9ef1-9175f6cccf23/download",
        "mediaType": "application/pdf",
        "type": [
          "Article",
          "sorg:ScholarlyArticle"
        ]
      },
      "type": "sorg:AboutPage"
    },
    "id": "urn:uuid:3c933c09-c246-473d-bea4-674db168cfee",
    "object": {
      "as:object": "doi: 10.5074/FKNOAHNQ",
      "as:relationship": "http://purl.org/vocab/frbr/core#supplement",
      "as:subject": "https://harvard-dash.staging.4science.cloud/handle/1/42718322",
      "id": "urn:uuid:0851f805-c52f-4d0b-81ac-a07e99c33e20",
      "type": "Relationship"
    },
    "origin": {
      "id": "https://harvard-dash.staging.4science.cloud",
      "inbox": "https://harvard-dash.staging.4science.cloud/server/ldn/inbox",
      "type": "Service"
    },
    "target": {
      "id": "http://ec2-3-238-245-253.compute-1.amazonaws.com/",
      "inbox": "http://ec2-3-238-245-253.compute-1.amazonaws.com/api/inbox",
      "type": "Service"
    },
    "type": [
      "Announce",
      "coar-notify:RelationshipAction"
    ]
  }
