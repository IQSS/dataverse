-- Replace the URI_NO field type with TEXT to be upstream compatible again
UPDATE datasetfieldtype SET fieldtype = 'TEXT' WHERE name = 'storage_location';