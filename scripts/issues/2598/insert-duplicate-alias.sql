-- This script should fail to insert a duplicate datavers alias (different case)
-- after a constraint has been added in https://github.com/IQSS/dataverse/issues/2598
DELETE FROM dataverse where id = 100;
DELETE FROM dataverse where id = 101;
DELETE FROM dvobject where id = 100;
DELETE FROM dvobject where id = 101;
INSERT INTO dvobject (id, createdate, modificationtime) VALUES (100, NOW(), NOW());
INSERT INTO dataverse (id, alias, name, dataversetype, defaultcontributorrole_id) VALUES (100, 'foo', 'foo is mine', 'UNCATEGORIZED', 1);
INSERT INTO dvobject (id, createdate, modificationtime) VALUES (101, NOW(), NOW());
INSERT INTO dataverse (id, alias, name, dataversetype, defaultcontributorrole_id) VALUES (101, 'FOO', 'uppercase foo', 'UNCATEGORIZED', 1);
