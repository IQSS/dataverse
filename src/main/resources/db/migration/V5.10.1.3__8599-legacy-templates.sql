-- this script finds legacy templates that do not hava an associated termsofuseandaccess 
-- and creates / links a termsofuseandaccess to them
with  _update as 
(
update template set termsofuseandaccess_id =  nextval('termsofuseandaccess_id_seq' )
where termsofuseandaccess_id is null
returning termsofuseandaccess_id
)
insert into termsofuseandaccess (id, fileaccessrequest, license_id) (select termsofuseandaccess_id, false, 1 from _update)

