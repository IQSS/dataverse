IP Groups
=========

IP Groups can be used to permit download of restricted files by IP addresses rather than people. For example, you may want to allow restricted files to be downloaded by researchers who physically enter a library and make use of the library's network.

.. contents:: Contents:
	:local:

Listing IP Groups
-----------------

IP Groups can be listed with the following curl command:

``curl http://localhost:8080/api/admin/groups/ip``

Creating an IP Group
--------------------

IP Groups must be expressed as ranges in IPv4 or IPv6 format. For illustrative purposes, here is a example of the entire IPv4 and IPv6 range that you can :download:`download <../_static/admin/ipGroupAll.json>` and edit to have a narrower range to meet your needs. If you need your IP Group to only encompass a single IP address, you must enter that IP address for the "start" and "end" of the range. If you don't use IPv6 addresses, you can delete that section of the JSON. Please note that the "alias" must be unique if you define multiple IP Groups. You should give it a meaningful "name" since both "alias" and "name" will appear and be searchable in the GUI when your users are assigning roles.

.. literalinclude:: ../_static/admin/ipGroupAll.json

Let's say you download the example above and edit it to give it a range used by your library, giving it a filename of ``ipGroup1.json`` and putting it in the ``/tmp`` directory. Next, load it into your Dataverse installation using the following curl command:

``curl -X POST -H 'Content-type: application/json' http://localhost:8080/api/admin/groups/ip --upload-file /tmp/ipGroup1.json``

Note that you can update a group the same way, as long as you use the same alias.

Listing an IP Group
--------------------

Let's say you used "ipGroup1" as the alias of the IP Group you created above. To list just that IP Group, you can include the alias in the curl command like this:

``curl http://localhost:8080/api/admin/groups/ip/ipGroup1``

Deleting an IP Group
--------------------

It is not recommended to delete an IP Group that has been assigned roles. If you want to delete an IP Group, you should first remove its permissions.

To delete an IP Group with an alias of "ipGroup1", use the curl command below:

``curl -X DELETE http://localhost:8080/api/admin/groups/ip/ipGroup1``
