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

-- migrate data from _dvn3_versioncontributor to datasetversionuser 
insert into datasetversionuser ( lastupdatedate, authenticateduser_id, datasetversion_id )  (
select  vc.lastupdatetime, vc.contributor_id,  dv.id
from _dvn3_versioncontributor vc,
_dvn3_studyversion sv,
_dvn3_study s,
dataset d,
datasetversion dv,
authenticateduser au
where vc.studyversion_id = sv.id
and sv.study_id = s.id
and d.authority = s.authority
and d.protocol = s.protocol
and d.identifier = s.studyid
and dv.dataset_id = d.id
and dv.versionnumber = sv.versionnumber
and au.id = vc.contributor_id);

-- modify versionstate for older versions of deaccessioned studies
update datasetversion
set versionstate = 'DEACCESSIONED'
where id in (
select dv1.id from datasetversion dv1, datasetversion dv2
where dv1.dataset_id = dv2.dataset_id 
and dv1.versionnumber < dv2.versionnumber
and dv2.versionstate  = 'DEACCESSIONED');

-- update the globalidcreatetime to be equal to the createdate,
-- as it should have been registered when the draft was created in 3.6
update dataset set globalidcreatetime = createdate
from dvobject dvo
where dataset.id = dvo.id;

-- set the license for all versions to be NONE by default
-- TODO: once create commands are done, this can be done in the code.
update termsofuseandaccess set license = 'NONE';

