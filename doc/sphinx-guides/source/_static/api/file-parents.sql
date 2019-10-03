select fmd.label as filename, dsfv.value as dataset_name, 'N/A' as dataverse_level_1_alias, dsv.releasetime as publication_date
from filemetadata fmd, datasetversion dsv, datasetfieldvalue dsfv, datasetfield dsf, dvobject dvo
where fmd.datasetversion_id = dsv.id
and dsv.id = dsf.datasetversion_id
and dsf.id = dsfv.datasetfield_id
and fmd.datafile_id = dvo.id
and dsf.datasetfieldtype_id=1;
