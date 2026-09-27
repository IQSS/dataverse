### Delegated Administrative Permissions ("Power User")

A new "Power User" role and "ScopedPowerUser" permission have been introduced to support delegated administrative tasks. This allows instance administrators to grant users administrative privileges over specific collections or datasets without granting them global superuser status. 

Users with the Power User role (or any role containing the ScopedPowerUser permission) assigned on a specific Dataverse object can perform various tasks previously restricted to global superusers, including:
- Moving Dataverse collections and datasets.
- Managing PIDs and registration metadata.
- Configuring storage quotas and drivers.
- Accessing administrative dashboard tools for their scoped objects.
- Bypassing certain upload limits and managing curation labels.
