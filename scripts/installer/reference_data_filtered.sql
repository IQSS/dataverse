INSERT INTO variableformattype (id, "name") VALUES (1,'numeric');
INSERT INTO variableformattype (id, "name") VALUES (2,'character');

INSERT INTO variableintervaltype (id, "name") VALUES (1, 'discrete');
INSERT INTO variableintervaltype (id, "name") VALUES (2, 'continuous');
INSERT INTO variableintervaltype (id, "name") VALUES (3, 'nominal');
INSERT INTO variableintervaltype (id, "name") VALUES (4, 'dichotomous');

INSERT INTO summarystatistictype (id, "name") VALUES (1, 'mean');
INSERT INTO summarystatistictype (id, "name") VALUES (2, 'medn');
INSERT INTO summarystatistictype (id, "name") VALUES (3, 'mode');
INSERT INTO summarystatistictype (id, "name") VALUES (4, 'min');
INSERT INTO summarystatistictype (id, "name") VALUES (5, 'max');
INSERT INTO summarystatistictype (id, "name") VALUES (6, 'stdev');
INSERT INTO summarystatistictype (id, "name") VALUES (7, 'vald');
INSERT INTO summarystatistictype (id, "name") VALUES (8, 'invd');

INSERT INTO variablerangetype (id, "name") VALUES (1, 'min');
INSERT INTO variablerangetype (id, "name") VALUES (2, 'max');
INSERT INTO variablerangetype (id, "name") VALUES (3, 'min');
INSERT INTO variablerangetype (id, "name") VALUES (4, 'max');
INSERT INTO variablerangetype (id, "name") VALUES (5, 'point');

CREATE SEQUENCE filesystemname_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 2
  CACHE 1;
ALTER TABLE filesystemname_seq OWNER TO "dvnapp";
