--copy studyversion fields to datasetversion
update datasetversion  
 set createtime = sv.createtime,
     lastupdatetime = sv.lastupdatetime,
     archivetime= sv.archivetime,
     archivenote = sv.archivenote,
     deaccessionlink = sv.deaccessionlink,
     versionnote = sv.versionnote
from  _dvn3_studyversion  sv, dataset  d,  _dvn3_study s
where d.authority = s.authority
and d.protocol = s.protocol
and d.identifier = s.studyid
and datasetversion.dataset_id = d.id
and datasetversion.versionnumber = sv.versionnumber
and sv.study_id = s.id;

-- set dataset.publication date to the releasetime of the earliest released studyversion
update dvobject 
set publicationdate = m.releasetime
from (select dvobject.id, sv.study_id, min(sv.releasetime) as releasetime
from _dvn3_studyversion  sv, dataset  d,  _dvn3_study s, dvobject
where d.authority = s.authority
and d.protocol = s.protocol
and d.identifier = s.studyid
and dvobject.id = d.id
and sv.study_id = s.id
and sv.versionstate!='DRAFT' group by sv.study_id, dvobject.id) m where m.id = dvobject.id;

-- set dvobject creator_id for each dataset to study.creator_id
update dvobject
set creator_id = s.creator_id, createdate = s.createtime
from _dvn3_study s, dataset d
where d.authority = s.authority
and d.protocol = s.protocol
and d.identifier = s.studyid
and dvobject.id = d.id;
