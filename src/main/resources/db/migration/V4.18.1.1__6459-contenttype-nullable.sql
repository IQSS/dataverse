-- contenttype can be non-null because dataset tools do not require it
ALTER TABLE externaltool ALTER contenttype DROP NOT NULL;
