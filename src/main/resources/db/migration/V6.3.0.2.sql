-- Dataset types have been added. See #10517 and #10694
--
-- First, insert some types (dataset is the default).
INSERT INTO datasettype (name) VALUES ('dataset');
INSERT INTO datasettype (name) VALUES ('software');
INSERT INTO datasettype (name) VALUES ('workflow');
--
-- Then, give existing datasets a type of "dataset".
UPDATE dataset SET datasettype_id = (SELECT id FROM datasettype WHERE name = 'dataset');
