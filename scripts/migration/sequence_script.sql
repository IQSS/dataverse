SELECT setval('datafile_id_seq', (SELECT MAX(id) FROM datafile));
SELECT setval('datafilecategory_id_seq', (SELECT MAX(id) FROM datafilecategory));
SELECT setval('datatable_id_seq', (SELECT MAX(id) FROM datatable));
SELECT setval('datavariable_id_seq', (SELECT MAX(id) FROM datavariable));
SELECT setval('dvobject_id_seq', (SELECT MAX(id) FROM dvobject));
SELECT setval('filemetadata_id_seq', (SELECT MAX(id) FROM filemetadata));
SELECT setval('variablecategory_id_seq', (SELECT MAX(id) FROM variablecategory));
