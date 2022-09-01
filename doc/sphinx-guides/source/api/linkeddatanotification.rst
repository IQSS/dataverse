Linked Data Notification API
============================

Dataverse has a limited, experimental API implementing a Linked Data Notification inbox allowing it to receive messages indicating a link between an external resource and a Dataverse dataset.
The motivating use case is to support a use case where Dataverse administrators may wish to create back-links to the remote resource (e.g. as a Related Publication, Related Material, etc.).

Upon receipt of a relevant message, Dataverse will create Announcement Received notifications for superusers, who can edit the dataset involved. (In the motivating use case, these users may then add an appropriate relationship and use the Update Curent Version publishing option to add it to the most recently published version of the dataset.)

The ``:LDNMessageHosts`` setting is a comma-separated whitelist of hosts from which Dataverse will accept and process messages. By default, no hosts are allowed. ``*`` can be used in testing to indicate all hosts are allowed.

Messages can be sent via POST, using the application/ld+json ContentType:

.. code-block:: bash

  export SERVER_URL=https://demo.dataverse.org
  
  curl -X POST -H 'ContentType:application/ld+json' $SERVER_URL/api/inbox --upload-file message.jsonld

The supported message format is described by `our preliminary specification <https://docs.google.com/document/d/1dqj8_vEcIBeyDIZCaPQvp0FM1eSGO_5CSNCdXOpoUz0/edit?usp=sharing>`_. The format is expected to change in the near future to match the standard for relationship announcements being developed as part of `the COAR Notify Project <https://notify.coar-repositories.org/>`_. 

An example message is shown below. It indicates that a resource with the name "An Interesting Title" exists and "IsSupplementedBy" the dataset with DOI https://doi.org/10.5072/FK2/GGCCDL. If this dataset is managed in the receiving Dataverse, a notification will be sent to user with the relevant permissions (as described above).

.. code:: json

  {
    "@context": [
      "https://www.w3.org/ns/activitystreams",
      "https://purl.org/coar/notify"
    ],
    "id": "urn:uuid:94ecae35-dcfd-4182-8550-22c7164fe23f",
    "actor": {
      "id": "https://research-organisation.org/dspace",
      "name": "DSpace Repository",
      "type": "Service"
    },
    "context": {
      "IsSupplementedBy":
        {
          "id": "http://dev-hdc3b.lib.harvard.edu/dataset.xhtml?persistentId=doi:10.5072/FK2/GGCCDL",
          "ietf:cite-as": "https://doi.org/10.5072/FK2/GGCCDL",
          "type": "sorg:Dataset"
        }
    },
    "object": {
      "id": "https://research-organisation.org/dspace/item/35759679-5df3-4633-b7e5-4cf24b4d0614",
      "ietf:cite-as": "https://research-organisation.org/authority/resolve/35759679-5df3-4633-b7e5-4cf24b4d0614",
      "sorg:name": "An Interesting Title",
      "type": "sorg:ScholarlyArticle"
    },
    "origin": {
      "id": "https://research-organisation.org/dspace",
      "inbox": "https://research-organisation.org/dspace/inbox/",
      "type": "Service"
    },
    "target": {
      "id": "https://research-organisation.org/dataverse",
      "inbox": "https://research-organisation.org/dataverse/inbox/",
      "type": "Service"
    },
    "type": [
      "Announce",
      "coar-notify:ReleaseAction"
    ]
  }

