create table dataverselocalizedbanner
(
	id serial not null
		constraint dataverselocalizedbanner_pkey
			primary key,
	image bytea not null,
	imagelink varchar(255),
	contenttype varchar(255),
	imagename varchar(255),
	locale varchar(255) not null,
	dataversebanner_id bigint
		constraint fk_dataverselocalizedbanner_dataversebanner_id
			references dataversebanner
);