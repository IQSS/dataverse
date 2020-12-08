## Introducing MicroProfile Config API

With this Dataverse release, we start to make use of the MicroProfile Config API. (As you might have noticed
for the database connection settings.)

This will benefit both devs and sysadmins, but the codebase will have to be refactored to make use of it.
As this will take time, we will always provide a backward compatible way of using it.

For more details, please see the development guide about "Consuming Configuration", which also
explains the benefits in more detail.