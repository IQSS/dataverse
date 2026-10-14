## Bug Fixes

- When all published versions of a dataset are deaccessioned, its DataCite DOI (and the DOIs of any files that are no longer in a published version) now changes from "findable" to "registered". The DOI continues to resolve to the tombstone page but is no longer listed in DataCite Commons and services that harvest from DataCite, such as OpenAIRE. See #11725.

  Note that this fix does not update DOIs of datasets that were deaccessioned before upgrading. These can be changed to "registered" manually in DataCite Fabrica.
