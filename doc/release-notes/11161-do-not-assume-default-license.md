### New Feature Flag: do-not-assume-default-license

A new feature flag `do-not-assume-default-license` has been added. This flag controls the behavior when creating a dataset via API without providing a license or terms of use.

- By default (flag disabled), the default license will be automatically assigned if no license and no terms are provided in the input JSON, regardless of whether custom terms are allowed.
- When enabled, no license (and no terms) will be assigned if none are provided.

**Note:** Previously, if custom terms were allowed, the system would not assign a default license in this case. To retain that behavior, you must now enable this feature flag.
