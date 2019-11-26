ALTER TABLE externalTool
ADD COLUMN IF NOT EXISTS hasPreviewMode BOOLEAN;
UPDATE externaltool SET hasPreviewMode = false;
ALTER TABLE externaltool ALTER COLUMN hasPreviewMode SET NOT NULL;

