alter table usernotification add column if not exists parameters json;
update usernotification
	set parameters = json_build_object('requestorId', requestor_id::text, 'message', additionalmessage)
	where type in ('SUBMITTEDDS', 'RETURNEDDS') and requestor_id is not null and additionalmessage is not null;
update usernotification
	set parameters = json_build_object('message', additionalmessage)
	where type in ('SUBMITTEDDS', 'RETURNEDDS') and requestor_id is null and additionalmessage is not null;
update usernotification
	set parameters = json_build_object('grantedBy', additionalmessage)
	where type in ('GRANTFILEACCESSINFO') and requestor_id is null and additionalmessage is not null;
update usernotification
	set parameters = json_build_object('requestorId',requestor_id::text,'grantedBy', additionalmessage)
	where type in ('GRANTFILEACCESSINFO') and requestor_id is not null and additionalmessage is not null;
update usernotification
	set parameters = json_build_object('rejectedBy', additionalmessage)
	where type in ('REJECTFILEACCESSINFO') and requestor_id is null and additionalmessage is not null;
update usernotification
	set parameters = json_build_object('requestorId', requestor_id::text, 'rejectedBy', additionalmessage)
	where type in ('REJECTFILEACCESSINFO') and requestor_id is not null and additionalmessage is not null;
update usernotification
	set parameters = json_build_object('requestorId', requestor_id::text)
	where type not in ('REJECTFILEACCESSINFO', 'GRANTFILEACCESSINFO', 'SUBMITTEDDS', 'RETURNEDDS') and requestor_id is not null;