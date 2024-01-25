ALTER TABLE dvobject ADD COLUMN IF NOT EXISTS harvestingclient_id BIGINT;

--add harvesting client id to dvobject records of harvested datasets
update dvobject dvo set harvestingclient_id = s.harvestingclient_id from
(select id, harvestingclient_id from dataset d where d.harvestingclient_id is not null) s
where s.id = dvo.id; 

--add harvesting client id to dvobject records of harvested files
update dvobject dvo set harvestingclient_id = s.harvestingclient_id from
(select id, harvestingclient_id from dataset d where d.harvestingclient_id is not null) s
where s.id = dvo.owner_id;

ALTER TABLE dataset drop COLUMN IF EXISTS harvestingclient_id;

