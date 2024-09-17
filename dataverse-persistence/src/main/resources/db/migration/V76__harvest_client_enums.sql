-- Harvest type enum
UPDATE harvestingclient SET harvesttype = 'OAI' WHERE harvesttype ='oai';

-- Harvest style enums
UPDATE harvestingclient SET harveststyle = 'DATAVERSE' WHERE harveststyle = 'dataverse';
UPDATE harvestingclient SET harveststyle = 'VDC' WHERE harveststyle = 'vdc';
UPDATE harvestingclient SET harveststyle = 'ICPSR' WHERE harveststyle = 'icpsr';
UPDATE harvestingclient SET harveststyle = 'NESSTAR' WHERE harveststyle = 'nesstar';
UPDATE harvestingclient SET harveststyle = 'ROPER' WHERE harveststyle = 'roper';
UPDATE harvestingclient SET harveststyle = 'HGL' WHERE harveststyle = 'hgl';
UPDATE harvestingclient SET harveststyle = 'DEFAULT' WHERE harveststyle = 'default';
