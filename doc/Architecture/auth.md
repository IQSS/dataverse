# Auth - internal

## Design Goals
* All groups and users are equal (i.e, Nothing special about Local Users, except that this user provider is bundled with the system)
* A single user object object might refer to a group of actual people (specifically, `GuestUser`). 
    - Single physical person is referred by subclasses of `AuthenticatedUser`
    - A `GuestUser` can be a member of a group, either explicitly (i.e. inclusion in a `ExplicitGroup`) or via other request headers (e.g. guest accessing Dataverse from an IP address included in an `IpGroup` ).
* DvObject access request are sent from `AutehnticatedUser`s. The "to" field is inferred - everyone that has a `Permission.GrantPermissions` permission on said DvObject.
* Invariant - there's always a user
    - Required for the Command Architecture
    - Also, required for auditing, logging, etc.
* All groups live in the DB. Their users might not.
* One cannot go from a user to all the groups said user belongs to. Example: IP group membership is determined at *query* time.
* Each assignee has a URL that allows the `RoleAssigneeManager` to locate the `RoleAssigneeProvider` that created it.
* No group membership is cached on our side, to avoid stale data (as in, user removed from institute directory but still has institute permission on Dataverse).

## Pluggability - for 4.0
* Pull-request based (not full .jar based plugins in a `plugin` directory)
* No "special cases" for different user providers at the back end (*including database schema*)
* UI can have special cases (JSF + backing beans) for each user provider.

## Activities
### Get the permissions of a User `u` on DvObject `d` when accessing from IP address `a`:
1. Get all the global groups `u` is member of when accessing from `a`. Call that `Gg(u)` 
2. `permissions `&larr; &empty;
2. Starting from `d`, and going up until hitting a permission root:
    3. `permissions = permissions U permissions( roles(d, u) )`
    3. `permissions = permissions U permissions( roles(d, Gg(u)) )`
    3. `permissions = permissions U permissions( roles(d, explicit_groups(d,u)) )`
4. Output `permissions`