select m.id, m.TimePeriodCoveredEnd, v.study_id from  metadata m, studyversion v   where  v.study_id = 121855 and m.id = v.metadata_id and  TimePeriodCoveredEnd = '[17820000]'; 
select m.id, m.DistributionDate, v.study_id from  metadata m, studyversion v   where  v.study_id = 117326 and m.id = v.metadata_id and DistributionDate = '2O14';
select m.id, a.date, v.study_id from metadata m, studyversion v, studyabstract a where v.study_id=47799 and m.id=v.metadata_id and m.id=a.metadata_id and a.date='201-';
select m.id, m.TimePeriodCoveredEnd, v.study_id from  metadata m, studyversion v   where  v.study_id = 88283 and m.id = v.metadata_id and TimePeriodCoveredEnd = '198x';
select m.id, m.TimePeriodCoveredStart, v.study_id from  metadata m, studyversion v   where  v.study_id = 215 and m.id = v.metadata_id and TimePeriodCoveredStart = '70s'; --should return 3 records
select m.id, a.date, v.study_id from metadata m, studyversion v, studyabstract a where v.study_id=91709 and m.id=v.metadata_id and m.id=a.metadata_id and a.date='2-13'; --should return 3 records
select m.id, a.date, v.study_id from metadata m, studyversion v, studyabstract a where v.study_id=114372 and m.id=v.metadata_id and m.id=a.metadata_id and a.date='2-14'; 
select m.id, m.DateOfCollectionStart, m.DateOfCollectionEnd, v.study_id from  metadata m, studyversion v   where  v.study_id = 155 and m.id = v.metadata_id and DateOfCollectionStart = '2004-01-01 to 2004-12-31' and m.DateOfCollectionEnd = '' ; -- should return 10 records

update metadata set TimePeriodCoveredEnd = '1782' from studyversion v where  v.study_id = 121855 and metadata.id = v.metadata_id and TimePeriodCoveredEnd = '[17820000]'; 
update metadata set DistributionDate = '2014' from  studyversion v   where  v.study_id = 117326 and metadata.id = v.metadata_id and DistributionDate = '2O14';
update studyabstract set date = '2010' from metadata m, studyversion v where v.study_id=47799 and m.id=v.metadata_id and m.id=studyabstract.metadata_id and studyabstract.date='201-';
update metadata set TimePeriodCoveredEnd = '198?' from studyversion v   where  v.study_id = 88283 and metadata.id = v.metadata_id and TimePeriodCoveredEnd = '198x';
update metadata set TimePeriodCoveredStart = '197?' from studyversion v   where  v.study_id = 215 and metadata.id = v.metadata_id and TimePeriodCoveredStart = '70s'; --should update 3 records
update studyabstract set date = '2014' from metadata m, studyversion v where v.study_id=114372 and m.id=v.metadata_id and m.id=studyabstract.metadata_id and studyabstract.date='2-14'; 
update studyabstract set date = '2013' from metadata m, studyversion v where v.study_id=91709 and m.id=v.metadata_id and m.id=studyabstract.metadata_id and studyabstract.date='2-13'; --should update 3 records
update metadata set DateOfCollectionStart = '2004-01-01', DateOfCollectionEnd = '2004-12-31' from studyversion v   where  v.study_id = 155 and metadata.id = v.metadata_id and DateOfCollectionStart = '2004-01-01 to 2004-12-31' and DateOfCollectionEnd = ''; -- should update 10 records


update studyfieldvalue set strvalue='English' where metadata_id=273999 and studyfield_id=218 and strValue='English and Dutch';
insert into studyfieldvalue (strvalue, metadata_id, studyfield_id, displayorder) values ('Dutch', 273999,218,1);

--Added for datasets with multiple failues 3/30
select m.id, m.TimePeriodCoveredStart, v.study_id from  metadata m, studyversion v   where  v.study_id = 88283 and m.id = v.metadata_id and TimePeriodCoveredStart = '198x';
select m.id, m.TimePeriodCoveredStart, v.study_id from  metadata m, studyversion v   where  v.study_id = 121855 and m.id = v.metadata_id and  TimePeriodCoveredStart = '[17820000]';
select m.id, m.ProductionDate, v.study_id from  metadata m, studyversion v   where  v.study_id = 121855 and m.id = v.metadata_id and  ProductionDate = '[17820000]';
select m.id, m.dateofdeposit, v.study_id from  metadata m, studyversion v   where  v.study_id = 74738 and m.id = v.metadata_id and  dateofdeposit = '\';

update metadata set TimePeriodCoveredStart = '198?' from studyversion v   where  v.study_id = 88283 and metadata.id = v.metadata_id and TimePeriodCoveredStart = '198x';
update metadata set ProductionDate = '1782' from studyversion v where  v.study_id = 121855 and metadata.id = v.metadata_id and ProductionDate = '[17820000]';
update metadata set TimePeriodCoveredStart = '1782' from studyversion v where  v.study_id = 121855 and metadata.id = v.metadata_id and TimePeriodCoveredStart = '[17820000]';
update metadata set dateofdeposit = '' from studyversion v where  v.study_id = 74738 and metadata.id = v.metadata_id and dateofdeposit = '\';
