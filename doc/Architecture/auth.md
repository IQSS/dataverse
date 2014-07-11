# Auth - internal

## System goals

> Go from (User, DvObject, Request) to a set of permissions

> For DvObject - who holds what permissions

## Design Goals
* Invariant - there's always a user
    - Required for the Command Architecture
    - Also, required for auditing, logging, etc.
* A single user object object might refer to a group of actual people (specifically, `GuestUser`). 
    - Single physical person is referred by subclasses of `AuthenticatedUser`
    - A `GuestUser` can be a member of a group, either explicitly (i.e. inclusion in a `ExplicitGroup`) or via other request headers (e.g. guest accessing Dataverse from an IP address included in an `IpGroup` ).
* All groups live in the DB. Their users might not.
* No group membership is cached on our side, to avoid stale data (as in, user removed from institute directory but still has institute permission on Dataverse).
* One cannot go from a user to all the groups said user belongs to. Example: IP group membership is determined at *query* time.
* All groups and users are equal (i.e, Nothing special about Local Users, except that this user provider is bundled with the system)
* JDBC inspired
* Assignees have "URL"s, the first part of which identifies the `RoleAssigneeProvider` that created the user. The suffix of the URL may allow the `RoleAssigneeProvider` to generate the user (e.g. `DatabaseUserProvider`).
* DvObject access request are sent from `AutehnticatedUser`s. The "to" field is inferred - everyone that has a `Permission.GrantPermissions` permission on said DvObject.

## Pluggability - for 4.0
* Pull-request based (not full .jar based plugins in a `plugin` directory)
* No "special cases" for different user providers at the back end (*including database schema*)
* UI can have special cases (JSF + backing beans) for each user provider.
* Groups are stored in the `Groups` table. Common fields, defined at the interface level, are normal database fields. Each row holds a reference to an `RoleAssigneeProvider`. Implementation specific data goes in a blob field (e.g. an `IpGroup` can store a JSON string there, with the ranges).

## Activities
### Get the permissions of a User `u` on DvObject `d` when accessing from IP address `a`:
1. Get all the global groups `u` is member of when accessing from `a`. Call that `Gg(u)` 
2. `permissions `&larr; &empty;
3. `dvObj` &larr; `d`
2. repeat:
    3. `permissions = permissions U permissions( roles(dvObj, u) )`
    3. `permissions = permissions U permissions( roles(dvObj, Gg(u)) )`
    3. `permissions = permissions U permissions( roles(dvObj, explicit_groups(dvObj,u)) )`
    4. If `dvObj` is a permission root, output `permissions`
    4. else, `dvObj` &larr; `parent(dvObj)`
