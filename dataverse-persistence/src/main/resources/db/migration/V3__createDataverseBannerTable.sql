create table dataversebanner
(
	id serial not null
		constraint dataversebanner_pkey
			primary key,
	active boolean,
	fromtime timestamp not null,
	totime timestamp not null,
	dataverse_id bigint
		constraint fk_dataversebanner_dataverse_id
			references dvobject
);