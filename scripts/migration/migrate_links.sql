
-- links to datasets
insert into datasetlinkingdataverse (linkingdataverse_id, dataset_id, linkcreatetime)
select c.owner_id, ds.id, now()
from _dvn3_coll_studies link, _dvn3_vdccollection c, _dvn3_study s, dataset ds
where link.vdc_collection_id=c.id
and link.study_id=s.id
and s.owner_id != c.owner_id --don't include if already part of this dataverse
and ds.authority = s.authority
and ds.protocol = s.protocol
and ds.identifier = s.studyid;


-- links to root collections (now linked to dataverses)
insert into dataverselinkingdataverse (linkingdataverse_id, dataverse_id, linkcreatetime)
select vdc_id, owner_id, now()
from _dvn3_vdc_linked_collections link, _dvn3_vdccollection c
where link.linked_collection_id=c.id
and c.parentcollection_id is null;

-- links to other, static collections (now linked to just the studies from them)
insert into datasetlinkingdataverse (linkingdataverse_id, dataset_id, linkcreatetime)
select vdc_id, ds.id, now()
from _dvn3_vdc_linked_collections link, _dvn3_coll_studies contents, _dvn3_vdccollection c, _dvn3_study s, dataset ds
where link.linked_collection_id=c.id
and c.parentcollection_id is not null
and c.type='static'
and c.id = contents.vdc_collection_id
and contents.study_id=s.id
and s.owner_id != vdc_id -- don't include if already part of this dataverse
and ds.authority = s.authority
and ds.protocol = s.protocol
and ds.identifier = s.studyid;



-----------------------
-- reset sequences
-----------------------

SELECT setval('datasetlinkingdataverse_id_seq', (SELECT MAX(id) FROM datasetlinkingdataverse));
SELECT setval('dataverselinkingdataverse_id_seq', (SELECT MAX(id) FROM dataverselinkingdataverse));


