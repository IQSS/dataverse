ALTER TABLE externaltool ADD COLUMN type character varying(255);
ALTER TABLE externaltool ALTER COLUMN type SET NOT NULL;
-- Previously, the only explore tool was TwoRavens. We now persist the name of the tool.
UPDATE guestbookresponse SET downloadtype = 'TwoRavens' WHERE downloadtype = 'Explore';
ALTER TABLE filemetadata ADD COLUMN prov_freeform text;
-- ALTER TABLE datafile ADD COLUMN prov_cplid int;
ALTER TABLE datafile ADD COLUMN prov_entityname text;

-- Moves DOI fields from Dataset to DVObject
-- so that Identifiers may be added to DataFiles

ALTER TABLE dvobject ADD COLUMN
 authority character varying(255),
 ADD COLUMN globalidcreatetime timestamp without time zone,
 ADD COLUMN doiseparator character varying(255),
 ADD COLUMN identifierRegistered boolean,
 ADD COLUMN identifier character varying(255),
 ADD COLUMN protocol character varying(255);

--Migrate data from Dataset to DvObject
UPDATE dvobject
SET authority=(SELECT dataset.authority
FROM dataset
WHERE dataset.id=dvobject.id AND dvobject.dtype='Dataset') where dvobject.dtype='Dataset';

UPDATE dvobject
SET globalidcreatetime=(SELECT dataset.globalidcreatetime
FROM dataset
WHERE dataset.id=dvobject.id AND dvobject.dtype='Dataset') where dvobject.dtype='Dataset';

UPDATE dvobject
SET doiseparator=(SELECT dataset.doiseparator
FROM dataset
WHERE dataset.id=dvobject.id AND dvobject.dtype='Dataset') where dvobject.dtype='Dataset';

UPDATE dvobject
SET identifierRegistered= true where globalidcreatetime is not null;

UPDATE dvobject
SET identifier=(SELECT dataset.identifier
FROM dataset
WHERE dataset.id=dvobject.id AND dvobject.dtype='Dataset') where dvobject.dtype='Dataset';

UPDATE dvobject
SET protocol=(SELECT dataset.protocol
FROM dataset
WHERE dataset.id=dvobject.id AND dvobject.dtype='Dataset' ) where dvobject.dtype='Dataset';

--Once in DvObject re-parse identifier and authority
UPDATE dvobject SET identifier=substring(authority, strpos(authority,'/')+1) || doiseparator || identifier WHERE strpos(authority,'/')>0 ;
UPDATE dvobject SET authority=substring(authority from 0 for strpos(authority,'/')) WHERE strpos(authority,'/')>0;

ALTER TABLE dataset ALTER identifier DROP NOT NULL;

ALTER TABLE dataset DROP COLUMN authority;
ALTER TABLE dataset DROP COLUMN doiseparator;
ALTER TABLE dataset DROP COLUMN globalidcreatetime;
ALTER TABLE dataset DROP COLUMN identifier;
ALTER TABLE dataset DROP COLUMN protocol;

ALTER TABLE dvobject DROP COLUMN doiseparator;

--Add new setting into content for shoulder if needed

INSERT INTO setting(name, content)
SELECT ':Shoulder2', substring(content, strpos(content,'/')+1) || '/' from setting where name = ':Authority' and strpos(content,'/')>0;

--strip shoulder from authority setting if the shoulder exists
SET content= case when (strpos(content,'/')>0) then substring(content from 0 for strpos(content,'/'))
else content end where name=':Authority';

update datasetfieldtype set displayformat = '<a href="#VALUE" target="_blank">#VALUE</a>' where name in ('alternativeURL', 'keywordVocabularyURI', 'topicClassVocabURI', 'publicationURL', 'producerURL', 'distributorURL');
