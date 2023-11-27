ALTER TABLE dvobject ADD COLUMN IF NOT EXISTS harvestingclient_id BIGINT;

update dvobject dvo set harvestingclient_id = s.harvestingclient_id from
(select id, harvestingclient_id from dataset d where d.harvestingclient_id is not null) s
where s.id = dvo.id; 

--ALTER TABLE dataset drop COLUMN IF EXISTS harvestingclient_id;

