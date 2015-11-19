-- A dataverse alias should not be case sensitive: https://github.com/IQSS/dataverse/issues/2598
CREATE UNIQUE INDEX dataverse_alias_unique_idx on dataverse (LOWER(alias));
-- If creating the index fails, check for dataverse with the same alias using this query:
-- select alias from dataverse where lower(alias) in (select lower(alias) from dataverse group by lower(alias) having count(*) >1) order by lower(alias);
