Mail Domain Groups
==================

Groups can be defined based on the domain part of users (verified) email addresses. Email addresses that match
one or more groups configuration will add the user to them.

Within the scientific community, in many cases users will use a institutional email address for their account in a
Dataverse installation. This might offer a simple solution for building groups of people, as the domain part can be
seen as a selector for group membership.

Some use cases: installations that like to avoid Shibboleth, enable self sign up, offer multi-tenancy or can't use
:doc:`ip-groups` plus many more.

.. hint:: Please be aware that non-verified mail addresses will exclude the user even if matching. This is to avoid
          privilege escalation.

Listing Mail Domain Groups
--------------------------

Mail Domain Groups can be listed with the following curl command:

``curl http://localhost:8080/api/admin/groups/domain``

Listing a specific Mail Domain Group
------------------------------------

Let's say you used "domainGroup1" as the alias of the Mail Domain Group you created below.
To list just that Mail Domain Group, you can include the alias in the curl command like this:

``curl http://localhost:8080/api/admin/groups/domain/domainGroup1``


Creating a Mail Domain Group
----------------------------

Mail Domain Groups can be created with a simple JSON file such as domainGroup1.json:

.. code-block:: json

    {
      "name": "Users from @example.org",
      "alias": "exampleorg",
      "description": "Any verified user from Example Org will be included in this group.",
      "domains": ["example.org"]
    }

Giving a ``description`` is optional. The ``name`` will be visible in the permission UI, so be sure to pick a sensible
value.

The ``domains`` field is mandatory to be an array. This enables creation of multi-domain groups, too.

Obviously you can create as many of these groups you might like, as long as the ``alias`` is unique.

To load it into your Dataverse installation, either use a ``POST`` or ``PUT`` request (see below):

``curl -X POST -H 'Content-type: application/json' http://localhost:8080/api/admin/groups/domain --upload-file domainGroup1.json``

Updating a Mail Domain Group
----------------------------

Editing a group is done by replacing it. Grab your group definition like the domainGroup1.json example above,
change it as you like and ``PUT`` it into your installation:

``curl -X PUT -H 'Content-type: application/json' http://localhost:8080/api/admin/groups/domain/domainGroup1 --upload-file domainGroup1.json``

Please make sure that the alias of the group you want to change is included in the path. You also need to ensure
that this alias matches with the one given in your JSON file.

.. hint:: This is an idempotent call, so it will create the group given if not present.

Deleting a Mail Domain Group
----------------------------

To delete a Mail Domain Group with an alias of "domainGroup1", use the curl command below:

``curl -X DELETE http://localhost:8080/api/admin/groups/domain/domainGroup1``

Please note: it is not recommended to delete a Mail Domain Group that has been assigned roles. If you want to delete
a Mail Domain Group, you should first remove its permissions.

