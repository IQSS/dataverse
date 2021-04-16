-- select * from guestbookresponse;
-- 150K would be a better test, see https://github.com/IQSS/dataverse/issues/3609#issuecomment-322559209
--for i in {0..2000}; do psql dataverse_db -f scripts/issues/3845/insert-guestbook-responses.sh; done
-- id  | downloadtype | email | institution | name  | position |      responsetime       |                     sessionid                      | authenticateduser_id | datafile_id | dataset_id | datasetversion_id | guestbook_id 
insert into guestbookresponse values (default, 1, null, null, null, null, null, null, null, 104, 103, null, 2);
