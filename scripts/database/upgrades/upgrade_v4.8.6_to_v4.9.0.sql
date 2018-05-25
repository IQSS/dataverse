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
 ADD COLUMN identifierRegistered boolean,
 ADD COLUMN identifier character varying(255),
 ADD COLUMN protocol character varying(255);

--add authority shoulder to identifier
UPDATE dvobject
SET identifier=(SELECT substring(authority, strpos(authority,doiseparator)+1) || doiseparator || identifier
FROM dataset
WHERE dataset.id=dvobject.id AND dvobject.dtype='Dataset' and strpos(authority,doiseparator)>0) where dvobject.dtype='Dataset';

--just copy if there's no shoulder
UPDATE dvobject
SET identifier=(SELECT identifier
FROM dataset
WHERE dataset.id=dvobject.id AND dvobject.dtype='Dataset' and strpos(authority,doiseparator)=0) where dvobject.dtype='Dataset';

--strip shoulder from authority 
UPDATE dvobject
SET authority=(SELECT substring(authority from 0 for strpos(authority,doiseparator))  
FROM dataset
WHERE dataset.id=dvobject.id AND dvobject.dtype='Dataset' and strpos(authority,doiseparator)>0) where dvobject.dtype='Dataset' ;

-- no shoulder 
UPDATE dvobject
SET authority=(SELECT authority 
FROM dataset
WHERE dataset.id=dvobject.id AND dvobject.dtype='Dataset' and strpos(authority,doiseparator)=0) where dvobject.dtype='Dataset';


UPDATE dvobject
SET globalidcreatetime=(SELECT dataset.globalidcreatetime
FROM dataset
WHERE dataset.id=dvobject.id AND dvobject.dtype='Dataset') where dvobject.dtype='Dataset';

UPDATE dvobject
SET identifierRegistered= true where globalidcreatetime is not null;

UPDATE dvobject
SET protocol=(SELECT dataset.protocol
FROM dataset
WHERE dataset.id=dvobject.id AND dvobject.dtype='Dataset') where dvobject.dtype='Dataset';

ALTER TABLE dataset ALTER identifier DROP NOT NULL;

ALTER TABLE dataset DROP COLUMN authority;
ALTER TABLE dataset DROP COLUMN doiseparator;
ALTER TABLE dataset DROP COLUMN globalidcreatetime;
ALTER TABLE dataset DROP COLUMN identifier;
ALTER TABLE dataset DROP COLUMN protocol;

--Add new setting into content for doishoulder
INSERT INTO setting(name, content)
VALUES (':DoiShoulder', (SELECT substring(content, strpos(content,'/')+1) || '/' from setting  where name = ':Authority'));

 --strip shoulder from authority setting
 UPDATE setting
 SET content=(SELECT substring(content from 0 for strpos(content,'/'))
 FROM setting
 WHERE name=':Authority' and strpos(content,'/')>0) where name=':Authority';
