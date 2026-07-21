### New JVM Option: assume-default-license-when-not-provided-via-api

A new JVM option `dataverse.api.assume-default-license-when-not-provided-via-api` has been added. This option controls the behavior when creating a dataset via API without providing a license or terms of use.

- When set to `true`, the default license will be automatically assigned if no license and no terms are provided in the input JSON.
- When set to `false`, no license (and no terms) will be assigned if none are provided.
- When not set (the default), the system maintains backward compatibility: the default license is assigned if custom terms are not allowed; if custom terms are allowed, no license is assigned.

This allows administrators more control over whether datasets created via API must have a license or can be created without one when terms are also missing.
