Added a new API for persistent identifier reconciliation. An unpublished dataset can be updated with a new
pidProvider. If a persistent identifier was already registered when the dataset was registered, this is undone and the
new provider (if changed in the meantime) is used. Note that this change does not affect the storage repository where the old identifier is still
used. 