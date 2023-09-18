-- remove multiple File access requests which are
-- in violation of the new key
CREATE TABLE tmp_fileaccessrequests (datafile_id bigint NOT NULL, authenticated_user_id INT);

insert into tmp_fileaccessrequests 
select distinct datafile_id, authenticated_user_id from fileaccessrequests;

DROP TABLE fileaccessrequests;

ALTER TABLE tmp_fileaccessrequests 
    RENAME TO fileaccessrequests;

ALTER TABLE fileaccessrequests
ADD COLUMN IF NOT EXISTS creation_time TIMESTAMP WITHOUT TIME ZONE DEFAULT now();

ALTER TABLE  fileaccessrequests ADD  CONSTRAINT fileaccessrequests_pkey PRIMARY KEY (authenticated_user_id, datafile_id);
ALTER TABLE  fileaccessrequests ADD  CONSTRAINT fk_fileaccessrequests_authenticated_user_id FOREIGN KEY (authenticated_user_id)
        REFERENCES public.authenticateduser (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION;
ALTER TABLE  fileaccessrequests ADD     CONSTRAINT fk_fileaccessrequests_datafile_id FOREIGN KEY (datafile_id)
        REFERENCES public.dvobject (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
