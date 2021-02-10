Mail Domain Groups
==================

It is possible to create groups that will automatically include all the users having certain mail domain (or part of it) – these are the mail domain groups. Currently they can only be managed using admin api, however adding permissions for these groups is ordinarily done in UI.

Please note that only the users with verified e-mails will be recognized as a members of such groups, however completing a verification will immediately give an access to all the groups that match user's mail domain.

Adding (or updating) a Group
----------------------------

In order to add a new mail domain group one have to prepare a JSON file with group data:

.. code-block:: json

    {
      "alias": "abc",
      "displayName": "Group ABC",
      "description": "Some mail domain group",
      "inclusions": ["icm.edu.pl", ".uw.edu.pl"],
      "exclusions": ["math.uw.edu.pl", ".math.uw.edu.pl"]
    }

Every group must have a unique, non-empty ``alias``. The ``displayName`` property is used for group presentation on UI – it can be left empty, but it's strongly recommended not to do so. Also ``description`` is not mandatory, and it is currently used only to store additional information about the group.

At least one, non-empty element is required in ``inclusions`` array. Here are listed all the domains that are used to match against the user e-mail domain in order to establish whether the user is a member of the group.

Every domain has to be non-empty string, build only from lower- or uppercase letters, digits, dots (.) and hyphens (-). If the domain **does not** start with a dot it is treated as **full-match** item, so the user's mail domain must be exactly the same to be matched. On the other hand, when the domain item **starts** with a dot, then we have **partial-matching** and it's sufficient that only end part of the e-mail domain matches the given item (eg. for the entry *.uw.edu.pl* all mail domains of the form *xxxxxxx.uw.edu.pl* where x-es are replaced by any non-empty string, will be treated as matching).

To exclude some domains we fill the ``exclusions`` array. The syntax of domain items is identical as in ``inclusions``. This array could be left empty.

The user with verified e-mail and the given mail domain is treated as a member of a group if there is at least one match between the mail domain and one of the elements of ``inclusions`` array. The user is treated as not belonging to the group if there is no match between mail domain and the elements of ``inclusions`` or if there is such a match, but there is also a match between mail domain and at least one element of ``exclusions``. The matching process is **case-sensitive**.

To **update** an existing group we prepare JSON file where ``alias`` is the same as the alias of the group we want to update. All data of that group will be replaced by data provided in the file.

To finish adding or updating a group we invoke:

``curl -X PUT -H 'Content-type: application/json' http://localhost:8080/api/admin/groups/mail --upload-file group.json``

In this case *group.json* is the name of the JSON file with group data.

Deleting a Group
----------------

In order to delete a group we invoke:

``curl -X DELETE http://localhost:8080/api/admin/groups/mail/{alias}``

In place of ``{alias}`` we put an alias of the group we want to delete.

When the group is deleted all permissions given to that group are also deleted.

Listing Groups
--------------

We could view data of selected group by invoking:

``curl http://localhost:8080/api/admin/groups/mail/{alias}``

In place of ``{alias}`` we put an alias of the group we want to view.

To list the data of all the mail domain groups, we invoke:

``curl http://localhost:8080/api/admin/groups/mail``

