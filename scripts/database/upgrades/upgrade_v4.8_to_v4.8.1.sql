
/**
 * Author:  skraffmi
 * Created: Sep 25, 2017
 */


-- Moves DOI fields from Dataset to DVObject
-- so that Identifiers may be added to DataFiles

ALTER TABLE dvobject ADD COLUMN   
  authority character varying(255),
  doiseparator character varying(255),
  globalidcreatetime timestamp without time zone,
  identifierRegistered boolean,
  identifier character varying(255),
  protocol character varying(255);

UPDATE dvobject
SET authority=(SELECT dataset.authority
FROM dataset
WHERE dataset.id=dvobject.id AND dvobject.dtype='Dataset') where dvobject.dtype='Dataset';

UPDATE dvobject
SET doiseparator=(SELECT dataset.doiseparator
FROM dataset
WHERE dataset.id=dvobject.id AND dvobject.dtype='Dataset') where dvobject.dtype='Dataset';

UPDATE dvobject
SET globalidcreatetime=(SELECT dataset.globalidcreatetime
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
WHERE dataset.id=dvobject.id AND dvobject.dtype='Dataset') where dvobject.dtype='Dataset';

/*
Run drop column after verifying proper transfer of datapoints 
Only dvObjects that are datasets will have these columns filled in 
on migration.

ALTER TABLE dataset DROP COLUMN authority;
ALTER TABLE dataset DROP COLUMN doiseparator;
ALTER TABLE dataset DROP COLUMN globalidcreatetime;
ALTER TABLE dataset DROP COLUMN identifier;
ALTER TABLE dataset DROP COLUMN protocol;

*/