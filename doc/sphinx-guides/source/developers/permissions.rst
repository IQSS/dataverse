========================
Permissions in Dataverse
========================

Resolving permission in Dataverse is non-trivial. This is because permissions may come from many sources, and can be assigned at multiple levels. This document describes how it's done and why. If you're in a hurry, you don't have do read all of it, but *please read the* `TL;DR`_ *section*.

.. contents:: |toctitle|
  	:local:

TL;DR
-----

.. warning::

  Do not roll your own version of checking for permissions! Use the ``PermissionServiceBean`` instead.

  This is because:

  * Permissions are complicated and, at least initially, non-intuitive.
  * Having multiple permission resolution implementations would lead to back doors and permission escalation vulnerabilities.
  * We've done the heavy lifting for you and all you have to do use the PermissionServiceBean_'s API.



The Basics
----------

The "business logic" layer of Dataverse contains dataverses (``Dataverse``), datasets (``Dataset``), and data files (``DataFile``). The base class for these objects is ``DvObject``. ``DvObject``\s are created, modified, and deleted by the backing beans and REST API beans. However, this is not done directly; Rather, these beans create command objects, which contain the requested action and some context, and submit them to a command engine. The engine then tests whether there are sufficient permissions in the context to execute the command. In case there aren't, the engine will reject the command, throwing a ``PermissionException``. If the permissions suffice, the engine will attempt to execute the command.

This design is covered in detail by an `academic paper <https://doi.org/10.1109/SecDev.2017.22>`_ and a `JavaOne talk`_. The design of the REST API beans using this approach is covered in `another JavaOne talk`_.

Concepts
~~~~~~~~

DataverseRequest
  A request sent to a Dataverse application. Contains the source IP address (if any) and a user. Note that *there is always a user*. If a request was made by a user that's not logged in, it will contain a ``GuestUser`` instance.

Permission
   An a subclass of the `Permission`_ class. The presence of a ``Permission`` object in a set signifies that a ``DataverseRequest`` has, or is required to have, this permission in real life. Some permissions only apply to certain type of ``DvObject``, say, only to a ``Dataset``. Some permissions can only be granted to authenticated users.

   There is a very limited set of permission objects, all enumerated in the `Permission`_ class.

Role
  A set of permission with a name. Used for a better user experience: it makes more sense for humans to reason about a *contributor* than about a ``Set(ViewUnpublishedDataset, DownloadFile, EditDataset, DeleteDatasetDraft)``.

RoleAssignee
  An entity that can be assigned a role. At the moment, there are two types of role assignees: users and groups (see below). However, more types could be added later.

  Group
    A role assignee that may relate to more than a single user. These include groups of users (e.g. users authenticated by Shibboleth), an explicit group of other role assignees (including other such groups - this is a recursive structure!), or all request coming from a certain IP address range. Corollary: **Permissions are given to *HTTP requests*, not to users**.

  User
    A person using the application. Logged in people are represented in the system by an instance of ``AuthenticatedUser``. Non-logged in people are represented by ``GuestUser``. People accessing data from a private url, are represented by a ``PrivateUrlUser``. More user types might be added later. Keep in mind that some user types (e.g. ``GuestUser``) are not stored in the database.

Role Assignment
  Assigning a ``Role`` to a ``RoleAssignee`` at a specific ``DvObject``. The role assignee will have the role at the ``DvObject`` where the role is assigned, and at all the ``DvObject``\s below it, except for those who are, or are descendant of, permission roots.

Permission Root
  A ``DvObject`` that does not honor role assignments done at its ancestors.

  .. note:: At the moment, all ``Dataverse`` objects are permission roots, and all ``Datasets`` are not. This will probably change in future versions. New code should not assume this, unless there's a very very good reason to.

Command
  An action performed on one or more ``DvObject`` on behalf of a ``DataverseRequest``. For each ``DvObject`` affected by a command, the command specifies a set of required ``Permission``\s.

The Whole (and Quite Complex) Picture
-------------------------------------

In Dataverse, acting on ``DvObject``\s is done by submitting ``Command``\s to a command engine, on behalf of a ``DataverseRequest``. Each command requires a specific set of permissions for each ``DvObject`` is affects. If said ``DataverseRequest`` was not granted the required permissions over the affected objects, the engine will reject the command.

Permissions come from role assignments. Roles are granted to ``RoleAssignee``\s, namely user or groups (for now). Group membership can be explicit or implied (e.g. granted by IP address or a login method). Roles may be assigned at the affected ``DvOvjects`` or at any of their ancestors (up to, and including, the nearest permission root). A role can be assigned directly to the ``RoleAssignee``\s involved in the ``DataverseRequest``, or to any ``Group`` that contains them, either directly or indirectly. Note that a ``Group`` can be a member of multiple ``Group``\s [#]_.

In order to find all permissions a request has, we need to find all the roles assigned to it. To do that, we list all the role assignments to the user initiating the request, and to all the groups the request is a member in, either directly or indirectly [#]_. The process starts from the ``DvObject`` affected by the request, and continues up the containment hierarchy until we reach a permission root. The actual process is optimized, so once all the required permissions were found, the search for permissions stops.

Example
~~~~~~~

Suppose ``d`` is an unpublished dataset that a user ``u`` can view. This means ``u`` has permission ``ViewUnpublishedDataset`` over ``d``. This may be because:

* ``u`` was assigned a role on ``d``, and that role has permission ``ViewUnpublishedDataset``.
* ``u`` is a member of a group that was assigned such a role on ``d``.
* ``u`` is a member of a group that was assigned such a role on ``d``\'s parent Dataverse.
* ``u`` is a member of a group that is a (possibly indirect) member of another group that was assigned such a role on ``d``, or its parent Dataverse.
* ``u`` is viewing ``d`` from an IP address that is a member of an IP group ``ig``, and ``ig`` was assigned such a role (perhaps indirectly, as detailed above). The main difference between this example and the former ones is that here, ``u`` is not a member of the group that was assigned the role -- the IP address she is accessing the Dataverse instance from is.

Checking for Permissions
------------------------

The good news are that most of the time there's no need to check for permissions - the engine does this for you. But when the need arises, here's how it's done.

Checking Whether a Command has Permission to Run
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Use ``isPermitted(Command cmd)``. This method receives a command object. It returns ``true`` if the command can run, and ``false`` otherwise. Because ``Command`` objects contain a ``DataverseRequest`` and all the affected ``DvObjects``\s, the ``PermissionServiceBean`` has all the data it needs to answer this question without additional method parameters.

Checking Whether a DataverseRequest has a Specific Permission
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Use ``request(req)`` or ``on(dvObject)``. The ``PermissionServiceBean`` supports a (very) small DSL for querying permissions. Sample usage::

  @EJB
  PermissionServiceBean psb
  ...

  if ( psb.on(dvObject).has(p) ) {
  ...do that thing you must have permission p in order to do
  }

In case there's a need to create a request (e.g. internally initiated command, with no IP address)::

  DataverseRequest req = ... // construct request here

  if ( psb.request(req).on(dvObject).has(p) ) {
   ...do the thing you must have p in order to do
  }


Special Case: JSF
~~~~~~~~~~~~~~~~~
Because permission resolution is complex, it can make the UI very slow. To this end, the ``PermissionsWrapper`` class was developed. ``PermissionsWrapper`` caches the permissions during the page rendering. It also makes some simplifying assumptions regarding permission resolution, that may not work in all cases. Thus, ``PermissionWrapper`` should only be used where short execution times are crucial, such as when rendering a JSF page. It should not be used in REST API beans, for example.



.. _PermissionServiceBean: https://github.com/IQSS/dataverse/blob/develop/src/main/java/edu/harvard/iq/dataverse/PermissionServiceBean.java
.. _JavaOne talk: https://oracleus.activeevents.com/2014/connect/sessionDetail.ww?SESSION_ID=5619&tclass=popup
.. _another JavaOne talk: https://oracle.rainfocus.com/scripts/catalog/oow16.jsp?event=javaone&search=bof4161&search.event=javaone
.. _Permission: https://github.com/IQSS/dataverse/blob/develop/src/main/java/edu/harvard/iq/dataverse/authorization/Permission.java

.. [#] Special code is in place to prevent circular containment, as in ``A`` is a member of ``B``, ``B`` is a member of ``C``, and ``C`` is a member of ``A``.
.. [#] Effectively, this is calculating the `trantisive closure <https://en.wikipedia.org/wiki/Transitive_closure>`_ of the groups the request is a member of.
