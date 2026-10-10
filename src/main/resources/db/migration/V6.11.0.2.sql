-- #6691: text_pattern_ops supports subtree LIKE scans under non-C collations.
-- Any pre-created index with this name must use the same definition.
CREATE INDEX IF NOT EXISTS ix_filemetadata_tree
    ON filemetadata (datasetversion_id, directorylabel text_pattern_ops, lower(label), datafile_id);
