
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
