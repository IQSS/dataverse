User Administration
===================

This section focuses on user administration tools and tasks. 

.. contents:: Contents:
	:local:

Manage Users
------------

The Manage Users table gives the network administrator a list of all user accounts in table form. You can access it by clicking the "Manage Users" button on the :doc:`dashboard`, which is linked from the header of all Dataverse installation pages (if you're logged in as an administrator). It lists username, full name, email address, affiliation, the authentication method they use, the roles their account has been granted, and whether or not they have Superuser status.

Users are listed alphabetically by username. The search bar above the table allows you to search for a specific user. It performs a right-truncated wildcard search of the Username, Name, and Email columns. This means, if you search "baseba" then it will search those three columns for any string of text that begins with "baseba", e.g. "baseball" or "baseballfan".

If you would like to assign or remove a user's Superuser status, then you can do so by checking or unchecking their checkbox under the "Superuser" column.

If you would like to remove all roles/permissions from a user's account (in the event of their leaving your organization, for example) then you can do so by clicking the "Remove All" button under the Roles column. This will keep the user's account active, but will revert it to put the account on the level of a default user with default permissions.

List Users via API
~~~~~~~~~~~~~~~~~~

There are two ways to list users via API. If you have relatively few users, you can get them all as a dump with this command with a superuser API token::

        curl -H "X-Dataverse-key: $API_TOKEN" http://localhost:8080/api/admin/authenticatedUsers

If you have many users and want to be able to search and paginate through the results, use the command below with a superuser API token::

    curl -H "X-Dataverse-key: $API_TOKEN" http://localhost:8080/api/admin/list-users

With the ``list-users`` form you can include the following optional query parameters:

* ``searchTerm`` A string that matches the beginning of a user identifier, first name, last name or email address.
* ``itemsPerPage`` The number of detailed results to return.  The default is 25.  This number has no limit. e.g. You could set it to 1000 to return 1,000 results
* ``selectedPage`` The page of results to return.  The default is 1.

Merge User Accounts
---------------------

See :ref:`merge-accounts-label`

Change User Identifier
-------------------------

See :ref:`change-identifier-label`

Delete a User
-------------

See :ref:`delete-a-user`

Deactivate a User
-----------------

See :ref:`deactivate-a-user`

Confirm Email
-------------

A Dataverse installation encourages builtin/local users to verify their email address upon sign up or email change so that sysadmins can be assured that users can be contacted.

The app will send a standard welcome email with a URL the user can click, which, when activated, will store a ``lastconfirmed`` timestamp in the ``authenticateduser`` table of the database. Any time this is "null" for a user (immediately after sign up and/or changing of their Dataverse installation email address), their current email on file is considered to not be verified. The link that is sent expires after a time (the default is 24 hours), but this is configurable by a superuser via the ``:MinutesUntilConfirmEmailTokenExpires`` config option.

Should users' URL token expire, they will see a "Verify Email" button on the account information page to send another URL.

Sysadmins can determine which users have verified their email addresses by looking for the presence of the value ``emailLastConfirmed`` in the JSON output from listing users (see :ref:`admin` section of Native API in the API Guide). As mentioned in the :doc:`/user/account` section of the User Guide, the email addresses for Shibboleth users are re-confirmed on every login (so their welcome email does not contain a URL to click for this purpose).

Deleting an API Token
---------------------

If an API token is compromised it should be deleted. Users can generate a new one for themselves as explained in the :doc:`/user/account` section of the User Guide, but you may want to preemptively delete tokens from the database.

Using the API token 7ae33670-be21-491d-a244-008149856437 as an example:

``delete from apitoken where tokenstring = '7ae33670-be21-491d-a244-008149856437';``

You should expect the output ``DELETE 1`` after issuing the command above.

.. _mute-notifications:

Letting Users Manage Notifications
-----------------------------------

See :ref:`account-notifications` in the User Guide for how notifications are described to end users.

You can let users manage which notification types they wish to receive by setting :ref:`:ShowMuteOptions` to "true":

``curl -X PUT -d 'true' http://localhost:8080/api/admin/settings/:ShowMuteOptions``

This enables additional settings for each user in the notifications tab of their account page. The users can select which in-app notifications and/or e-mails they wish to receive out of the following list:

* ``APIGENERATED`` API token is generated
* ``ASSIGNROLE`` Role is assigned
* ``CHECKSUMFAIL`` Checksum validation failed
* ``CHECKSUMIMPORT`` Dataset had file checksums added via a batch job
* ``CONFIRMEMAIL`` Email Verification
* ``CREATEACC`` Account is created
* ``CREATEDS`` Your dataset is created
* ``CREATEDV`` Dataverse collection is created
* ``DATASETCREATED`` Dataset was created by user
* ``FILESYSTEMIMPORT`` Dataset has been successfully uploaded and verified
* ``GRANTFILEACCESS`` Access to file is granted
* ``INGESTCOMPLETEDWITHERRORS`` Ingest completed with errors
* ``INGESTCOMPLETED`` Ingest is completed
* ``PUBLISHEDDS`` Dataset is published
* ``PUBLISHFAILED_PIDREG`` Publish has failed
* ``REJECTFILEACCESS`` Access to file is rejected
* ``REQUESTFILEACCESS`` Access to file is requested
* ``RETURNEDDS`` Returned from review
* ``REVOKEROLE`` Role is revoked
* ``STATUSUPDATED`` Status of dataset has been updated
* ``SUBMITTEDDS`` Submitted for review
* ``WORKFLOW_FAILURE`` External workflow run has failed
* ``WORKFLOW_SUCCESS`` External workflow run has succeeded
* ``PIDRECONCILED``   Dataset persistent identifier changed

After enabling this feature, all notifications are enabled by default, until this is changed by the user.

You can shorten this list by configuring some notification types (e.g., ``ASSIGNROLE`` and ``REVOKEROLE``) to be always muted for everyone and not manageable by users (not visible in the user interface) with the :ref:`:AlwaysMuted` setting:

``curl -X PUT -d 'ASSIGNROLE,REVOKEROLE' http://localhost:8080/api/admin/settings/:AlwaysMuted``

Finally, you can set some notifications (e.g., ``REQUESTFILEACCESS``, ``GRANTFILEACCESS`` and ``REJECTFILEACCESS``) as always enabled for everyone and not manageable by users (grayed out in the user interface) with the :ref:`:NeverMuted` setting:

``curl -X PUT -d 'REQUESTFILEACCESS,GRANTFILEACCESS,REJECTFILEACCESS' http://localhost:8080/api/admin/settings/:NeverMuted``
