begin;
insert into filedownload(GUESTBOOKRESPONSE_ID,DOWNLOADTYPE,DOWNLOADTIMESTAMP,SESSIONID) select ID, DOWNLOADTYPE,RESPONSETIME,SESSIONID from guestbookresponse;
alter table guestbookresponse drop column if exists  DOWNLOADTYPE, drop column if exists SESSIONID;
commit;
