### Delegated Administrative Permissions ("Scoped Superuser")

A new "Scoped Superuser" role and "ActAsSuperuser" permission have been introduced to support delegated administrative tasks. This allows instance administrators to grant users administrative privileges over specific collections or datasets without granting them global superuser status. 

Users with the Scoped Superuser role (or any role containing the ActAsSuperuser permission) assigned on a specific Dataverse object can perform various tasks previously restricted to global superusers, including:
- Moving Dataverse collections and datasets.
- Managing PIDs and registration metadata.
- Configuring storage quotas and drivers.
- Accessing administrative dashboard tools for their scoped objects.
- Bypassing certain upload limits and managing curation labels.
