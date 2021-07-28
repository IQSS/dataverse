-- #6919 is about making preview tools first class instead a dimension of
-- explore tools. Toward that end, we are dropping the "hasPreviewMode" column
-- that was used to indicate that an explore tool had a preview mode. We are
-- also moving the "type" column to a new dedicated table called
-- "externaltooltype".
DO $$
BEGIN
-- Migrate existing external tools into to new schema. This is for upgrades,
-- which have a "type" column from when the entity had a "type" field.
IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='externaltool' AND column_name='type') THEN
  -- For each explore tool that has preview, make an EXPLORE row.
  insert into externaltooltype(externaltool_id,type) select id,'EXPLORE' from externaltool where haspreviewmode = true and type = 'EXPLORE';
  -- For each explore tool that has preview, make a PREVIEW row.
  insert into externaltooltype(externaltool_id,type) select id,'PREVIEW' from externaltool where haspreviewmode = true and type = 'EXPLORE';
  -- For all the other tools, make a row.
  insert into externaltooltype(externaltool_id,type) select id,type from externaltool where haspreviewmode = false or type != 'EXPLORE';
END IF;
-- Drop "type" and "haspreviewmode" from externaltool.
alter table externaltool drop column if exists type;
alter table externaltool drop column if exists haspreviewmode;
END
$$
