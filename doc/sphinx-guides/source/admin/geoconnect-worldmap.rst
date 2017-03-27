Geoconnect and WorldMap
=======================

.. contents:: :local:

One of the optional components listed under "Architecture and Components" in the :doc:`/installation/prep` section of the Installation Guide is `Geoconnect <https://github.com/IQSS/geoconnect>`_, piece of middleware that allows Dataverse users to create maps in `WorldMap <http://worldmap.harvard.edu>`_ based on geospatial data stored in Dataverse. For more details on the feature from the user perspective, see the :doc:`/user/data-exploration/worldmap` section of the User Guide.

Removing Dead Explore Links
---------------------------

After a map has been created in WorldMap (assuming all the setup has been done), an "Explore" button will appear next to the name of the file in Dataverse. The "Explore" button should open the map in WorldMap. In rare occasions, the map has been deleted on the WorldMap side such that the "Explore" button goes nowhere, resulting in a dead link, a 404.

Functionality has been added on the Dataverse side to iterate through all the maps Dataverse knows about and to check for the existence of each map in WorldMap. The status code returned from WorldMap (200, 404, etc.) is recorded in Dataverse along with a timestamp of when the check was performed. To perform this check, you can execute the following ``curl`` command:

``curl -X POST http://localhost:8080/api/admin/geoconnect/mapLayerMetadatas/check``

The output above will contain the ``layerLink`` being checked as well as the HTTP response status code (200, 404, etc.) in the ``lastVerifiedStatus`` field. 200 means OK and 404 means not found. 500 might indicate that the map is only temporarily unavailable.

One you have performed the check above, the fields ``lastVerifiedStatus`` and ``lastVerifiedTime`` will be populated in the output of the following curl command:

``curl http://localhost:8080/api/admin/geoconnect/mapLayerMetadatas``

That is to say, the first ``curl`` command reaches out to the WorldMap server over and over for each map in Dataverse and records resultus (status codes and timestamps) in the Dataverse database.

The second ``curl`` command gives you a dump off the ``maplayermetadatas`` database table, including fields for ``lastVerifiedStatus`` and ``lastVerifiedTime`` as recorded by the first ``curl`` command.

Armed with this information about WorldMap returning a 404 for a map, you may want to delete any record of the map on the Dataverse side so that the "Explore" button goes away (and so that thumbnail files are cleaned up). To accomplish this, use the following ``curl`` command, substituting the id of the file:

``curl -H "X-Dataverse-key: $API_TOKEN" -X DELETE http://localhost:8080/api/files/{file_id}/map``

End users themselves who have permission to edit a dataset can check on the status of their map on the WorldMap side with the following ``curl`` command, substituting the id of the file:

``curl -H "X-Dataverse-key: $API_TOKEN" http://localhost:8080/api/files/{file_id}/map/check``

In addition, end users can get the same dump from the ``maplayermetadatas`` for just their file using the following command, substituting the id of the file:

``curl -H "X-Dataverse-key: $API_TOKEN" http://localhost:8080/api/files/{file_id}/map``

End users can also run the ``DELETE`` command above but it's more likely that the sysadmin reading this guide will run it for them. It is recommended that you add the ``mapLayerMetadatas/check`` command above to cron so that Dataverse periodically checks on the status of maps in WorldMap.
