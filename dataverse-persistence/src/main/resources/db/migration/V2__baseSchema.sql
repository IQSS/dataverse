create table workflow
(
	id serial not null
		constraint workflow_pkey
			primary key,
	name varchar(255)
);



create table oaiset
(
	id serial not null
		constraint oaiset_pkey
			primary key,
	definition text,
	deleted boolean,
	description text,
	name text,
	spec text,
	updateinprogress boolean,
	version bigint
);



create table storagesite
(
	id serial not null
		constraint storagesite_pkey
			primary key,
	hostname text,
	name text,
	primarystorage boolean not null,
	transferprotocols text
);



create table oairecord
(
	id serial not null
		constraint oairecord_pkey
			primary key,
	globalid varchar(255),
	lastupdatetime timestamp,
	removed boolean,
	setname varchar(255)
);



create table shibgroup
(
	id serial not null
		constraint shibgroup_pkey
			primary key,
	attribute varchar(255) not null,
	name varchar(255) not null,
	pattern varchar(255) not null
);



create table persistedglobalgroup
(
	id bigint not null
		constraint persistedglobalgroup_pkey
			primary key,
	dtype varchar(31),
	description varchar(255),
	displayname varchar(255),
	persistedgroupalias varchar(255)
		constraint persistedglobalgroup_persistedgroupalias_key
			unique
);



create table ipv6range
(
	id bigint not null
		constraint ipv6range_pkey
			primary key,
	bottoma bigint,
	bottomb bigint,
	bottomc bigint,
	bottomd bigint,
	topa bigint,
	topb bigint,
	topc bigint,
	topd bigint,
	owner_id bigint
		constraint fk_ipv6range_owner_id
			references persistedglobalgroup
);



create index index_ipv6range_owner_id
	on ipv6range (owner_id);

create index index_persistedglobalgroup_dtype
	on persistedglobalgroup (dtype);

create table ipv4range
(
	id bigint not null
		constraint ipv4range_pkey
			primary key,
	bottomaslong bigint,
	topaslong bigint,
	owner_id bigint
		constraint fk_ipv4range_owner_id
			references persistedglobalgroup
);



create index index_ipv4range_owner_id
	on ipv4range (owner_id);

create table metric
(
	id serial not null
		constraint metric_pkey
			primary key,
	lastcalleddate timestamp not null,
	metricname varchar(255) not null
		constraint metric_metricname_key
			unique,
	metricvalue text
);



create index index_metric_id
	on metric (id);

create table workflowstepdata
(
	id serial not null
		constraint workflowstepdata_pkey
			primary key,
	providerid varchar(255),
	steptype varchar(255),
	parent_id bigint
		constraint fk_workflowstepdata_parent_id
			references workflow,
	index integer
);



create table customfieldmap
(
	id serial not null
		constraint customfieldmap_pkey
			primary key,
	sourcedatasetfield varchar(255),
	sourcetemplate varchar(255),
	targetdatasetfield varchar(255)
);



create index index_customfieldmap_sourcedatasetfield
	on customfieldmap (sourcedatasetfield);

create index index_customfieldmap_sourcetemplate
	on customfieldmap (sourcetemplate);

create table actionlogrecord
(
	id varchar(36) not null
		constraint actionlogrecord_pkey
			primary key,
	actionresult varchar(255),
	actionsubtype varchar(255),
	actiontype varchar(255),
	endtime timestamp,
	info text,
	starttime timestamp,
	useridentifier varchar(255)
);



create index index_actionlogrecord_useridentifier
	on actionlogrecord (useridentifier);

create index index_actionlogrecord_actiontype
	on actionlogrecord (actiontype);

create index index_actionlogrecord_starttime
	on actionlogrecord (starttime);

create table foreignmetadataformatmapping
(
	id serial not null
		constraint foreignmetadataformatmapping_pkey
			primary key,
	displayname varchar(255) not null,
	name varchar(255) not null,
	schemalocation varchar(255),
	startelement varchar(255)
);



create index index_foreignmetadataformatmapping_name
	on foreignmetadataformatmapping (name);

create table externaltool
(
	id serial not null
		constraint externaltool_pkey
			primary key,
	description text,
	displayname varchar(255) not null,
	toolparameters varchar(255) not null,
	toolurl varchar(255) not null,
	type varchar(255) not null
);



create table defaultvalueset
(
	id serial not null
		constraint defaultvalueset_pkey
			primary key,
	name varchar(255) not null
);



create table authenticateduser
(
	id serial not null
		constraint authenticateduser_pkey
			primary key,
	affiliation varchar(255),
	createdtime timestamp not null,
	email varchar(255) not null
		constraint authenticateduser_email_key
			unique,
	emailconfirmed timestamp,
	firstname varchar(255),
	lastapiusetime timestamp,
	lastlogintime timestamp,
	lastname varchar(255),
	position varchar(255),
	superuser boolean,
	useridentifier varchar(255) not null
		constraint authenticateduser_useridentifier_key
			unique
);



create table confirmemaildata
(
	id serial not null
		constraint confirmemaildata_pkey
			primary key,
	created timestamp not null,
	expires timestamp not null,
	token varchar(255),
	authenticateduser_id bigint not null
		constraint confirmemaildata_authenticateduser_id_key
			unique
		constraint fk_confirmemaildata_authenticateduser_id
			references authenticateduser
);



create index index_confirmemaildata_token
	on confirmemaildata (token);

create index index_confirmemaildata_authenticateduser_id
	on confirmemaildata (authenticateduser_id);

create table oauth2tokendata
(
	id serial not null
		constraint oauth2tokendata_pkey
			primary key,
	accesstoken text,
	expirydate timestamp,
	oauthproviderid varchar(255),
	rawresponse text,
	refreshtoken varchar(64),
	scope varchar(64),
	tokentype varchar(32),
	user_id bigint
		constraint fk_oauth2tokendata_user_id
			references authenticateduser
);



create table dvobject
(
	id serial not null
		constraint dvobject_pkey
			primary key,
	dtype varchar(31),
	authority varchar(255),
	createdate timestamp not null,
	globalidcreatetime timestamp,
	identifier varchar(255),
	identifierregistered boolean,
	indextime timestamp,
	modificationtime timestamp not null,
	permissionindextime timestamp,
	permissionmodificationtime timestamp,
	previewimageavailable boolean,
	protocol varchar(255),
	publicationdate timestamp,
	storageidentifier varchar(255),
	creator_id bigint
		constraint fk_dvobject_creator_id
			references authenticateduser,
	owner_id bigint
		constraint fk_dvobject_owner_id
			references dvobject,
	releaseuser_id bigint
		constraint fk_dvobject_releaseuser_id
			references authenticateduser,
	constraint unq_dvobject_0
		unique (authority, protocol, identifier)
);



create table dataversetheme
(
	id serial not null
		constraint dataversetheme_pkey
			primary key,
	backgroundcolor varchar(255),
	linkcolor varchar(255),
	linkurl varchar(255),
	logo varchar(255),
	logoalignment varchar(255),
	logobackgroundcolor varchar(255),
	logoformat varchar(255),
	tagline varchar(255),
	textcolor varchar(255),
	dataverse_id bigint
		constraint fk_dataversetheme_dataverse_id
			references dvobject
);



create index index_dataversetheme_dataverse_id
	on dataversetheme (dataverse_id);

create table datafilecategory
(
	id serial not null
		constraint datafilecategory_pkey
			primary key,
	name varchar(255) not null,
	dataset_id bigint not null
		constraint fk_datafilecategory_dataset_id
			references dvobject
);



create index index_datafilecategory_dataset_id
	on datafilecategory (dataset_id);

create table dataverselinkingdataverse
(
	id serial not null
		constraint dataverselinkingdataverse_pkey
			primary key,
	linkcreatetime timestamp,
	dataverse_id bigint not null
		constraint fk_dataverselinkingdataverse_dataverse_id
			references dvobject,
	linkingdataverse_id bigint not null
		constraint fk_dataverselinkingdataverse_linkingdataverse_id
			references dvobject
);



create index index_dataverselinkingdataverse_dataverse_id
	on dataverselinkingdataverse (dataverse_id);

create index index_dataverselinkingdataverse_linkingdataverse_id
	on dataverselinkingdataverse (linkingdataverse_id);

create table metadatablock
(
	id serial not null
		constraint metadatablock_pkey
			primary key,
	displayname varchar(255) not null,
	name varchar(255) not null,
	namespaceuri text,
	owner_id bigint
		constraint fk_metadatablock_owner_id
			references dvobject
);



create index index_metadatablock_name
	on metadatablock (name);

create index index_metadatablock_owner_id
	on metadatablock (owner_id);

create index index_dvobject_dtype
	on dvobject (dtype);

create index index_dvobject_owner_id
	on dvobject (owner_id);

create index index_dvobject_creator_id
	on dvobject (creator_id);

create index index_dvobject_releaseuser_id
	on dvobject (releaseuser_id);

create table dataversefeatureddataverse
(
	id serial not null
		constraint dataversefeatureddataverse_pkey
			primary key,
	displayorder integer,
	dataverse_id bigint
		constraint fk_dataversefeatureddataverse_dataverse_id
			references dvobject,
	featureddataverse_id bigint
		constraint fk_dataversefeatureddataverse_featureddataverse_id
			references dvobject
);



create index index_dataversefeatureddataverse_dataverse_id
	on dataversefeatureddataverse (dataverse_id);

create index index_dataversefeatureddataverse_featureddataverse_id
	on dataversefeatureddataverse (featureddataverse_id);

create index index_dataversefeatureddataverse_displayorder
	on dataversefeatureddataverse (displayorder);

create table harvestingclient
(
	id serial not null
		constraint harvestingclient_pkey
			primary key,
	archivedescription text,
	archiveurl varchar(255),
	deleted boolean,
	harveststyle varchar(255),
	harvesttype varchar(255),
	harvestingnow boolean,
	harvestingset varchar(255),
	harvestingurl varchar(255),
	metadataprefix varchar(255),
	name varchar(255) not null
		constraint harvestingclient_name_key
			unique,
	scheduledayofweek integer,
	schedulehourofday integer,
	scheduleperiod varchar(255),
	scheduled boolean,
	dataverse_id bigint
		constraint fk_harvestingclient_dataverse_id
			references dvobject
);



create index index_harvestingclient_dataverse_id
	on harvestingclient (dataverse_id);

create index index_harvestingclient_harvesttype
	on harvestingclient (harvesttype);

create index index_harvestingclient_harveststyle
	on harvestingclient (harveststyle);

create index index_harvestingclient_harvestingurl
	on harvestingclient (harvestingurl);

create table apitoken
(
	id serial not null
		constraint apitoken_pkey
			primary key,
	createtime timestamp not null,
	disabled boolean not null,
	expiretime timestamp not null,
	tokenstring varchar(255) not null
		constraint apitoken_tokenstring_key
			unique,
	authenticateduser_id bigint not null
		constraint fk_apitoken_authenticateduser_id
			references authenticateduser
);



create index index_apitoken_authenticateduser_id
	on apitoken (authenticateduser_id);

create table dataversetextmessage
(
	id serial not null
		constraint dataversetextmessage_pkey
			primary key,
	active boolean,
	fromtime timestamp,
	totime timestamp,
	version bigint,
	dataverse_id bigint
		constraint fk_dataversetextmessage_dataverse_id
			references dvobject
);



create index index_dataversetextmessage_dataverse_id
	on dataversetextmessage (dataverse_id);

create table usernotification
(
	id serial not null
		constraint usernotification_pkey
			primary key,
	emailed boolean,
	objectid bigint,
	readnotification boolean,
	senddate timestamp,
	type integer not null,
	requestor_id bigint
		constraint fk_usernotification_requestor_id
			references authenticateduser,
	user_id bigint not null
		constraint fk_usernotification_user_id
			references authenticateduser
);



create index index_usernotification_user_id
	on usernotification (user_id);

create table guestbook
(
	id serial not null
		constraint guestbook_pkey
			primary key,
	createtime timestamp not null,
	emailrequired boolean,
	enabled boolean,
	institutionrequired boolean,
	name varchar(255),
	namerequired boolean,
	positionrequired boolean,
	dataverse_id bigint
		constraint fk_guestbook_dataverse_id
			references dvobject
);



create table customquestion
(
	id serial not null
		constraint customquestion_pkey
			primary key,
	displayorder integer,
	hidden boolean,
	questionstring varchar(255) not null,
	questiontype varchar(255) not null,
	required boolean,
	guestbook_id bigint not null
		constraint fk_customquestion_guestbook_id
			references guestbook
);



create index index_customquestion_guestbook_id
	on customquestion (guestbook_id);

create table maplayermetadata
(
	id serial not null
		constraint maplayermetadata_pkey
			primary key,
	embedmaplink varchar(255) not null,
	isjoinlayer boolean,
	joindescription text,
	lastverifiedstatus integer,
	lastverifiedtime timestamp,
	layerlink varchar(255) not null,
	layername varchar(255) not null,
	mapimagelink varchar(255),
	maplayerlinks text,
	worldmapusername varchar(255) not null,
	dataset_id bigint not null
		constraint fk_maplayermetadata_dataset_id
			references dvobject,
	datafile_id bigint not null
		constraint maplayermetadata_datafile_id_key
			unique
		constraint fk_maplayermetadata_datafile_id
			references dvobject
);



create index index_maplayermetadata_dataset_id
	on maplayermetadata (dataset_id);

create table savedsearch
(
	id serial not null
		constraint savedsearch_pkey
			primary key,
	query text,
	creator_id bigint not null
		constraint fk_savedsearch_creator_id
			references authenticateduser,
	definitionpoint_id bigint not null
		constraint fk_savedsearch_definitionpoint_id
			references dvobject
);



create table savedsearchfilterquery
(
	id serial not null
		constraint savedsearchfilterquery_pkey
			primary key,
	filterquery text,
	savedsearch_id bigint not null
		constraint fk_savedsearchfilterquery_savedsearch_id
			references savedsearch
);



create index index_savedsearchfilterquery_savedsearch_id
	on savedsearchfilterquery (savedsearch_id);

create index index_savedsearch_definitionpoint_id
	on savedsearch (definitionpoint_id);

create index index_savedsearch_creator_id
	on savedsearch (creator_id);

create table explicitgroup
(
	id serial not null
		constraint explicitgroup_pkey
			primary key,
	description varchar(1024),
	displayname varchar(255),
	groupalias varchar(255)
		constraint explicitgroup_groupalias_key
			unique,
	groupaliasinowner varchar(255),
	owner_id bigint
		constraint fk_explicitgroup_owner_id
			references dvobject
);



create index index_explicitgroup_owner_id
	on explicitgroup (owner_id);

create index index_explicitgroup_groupaliasinowner
	on explicitgroup (groupaliasinowner);

create table pendingworkflowinvocation
(
	invocationid varchar(255) not null
		constraint pendingworkflowinvocation_pkey
			primary key,
	datasetexternallyreleased boolean,
	ipaddress varchar(255),
	nextminorversionnumber bigint,
	nextversionnumber bigint,
	pendingstepidx integer,
	typeordinal integer,
	userid varchar(255),
	workflow_id bigint
		constraint fk_pendingworkflowinvocation_workflow_id
			references workflow,
	dataset_id bigint
		constraint fk_pendingworkflowinvocation_dataset_id
			references dvobject
);



create unique index index_authenticateduser_lower_email
	on authenticateduser (lower(email::text));

create table datatable
(
	id serial not null
		constraint datatable_pkey
			primary key,
	casequantity bigint,
	originalfileformat varchar(255),
	originalformatversion varchar(255),
	recordspercase bigint,
	unf varchar(255) not null,
	varquantity bigint,
	datafile_id bigint not null
		constraint fk_datatable_datafile_id
			references dvobject
);



create index index_datatable_datafile_id
	on datatable (datafile_id);

create table ingestreport
(
	id serial not null
		constraint ingestreport_pkey
			primary key,
	endtime timestamp,
	report text,
	starttime timestamp,
	status integer,
	type integer,
	datafile_id bigint not null
		constraint fk_ingestreport_datafile_id
			references dvobject
);



create index index_ingestreport_datafile_id
	on ingestreport (datafile_id);

create table authenticationproviderrow
(
	id varchar(255) not null
		constraint authenticationproviderrow_pkey
			primary key,
	enabled boolean,
	factoryalias varchar(255),
	factorydata text,
	subtitle varchar(255),
	title varchar(255)
);



create index index_authenticationproviderrow_enabled
	on authenticationproviderrow (enabled);

create table foreignmetadatafieldmapping
(
	id serial not null
		constraint foreignmetadatafieldmapping_pkey
			primary key,
	datasetfieldname text,
	foreignfieldxpath text,
	isattribute boolean,
	foreignmetadataformatmapping_id bigint
		constraint fk_foreignmetadatafieldmapping_foreignmetadataformatmapping_id
			references foreignmetadataformatmapping,
	parentfieldmapping_id bigint
		constraint fk_foreignmetadatafieldmapping_parentfieldmapping_id
			references foreignmetadatafieldmapping,
	constraint unq_foreignmetadatafieldmapping_0
		unique (foreignmetadataformatmapping_id, foreignfieldxpath)
);



create index index_foreignmetadatafieldmapping_foreignmetadataformatmapping_
	on foreignmetadatafieldmapping (foreignmetadataformatmapping_id);

create index index_foreignmetadatafieldmapping_foreignfieldxpath
	on foreignmetadatafieldmapping (foreignfieldxpath);

create index index_foreignmetadatafieldmapping_parentfieldmapping_id
	on foreignmetadatafieldmapping (parentfieldmapping_id);

create table dataverselocalizedmessage
(
	id serial not null
		constraint dataverselocalizedmessage_pkey
			primary key,
	locale varchar(255) not null,
	message varchar(255) not null,
	dataversetextmessage_id bigint
		constraint fk_dataverselocalizedmessage_dataversetextmessage_id
			references dataversetextmessage
);



create index index_dataverselocalizedmessage_dataversetextmessage_id
	on dataverselocalizedmessage (dataversetextmessage_id);

create table customquestionvalue
(
	id serial not null
		constraint customquestionvalue_pkey
			primary key,
	displayorder integer,
	valuestring varchar(255) not null,
	customquestion_id bigint not null
		constraint fk_customquestionvalue_customquestion_id
			references customquestion
);



create table datasetlinkingdataverse
(
	id serial not null
		constraint datasetlinkingdataverse_pkey
			primary key,
	linkcreatetime timestamp not null,
	dataset_id bigint not null
		constraint fk_datasetlinkingdataverse_dataset_id
			references dvobject,
	linkingdataverse_id bigint not null
		constraint fk_datasetlinkingdataverse_linkingdataverse_id
			references dvobject
);



create index index_datasetlinkingdataverse_dataset_id
	on datasetlinkingdataverse (dataset_id);

create index index_datasetlinkingdataverse_linkingdataverse_id
	on datasetlinkingdataverse (linkingdataverse_id);

create table clientharvestrun
(
	id serial not null
		constraint clientharvestrun_pkey
			primary key,
	deleteddatasetcount bigint,
	faileddatasetcount bigint,
	finishtime timestamp,
	harvestresult integer,
	harvesteddatasetcount bigint,
	starttime timestamp,
	harvestingclient_id bigint not null
		constraint fk_clientharvestrun_harvestingclient_id
			references harvestingclient
);



create table worldmapauth_tokentype
(
	id serial not null
		constraint worldmapauth_tokentype_pkey
			primary key,
	contactemail varchar(255),
	created timestamp not null,
	hostname varchar(255),
	ipaddress varchar(255),
	mapitlink varchar(255) not null,
	md5 varchar(255) not null,
	modified timestamp not null,
	name varchar(255) not null,
	timelimitminutes integer default 30,
	timelimitseconds bigint default 1800
);



create table worldmapauth_token
(
	id serial not null
		constraint worldmapauth_token_pkey
			primary key,
	created timestamp not null,
	hasexpired boolean not null,
	lastrefreshtime timestamp not null,
	modified timestamp not null,
	token varchar(255),
	application_id bigint not null
		constraint fk_worldmapauth_token_application_id
			references worldmapauth_tokentype,
	datafile_id bigint not null
		constraint fk_worldmapauth_token_datafile_id
			references dvobject,
	dataverseuser_id bigint not null
		constraint fk_worldmapauth_token_dataverseuser_id
			references authenticateduser
);



create unique index token_value
	on worldmapauth_token (token);

create index index_worldmapauth_token_application_id
	on worldmapauth_token (application_id);

create index index_worldmapauth_token_datafile_id
	on worldmapauth_token (datafile_id);

create index index_worldmapauth_token_dataverseuser_id
	on worldmapauth_token (dataverseuser_id);

create unique index application_name
	on worldmapauth_tokentype (name);

create table datafiletag
(
	id serial not null
		constraint datafiletag_pkey
			primary key,
	type integer not null,
	datafile_id bigint not null
		constraint fk_datafiletag_datafile_id
			references dvobject
);



create index index_datafiletag_datafile_id
	on datafiletag (datafile_id);

create table authenticateduserlookup
(
	id serial not null
		constraint authenticateduserlookup_pkey
			primary key,
	authenticationproviderid varchar(255),
	persistentuserid varchar(255),
	authenticateduser_id bigint not null
		constraint authenticateduserlookup_authenticateduser_id_key
			unique
		constraint fk_authenticateduserlookup_authenticateduser_id
			references authenticateduser,
	constraint unq_authenticateduserlookup_0
		unique (persistentuserid, authenticationproviderid)
);



create table ingestrequest
(
	id serial not null
		constraint ingestrequest_pkey
			primary key,
	controlcard varchar(255),
	forcetypecheck boolean,
	labelsfile varchar(255),
	textencoding varchar(255),
	datafile_id bigint
		constraint fk_ingestrequest_datafile_id
			references dvobject
);



create index index_ingestrequest_datafile_id
	on ingestrequest (datafile_id);

create table setting
(
	name varchar(255) not null
		constraint setting_pkey
			primary key,
	content text
);



create table dataversecontact
(
	id serial not null
		constraint dataversecontact_pkey
			primary key,
	contactemail varchar(255) not null,
	displayorder integer,
	dataverse_id bigint
		constraint fk_dataversecontact_dataverse_id
			references dvobject
);



create index index_dataversecontact_dataverse_id
	on dataversecontact (dataverse_id);

create index index_dataversecontact_contactemail
	on dataversecontact (contactemail);

create index index_dataversecontact_displayorder
	on dataversecontact (displayorder);

create table datavariable
(
	id serial not null
		constraint datavariable_pkey
			primary key,
	factor boolean,
	fileendposition bigint,
	fileorder integer,
	filestartposition bigint,
	format varchar(255),
	formatcategory varchar(255),
	interval integer,
	label text,
	name varchar(255),
	numberofdecimalpoints bigint,
	orderedfactor boolean,
	recordsegmentnumber bigint,
	type integer,
	unf varchar(255),
	universe varchar(255),
	weighted boolean,
	datatable_id bigint not null
		constraint fk_datavariable_datatable_id
			references datatable
);



create table variablerangeitem
(
	id serial not null
		constraint variablerangeitem_pkey
			primary key,
	value numeric(38),
	datavariable_id bigint not null
		constraint fk_variablerangeitem_datavariable_id
			references datavariable
);



create index index_variablerangeitem_datavariable_id
	on variablerangeitem (datavariable_id);

create table variablerange
(
	id serial not null
		constraint variablerange_pkey
			primary key,
	beginvalue varchar(255),
	beginvaluetype integer,
	endvalue varchar(255),
	endvaluetype integer,
	datavariable_id bigint not null
		constraint fk_variablerange_datavariable_id
			references datavariable
);



create index index_variablerange_datavariable_id
	on variablerange (datavariable_id);

create table summarystatistic
(
	id serial not null
		constraint summarystatistic_pkey
			primary key,
	type integer,
	value varchar(255),
	datavariable_id bigint not null
		constraint fk_summarystatistic_datavariable_id
			references datavariable
);



create index index_summarystatistic_datavariable_id
	on summarystatistic (datavariable_id);

create table variablecategory
(
	id serial not null
		constraint variablecategory_pkey
			primary key,
	catorder integer,
	frequency double precision,
	label varchar(255),
	missing boolean,
	value varchar(255),
	datavariable_id bigint not null
		constraint fk_variablecategory_datavariable_id
			references datavariable
);



create index index_variablecategory_datavariable_id
	on variablecategory (datavariable_id);

create index index_datavariable_datatable_id
	on datavariable (datatable_id);

create table datafile
(
	id bigint not null
		constraint datafile_pkey
			primary key
		constraint fk_datafile_id
			references dvobject,
	checksumtype varchar(255) not null,
	checksumvalue varchar(255) not null,
	contenttype varchar(255) not null,
	filesize bigint,
	ingeststatus char,
	previousdatafileid bigint,
	prov_entityname text,
	restricted boolean,
	rootdatafileid bigint not null
);



create index index_datafile_ingeststatus
	on datafile (ingeststatus);

create index index_datafile_checksumvalue
	on datafile (checksumvalue);

create index index_datafile_contenttype
	on datafile (contenttype);

create index index_datafile_restricted
	on datafile (restricted);

create table builtinuser
(
	id serial not null
		constraint builtinuser_pkey
			primary key,
	encryptedpassword varchar(255),
	passwordencryptionversion integer,
	username varchar(255) not null
		constraint builtinuser_username_key
			unique
);



create table passwordresetdata
(
	id serial not null
		constraint passwordresetdata_pkey
			primary key,
	created timestamp not null,
	expires timestamp not null,
	reason varchar(255),
	token varchar(255),
	builtinuser_id bigint not null
		constraint fk_passwordresetdata_builtinuser_id
			references builtinuser
);



create index index_passwordresetdata_token
	on passwordresetdata (token);

create index index_passwordresetdata_builtinuser_id
	on passwordresetdata (builtinuser_id);

create index index_builtinuser_username
	on builtinuser (username);

create table termsofuseandaccess
(
	id serial not null
		constraint termsofuseandaccess_pkey
			primary key,
	availabilitystatus text,
	citationrequirements text,
	conditions text,
	confidentialitydeclaration text,
	contactforaccess text,
	dataaccessplace text,
	depositorrequirements text,
	disclaimer text,
	fileaccessrequest boolean,
	license varchar(255),
	originalarchive text,
	restrictions text,
	sizeofcollection text,
	specialpermissions text,
	studycompletion text,
	termsofaccess text,
	termsofuse text
);



create table datasetversion
(
	id serial not null
		constraint datasetversion_pkey
			primary key,
	unf varchar(255),
	archivenote varchar(1000),
	archivetime timestamp,
	createtime timestamp not null,
	deaccessionlink varchar(255),
	lastupdatetime timestamp not null,
	minorversionnumber bigint,
	releasetime timestamp,
	version bigint,
	versionnote varchar(1000),
	versionnumber bigint,
	versionstate varchar(255),
	dataset_id bigint
		constraint fk_datasetversion_dataset_id
			references dvobject,
	termsofuseandaccess_id bigint
		constraint fk_datasetversion_termsofuseandaccess_id
			references termsofuseandaccess,
	constraint unq_datasetversion_0
		unique (dataset_id, versionnumber, minorversionnumber)
);



create table workflowcomment
(
	id serial not null
		constraint workflowcomment_pkey
			primary key,
	created timestamp not null,
	message text,
	type varchar(255) not null,
	authenticateduser_id bigint
		constraint fk_workflowcomment_authenticateduser_id
			references authenticateduser,
	datasetversion_id bigint not null
		constraint fk_workflowcomment_datasetversion_id
			references datasetversion
);



create index index_datasetversion_dataset_id
	on datasetversion (dataset_id);

create unique index one_draft_version_per_dataset
	on datasetversion (dataset_id)
	where ((versionstate)::text = 'DRAFT'::text);

create table filemetadata
(
	id serial not null
		constraint filemetadata_pkey
			primary key,
	description text,
	directorylabel varchar(255),
	label varchar(255) not null,
	prov_freeform text,
	restricted boolean,
	version bigint,
	datafile_id bigint not null
		constraint fk_filemetadata_datafile_id
			references dvobject,
	datasetversion_id bigint not null
		constraint fk_filemetadata_datasetversion_id
			references datasetversion,
	displayorder integer
);



create index index_filemetadata_datafile_id
	on filemetadata (datafile_id);

create index index_filemetadata_datasetversion_id
	on filemetadata (datasetversion_id);

create table doidataciteregistercache
(
	id serial not null
		constraint doidataciteregistercache_pkey
			primary key,
	doi varchar(255)
		constraint doidataciteregistercache_doi_key
			unique,
	status varchar(255),
	url varchar(255),
	xml text
);



create table harvestingdataverseconfig
(
	id bigint not null
		constraint harvestingdataverseconfig_pkey
			primary key,
	archivedescription text,
	archiveurl varchar(255),
	harveststyle varchar(255),
	harvesttype varchar(255),
	harvestingset varchar(255),
	harvestingurl varchar(255),
	dataverse_id bigint
		constraint fk_harvestingdataverseconfig_dataverse_id
			references dvobject
);



create index index_harvestingdataverseconfig_dataverse_id
	on harvestingdataverseconfig (dataverse_id);

create index index_harvestingdataverseconfig_harvesttype
	on harvestingdataverseconfig (harvesttype);

create index index_harvestingdataverseconfig_harveststyle
	on harvestingdataverseconfig (harveststyle);

create index index_harvestingdataverseconfig_harvestingurl
	on harvestingdataverseconfig (harvestingurl);

create table alternativepersistentidentifier
(
	id serial not null
		constraint alternativepersistentidentifier_pkey
			primary key,
	authority varchar(255),
	globalidcreatetime timestamp,
	identifier varchar(255),
	identifierregistered boolean,
	protocol varchar(255),
	storagelocationdesignator boolean,
	dvobject_id bigint not null
		constraint fk_alternativepersistentidentifier_dvobject_id
			references dvobject
);



create table datasetversionuser
(
	id serial not null
		constraint datasetversionuser_pkey
			primary key,
	lastupdatedate timestamp not null,
	authenticateduser_id bigint
		constraint fk_datasetversionuser_authenticateduser_id
			references authenticateduser,
	datasetversion_id bigint
		constraint fk_datasetversionuser_datasetversion_id
			references datasetversion
);



create index index_datasetversionuser_authenticateduser_id
	on datasetversionuser (authenticateduser_id);

create index index_datasetversionuser_datasetversion_id
	on datasetversionuser (datasetversion_id);

create table guestbookresponse
(
	id serial not null
		constraint guestbookresponse_pkey
			primary key,
	downloadtype varchar(255),
	email varchar(255),
	institution varchar(255),
	name varchar(255),
	position varchar(255),
	responsetime timestamp,
	sessionid varchar(255),
	authenticateduser_id bigint
		constraint fk_guestbookresponse_authenticateduser_id
			references authenticateduser,
	datafile_id bigint not null
		constraint fk_guestbookresponse_datafile_id
			references dvobject,
	dataset_id bigint not null
		constraint fk_guestbookresponse_dataset_id
			references dvobject,
	datasetversion_id bigint
		constraint fk_guestbookresponse_datasetversion_id
			references datasetversion,
	guestbook_id bigint not null
		constraint fk_guestbookresponse_guestbook_id
			references guestbook
);



create index index_guestbookresponse_guestbook_id
	on guestbookresponse (guestbook_id);

create index index_guestbookresponse_datafile_id
	on guestbookresponse (datafile_id);

create index index_guestbookresponse_dataset_id
	on guestbookresponse (dataset_id);

create table customquestionresponse
(
	id serial not null
		constraint customquestionresponse_pkey
			primary key,
	response text,
	customquestion_id bigint not null
		constraint fk_customquestionresponse_customquestion_id
			references customquestion,
	guestbookresponse_id bigint not null
		constraint fk_customquestionresponse_guestbookresponse_id
			references guestbookresponse
);



create index index_customquestionresponse_guestbookresponse_id
	on customquestionresponse (guestbookresponse_id);

create table template
(
	id serial not null
		constraint template_pkey
			primary key,
	createtime timestamp not null,
	name varchar(255) not null,
	usagecount bigint,
	dataverse_id bigint
		constraint fk_template_dataverse_id
			references dvobject,
	termsofuseandaccess_id bigint
		constraint fk_template_termsofuseandaccess_id
			references termsofuseandaccess
);



create index index_template_dataverse_id
	on template (dataverse_id);

create table datasetlock
(
	id serial not null
		constraint datasetlock_pkey
			primary key,
	info varchar(255),
	reason varchar(255) not null,
	starttime timestamp,
	dataset_id bigint not null
		constraint fk_datasetlock_dataset_id
			references dvobject,
	user_id bigint not null
		constraint fk_datasetlock_user_id
			references authenticateduser
);



create index index_datasetlock_user_id
	on datasetlock (user_id);

create index index_datasetlock_dataset_id
	on datasetlock (dataset_id);

create table dataverserole
(
	id serial not null
		constraint dataverserole_pkey
			primary key,
	alias varchar(255) not null
		constraint dataverserole_alias_key
			unique,
	description varchar(255),
	name varchar(255) not null,
	permissionbits bigint,
	owner_id bigint
		constraint fk_dataverserole_owner_id
			references dvobject
);



create table roleassignment
(
	id serial not null
		constraint roleassignment_pkey
			primary key,
	assigneeidentifier varchar(255) not null,
	privateurltoken varchar(255),
	definitionpoint_id bigint not null
		constraint fk_roleassignment_definitionpoint_id
			references dvobject,
	role_id bigint not null
		constraint fk_roleassignment_role_id
			references dataverserole,
	constraint unq_roleassignment_0
		unique (assigneeidentifier, role_id, definitionpoint_id)
);



create index index_roleassignment_assigneeidentifier
	on roleassignment (assigneeidentifier);

create index index_roleassignment_definitionpoint_id
	on roleassignment (definitionpoint_id);

create index index_roleassignment_role_id
	on roleassignment (role_id);

create table dataverse
(
	id bigint not null
		constraint dataverse_pkey
			primary key
		constraint fk_dataverse_id
			references dvobject,
	affiliation varchar(255),
	alias varchar(255) not null
		constraint dataverse_alias_key
			unique,
	allowmessagesbanners boolean,
	dataversetype varchar(255) not null,
	description text,
	facetroot boolean,
	guestbookroot boolean,
	metadatablockroot boolean,
	name varchar(255) not null,
	permissionroot boolean,
	templateroot boolean,
	themeroot boolean,
	defaultcontributorrole_id bigint
		constraint fk_dataverse_defaultcontributorrole_id
			references dataverserole,
	defaulttemplate_id bigint
		constraint fk_dataverse_defaulttemplate_id
			references template
);



create index index_dataverse_defaultcontributorrole_id
	on dataverse (defaultcontributorrole_id);

create index index_dataverse_defaulttemplate_id
	on dataverse (defaulttemplate_id);

create index index_dataverse_alias
	on dataverse (alias);

create index index_dataverse_affiliation
	on dataverse (affiliation);

create index index_dataverse_dataversetype
	on dataverse (dataversetype);

create index index_dataverse_facetroot
	on dataverse (facetroot);

create index index_dataverse_guestbookroot
	on dataverse (guestbookroot);

create index index_dataverse_metadatablockroot
	on dataverse (metadatablockroot);

create index index_dataverse_templateroot
	on dataverse (templateroot);

create index index_dataverse_permissionroot
	on dataverse (permissionroot);

create index index_dataverse_themeroot
	on dataverse (themeroot);

create unique index dataverse_alias_unique_idx
	on dataverse (lower(alias::text));

create index index_dataverserole_owner_id
	on dataverserole (owner_id);

create index index_dataverserole_name
	on dataverserole (name);

create index index_dataverserole_alias
	on dataverserole (alias);

create table datasetfieldtype
(
	id serial not null
		constraint datasetfieldtype_pkey
			primary key,
	advancedsearchfieldtype boolean,
	allowcontrolledvocabulary boolean,
	allowmultiples boolean,
	description text,
	displayformat varchar(255),
	displayoncreate boolean,
	displayorder integer,
	facetable boolean,
	fieldtype varchar(255) not null,
	name text,
	required boolean,
	title text,
	uri text,
	validationformat varchar(255),
	watermark varchar(255),
	metadatablock_id bigint
		constraint fk_datasetfieldtype_metadatablock_id
			references metadatablock,
	parentdatasetfieldtype_id bigint
		constraint fk_datasetfieldtype_parentdatasetfieldtype_id
			references datasetfieldtype
);



create table dataversefacet
(
	id serial not null
		constraint dataversefacet_pkey
			primary key,
	displayorder integer,
	datasetfieldtype_id bigint
		constraint fk_dataversefacet_datasetfieldtype_id
			references datasetfieldtype,
	dataverse_id bigint
		constraint fk_dataversefacet_dataverse_id
			references dvobject
);



create index index_dataversefacet_dataverse_id
	on dataversefacet (dataverse_id);

create index index_dataversefacet_datasetfieldtype_id
	on dataversefacet (datasetfieldtype_id);

create index index_dataversefacet_displayorder
	on dataversefacet (displayorder);

create table datasetfield
(
	id serial not null
		constraint datasetfield_pkey
			primary key,
	datasetfieldtype_id bigint not null
		constraint fk_datasetfield_datasetfieldtype_id
			references datasetfieldtype,
	datasetversion_id bigint
		constraint fk_datasetfield_datasetversion_id
			references datasetversion,
	parentdatasetfieldcompoundvalue_id bigint,
	template_id bigint
		constraint fk_datasetfield_template_id
			references template
);



create table datasetfieldvalue
(
	id serial not null
		constraint datasetfieldvalue_pkey
			primary key,
	displayorder integer,
	value text,
	datasetfield_id bigint not null
		constraint fk_datasetfieldvalue_datasetfield_id
			references datasetfield
);



create index index_datasetfieldvalue_datasetfield_id
	on datasetfieldvalue (datasetfield_id);

create index index_datasetfield_datasetfieldtype_id
	on datasetfield (datasetfieldtype_id);

create index index_datasetfield_datasetversion_id
	on datasetfield (datasetversion_id);

create index index_datasetfield_parentdatasetfieldcompoundvalue_id
	on datasetfield (parentdatasetfieldcompoundvalue_id);

create index index_datasetfield_template_id
	on datasetfield (template_id);

create table datasetfielddefaultvalue
(
	id serial not null
		constraint datasetfielddefaultvalue_pkey
			primary key,
	displayorder integer,
	strvalue text,
	datasetfield_id bigint not null
		constraint fk_datasetfielddefaultvalue_datasetfield_id
			references datasetfieldtype,
	defaultvalueset_id bigint not null
		constraint fk_datasetfielddefaultvalue_defaultvalueset_id
			references defaultvalueset,
	parentdatasetfielddefaultvalue_id bigint
		constraint fk_datasetfielddefaultvalue_parentdatasetfielddefaultvalue_id
			references datasetfielddefaultvalue
);



create index index_datasetfielddefaultvalue_datasetfield_id
	on datasetfielddefaultvalue (datasetfield_id);

create index index_datasetfielddefaultvalue_defaultvalueset_id
	on datasetfielddefaultvalue (defaultvalueset_id);

create index index_datasetfielddefaultvalue_parentdatasetfielddefaultvalue_i
	on datasetfielddefaultvalue (parentdatasetfielddefaultvalue_id);

create index index_datasetfielddefaultvalue_displayorder
	on datasetfielddefaultvalue (displayorder);

create table controlledvocabularyvalue
(
	id serial not null
		constraint controlledvocabularyvalue_pkey
			primary key,
	displayorder integer,
	identifier varchar(255),
	strvalue text,
	datasetfieldtype_id bigint
		constraint fk_controlledvocabularyvalue_datasetfieldtype_id
			references datasetfieldtype
);



create index index_controlledvocabularyvalue_datasetfieldtype_id
	on controlledvocabularyvalue (datasetfieldtype_id);

create index index_controlledvocabularyvalue_displayorder
	on controlledvocabularyvalue (displayorder);

create table dataset
(
	id bigint not null
		constraint dataset_pkey
			primary key
		constraint fk_dataset_id
			references dvobject,
	fileaccessrequest boolean,
	harvestidentifier varchar(255),
	lastexporttime timestamp,
	usegenericthumbnail boolean,
	citationdatedatasetfieldtype_id bigint
		constraint fk_dataset_citationdatedatasetfieldtype_id
			references datasetfieldtype,
	harvestingclient_id bigint
		constraint fk_dataset_harvestingclient_id
			references harvestingclient,
	guestbook_id bigint
		constraint fk_dataset_guestbook_id
			references guestbook,
	thumbnailfile_id bigint
		constraint fk_dataset_thumbnailfile_id
			references dvobject
);



create index index_dataset_guestbook_id
	on dataset (guestbook_id);

create index index_dataset_thumbnailfile_id
	on dataset (thumbnailfile_id);

create table controlledvocabalternate
(
	id serial not null
		constraint controlledvocabalternate_pkey
			primary key,
	strvalue text,
	controlledvocabularyvalue_id bigint not null
		constraint fk_controlledvocabalternate_controlledvocabularyvalue_id
			references controlledvocabularyvalue,
	datasetfieldtype_id bigint not null
		constraint fk_controlledvocabalternate_datasetfieldtype_id
			references datasetfieldtype
);



create index index_controlledvocabalternate_controlledvocabularyvalue_id
	on controlledvocabalternate (controlledvocabularyvalue_id);

create index index_controlledvocabalternate_datasetfieldtype_id
	on controlledvocabalternate (datasetfieldtype_id);

create table dataversefieldtypeinputlevel
(
	id serial not null
		constraint dataversefieldtypeinputlevel_pkey
			primary key,
	include boolean,
	required boolean,
	datasetfieldtype_id bigint
		constraint fk_dataversefieldtypeinputlevel_datasetfieldtype_id
			references datasetfieldtype,
	dataverse_id bigint
		constraint fk_dataversefieldtypeinputlevel_dataverse_id
			references dvobject,
	constraint unq_dataversefieldtypeinputlevel_0
		unique (dataverse_id, datasetfieldtype_id)
);



create index index_dataversefieldtypeinputlevel_dataverse_id
	on dataversefieldtypeinputlevel (dataverse_id);

create index index_dataversefieldtypeinputlevel_datasetfieldtype_id
	on dataversefieldtypeinputlevel (datasetfieldtype_id);

create index index_dataversefieldtypeinputlevel_required
	on dataversefieldtypeinputlevel (required);

create table datasetfieldcompoundvalue
(
	id serial not null
		constraint datasetfieldcompoundvalue_pkey
			primary key,
	displayorder integer,
	parentdatasetfield_id bigint
		constraint fk_datasetfieldcompoundvalue_parentdatasetfield_id
			references datasetfield
);



alter table datasetfield
	add constraint fk_datasetfield_parentdatasetfieldcompoundvalue_id
		foreign key (parentdatasetfieldcompoundvalue_id) references datasetfieldcompoundvalue;

create index index_datasetfieldcompoundvalue_parentdatasetfield_id
	on datasetfieldcompoundvalue (parentdatasetfield_id);

create index index_datasetfieldtype_metadatablock_id
	on datasetfieldtype (metadatablock_id);

create index index_datasetfieldtype_parentdatasetfieldtype_id
	on datasetfieldtype (parentdatasetfieldtype_id);

create table filemetadata_datafilecategory
(
	filecategories_id bigint not null
		constraint fk_filemetadata_datafilecategory_filecategories_id
			references datafilecategory,
	filemetadatas_id bigint not null
		constraint fk_filemetadata_datafilecategory_filemetadatas_id
			references filemetadata,
	constraint filemetadata_datafilecategory_pkey
		primary key (filecategories_id, filemetadatas_id)
);



create index index_filemetadata_datafilecategory_filecategories_id
	on filemetadata_datafilecategory (filecategories_id);

create index index_filemetadata_datafilecategory_filemetadatas_id
	on filemetadata_datafilecategory (filemetadatas_id);

create table dataverse_citationdatasetfieldtypes
(
	dataverse_id bigint not null
		constraint fk_dataverse_citationdatasetfieldtypes_dataverse_id
			references dvobject,
	citationdatasetfieldtype_id bigint not null
		constraint dataverse_citationdatasetfieldtypes_citationdatasetfieldtype_id
			references datasetfieldtype,
	constraint dataverse_citationdatasetfieldtypes_pkey
		primary key (dataverse_id, citationdatasetfieldtype_id)
);



create table dataversesubjects
(
	dataverse_id bigint not null
		constraint fk_dataversesubjects_dataverse_id
			references dvobject,
	controlledvocabularyvalue_id bigint not null
		constraint fk_dataversesubjects_controlledvocabularyvalue_id
			references controlledvocabularyvalue,
	constraint dataversesubjects_pkey
		primary key (dataverse_id, controlledvocabularyvalue_id)
);



create table dataverse_metadatablock
(
	dataverse_id bigint not null
		constraint fk_dataverse_metadatablock_dataverse_id
			references dvobject,
	metadatablocks_id bigint not null
		constraint fk_dataverse_metadatablock_metadatablocks_id
			references metadatablock,
	constraint dataverse_metadatablock_pkey
		primary key (dataverse_id, metadatablocks_id)
);



create table datasetfield_controlledvocabularyvalue
(
	datasetfield_id bigint not null
		constraint fk_datasetfield_controlledvocabularyvalue_datasetfield_id
			references datasetfield,
	controlledvocabularyvalues_id bigint not null
		constraint dtasetfieldcontrolledvocabularyvaluecntrolledvocabularyvaluesid
			references controlledvocabularyvalue,
	constraint datasetfield_controlledvocabularyvalue_pkey
		primary key (datasetfield_id, controlledvocabularyvalues_id)
);



create index index_datasetfield_controlledvocabularyvalue_datasetfield_id
	on datasetfield_controlledvocabularyvalue (datasetfield_id);

create index index_datasetfield_controlledvocabularyvalue_controlledvocabula
	on datasetfield_controlledvocabularyvalue (controlledvocabularyvalues_id);

create table workflowstepdata_stepparameters
(
	workflowstepdata_id bigint
		constraint fk_workflowstepdata_stepparameters_workflowstepdata_id
			references workflowstepdata,
	stepparameters varchar(2048),
	stepparameters_key varchar(255)
);



create table workflowstepdata_stepsettings
(
	workflowstepdata_id bigint
		constraint fk_workflowstepdata_stepsettings_workflowstepdata_id
			references workflowstepdata,
	stepsettings varchar(2048),
	stepsettings_key varchar(255)
);



create table explicitgroup_containedroleassignees
(
	explicitgroup_id bigint
		constraint fk_explicitgroup_containedroleassignees_explicitgroup_id
			references explicitgroup,
	containedroleassignees varchar(255)
);



create table explicitgroup_authenticateduser
(
	explicitgroup_id bigint not null
		constraint fk_explicitgroup_authenticateduser_explicitgroup_id
			references explicitgroup,
	containedauthenticatedusers_id bigint not null
		constraint explicitgroup_authenticateduser_containedauthenticatedusers_id
			references authenticateduser,
	constraint explicitgroup_authenticateduser_pkey
		primary key (explicitgroup_id, containedauthenticatedusers_id)
);



create table explicitgroup_explicitgroup
(
	explicitgroup_id bigint not null
		constraint fk_explicitgroup_explicitgroup_explicitgroup_id
			references explicitgroup,
	containedexplicitgroups_id bigint not null
		constraint fk_explicitgroup_explicitgroup_containedexplicitgroups_id
			references explicitgroup,
	constraint explicitgroup_explicitgroup_pkey
		primary key (explicitgroup_id, containedexplicitgroups_id)
);



create table pendingworkflowinvocation_localdata
(
	pendingworkflowinvocation_invocationid varchar(255)
		constraint pndngwrkflwinvocationlocaldatapndngwrkflwinvocationinvocationid
			references pendingworkflowinvocation,
	localdata varchar(255),
	localdata_key varchar(255)
);



create table fileaccessrequests
(
	datafile_id bigint not null
		constraint fk_fileaccessrequests_datafile_id
			references dvobject,
	authenticated_user_id bigint not null
		constraint fk_fileaccessrequests_authenticated_user_id
			references authenticateduser,
	constraint fileaccessrequests_pkey
		primary key (datafile_id, authenticated_user_id)
);


create table sequence
(
	seq_name varchar(50) not null
		constraint sequence_pkey
			primary key,
	seq_count numeric(38)
);



INSERT INTO SEQUENCE(SEQ_NAME, SEQ_COUNT) values ('SEQ_GEN', 0);

-- using http://dublincore.org/schemas/xmls/qdc/dcterms.xsd because at http://dublincore.org/schemas/xmls/ it's the schema location for http://purl.org/dc/terms/ which is referenced in http://swordapp.github.io/SWORDv2-Profile/SWORDProfile.html
INSERT INTO foreignmetadataformatmapping(id, name, startelement, displayName, schemalocation) VALUES (1, 'http://purl.org/dc/terms/', 'entry', 'dcterms: DCMI Metadata Terms', 'http://dublincore.org/schemas/xmls/qdc/dcterms.xsd');
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (1, ':title', 'title', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (2, ':identifier', 'otherIdValue', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (3, ':creator', 'authorName', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (4, ':date', 'productionDate', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (5, ':subject', 'keywordValue', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (6, ':description', 'dsDescriptionValue', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (7, ':relation', 'relatedMaterial', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (8, ':isReferencedBy', 'publicationCitation', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (9, 'holdingsURI', 'publicationURL', TRUE, 8, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (10, 'agency', 'publicationIDType', TRUE, 8, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (11, 'IDNo', 'publicationIDNumber', TRUE, 8, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (12, ':coverage', 'otherGeographicCoverage', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (13, ':type', 'kindOfData', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (14, ':source', 'dataSources', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (15, 'affiliation', 'authorAffiliation', TRUE, 3, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (16, ':contributor', 'contributorName', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (17, 'type', 'contributorType', TRUE, 16, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (18, ':publisher', 'producerName', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (19, ':language', 'language', FALSE, NULL, 1 );

SELECT setval('foreignmetadataformatmapping_id_seq', COALESCE((SELECT MAX(id)+1 FROM foreignmetadataformatmapping), 1), false);
SELECT setval('foreignmetadatafieldmapping_id_seq', COALESCE((SELECT MAX(id)+1 FROM foreignmetadatafieldmapping), 1), false);


INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (1, 0, NULL, 'N/A', NULL);

INSERT INTO metadatablock (id, displayname, name, namespaceuri, owner_id) VALUES (1, 'Citation Metadata', 'citation', 'https://dataverse.org/schema/citation/', NULL);

INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (1, true, false, false, 'Full title by which the Dataset is known.', '', true, 0, false, 'TEXT', 'title', true, 'Title', 'http://purl.org/dc/terms/title', NULL, 'Enter title...', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (2, false, false, false, 'A secondary title used to amplify or state certain limitations on the main title.', '', false, 1, false, 'TEXT', 'subtitle', false, 'Subtitle', NULL, NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (3, false, false, false, 'A title by which the work is commonly referred, or an abbreviation of the title.', '', false, 2, false, 'TEXT', 'alternativeTitle', false, 'Alternative Title', 'http://purl.org/dc/terms/alternative', NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (4, false, false, false, 'A URL where the dataset can be viewed, such as a personal or project website.  ', '<a href="#VALUE" target="_blank">#VALUE</a>', false, 3, false, 'URL', 'alternativeURL', false, 'Alternative URL', 'https://schema.org/distribution', NULL, 'Enter full URL, starting with http://', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (5, false, false, true, 'Another unique identifier that identifies this Dataset (e.g., producer''s or another repository''s number).', ':', false, 4, false, 'NONE', 'otherId', false, 'Other ID', NULL, NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (6, false, false, false, 'Name of agency which generated this identifier.', '#VALUE', false, 5, false, 'TEXT', 'otherIdAgency', false, 'Agency', NULL, NULL, '', 1, 5);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (7, false, false, false, 'Other identifier that corresponds to this Dataset.', '#VALUE', false, 6, false, 'TEXT', 'otherIdValue', false, 'Identifier', NULL, NULL, '', 1, 5);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (8, false, false, true, 'The person(s), corporate body(ies), or agency(ies) responsible for creating the work.', '', true, 7, false, 'NONE', 'author', false, 'Author', 'http://purl.org/dc/terms/creator', NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (9, true, false, false, 'The author''s Family Name, Given Name or the name of the organization responsible for this Dataset.', '#VALUE', true, 8, true, 'TEXT', 'authorName', true, 'Name', NULL, NULL, 'FamilyName, GivenName or Organization', 1, 8);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (10, true, false, false, 'The organization with which the author is affiliated.', '(#VALUE)', true, 9, true, 'TEXT', 'authorAffiliation', false, 'Affiliation', NULL, NULL, '', 1, 8);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (11, false, true, false, 'Name of the identifier scheme (ORCID, ISNI).', '- #VALUE:', true, 10, false, 'TEXT', 'authorIdentifierScheme', false, 'Identifier Scheme', 'http://purl.org/spar/datacite/AgentIdentifierScheme', NULL, '', 1, 8);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (12, false, false, false, 'Uniquely identifies an individual author or organization, according to various schemes.', '#VALUE', true, 11, false, 'TEXT', 'authorIdentifier', false, 'Identifier', 'http://purl.org/spar/datacite/AgentIdentifier', NULL, '', 1, 8);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (13, false, false, true, 'The contact(s) for this Dataset.', '', true, 12, false, 'NONE', 'datasetContact', false, 'Contact', NULL, NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (14, false, false, false, 'The contact''s Family Name, Given Name or the name of the organization.', '#VALUE', true, 13, false, 'TEXT', 'datasetContactName', false, 'Name', NULL, NULL, 'FamilyName, GivenName or Organization', 1, 13);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (15, false, false, false, 'The organization with which the contact is affiliated.', '(#VALUE)', true, 14, false, 'TEXT', 'datasetContactAffiliation', false, 'Affiliation', NULL, NULL, '', 1, 13);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (16, false, false, false, 'The e-mail address(es) of the contact(s) for the Dataset. This will not be displayed.', '#EMAIL', true, 15, false, 'EMAIL', 'datasetContactEmail', true, 'E-mail', NULL, NULL, '', 1, 13);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (17, false, false, true, 'A summary describing the purpose, nature, and scope of the Dataset.', '', true, 16, false, 'NONE', 'dsDescription', false, 'Description', NULL, NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (18, true, false, false, 'A summary describing the purpose, nature, and scope of the Dataset.', '#VALUE', true, 17, false, 'TEXTBOX', 'dsDescriptionValue', true, 'Text', NULL, NULL, '', 1, 17);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (19, false, false, false, 'In cases where a Dataset contains more than one description (for example, one might be supplied by the data producer and another prepared by the data repository where the data are deposited), the date attribute is used to distinguish between the two descriptions. The date attribute follows the ISO convention of YYYY-MM-DD.', '(#VALUE)', true, 18, false, 'DATE', 'dsDescriptionDate', false, 'Date', NULL, NULL, 'YYYY-MM-DD', 1, 17);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (20, true, true, true, 'Domain-specific Subject Categories that are topically relevant to the Dataset.', '', true, 19, true, 'TEXT', 'subject', true, 'Subject', 'http://purl.org/dc/terms/subject', NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (21, false, false, true, 'Key terms that describe important aspects of the Dataset.', '', true, 20, false, 'NONE', 'keyword', false, 'Keyword', NULL, NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (22, true, false, false, 'Key terms that describe important aspects of the Dataset. Can be used for building keyword indexes and for classification and retrieval purposes. A controlled vocabulary can be employed. The vocab attribute is provided for specification of the controlled vocabulary in use, such as LCSH, MeSH, or others. The vocabURI attribute specifies the location for the full controlled vocabulary.', '#VALUE', true, 21, true, 'TEXT', 'keywordValue', false, 'Term', NULL, NULL, '', 1, 21);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (23, false, false, false, 'For the specification of the keyword controlled vocabulary in use, such as LCSH, MeSH, or others.', '(#VALUE)', true, 22, false, 'TEXT', 'keywordVocabulary', false, 'Vocabulary', NULL, NULL, '', 1, 21);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (24, false, false, false, 'Keyword vocabulary URL points to the web presence that describes the keyword vocabulary, if appropriate. Enter an absolute URL where the keyword vocabulary web site is found, such as http://www.my.org.', '<a href="#VALUE" target="_blank">#VALUE</a>', true, 23, false, 'URL', 'keywordVocabularyURI', false, 'Vocabulary URL', NULL, NULL, 'Enter full URL, starting with http://', 1, 21);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (25, false, false, true, 'The classification field indicates the broad important topic(s) and subjects that the data cover. Library of Congress subject terms may be used here.  ', '', false, 24, false, 'NONE', 'topicClassification', false, 'Topic Classification', NULL, NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (26, true, false, false, 'Topic or Subject term that is relevant to this Dataset.', '#VALUE', false, 25, true, 'TEXT', 'topicClassValue', false, 'Term', NULL, NULL, '', 1, 25);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (27, false, false, false, 'Provided for specification of the controlled vocabulary in use, e.g., LCSH, MeSH, etc.', '(#VALUE)', false, 26, false, 'TEXT', 'topicClassVocab', false, 'Vocabulary', NULL, NULL, '', 1, 25);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (28, false, false, false, 'Specifies the URL location for the full controlled vocabulary.', '<a href="#VALUE" target="_blank">#VALUE</a>', false, 27, false, 'URL', 'topicClassVocabURI', false, 'Vocabulary URL', NULL, NULL, 'Enter full URL, starting with http://', 1, 25);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (29, false, false, true, 'Publications that use the data from this Dataset.', '', true, 28, false, 'NONE', 'publication', false, 'Related Publication', 'http://purl.org/dc/terms/isReferencedBy', NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (30, true, false, false, 'The full bibliographic citation for this related publication.', '#VALUE', true, 29, false, 'TEXTBOX', 'publicationCitation', false, 'Citation', 'http://purl.org/dc/terms/bibliographicCitation', NULL, '', 1, 29);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (31, true, true, false, 'The type of digital identifier used for this publication (e.g., Digital Object Identifier (DOI)).', '#VALUE: ', true, 30, false, 'TEXT', 'publicationIDType', false, 'ID Type', 'http://purl.org/spar/datacite/ResourceIdentifierScheme', NULL, '', 1, 29);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (32, true, false, false, 'The identifier for the selected ID type.', '#VALUE', true, 31, false, 'TEXT', 'publicationIDNumber', false, 'ID Number', NULL, NULL, '', 1, 29);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (33, false, false, false, 'Link to the publication web page (e.g., journal article page, archive record page, or other).', '<a href="#VALUE" target="_blank">#VALUE</a>', false, 32, false, 'URL', 'publicationURL', false, 'URL', 'https://schema.org/distribution', NULL, 'Enter full URL, starting with http://', 1, 29);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (34, false, false, false, 'Additional important information about the Dataset.', '', true, 33, false, 'TEXTBOX', 'notesText', false, 'Notes', NULL, NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (35, true, true, true, 'Language of the Dataset', '', false, 34, true, 'TEXT', 'language', false, 'Language', 'http://purl.org/dc/terms/language', NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (36, false, false, true, 'Person or organization with the financial or administrative responsibility over this Dataset', '', false, 35, false, 'NONE', 'producer', false, 'Producer', NULL, NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (37, true, false, false, 'Producer name', '#VALUE', false, 36, true, 'TEXT', 'producerName', false, 'Name', NULL, NULL, 'FamilyName, GivenName or Organization', 1, 36);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (38, false, false, false, 'The organization with which the producer is affiliated.', '(#VALUE)', false, 37, false, 'TEXT', 'producerAffiliation', false, 'Affiliation', NULL, NULL, '', 1, 36);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (39, false, false, false, 'The abbreviation by which the producer is commonly known. (ex. IQSS, ICPSR)', '(#VALUE)', false, 38, false, 'TEXT', 'producerAbbreviation', false, 'Abbreviation', NULL, NULL, '', 1, 36);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (40, false, false, false, 'Producer URL points to the producer''s web presence, if appropriate. Enter an absolute URL where the producer''s web site is found, such as http://www.my.org.  ', '<a href="#VALUE" target="_blank">#VALUE</a>', false, 39, false, 'URL', 'producerURL', false, 'URL', NULL, NULL, 'Enter full URL, starting with http://', 1, 36);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (41, false, false, false, 'URL for the producer''s logo, which points to this  producer''s web-accessible logo image. Enter an absolute URL where the producer''s logo image is found, such as http://www.my.org/images/logo.gif.', '<img src="#VALUE" alt="#NAME" class="metadata-logo"/><br/>', false, 40, false, 'URL', 'producerLogoURL', false, 'Logo URL', NULL, NULL, 'Enter full URL for image, starting with http://', 1, 36);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (42, true, false, false, 'Date when the data collection or other materials were produced (not distributed, published or archived).', '', false, 41, true, 'DATE', 'productionDate', false, 'Production Date', NULL, NULL, 'YYYY-MM-DD', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (43, false, false, false, 'The location where the data collection and any other related materials were produced.', '', false, 42, false, 'TEXT', 'productionPlace', false, 'Production Place', NULL, NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (44, false, false, true, 'The organization or person responsible for either collecting, managing, or otherwise contributing in some form to the development of the resource.', ':', false, 43, false, 'NONE', 'contributor', false, 'Contributor', 'http://purl.org/dc/terms/contributor', NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (45, true, true, false, 'The type of contributor of the  resource.  ', '#VALUE ', false, 44, true, 'TEXT', 'contributorType', false, 'Type', NULL, NULL, '', 1, 44);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (46, true, false, false, 'The Family Name, Given Name or organization name of the contributor.', '#VALUE', false, 45, true, 'TEXT', 'contributorName', false, 'Name', NULL, NULL, 'FamilyName, GivenName or Organization', 1, 44);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (47, false, false, true, 'Grant Information', ':', false, 46, false, 'NONE', 'grantNumber', false, 'Grant Information', 'https://schema.org/sponsor', NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (48, true, false, false, 'Grant Number Agency', '#VALUE', false, 47, true, 'TEXT', 'grantNumberAgency', false, 'Grant Agency', NULL, NULL, '', 1, 47);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (49, true, false, false, 'The grant or contract number of the project that  sponsored the effort.', '#VALUE', false, 48, true, 'TEXT', 'grantNumberValue', false, 'Grant Number', NULL, NULL, '', 1, 47);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (50, false, false, true, 'The organization designated by the author or producer to generate copies of the particular work including any necessary editions or revisions.', '', false, 49, false, 'NONE', 'distributor', false, 'Distributor', NULL, NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (51, true, false, false, 'Distributor name', '#VALUE', false, 50, true, 'TEXT', 'distributorName', false, 'Name', NULL, NULL, 'FamilyName, GivenName or Organization', 1, 50);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (52, false, false, false, 'The organization with which the distributor contact is affiliated.', '(#VALUE)', false, 51, false, 'TEXT', 'distributorAffiliation', false, 'Affiliation', NULL, NULL, '', 1, 50);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (53, false, false, false, 'The abbreviation by which this distributor is commonly known (e.g., IQSS, ICPSR).', '(#VALUE)', false, 52, false, 'TEXT', 'distributorAbbreviation', false, 'Abbreviation', NULL, NULL, '', 1, 50);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (54, false, false, false, 'Distributor URL points to the distributor''s web presence, if appropriate. Enter an absolute URL where the distributor''s web site is found, such as http://www.my.org.', '<a href="#VALUE" target="_blank">#VALUE</a>', false, 53, false, 'URL', 'distributorURL', false, 'URL', NULL, NULL, 'Enter full URL, starting with http://', 1, 50);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (55, false, false, false, 'URL of the distributor''s logo, which points to this  distributor''s web-accessible logo image. Enter an absolute URL where the distributor''s logo image is found, such as http://www.my.org/images/logo.gif.', '<img src="#VALUE" alt="#NAME" class="metadata-logo"/><br/>', false, 54, false, 'URL', 'distributorLogoURL', false, 'Logo URL', NULL, NULL, 'Enter full URL for image, starting with http://', 1, 50);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (56, true, false, false, 'Date that the work was made available for distribution/presentation.', '', false, 55, true, 'DATE', 'distributionDate', false, 'Distribution Date', NULL, NULL, 'YYYY-MM-DD', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (57, false, false, false, 'The person (Family Name, Given Name) or the name of the organization that deposited this Dataset to the repository.', '', false, 56, false, 'TEXT', 'depositor', false, 'Depositor', NULL, NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (58, false, false, false, 'Date that the Dataset was deposited into the repository.', '', false, 57, true, 'DATE', 'dateOfDeposit', false, 'Deposit Date', 'http://purl.org/dc/terms/dateSubmitted', NULL, 'YYYY-MM-DD', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (59, false, false, true, 'Time period to which the data refer. This item reflects the time period covered by the data, not the dates of coding or making documents machine-readable or the dates the data were collected. Also known as span.', ';', false, 58, false, 'NONE', 'timePeriodCovered', false, 'Time Period Covered', 'https://schema.org/temporalCoverage', NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (60, true, false, false, 'Start date which reflects the time period covered by the data, not the dates of coding or making documents machine-readable or the dates the data were collected.', '#NAME: #VALUE ', false, 59, true, 'DATE', 'timePeriodCoveredStart', false, 'Start', NULL, NULL, 'YYYY-MM-DD', 1, 59);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (61, true, false, false, 'End date which reflects the time period covered by the data, not the dates of coding or making documents machine-readable or the dates the data were collected.', '#NAME: #VALUE ', false, 60, true, 'DATE', 'timePeriodCoveredEnd', false, 'End', NULL, NULL, 'YYYY-MM-DD', 1, 59);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (62, false, false, true, 'Contains the date(s) when the data were collected.', ';', false, 61, false, 'NONE', 'dateOfCollection', false, 'Date of Collection', NULL, NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (63, false, false, false, 'Date when the data collection started.', '#NAME: #VALUE ', false, 62, false, 'DATE', 'dateOfCollectionStart', false, 'Start', NULL, NULL, 'YYYY-MM-DD', 1, 62);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (64, false, false, false, 'Date when the data collection ended.', '#NAME: #VALUE ', false, 63, false, 'DATE', 'dateOfCollectionEnd', false, 'End', NULL, NULL, 'YYYY-MM-DD', 1, 62);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (65, true, false, true, 'Type of data included in the file: survey data, census/enumeration data, aggregate data, clinical data, event/transaction data, program source code, machine-readable text, administrative records data, experimental data, psychological test, textual data, coded textual, coded documents, time budget diaries, observation data/ratings, process-produced data, or other.', '', false, 64, true, 'TEXT', 'kindOfData', false, 'Kind of Data', 'http://rdf-vocabulary.ddialliance.org/discovery#kindOfData', NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (66, false, false, false, 'Information about the Dataset series.', ':', false, 65, false, 'NONE', 'series', false, 'Series', NULL, NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (67, true, false, false, 'Name of the dataset series to which the Dataset belongs.', '#VALUE', false, 66, true, 'TEXT', 'seriesName', false, 'Name', NULL, NULL, '', 1, 66);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (68, false, false, false, 'History of the series and summary of those features that apply to the series as a whole.', '#VALUE', false, 67, false, 'TEXTBOX', 'seriesInformation', false, 'Information', NULL, NULL, '', 1, 66);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (69, false, false, true, 'Information about the software used to generate the Dataset.', ',', false, 68, false, 'NONE', 'software', false, 'Software', 'https://www.w3.org/TR/prov-o/#wasGeneratedBy', NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (70, false, true, false, 'Name of software used to generate the Dataset.', '#VALUE', false, 69, false, 'TEXT', 'softwareName', false, 'Name', NULL, NULL, '', 1, 69);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (71, false, false, false, 'Version of the software used to generate the Dataset.', '#NAME: #VALUE', false, 70, false, 'TEXT', 'softwareVersion', false, 'Version', NULL, NULL, '', 1, 69);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (72, false, false, true, 'Any material related to this Dataset.', '', false, 71, false, 'TEXTBOX', 'relatedMaterial', false, 'Related Material', NULL, NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (73, false, false, true, 'Any Datasets that are related to this Dataset, such as previous research on this subject.', '', false, 72, false, 'TEXTBOX', 'relatedDatasets', false, 'Related Datasets', 'http://purl.org/dc/terms/relation', NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (74, false, false, true, 'Any references that would serve as background or supporting material to this Dataset.', '', false, 73, false, 'TEXT', 'otherReferences', false, 'Other References', 'http://purl.org/dc/terms/references', NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (75, false, false, true, 'List of books, articles, serials, or machine-readable data files that served as the sources of the data collection.', '', false, 74, false, 'TEXTBOX', 'dataSources', false, 'Data Sources', 'https://www.w3.org/TR/prov-o/#wasDerivedFrom', NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (76, false, false, false, 'For historical materials, information about the origin of the sources and the rules followed in establishing the sources should be specified.', '', false, 75, false, 'TEXTBOX', 'originOfSources', false, 'Origin of Sources', NULL, NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (77, false, false, false, 'Assessment of characteristics and source material.', '', false, 76, false, 'TEXTBOX', 'characteristicOfSources', false, 'Characteristic of Sources Noted', NULL, NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (78, false, false, false, 'Level of documentation of the original sources.', '', false, 77, false, 'TEXTBOX', 'accessToSources', false, 'Documentation and Access to Sources', NULL, NULL, '', 1, NULL);

INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (2, 0, 'D01', 'Agricultural Sciences', 20);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (3, 1, 'D0', 'Arts and Humanities', 20);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (4, 2, 'D1', 'Astronomy and Astrophysics', 20);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (5, 3, 'D2', 'Business and Management', 20);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (6, 4, 'D3', 'Chemistry', 20);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (7, 5, 'D7', 'Computer and Information Science', 20);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (8, 6, 'D4', 'Earth and Environmental Sciences', 20);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (9, 7, 'D5', 'Engineering', 20);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (10, 8, 'D8', 'Law', 20);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (11, 9, 'D9', 'Mathematical Sciences', 20);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (12, 10, 'D6', 'Medicine, Health and Life Sciences', 20);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (13, 11, 'D10', 'Physics', 20);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (14, 12, 'D11', 'Social Sciences', 20);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (15, 13, 'D12', 'Other', 20);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (16, 0, '', 'ark', 31);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (17, 1, '', 'arXiv', 31);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (18, 2, '', 'bibcode', 31);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (19, 3, '', 'doi', 31);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (20, 4, '', 'ean13', 31);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (21, 5, '', 'eissn', 31);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (22, 6, '', 'handle', 31);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (23, 7, '', 'isbn', 31);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (24, 8, '', 'issn', 31);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (25, 9, '', 'istc', 31);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (26, 10, '', 'lissn', 31);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (27, 11, '', 'lsid', 31);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (28, 12, '', 'pmid', 31);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (29, 13, '', 'purl', 31);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (30, 14, '', 'upc', 31);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (31, 15, '', 'url', 31);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (32, 16, '', 'urn', 31);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (1, 'arxiv', 17, 31);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (33, 0, '', 'Data Collector', 45);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (34, 1, '', 'Data Curator', 45);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (35, 2, '', 'Data Manager', 45);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (36, 3, '', 'Editor', 45);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (37, 4, '', 'Funder', 45);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (38, 5, '', 'Hosting Institution', 45);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (39, 6, '', 'Project Leader', 45);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (40, 7, '', 'Project Manager', 45);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (41, 8, '', 'Project Member', 45);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (42, 9, '', 'Related Person', 45);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (43, 10, '', 'Researcher', 45);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (44, 11, '', 'Research Group', 45);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (45, 12, '', 'Rights Holder', 45);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (46, 13, '', 'Sponsor', 45);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (47, 14, '', 'Supervisor', 45);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (48, 15, '', 'Work Package Leader', 45);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (49, 16, '', 'Other', 45);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (50, 0, '', 'ORCID', 11);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (51, 1, '', 'ISNI', 11);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (52, 2, '', 'LCNA', 11);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (53, 3, '', 'VIAF', 11);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (54, 4, '', 'GND', 11);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (55, 0, '', 'Abkhaz', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (56, 1, '', 'Afar', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (57, 2, '', 'Afrikaans', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (58, 3, '', 'Akan', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (59, 4, '', 'Albanian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (60, 5, '', 'Amharic', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (61, 6, '', 'Arabic', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (62, 7, '', 'Aragonese', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (63, 8, '', 'Armenian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (64, 9, '', 'Assamese', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (65, 10, '', 'Avaric', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (66, 11, '', 'Avestan', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (67, 12, '', 'Aymara', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (68, 13, '', 'Azerbaijani', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (69, 14, '', 'Bambara', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (70, 15, '', 'Bashkir', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (71, 16, '', 'Basque', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (72, 17, '', 'Belarusian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (73, 18, '', 'Bengali, Bangla', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (74, 19, '', 'Bihari', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (75, 20, '', 'Bislama', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (76, 21, '', 'Bosnian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (77, 22, '', 'Breton', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (78, 23, '', 'Bulgarian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (79, 24, '', 'Burmese', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (80, 25, '', 'Catalan,Valencian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (81, 26, '', 'Chamorro', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (82, 27, '', 'Chechen', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (83, 28, '', 'Chichewa, Chewa, Nyanja', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (84, 29, '', 'Chinese', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (85, 30, '', 'Chuvash', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (86, 31, '', 'Cornish', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (87, 32, '', 'Corsican', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (88, 33, '', 'Cree', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (89, 34, '', 'Croatian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (90, 35, '', 'Czech', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (91, 36, '', 'Danish', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (92, 37, '', 'Divehi, Dhivehi, Maldivian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (93, 38, '', 'Dutch', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (94, 39, '', 'Dzongkha', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (95, 40, '', 'English', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (96, 41, '', 'Esperanto', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (97, 42, '', 'Estonian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (98, 43, '', 'Ewe', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (99, 44, '', 'Faroese', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (100, 45, '', 'Fijian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (101, 46, '', 'Finnish', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (102, 47, '', 'French', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (103, 48, '', 'Fula, Fulah, Pulaar, Pular', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (104, 49, '', 'Galician', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (105, 50, '', 'Georgian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (106, 51, '', 'German', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (107, 52, '', 'Greek (modern)', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (108, 53, '', 'Guaraní', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (109, 54, '', 'Gujarati', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (110, 55, '', 'Haitian, Haitian Creole', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (111, 56, '', 'Hausa', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (112, 57, '', 'Hebrew (modern)', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (113, 58, '', 'Herero', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (114, 59, '', 'Hindi', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (115, 60, '', 'Hiri Motu', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (116, 61, '', 'Hungarian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (117, 62, '', 'Interlingua', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (118, 63, '', 'Indonesian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (119, 64, '', 'Interlingue', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (120, 65, '', 'Irish', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (121, 66, '', 'Igbo', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (122, 67, '', 'Inupiaq', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (123, 68, '', 'Ido', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (124, 69, '', 'Icelandic', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (125, 70, '', 'Italian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (126, 71, '', 'Inuktitut', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (127, 72, '', 'Japanese', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (128, 73, '', 'Javanese', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (129, 74, '', 'Kalaallisut, Greenlandic', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (130, 75, '', 'Kannada', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (131, 76, '', 'Kanuri', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (132, 77, '', 'Kashmiri', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (133, 78, '', 'Kazakh', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (134, 79, '', 'Khmer', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (135, 80, '', 'Kikuyu, Gikuyu', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (136, 81, '', 'Kinyarwanda', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (137, 82, '', 'Kyrgyz', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (138, 83, '', 'Komi', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (139, 84, '', 'Kongo', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (140, 85, '', 'Korean', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (141, 86, '', 'Kurdish', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (142, 87, '', 'Kwanyama, Kuanyama', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (143, 88, '', 'Latin', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (144, 89, '', 'Luxembourgish, Letzeburgesch', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (145, 90, '', 'Ganda', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (146, 91, '', 'Limburgish, Limburgan, Limburger', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (147, 92, '', 'Lingala', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (148, 93, '', 'Lao', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (149, 94, '', 'Lithuanian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (150, 95, '', 'Luba-Katanga', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (151, 96, '', 'Latvian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (152, 97, '', 'Manx', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (153, 98, '', 'Macedonian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (154, 99, '', 'Malagasy', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (155, 100, '', 'Malay', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (156, 101, '', 'Malayalam', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (157, 102, '', 'Maltese', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (158, 103, '', 'Māori', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (159, 104, '', 'Marathi (Marāṭhī)', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (160, 105, '', 'Marshallese', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (161, 106, '', 'Mixtepec Mixtec', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (162, 107, '', 'Mongolian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (163, 108, '', 'Nauru', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (164, 109, '', 'Navajo, Navaho', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (165, 110, '', 'Northern Ndebele', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (166, 111, '', 'Nepali', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (167, 112, '', 'Ndonga', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (168, 113, '', 'Norwegian Bokmål', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (169, 114, '', 'Norwegian Nynorsk', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (170, 115, '', 'Norwegian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (171, 116, '', 'Nuosu', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (172, 117, '', 'Southern Ndebele', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (173, 118, '', 'Occitan', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (174, 119, '', 'Ojibwe, Ojibwa', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (175, 120, '', 'Old Church Slavonic,Church Slavonic,Old Bulgarian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (176, 121, '', 'Oromo', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (177, 122, '', 'Oriya', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (178, 123, '', 'Ossetian, Ossetic', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (179, 124, '', 'Panjabi, Punjabi', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (180, 125, '', 'Pāli', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (181, 126, '', 'Persian (Farsi)', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (182, 127, '', 'Polish', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (183, 128, '', 'Pashto, Pushto', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (184, 129, '', 'Portuguese', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (185, 130, '', 'Quechua', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (186, 131, '', 'Romansh', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (187, 132, '', 'Kirundi', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (188, 133, '', 'Romanian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (189, 134, '', 'Russian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (190, 135, '', 'Sanskrit (Saṁskṛta)', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (191, 136, '', 'Sardinian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (192, 137, '', 'Sindhi', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (193, 138, '', 'Northern Sami', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (194, 139, '', 'Samoan', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (195, 140, '', 'Sango', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (196, 141, '', 'Serbian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (197, 142, '', 'Scottish Gaelic, Gaelic', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (198, 143, '', 'Shona', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (199, 144, '', 'Sinhala, Sinhalese', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (200, 145, '', 'Slovak', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (201, 146, '', 'Slovene', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (202, 147, '', 'Somali', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (203, 148, '', 'Southern Sotho', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (204, 149, '', 'Spanish, Castilian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (205, 150, '', 'Sundanese', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (206, 151, '', 'Swahili', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (207, 152, '', 'Swati', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (208, 153, '', 'Swedish', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (209, 154, '', 'Tamil', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (210, 155, '', 'Telugu', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (211, 156, '', 'Tajik', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (212, 157, '', 'Thai', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (213, 158, '', 'Tigrinya', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (214, 159, '', 'Tibetan Standard, Tibetan, Central', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (215, 160, '', 'Turkmen', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (216, 161, '', 'Tagalog', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (217, 162, '', 'Tswana', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (218, 163, '', 'Tonga (Tonga Islands)', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (219, 164, '', 'Turkish', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (220, 165, '', 'Tsonga', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (221, 166, '', 'Tatar', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (222, 167, '', 'Twi', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (223, 168, '', 'Tahitian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (224, 169, '', 'Uyghur, Uighur', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (225, 170, '', 'Ukrainian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (226, 171, '', 'Urdu', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (227, 172, '', 'Uzbek', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (228, 173, '', 'Venda', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (229, 174, '', 'Vietnamese', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (230, 175, '', 'Volapük', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (231, 176, '', 'Walloon', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (232, 177, '', 'Welsh', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (233, 178, '', 'Wolof', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (234, 179, '', 'Western Frisian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (235, 180, '', 'Xhosa', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (236, 181, '', 'Yiddish', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (237, 182, '', 'Yoruba', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (238, 183, '', 'Zhuang, Chuang', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (239, 184, '', 'Zulu', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (240, 185, '', 'Not applicable', 35);


INSERT INTO metadatablock (id, displayname, name, namespaceuri, owner_id) VALUES (2, 'Geospatial Metadata', 'geospatial', NULL, NULL);

INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (79, false, false, true, 'Information on the geographic coverage of the data. Includes the total geographic scope of the data.', '', false, 0, false, 'NONE', 'geographicCoverage', false, 'Geographic Coverage', NULL, NULL, '', 2, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (80, true, true, false, 'The country or nation that the Dataset is about.', '', false, 1, true, 'TEXT', 'country', false, 'Country / Nation', NULL, NULL, '', 2, 79);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (81, true, false, false, 'The state or province that the Dataset is about. Use GeoNames for correct spelling and avoid abbreviations.', '', false, 2, true, 'TEXT', 'state', false, 'State / Province', NULL, NULL, '', 2, 79);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (82, true, false, false, 'The name of the city that the Dataset is about. Use GeoNames for correct spelling and avoid abbreviations.', '', false, 3, true, 'TEXT', 'city', false, 'City', NULL, NULL, '', 2, 79);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (83, false, false, false, 'Other information on the geographic coverage of the data.', '', false, 4, false, 'TEXT', 'otherGeographicCoverage', false, 'Other', NULL, NULL, '', 2, 79);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (84, true, false, true, 'Lowest level of geographic aggregation covered by the Dataset, e.g., village, county, region.', '', false, 5, true, 'TEXT', 'geographicUnit', false, 'Geographic Unit', NULL, NULL, '', 2, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (85, false, false, true, 'The fundamental geometric description for any Dataset that models geography is the geographic bounding box. It describes the minimum box, defined by west and east longitudes and north and south latitudes, which includes the largest geographic extent of the  Dataset''s geographic coverage. This element is used in the first pass of a coordinate-based search. Inclusion of this element in the codebook is recommended, but is required if the bound polygon box is included. ', '', false, 6, false, 'NONE', 'geographicBoundingBox', false, 'Geographic Bounding Box', NULL, NULL, '', 2, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (86, false, false, false, 'Westernmost coordinate delimiting the geographic extent of the Dataset. A valid range of values,  expressed in decimal degrees, is -180,0 <= West  Bounding Longitude Value <= 180,0.', '', false, 7, false, 'TEXT', 'westLongitude', false, 'West Longitude', NULL, NULL, '', 2, 85);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (87, false, false, false, 'Easternmost coordinate delimiting the geographic extent of the Dataset. A valid range of values,  expressed in decimal degrees, is -180,0 <= East Bounding Longitude Value <= 180,0.', '', false, 8, false, 'TEXT', 'eastLongitude', false, 'East Longitude', NULL, NULL, '', 2, 85);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (88, false, false, false, 'Northernmost coordinate delimiting the geographic extent of the Dataset. A valid range of values,  expressed in decimal degrees, is -90,0 <= North Bounding Latitude Value <= 90,0.', '', false, 9, false, 'TEXT', 'northLongitude', false, 'North Latitude', NULL, NULL, '', 2, 85);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (89, false, false, false, 'Southernmost coordinate delimiting the geographic extent of the Dataset. A valid range of values,  expressed in decimal degrees, is -90,0 <= South Bounding Latitude Value <= 90,0.', '', false, 10, false, 'TEXT', 'southLongitude', false, 'South Latitude', NULL, NULL, '', 2, 85);

INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (241, 0, '', 'Afghanistan', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (242, 1, '', 'Albania', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (243, 2, '', 'Algeria', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (244, 3, '', 'American Samoa', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (245, 4, '', 'Andorra', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (246, 5, '', 'Angola', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (247, 6, '', 'Anguilla', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (248, 7, '', 'Antarctica', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (249, 8, '', 'Antigua and Barbuda', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (250, 9, '', 'Argentina', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (251, 10, '', 'Armenia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (252, 11, '', 'Aruba', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (253, 12, '', 'Australia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (254, 13, '', 'Austria', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (255, 14, '', 'Azerbaijan', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (256, 15, '', 'Bahamas', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (257, 16, '', 'Bahrain', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (258, 17, '', 'Bangladesh', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (259, 18, '', 'Barbados', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (260, 19, '', 'Belarus', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (261, 20, '', 'Belgium', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (262, 21, '', 'Belize', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (263, 22, '', 'Benin', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (264, 23, '', 'Bermuda', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (265, 24, '', 'Bhutan', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (266, 25, '', 'Bolivia, Plurinational State of', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (267, 26, '', 'Bonaire, Sint Eustatius and Saba', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (268, 27, '', 'Bosnia and Herzegovina', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (269, 28, '', 'Botswana', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (270, 29, '', 'Bouvet Island', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (271, 30, '', 'Brazil', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (272, 31, '', 'British Indian Ocean Territory', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (273, 32, '', 'Brunei Darussalam', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (274, 33, '', 'Bulgaria', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (275, 34, '', 'Burkina Faso', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (276, 35, '', 'Burundi', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (277, 36, '', 'Cambodia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (278, 37, '', 'Cameroon', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (279, 38, '', 'Canada', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (280, 39, '', 'Cape Verde', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (281, 40, '', 'Cayman Islands', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (282, 41, '', 'Central African Republic', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (283, 42, '', 'Chad', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (284, 43, '', 'Chile', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (285, 44, '', 'China', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (286, 45, '', 'Christmas Island', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (287, 46, '', 'Cocos (Keeling) Islands', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (288, 47, '', 'Colombia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (289, 48, '', 'Comoros', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (290, 49, '', 'Congo', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (291, 50, '', 'Congo, the Democratic Republic of the', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (292, 51, '', 'Cook Islands', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (293, 52, '', 'Costa Rica', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (294, 53, '', 'Croatia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (295, 54, '', 'Cuba', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (296, 55, '', 'Curaçao', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (297, 56, '', 'Cyprus', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (298, 57, '', 'Czech Republic', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (299, 58, '', 'Côte d''Ivoire', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (300, 59, '', 'Denmark', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (301, 60, '', 'Djibouti', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (302, 61, '', 'Dominica', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (303, 62, '', 'Dominican Republic', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (304, 63, '', 'Ecuador', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (305, 64, '', 'Egypt', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (306, 65, '', 'El Salvador', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (307, 66, '', 'Equatorial Guinea', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (308, 67, '', 'Eritrea', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (309, 68, '', 'Estonia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (310, 69, '', 'Ethiopia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (311, 70, '', 'Falkland Islands (Malvinas)', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (312, 71, '', 'Faroe Islands', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (313, 72, '', 'Fiji', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (314, 73, '', 'Finland', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (315, 74, '', 'France', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (316, 75, '', 'French Guiana', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (317, 76, '', 'French Polynesia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (318, 77, '', 'French Southern Territories', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (319, 78, '', 'Gabon', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (320, 79, '', 'Gambia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (321, 80, '', 'Georgia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (322, 81, '', 'Germany', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (323, 82, '', 'Ghana', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (324, 83, '', 'Gibraltar', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (325, 84, '', 'Greece', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (326, 85, '', 'Greenland', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (327, 86, '', 'Grenada', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (328, 87, '', 'Guadeloupe', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (329, 88, '', 'Guam', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (330, 89, '', 'Guatemala', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (331, 90, '', 'Guernsey', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (332, 91, '', 'Guinea', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (333, 92, '', 'Guinea-Bissau', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (334, 93, '', 'Guyana', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (335, 94, '', 'Haiti', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (336, 95, '', 'Heard Island and Mcdonald Islands', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (337, 96, '', 'Holy See (Vatican City State)', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (338, 97, '', 'Honduras', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (339, 98, '', 'Hong Kong', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (340, 99, '', 'Hungary', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (341, 100, '', 'Iceland', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (342, 101, '', 'India', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (343, 102, '', 'Indonesia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (344, 103, '', 'Iran, Islamic Republic of', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (345, 104, '', 'Iraq', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (346, 105, '', 'Ireland', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (347, 106, '', 'Isle of Man', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (348, 107, '', 'Israel', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (349, 108, '', 'Italy', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (350, 109, '', 'Jamaica', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (351, 110, '', 'Japan', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (352, 111, '', 'Jersey', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (353, 112, '', 'Jordan', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (354, 113, '', 'Kazakhstan', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (355, 114, '', 'Kenya', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (356, 115, '', 'Kiribati', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (357, 116, '', 'Korea, Democratic People''s Republic of', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (358, 117, '', 'Korea, Republic of', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (359, 118, '', 'Kuwait', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (360, 119, '', 'Kyrgyzstan', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (361, 120, '', 'Lao People''s Democratic Republic', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (362, 121, '', 'Latvia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (363, 122, '', 'Lebanon', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (364, 123, '', 'Lesotho', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (365, 124, '', 'Liberia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (366, 125, '', 'Libya', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (367, 126, '', 'Liechtenstein', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (368, 127, '', 'Lithuania', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (369, 128, '', 'Luxembourg', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (370, 129, '', 'Macao', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (371, 130, '', 'Macedonia, the Former Yugoslav Republic of', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (372, 131, '', 'Madagascar', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (373, 132, '', 'Malawi', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (374, 133, '', 'Malaysia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (375, 134, '', 'Maldives', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (376, 135, '', 'Mali', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (377, 136, '', 'Malta', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (378, 137, '', 'Marshall Islands', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (379, 138, '', 'Martinique', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (380, 139, '', 'Mauritania', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (381, 140, '', 'Mauritius', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (382, 141, '', 'Mayotte', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (383, 142, '', 'Mexico', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (384, 143, '', 'Micronesia, Federated States of', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (385, 144, '', 'Moldova, Republic of', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (386, 145, '', 'Monaco', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (387, 146, '', 'Mongolia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (388, 147, '', 'Montenegro', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (389, 148, '', 'Montserrat', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (390, 149, '', 'Morocco', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (391, 150, '', 'Mozambique', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (392, 151, '', 'Myanmar', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (393, 152, '', 'Namibia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (394, 153, '', 'Nauru', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (395, 154, '', 'Nepal', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (396, 155, '', 'Netherlands', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (397, 156, '', 'New Caledonia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (398, 157, '', 'New Zealand', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (399, 158, '', 'Nicaragua', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (400, 159, '', 'Niger', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (401, 160, '', 'Nigeria', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (402, 161, '', 'Niue', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (403, 162, '', 'Norfolk Island', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (404, 163, '', 'Northern Mariana Islands', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (405, 164, '', 'Norway', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (406, 165, '', 'Oman', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (407, 166, '', 'Pakistan', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (408, 167, '', 'Palau', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (409, 168, '', 'Palestine, State of', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (410, 169, '', 'Panama', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (411, 170, '', 'Papua New Guinea', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (412, 171, '', 'Paraguay', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (413, 172, '', 'Peru', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (414, 173, '', 'Philippines', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (415, 174, '', 'Pitcairn', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (416, 175, '', 'Poland', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (417, 176, '', 'Portugal', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (418, 177, '', 'Puerto Rico', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (419, 178, '', 'Qatar', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (420, 179, '', 'Romania', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (421, 180, '', 'Russian Federation', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (422, 181, '', 'Rwanda', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (423, 182, '', 'Réunion', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (424, 183, '', 'Saint Barthélemy', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (425, 184, '', 'Saint Helena, Ascension and Tristan da Cunha', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (426, 185, '', 'Saint Kitts and Nevis', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (427, 186, '', 'Saint Lucia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (428, 187, '', 'Saint Martin (French part)', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (429, 188, '', 'Saint Pierre and Miquelon', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (430, 189, '', 'Saint Vincent and the Grenadines', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (431, 190, '', 'Samoa', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (432, 191, '', 'San Marino', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (433, 192, '', 'Sao Tome and Principe', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (434, 193, '', 'Saudi Arabia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (435, 194, '', 'Senegal', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (436, 195, '', 'Serbia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (437, 196, '', 'Seychelles', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (438, 197, '', 'Sierra Leone', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (439, 198, '', 'Singapore', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (440, 199, '', 'Sint Maarten (Dutch part)', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (441, 200, '', 'Slovakia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (442, 201, '', 'Slovenia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (443, 202, '', 'Solomon Islands', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (444, 203, '', 'Somalia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (445, 204, '', 'South Africa', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (446, 205, '', 'South Georgia and the South Sandwich Islands', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (447, 206, '', 'South Sudan', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (448, 207, '', 'Spain', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (449, 208, '', 'Sri Lanka', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (450, 209, '', 'Sudan', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (451, 210, '', 'Suriname', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (452, 211, '', 'Svalbard and Jan Mayen', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (453, 212, '', 'Swaziland', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (454, 213, '', 'Sweden', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (455, 214, '', 'Switzerland', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (456, 215, '', 'Syrian Arab Republic', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (457, 216, '', 'Taiwan, Province of China', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (458, 217, '', 'Tajikistan', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (459, 218, '', 'Tanzania, United Republic of', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (460, 219, '', 'Thailand', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (461, 220, '', 'Timor-Leste', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (462, 221, '', 'Togo', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (463, 222, '', 'Tokelau', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (464, 223, '', 'Tonga', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (465, 224, '', 'Trinidad and Tobago', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (466, 225, '', 'Tunisia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (467, 226, '', 'Turkey', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (468, 227, '', 'Turkmenistan', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (469, 228, '', 'Turks and Caicos Islands', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (470, 229, '', 'Tuvalu', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (471, 230, '', 'Uganda', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (472, 231, '', 'Ukraine', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (473, 232, '', 'United Arab Emirates', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (474, 233, '', 'United Kingdom', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (475, 234, '', 'United States', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (476, 235, '', 'United States Minor Outlying Islands', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (477, 236, '', 'Uruguay', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (478, 237, '', 'Uzbekistan', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (479, 238, '', 'Vanuatu', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (480, 239, '', 'Venezuela, Bolivarian Republic of', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (481, 240, '', 'Viet Nam', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (482, 241, '', 'Virgin Islands, British', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (483, 242, '', 'Virgin Islands, U.S.', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (484, 243, '', 'Wallis and Futuna', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (485, 244, '', 'Western Sahara', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (486, 245, '', 'Yemen', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (487, 246, '', 'Zambia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (488, 247, '', 'Zimbabwe', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (489, 248, '', 'Åland Islands', 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (2, 'BOTSWANA', 269, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (3, 'Brasil', 271, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (4, 'Gambia, The', 320, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (5, 'Germany (Federal Republic of)', 322, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (6, 'GHANA', 323, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (7, 'INDIA', 342, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (8, 'Sumatra', 343, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (9, 'Iran', 344, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (10, 'Iran (Islamic Republic of)', 344, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (11, 'IRAQ', 345, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (12, 'Laos', 361, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (13, 'LESOTHO', 364, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (14, 'MOZAMBIQUE', 391, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (15, 'NAMIBIA', 393, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (16, 'SWAZILAND', 453, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (17, 'Taiwan', 457, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (18, 'Tanzania', 459, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (19, 'UAE', 473, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (20, 'USA', 475, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (21, 'U.S.A', 475, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (22, 'U.S.A.', 475, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (23, 'United States of America', 475, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (24, 'YEMEN', 486, 80);


INSERT INTO metadatablock (id, displayname, name, namespaceuri, owner_id) VALUES (3, 'Social Science and Humanities Metadata', 'socialscience', NULL, NULL);

INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (90, true, false, true, 'Basic unit of analysis or observation that this Dataset describes, such as individuals, families/households, groups, institutions/organizations, administrative units, and more. For information about the DDI''s controlled vocabulary for this element, please refer to the DDI web page at http://www.ddialliance.org/controlled-vocabularies.', '', false, 0, true, 'TEXTBOX', 'unitOfAnalysis', false, 'Unit of Analysis', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (91, true, false, true, 'Description of the population covered by the data in the file; the group of people or other elements that are the object of the study and to which the study results refer. Age, nationality, and residence commonly help to  delineate a given universe, but any number of other factors may be used, such as age limits, sex, marital status, race, ethnic group, nationality, income, veteran status, criminal convictions, and more. The universe may consist of elements other than persons, such as housing units, court cases, deaths, countries, and so on. In general, it should be possible to tell from the description of the universe whether a given individual or element is a member of the population under study. Also known as the universe of interest, population of interest, and target population.', '', false, 1, true, 'TEXTBOX', 'universe', false, 'Universe', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (92, true, false, false, 'The time method or time dimension of the data collection, such as panel, cross-sectional, trend, time- series, or other.', '', false, 2, true, 'TEXT', 'timeMethod', false, 'Time Method', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (93, false, false, false, 'Individual, agency or organization responsible for  administering the questionnaire or interview or compiling the data.', '', false, 3, false, 'TEXT', 'dataCollector', false, 'Data Collector', NULL, NULL, 'FamilyName, GivenName or Organization', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (94, false, false, false, 'Type of training provided to the data collector', '', false, 4, false, 'TEXT', 'collectorTraining', false, 'Collector Training', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (95, true, false, false, 'If the data collected includes more than one point in time, indicate the frequency with which the data was collected; that is, monthly, quarterly, or other.', '', false, 5, true, 'TEXT', 'frequencyOfDataCollection', false, 'Frequency', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (96, false, false, false, 'Type of sample and sample design used to select the survey respondents to represent the population. May include reference to the target sample size and the sampling fraction.', '', false, 6, false, 'TEXTBOX', 'samplingProcedure', false, 'Sampling Procedure', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (97, false, false, false, 'Specific information regarding the target sample size, actual  sample size, and the formula used to determine this.', '', false, 7, false, 'NONE', 'targetSampleSize', false, 'Target Sample Size', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (98, false, false, false, 'Actual sample size.', '', false, 8, false, 'INT', 'targetSampleActualSize', false, 'Actual', NULL, NULL, 'Enter an integer...', 3, 97);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (99, false, false, false, 'Formula used to determine target sample size.', '', false, 9, false, 'TEXT', 'targetSampleSizeFormula', false, 'Formula', NULL, NULL, '', 3, 97);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (100, false, false, false, 'Show correspondence as well as discrepancies between the sampled units (obtained) and available statistics for the population (age, sex-ratio, marital status, etc.) as a whole.', '', false, 10, false, 'TEXT', 'deviationsFromSampleDesign', false, 'Major Deviations for Sample Design', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (101, false, false, false, 'Method used to collect the data; instrumentation characteristics (e.g., telephone interview, mail questionnaire, or other).', '', false, 11, false, 'TEXTBOX', 'collectionMode', false, 'Collection Mode', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (102, false, false, false, 'Type of data collection instrument used. Structured indicates an instrument in which all respondents are asked the same questions/tests, possibly with precoded answers. If a small portion of such a questionnaire includes open-ended questions, provide appropriate comments. Semi-structured indicates that the research instrument contains mainly open-ended questions. Unstructured indicates that in-depth interviews were conducted.', '', false, 12, false, 'TEXT', 'researchInstrument', false, 'Type of Research Instrument', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (103, false, false, false, 'Description of noteworthy aspects of the data collection situation. Includes information on factors such as cooperativeness of respondents, duration of interviews, number of call backs, or similar.', '', false, 13, false, 'TEXTBOX', 'dataCollectionSituation', false, 'Characteristics of Data Collection Situation', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (104, false, false, false, 'Summary of actions taken to minimize data loss. Include information on actions such as follow-up visits, supervisory checks, historical matching, estimation, and so on.', '', false, 14, false, 'TEXT', 'actionsToMinimizeLoss', false, 'Actions to Minimize Losses', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (105, false, false, false, 'Control OperationsMethods to facilitate data control performed by the primary investigator or by the data archive.', '', false, 15, false, 'TEXT', 'controlOperations', false, 'Control Operations', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (106, false, false, false, 'The use of sampling procedures might make it necessary to apply weights to produce accurate statistical results. Describes the criteria for using weights in analysis of a collection. If a weighting formula or coefficient was developed, the formula is provided, its elements are defined, and it is indicated how the formula was applied to the data.', '', false, 16, false, 'TEXTBOX', 'weighting', false, 'Weighting', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (107, false, false, false, 'Methods used to clean the data collection, such as consistency checking, wildcode checking, or other.', '', false, 17, false, 'TEXT', 'cleaningOperations', false, 'Cleaning Operations', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (108, false, false, false, 'Note element used for any information annotating or clarifying the methodology and processing of the study. ', '', false, 18, false, 'TEXT', 'datasetLevelErrorNotes', false, 'Study Level Error Notes', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (109, true, false, false, 'Percentage of sample members who provided information.', '', false, 19, true, 'TEXTBOX', 'responseRate', false, 'Response Rate', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (110, false, false, false, 'Measure of how precisely one can estimate a population value from a given sample.', '', false, 20, false, 'TEXT', 'samplingErrorEstimates', false, 'Estimates of Sampling Error', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (111, false, false, false, 'Other issues pertaining to the data appraisal. Describe issues such as response variance, nonresponse rate  and testing for bias, interviewer and response bias, confidence levels, question bias, or similar.', '', false, 21, false, 'TEXT', 'otherDataAppraisal', false, 'Other Forms of Data Appraisal', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (112, false, false, false, 'General notes about this Dataset.', '', false, 22, false, 'NONE', 'socialScienceNotes', false, 'Notes', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (113, false, false, false, 'Type of note.', '', false, 23, false, 'TEXT', 'socialScienceNotesType', false, 'Type', NULL, NULL, '', 3, 112);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (114, false, false, false, 'Note subject.', '', false, 24, false, 'TEXT', 'socialScienceNotesSubject', false, 'Subject', NULL, NULL, '', 3, 112);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (115, false, false, false, 'Text for this note.', '', false, 25, false, 'TEXTBOX', 'socialScienceNotesText', false, 'Text', NULL, NULL, '', 3, 112);


INSERT INTO metadatablock (id, displayname, name, namespaceuri, owner_id) VALUES (4, 'Astronomy and Astrophysics Metadata', 'astrophysics', NULL, NULL);

INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (116, true, true, true, 'The nature or genre of the content of the files in the dataset.', '', false, 0, true, 'TEXT', 'astroType', false, 'Type', NULL, NULL, '', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (117, true, true, true, 'The observatory or facility where the data was obtained. ', '', false, 1, true, 'TEXT', 'astroFacility', false, 'Facility', NULL, NULL, '', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (118, true, true, true, 'The instrument used to collect the data.', '', false, 2, true, 'TEXT', 'astroInstrument', false, 'Instrument', NULL, NULL, '', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (119, true, false, true, 'Astronomical Objects represented in the data (Given as SIMBAD recognizable names preferred).', '', false, 3, true, 'TEXT', 'astroObject', false, 'Object', NULL, NULL, '', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (120, true, false, false, 'The spatial (angular) resolution that is typical of the observations, in decimal degrees.', '', false, 4, true, 'TEXT', 'resolution.Spatial', false, 'Spatial Resolution', NULL, NULL, '', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (121, true, false, false, 'The spectral resolution that is typical of the observations, given as the ratio λ/Δλ.', '', false, 5, true, 'TEXT', 'resolution.Spectral', false, 'Spectral Resolution', NULL, NULL, '', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (122, false, false, false, 'The temporal resolution that is typical of the observations, given in seconds.', '', false, 6, false, 'TEXT', 'resolution.Temporal', false, 'Time Resolution', NULL, NULL, '', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (123, true, true, true, 'Conventional bandpass name', '', false, 7, true, 'TEXT', 'coverage.Spectral.Bandpass', false, 'Bandpass', NULL, NULL, '', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (124, true, false, true, 'The central wavelength of the spectral bandpass, in meters.', '', false, 8, true, 'FLOAT', 'coverage.Spectral.CentralWavelength', false, 'Central Wavelength (m)', NULL, NULL, 'Enter a floating-point number.', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (125, false, false, true, 'The minimum and maximum wavelength of the spectral bandpass.', '', false, 9, false, 'NONE', 'coverage.Spectral.Wavelength', false, 'Wavelength Range', NULL, NULL, 'Enter a floating-point number.', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (126, true, false, false, 'The minimum wavelength of the spectral bandpass, in meters.', '', false, 10, true, 'FLOAT', 'coverage.Spectral.MinimumWavelength', false, 'Minimum (m)', NULL, NULL, 'Enter a floating-point number.', 4, 125);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (127, true, false, false, 'The maximum wavelength of the spectral bandpass, in meters.', '', false, 11, true, 'FLOAT', 'coverage.Spectral.MaximumWavelength', false, 'Maximum (m)', NULL, NULL, 'Enter a floating-point number.', 4, 125);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (128, false, false, true, ' Time period covered by the data.', '', false, 12, false, 'NONE', 'coverage.Temporal', false, 'Dataset Date Range', NULL, NULL, '', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (129, true, false, false, 'Dataset Start Date', '', false, 13, true, 'DATE', 'coverage.Temporal.StartTime', false, 'Start', NULL, NULL, 'YYYY-MM-DD', 4, 128);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (130, true, false, false, 'Dataset End Date', '', false, 14, true, 'DATE', 'coverage.Temporal.StopTime', false, 'End', NULL, NULL, 'YYYY-MM-DD', 4, 128);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (131, false, false, true, 'The sky coverage of the data object.', '', false, 15, false, 'TEXT', 'coverage.Spatial', false, 'Sky Coverage', NULL, NULL, '', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (132, false, false, false, 'The (typical) depth coverage, or sensitivity, of the data object in Jy.', '', false, 16, false, 'FLOAT', 'coverage.Depth', false, 'Depth Coverage', NULL, NULL, 'Enter a floating-point number.', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (133, false, false, false, 'The (typical) density of objects, catalog entries, telescope pointings, etc., on the sky, in number per square degree.', '', false, 17, false, 'FLOAT', 'coverage.ObjectDensity', false, 'Object Density', NULL, NULL, 'Enter a floating-point number.', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (134, false, false, false, 'The total number of objects, catalog entries, etc., in the data object.', '', false, 18, false, 'INT', 'coverage.ObjectCount', false, 'Object Count', NULL, NULL, 'Enter an integer.', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (135, false, false, false, 'The fraction of the sky represented in the observations, ranging from 0 to 1.', '', false, 19, false, 'FLOAT', 'coverage.SkyFraction', false, 'Fraction of Sky', NULL, NULL, 'Enter a floating-point number.', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (136, false, false, false, 'The polarization coverage', '', false, 20, false, 'TEXT', 'coverage.Polarization', false, 'Polarization', NULL, NULL, '', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (137, false, false, false, 'RedshiftType string C "Redshift"; or "Optical" or "Radio" definitions of Doppler velocity used in the data object.', '', false, 21, false, 'TEXT', 'redshiftType', false, 'RedshiftType', NULL, NULL, '', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (138, false, false, false, 'The resolution in redshift (unitless) or Doppler velocity (km/s) in the data object.', '', false, 22, false, 'FLOAT', 'resolution.Redshift', false, 'Redshift Resolution', NULL, NULL, 'Enter a floating-point number.', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (139, false, false, true, 'The value of the redshift (unitless) or Doppler velocity (km/s in the data object.', '', false, 23, false, 'FLOAT', 'coverage.RedshiftValue', false, 'Redshift Value', NULL, NULL, 'Enter a floating-point number.', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (140, false, false, false, 'The minimum value of the redshift (unitless) or Doppler velocity (km/s in the data object.', '', false, 24, false, 'FLOAT', 'coverage.Redshift.MinimumValue', false, 'Minimum', NULL, NULL, 'Enter a floating-point number.', 4, 139);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (141, false, false, false, 'The maximum value of the redshift (unitless) or Doppler velocity (km/s in the data object.', '', false, 25, false, 'FLOAT', 'coverage.Redshift.MaximumValue', false, 'Maximum', NULL, NULL, 'Enter a floating-point number.', 4, 139);

INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (490, 0, '', 'Image', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (491, 1, '', 'Mosaic', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (492, 2, '', 'EventList', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (493, 3, '', 'Spectrum', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (494, 4, '', 'Cube', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (495, 5, '', 'Table', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (496, 6, '', 'Catalog', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (497, 7, '', 'LightCurve', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (498, 8, '', 'Simulation', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (499, 9, '', 'Figure', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (500, 10, '', 'Artwork', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (501, 11, '', 'Animation', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (502, 12, '', 'PrettyPicture', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (503, 13, '', 'Documentation', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (504, 14, '', 'Other', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (505, 15, '', 'Library', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (506, 16, '', 'Press Release', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (507, 17, '', 'Facsimile', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (508, 18, '', 'Historical', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (509, 19, '', 'Observation', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (510, 20, '', 'Object', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (511, 21, '', 'Value', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (512, 22, '', 'ValuePair', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (513, 23, '', 'Survey', 116);


INSERT INTO metadatablock (id, displayname, name, namespaceuri, owner_id) VALUES (5, 'Life Sciences Metadata', 'biomedical', NULL, NULL);

INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (142, true, true, true, 'Design types that are based on the overall experimental design.', '', false, 0, true, 'TEXT', 'studyDesignType', false, 'Design Type', NULL, NULL, '', 5, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (143, true, true, true, 'Factors used in the Dataset. ', '', false, 1, true, 'TEXT', 'studyFactorType', false, 'Factor Type', NULL, NULL, '', 5, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (144, true, true, true, 'The taxonomic name of the organism used in the Dataset or from which the  starting biological material derives.', '', false, 2, true, 'TEXT', 'studyAssayOrganism', false, 'Organism', NULL, NULL, '', 5, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (145, true, false, true, 'If Other was selected in Organism, list any other organisms that were used in this Dataset. Terms from the NCBI Taxonomy are recommended.', '', false, 3, true, 'TEXT', 'studyAssayOtherOrganism', false, 'Other Organism', NULL, NULL, '', 5, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (146, true, true, true, 'A term to qualify the endpoint, or what is being measured (e.g. gene expression profiling; protein identification). ', '', false, 4, true, 'TEXT', 'studyAssayMeasurementType', false, 'Measurement Type', NULL, NULL, '', 5, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (147, true, false, true, 'If Other was selected in Measurement Type, list any other measurement types that were used. Terms from NCBO Bioportal are recommended.', '', false, 5, true, 'TEXT', 'studyAssayOtherMeasurmentType', false, 'Other Measurement Type', NULL, NULL, '', 5, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (148, true, true, true, 'A term to identify the technology used to perform the measurement (e.g. DNA microarray; mass spectrometry).', '', false, 6, true, 'TEXT', 'studyAssayTechnologyType', false, 'Technology Type', NULL, NULL, '', 5, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (149, true, true, true, 'The manufacturer and name of the technology platform used in the assay (e.g. Bruker AVANCE).', '', false, 7, true, 'TEXT', 'studyAssayPlatform', false, 'Technology Platform', NULL, NULL, '', 5, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (150, true, true, true, 'The name of the cell line from which the source or sample derives.', '', false, 8, true, 'TEXT', 'studyAssayCellType', false, 'Cell Type', NULL, NULL, '', 5, NULL);

INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (514, 0, 'EFO_0001427', 'Case Control', 142);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (515, 1, 'EFO_0001428', 'Cross Sectional', 142);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (516, 2, 'OCRE100078', 'Cohort Study', 142);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (517, 3, 'NCI_C48202', 'Nested Case Control Design', 142);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (518, 4, 'OTHER_DESIGN', 'Not Specified', 142);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (519, 5, 'OBI_0500006', 'Parallel Group Design', 142);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (520, 6, 'OBI_0001033', 'Perturbation Design', 142);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (521, 7, 'MESH_D016449', 'Randomized Controlled Trial', 142);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (522, 8, 'TECH_DESIGN', 'Technological Design', 142);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (523, 0, 'EFO_0000246', 'Age', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (524, 1, 'BIOMARKERS', 'Biomarkers', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (525, 2, 'CELL_SURFACE_M', 'Cell Surface Markers', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (526, 3, 'EFO_0000324;EFO_0000322', 'Cell Type/Cell Line', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (527, 4, 'EFO_0000399', 'Developmental Stage', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (528, 5, 'OBI_0001293', 'Disease State', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (529, 6, 'IDO_0000469', 'Drug Susceptibility', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (530, 7, 'FBcv_0010001', 'Extract Molecule', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (531, 8, 'OBI_0001404', 'Genetic Characteristics', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (532, 9, 'OBI_0000690', 'Immunoprecipitation Antibody', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (533, 10, 'OBI_0100026', 'Organism', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (534, 11, 'OTHER_FACTOR', 'Other', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (535, 12, 'PASSAGES_FACTOR', 'Passages', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (536, 13, 'OBI_0000050', 'Platform', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (537, 14, 'EFO_0000695', 'Sex', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (538, 15, 'EFO_0005135', 'Strain', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (539, 16, 'EFO_0000724', 'Time Point', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (540, 17, 'BTO_0001384', 'Tissue Type', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (541, 18, 'EFO_0000369', 'Treatment Compound', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (542, 19, 'EFO_0000727', 'Treatment Type', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (543, 0, 'ERO_0001899', 'cell counting', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (544, 1, 'CHMO_0001085', 'cell sorting', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (545, 2, 'OBI_0000520', 'clinical chemistry analysis', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (546, 3, 'OBI_0000537', 'copy number variation profiling', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (547, 4, 'OBI_0000634', 'DNA methylation profiling', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (548, 5, 'OBI_0000748', 'DNA methylation profiling (Bisulfite-Seq)', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (549, 6, '_OBI_0000634', 'DNA methylation profiling (MeDIP-Seq)', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (550, 7, '_IDO_0000469', 'drug susceptibility', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (551, 8, 'ENV_GENE_SURVEY', 'environmental gene survey', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (552, 9, 'ERO_0001183', 'genome sequencing', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (553, 10, 'OBI_0000630', 'hematology', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (554, 11, 'OBI_0600020', 'histology', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (555, 12, 'OBI_0002017', 'Histone Modification (ChIP-Seq)', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (556, 13, 'SO_0001786', 'loss of heterozygosity profiling', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (557, 14, 'OBI_0000366', 'metabolite profiling', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (558, 15, 'METAGENOME_SEQ', 'metagenome sequencing', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (559, 16, 'OBI_0000615', 'protein expression profiling', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (560, 17, 'ERO_0000346', 'protein identification', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (561, 18, 'PROTEIN_DNA_BINDING', 'protein-DNA binding site identification', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (562, 19, 'OBI_0000288', 'protein-protein interaction detection', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (563, 20, 'PROTEIN_RNA_BINDING', 'protein-RNA binding (RIP-Seq)', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (564, 21, 'OBI_0000435', 'SNP analysis', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (565, 22, 'TARGETED_SEQ', 'targeted sequencing', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (566, 23, 'OBI_0002018', 'transcription factor binding (ChIP-Seq)', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (567, 24, 'OBI_0000291', 'transcription factor binding site identification', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (568, 26, 'EFO_0001032', 'transcription profiling', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (569, 27, 'TRANSCRIPTION_PROF', 'transcription profiling (Microarray)', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (570, 28, 'OBI_0001271', 'transcription profiling (RNA-Seq)', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (571, 29, 'TRAP_TRANS_PROF', 'TRAP translational profiling', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (572, 30, 'OTHER_MEASUREMENT', 'Other', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (573, 0, 'NCBITaxon_3702', 'Arabidopsis thaliana', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (574, 1, 'NCBITaxon_9913', 'Bos taurus', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (575, 2, 'NCBITaxon_6239', 'Caenorhabditis elegans', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (576, 3, 'NCBITaxon_3055', 'Chlamydomonas reinhardtii', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (577, 4, 'NCBITaxon_7955', 'Danio rerio (zebrafish)', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (578, 5, 'NCBITaxon_44689', 'Dictyostelium discoideum', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (579, 6, 'NCBITaxon_7227', 'Drosophila melanogaster', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (580, 7, 'NCBITaxon_562', 'Escherichia coli', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (581, 8, 'NCBITaxon_11103', 'Hepatitis C virus', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (582, 9, 'NCBITaxon_9606', 'Homo sapiens', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (583, 10, 'NCBITaxon_10090', 'Mus musculus', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (584, 11, 'NCBITaxon_33894', 'Mycobacterium africanum', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (585, 12, 'NCBITaxon_78331', 'Mycobacterium canetti', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (586, 13, 'NCBITaxon_1773', 'Mycobacterium tuberculosis', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (587, 14, 'NCBITaxon_2104', 'Mycoplasma pneumoniae', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (588, 15, 'NCBITaxon_4530', 'Oryza sativa', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (589, 16, 'NCBITaxon_5833', 'Plasmodium falciparum', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (590, 17, 'NCBITaxon_4754', 'Pneumocystis carinii', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (591, 18, 'NCBITaxon_10116', 'Rattus norvegicus', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (592, 19, 'NCBITaxon_4932', 'Saccharomyces cerevisiae (brewer''s yeast)', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (593, 20, 'NCBITaxon_4896', 'Schizosaccharomyces pombe', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (594, 21, 'NCBITaxon_31033', 'Takifugu rubripes', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (595, 22, 'NCBITaxon_8355', 'Xenopus laevis', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (596, 23, 'NCBITaxon_4577', 'Zea mays', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (597, 24, 'OTHER_TAXONOMY', 'Other', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (598, 0, 'CULTURE_DRUG_TEST_SINGLE', 'culture based drug susceptibility testing, single concentration', 148);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (599, 1, 'CULTURE_DRUG_TEST_TWO', 'culture based drug susceptibility testing, two concentrations', 148);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (600, 2, 'CULTURE_DRUG_TEST_THREE', 'culture based drug susceptibility testing, three or more concentrations (minimium inhibitory concentration measurement)', 148);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (601, 3, 'OBI_0400148', 'DNA microarray', 148);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (602, 4, 'OBI_0000916', 'flow cytometry', 148);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (603, 5, 'OBI_0600053', 'gel electrophoresis', 148);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (604, 6, 'OBI_0000470', 'mass spectrometry', 148);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (605, 7, 'OBI_0000623', 'NMR spectroscopy', 148);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (606, 8, 'OBI_0000626', 'nucleotide sequencing', 148);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (607, 9, 'OBI_0400149', 'protein microarray', 148);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (608, 10, 'OBI_0000893', 'real time PCR', 148);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (609, 11, 'NO_TECHNOLOGY', 'no technology required', 148);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (610, 12, 'OTHER_TECHNOLOGY', 'Other', 148);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (611, 0, '210_MS_GC', '210-MS GC Ion Trap (Varian)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (612, 1, '220_MS_GC', '220-MS GC Ion Trap (Varian)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (613, 2, '225_MS_GC', '225-MS GC Ion Trap (Varian)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (614, 3, '240_MS_GC', '240-MS GC Ion Trap (Varian)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (615, 4, '300_MS_GCMS', '300-MS quadrupole GC/MS (Varian)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (616, 5, '320_MS_LCMS', '320-MS LC/MS (Varian)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (617, 6, '325_MS_LCMS', '325-MS LC/MS (Varian)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (618, 7, '500_MS_GCMS', '320-MS GC/MS (Varian)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (619, 8, '500_MS_LCMS', '500-MS LC/MS (Varian)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (620, 9, '800D', '800D (Jeol)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (621, 10, '910_MS_TQFT', '910-MS TQ-FT (Varian)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (622, 11, '920_MS_TQFT', '920-MS TQ-FT (Varian)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (623, 12, '3100_MASS_D', '3100 Mass Detector (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (624, 13, '6110_QUAD_LCMS', '6110 Quadrupole LC/MS (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (625, 14, '6120_QUAD_LCMS', '6120 Quadrupole LC/MS (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (626, 15, '6130_QUAD_LCMS', '6130 Quadrupole LC/MS (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (627, 16, '6140_QUAD_LCMS', '6140 Quadrupole LC/MS (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (628, 17, '6310_ION_LCMS', '6310 Ion Trap LC/MS (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (629, 18, '6320_ION_LCMS', '6320 Ion Trap LC/MS (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (630, 19, '6330_ION_LCMS', '6330 Ion Trap LC/MS (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (631, 20, '6340_ION_LCMS', '6340 Ion Trap LC/MS (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (632, 21, '6410_TRIPLE_LCMS', '6410 Triple Quadrupole LC/MS (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (633, 22, '6430_TRIPLE_LCMS', '6430 Triple Quadrupole LC/MS (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (634, 23, '6460_TRIPLE_LCMS', '6460 Triple Quadrupole LC/MS (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (635, 24, '6490_TRIPLE_LCMS', '6490 Triple Quadrupole LC/MS (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (636, 25, '6530_Q_TOF_LCMS', '6530 Q-TOF LC/MS (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (637, 26, '6540_Q_TOF_LCMS', '6540 Q-TOF LC/MS (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (638, 27, '6210_Q_TOF_LCMS', '6210 TOF LC/MS (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (639, 28, '6220_Q_TOF_LCMS', '6220 TOF LC/MS (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (640, 29, '6230_Q_TOF_LCMS', '6230 TOF LC/MS (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (641, 30, '700B_TRIPLE_GCMS', '7000B Triple Quadrupole GC/MS (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (642, 31, 'ACCUTO_DART', 'AccuTO DART (Jeol)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (643, 32, 'ACCUTOF_GC', 'AccuTOF GC (Jeol)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (644, 33, 'ACCUTOF_LC', 'AccuTOF LC (Jeol)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (645, 34, 'ACQUITY_SQD', 'ACQUITY SQD (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (646, 35, 'ACQUITY_TQD', 'ACQUITY TQD (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (647, 36, 'AGILENT', 'Agilent', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (648, 37, 'AGILENT_ 5975E_GCMSD', 'Agilent 5975E GC/MSD (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (649, 38, 'AGILENT_5975T_LTM_GCMSD', 'Agilent 5975T LTM GC/MSD (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (650, 39, '5975C_GCMSD', '5975C Series GC/MSD (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (651, 40, 'AFFYMETRIX', 'Affymetrix', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (652, 41, 'AMAZON_ETD_ESI', 'amaZon ETD ESI Ion Trap (Bruker)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (653, 42, 'AMAZON_X_ESI', 'amaZon X ESI Ion Trap (Bruker)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (654, 43, 'APEX_ULTRA_QQ_FTMS', 'apex-ultra hybrid Qq-FTMS (Bruker)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (655, 44, 'API_2000', 'API 2000 (AB Sciex)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (656, 45, 'API_3200', 'API 3200 (AB Sciex)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (657, 46, 'API_3200_QTRAP', 'API 3200 QTRAP (AB Sciex)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (658, 47, 'API_4000', 'API 4000 (AB Sciex)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (659, 48, 'API_4000_QTRAP', 'API 4000 QTRAP (AB Sciex)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (660, 49, 'API_5000', 'API 5000 (AB Sciex)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (661, 50, 'API_5500', 'API 5500 (AB Sciex)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (662, 51, 'API_5500_QTRAP', 'API 5500 QTRAP (AB Sciex)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (663, 52, 'APPLIED_BIOSYSTEMS', 'Applied Biosystems Group (ABI)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (664, 53, 'AQI_BIOSCIENCES', 'AQI Biosciences', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (665, 54, 'ATMOS_GC', 'Atmospheric Pressure GC (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (666, 55, 'AUTOFLEX_III_MALDI_TOF_MS', 'autoflex III MALDI-TOF MS (Bruker)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (667, 56, 'AUTOFLEX_SPEED', 'autoflex speed(Bruker)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (668, 57, 'AUTOSPEC_PREMIER', 'AutoSpec Premier (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (669, 58, 'AXIMA_MEGA_TOF', 'AXIMA Mega TOF (Shimadzu)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (670, 59, 'AXIMA_PERF_MALDI_TOF', 'AXIMA Performance MALDI TOF/TOF (Shimadzu)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (671, 60, 'A_10_ANALYZER', 'A-10 Analyzer (Apogee)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (672, 61, 'A_40_MINIFCM', 'A-40-MiniFCM (Apogee)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (673, 62, 'BACTIFLOW', 'Bactiflow (Chemunex SA)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (674, 63, 'BASE4INNOVATION', 'Base4innovation', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (675, 64, 'BD_BACTEC_MGIT_320', 'BD BACTEC MGIT 320', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (676, 65, 'BD_BACTEC_MGIT_960', 'BD BACTEC MGIT 960', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (677, 66, 'BD_RADIO_BACTEC_460TB', 'BD Radiometric BACTEC 460TB', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (678, 67, 'BIONANOMATRIX', 'BioNanomatrix', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (679, 68, 'CELL_LAB_QUANTA_SC', 'Cell Lab Quanta SC (Becman Coulter)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (680, 69, 'CLARUS_560_D_GCMS', 'Clarus 560 D GC/MS (PerkinElmer)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (681, 70, 'CLARUS_560_S_GCMS', 'Clarus 560 S GC/MS (PerkinElmer)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (682, 71, 'CLARUS_600_GCMS', 'Clarus 600 GC/MS (PerkinElmer)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (683, 72, 'COMPLETE_GENOMICS', 'Complete Genomics', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (684, 73, 'CYAN', 'Cyan (Dako Cytomation)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (685, 74, 'CYFLOW_ML', 'CyFlow ML (Partec)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (686, 75, 'CYFLOW_SL', 'Cyow SL (Partec)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (687, 76, 'CYFLOW_SL3', 'CyFlow SL3 (Partec)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (688, 77, 'CYTOBUOY', 'CytoBuoy (Cyto Buoy Inc)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (689, 78, 'CYTOSENCE', 'CytoSence (Cyto Buoy Inc)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (690, 79, 'CYTOSUB', 'CytoSub (Cyto Buoy Inc)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (691, 80, 'DANAHER', 'Danaher', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (692, 81, 'DFS', 'DFS (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (693, 82, 'EXACTIVE', 'Exactive(Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (694, 83, 'FACS_CANTO', 'FACS Canto (Becton Dickinson)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (695, 84, 'FACS_CANTO2', 'FACS Canto2 (Becton Dickinson)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (696, 85, 'FACS_SCAN', 'FACS Scan (Becton Dickinson)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (697, 86, 'FC_500', 'FC 500 (Becman Coulter)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (698, 87, 'GCMATE_II', 'GCmate II GC/MS (Jeol)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (699, 88, 'GCMS_QP2010_PLUS', 'GCMS-QP2010 Plus (Shimadzu)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (700, 89, 'GCMS_QP2010S_PLUS', 'GCMS-QP2010S Plus (Shimadzu)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (701, 90, 'GCT_PREMIER', 'GCT Premier (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (702, 91, 'GENEQ', 'GENEQ', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (703, 92, 'GENOME_CORP', 'Genome Corp.', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (704, 93, 'GENOVOXX', 'GenoVoxx', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (705, 94, 'GNUBIO', 'GnuBio', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (706, 95, 'GUAVA_EASYCYTE_MINI', 'Guava EasyCyte Mini (Millipore)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (707, 96, 'GUAVA_EASYCYTE_PLUS', 'Guava EasyCyte Plus (Millipore)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (708, 97, 'GUAVA_PERSONAL_CELL', 'Guava Personal Cell Analysis (Millipore)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (709, 98, 'GUAVA_PERSONAL_CELL_96', 'Guava Personal Cell Analysis-96 (Millipore)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (710, 99, 'HELICOS_BIO', 'Helicos BioSciences', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (711, 100, 'ILLUMINA', 'Illumina', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (712, 101, 'INDIRECT_LJ_MEDIUM', 'Indirect proportion method on LJ medium', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (713, 102, 'INDIRECT_AGAR_7H9', 'Indirect proportion method on Middlebrook Agar 7H9', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (714, 103, 'INDIRECT_AGAR_7H10', 'Indirect proportion method on Middlebrook Agar 7H10', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (715, 104, 'INDIRECT_AGAR_7H11', 'Indirect proportion method on Middlebrook Agar 7H11', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (716, 105, 'INFLUX_ANALYZER', 'inFlux Analyzer (Cytopeia)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (717, 106, 'INTELLIGENT_BIOSYSTEMS', 'Intelligent Bio-Systems', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (718, 107, 'ITQ_700', 'ITQ 700 (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (719, 108, 'ITQ_900', 'ITQ 900 (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (720, 109, 'ITQ_1100', 'ITQ 1100 (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (721, 110, 'JMS_53000_SPIRAL', 'JMS-53000 SpiralTOF (Jeol)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (722, 111, 'LASERGEN', 'LaserGen', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (723, 112, 'LCMS_2020', 'LCMS-2020 (Shimadzu)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (724, 113, 'LCMS_2010EV', 'LCMS-2010EV (Shimadzu)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (725, 114, 'LCMS_IT_TOF', 'LCMS-IT-TOF (Shimadzu)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (726, 115, 'LI_COR', 'Li-Cor', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (727, 116, 'LIFE_TECH', 'Life Tech', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (728, 117, 'LIGHTSPEED_GENOMICS', 'LightSpeed Genomics', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (729, 118, 'LCT_PREMIER_XE', 'LCT Premier XE (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (730, 119, 'LCQ_DECA_XP_MAX', 'LCQ Deca XP MAX (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (731, 120, 'LCQ_FLEET', 'LCQ Fleet (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (732, 121, 'LXQ_THERMO', 'LXQ (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (733, 122, 'LTQ_CLASSIC', 'LTQ Classic (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (734, 123, 'LTQ_XL', 'LTQ XL (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (735, 124, 'LTQ_VELOS', 'LTQ Velos (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (736, 125, 'LTQ_ORBITRAP_CLASSIC', 'LTQ Orbitrap Classic (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (737, 126, 'LTQ_ORBITRAP_XL', 'LTQ Orbitrap XL (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (738, 127, 'LTQ_ORBITRAP_DISCOVERY', 'LTQ Orbitrap Discovery (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (739, 128, 'LTQ_ORBITRAP_VELOS', 'LTQ Orbitrap Velos (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (740, 129, 'LUMINEX_100', 'Luminex 100 (Luminex)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (741, 130, 'LUMINEX_200', 'Luminex 200 (Luminex)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (742, 131, 'MACS_QUANT', 'MACS Quant (Miltenyi)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (743, 132, 'MALDI_SYNAPT_G2_HDMS', 'MALDI SYNAPT G2 HDMS (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (744, 133, 'MALDI_SYNAPT_G2_MS', 'MALDI SYNAPT G2 MS (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (745, 134, 'MALDI_SYNAPT_HDMS', 'MALDI SYNAPT HDMS (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (746, 135, 'MALDI_SYNAPT_MS', 'MALDI SYNAPT MS (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (747, 136, 'MALDI_MICROMX', 'MALDI micro MX (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (748, 137, 'MAXIS', 'maXis (Bruker)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (749, 138, 'MAXISG4', 'maXis G4 (Bruker)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (750, 139, 'MICROFLEX_LT_MALDI_TOF_MS', 'microflex LT MALDI-TOF MS (Bruker)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (751, 140, 'MICROFLEX_LRF_MALDI_TOF_MS', 'microflex LRF MALDI-TOF MS (Bruker)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (752, 141, 'MICROFLEX_III_TOF_MS', 'microflex III MALDI-TOF MS (Bruker)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (753, 142, 'MICROTOF_II_ESI_TOF', 'micrOTOF II ESI TOF (Bruker)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (754, 143, 'MICROTOF_Q_II_ESI_QQ_TOF', 'micrOTOF-Q II ESI-Qq-TOF (Bruker)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (755, 144, 'MICROPLATE_ALAMAR_BLUE_COLORIMETRIC', 'microplate Alamar Blue (resazurin) colorimetric method', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (756, 145, 'MSTATION', 'Mstation (Jeol)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (757, 146, 'MSQ_PLUS', 'MSQ Plus (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (758, 147, 'NABSYS', 'NABsys', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (759, 148, 'NANOPHOTONICS_BIOSCIENCES', 'Nanophotonics Biosciences', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (760, 149, 'NETWORK_BIOSYSTEMS', 'Network Biosystems', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (761, 150, 'NIMBLEGEN', 'Nimblegen', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (762, 151, 'OXFORD_NANOPORE_TECHNOLOGIES', 'Oxford Nanopore Technologies', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (763, 152, 'PACIFIC_BIOSCIENCES', 'Pacific Biosciences', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (764, 153, 'POPULATION_GENETICS_TECHNOLOGIES', 'Population Genetics Technologies', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (765, 154, 'Q1000GC_ULTRAQUAD', 'Q1000GC UltraQuad (Jeol)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (766, 155, 'QUATTRO_MICRO_API', 'Quattro micro API (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (767, 156, 'QUATTRO_MICRO_GC', 'Quattro micro GC (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (768, 157, 'QUATTRO_PREMIER_XE', 'Quattro Premier XE (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (769, 158, 'QSTAR', 'QSTAR (AB Sciex)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (770, 159, 'REVEO', 'Reveo', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (771, 160, 'ROCHE', 'Roche', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (772, 161, 'SEIRAD', 'Seirad', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (773, 162, 'SOLARIX_HYBRID_QQ_FTMS', 'solariX hybrid Qq-FTMS (Bruker)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (774, 163, 'SOMACOUNT', 'Somacount (Bently Instruments)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (775, 164, 'SOMASCOPE', 'SomaScope (Bently Instruments)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (776, 165, 'SYNAPT_G2_HDMS', 'SYNAPT G2 HDMS (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (777, 166, 'SYNAPT_G2_MS', 'SYNAPT G2 MS (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (778, 167, 'SYNAPT_HDMS', 'SYNAPT HDMS (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (779, 168, 'SYNAPT_MS', 'SYNAPT MS (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (780, 169, 'TRIPLETOF_5600', 'TripleTOF 5600 (AB Sciex)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (781, 170, 'TSQ_QUANTUM_ULTRA', 'TSQ Quantum Ultra (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (782, 171, 'TSQ_QUANTUM_ACCESS', 'TSQ Quantum Access (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (783, 172, 'TSQ_QUANTUM_ACCESS_MAX', 'TSQ Quantum Access MAX (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (784, 173, 'TSQ_QUANTUM_DISCOVERY_MAX', 'TSQ Quantum Discovery MAX (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (785, 174, 'TSQ_QUANTUM_GC', 'TSQ Quantum GC (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (786, 175, 'TSQ_QUANTUM_XLS', 'TSQ Quantum XLS (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (787, 176, 'TSQ_VANTAGE', 'TSQ Vantage (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (788, 177, 'ULTRAFLEXTREME_MALDI_TOF_MS', 'ultrafleXtreme MALDI-TOF MS (Bruker)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (789, 178, 'VISIGEN_BIO', 'VisiGen Biotechnologies', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (790, 179, 'XEVO_G2_QTOF', 'Xevo G2 QTOF (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (791, 180, 'XEVO_QTOF_MS', 'Xevo QTof MS (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (792, 181, 'XEVO_TQ_MS', 'Xevo TQ MS (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (793, 182, 'XEVO_TQ_S', 'Xevo TQ-S (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (794, 183, 'OTHER_PLATFORM', 'Other', 149);



INSERT INTO metadatablock (id, displayname, name, namespaceuri, owner_id) VALUES (6, 'Journal Metadata', 'journal', NULL, NULL);

INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (151, false, false, true, 'Indicates the volume, issue and date of a journal, which this Dataset is associated with.', '', false, 0, false, 'NONE', 'journalVolumeIssue', false, 'Journal', NULL, NULL, '', 6, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (152, true, false, false, 'The journal volume which this Dataset is associated with (e.g., Volume 4).', '', false, 1, true, 'TEXT', 'journalVolume', false, 'Volume', NULL, NULL, '', 6, 151);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (153, true, false, false, 'The journal issue number which this Dataset is associated with (e.g., Number 2, Autumn).', '', false, 2, true, 'TEXT', 'journalIssue', false, 'Issue', NULL, NULL, '', 6, 151);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (154, true, false, false, 'The publication date for this journal volume/issue, which this Dataset is associated with (e.g., 1999).', '', false, 3, true, 'DATE', 'journalPubDate', false, 'Publication Date', NULL, NULL, 'YYYY or YYYY-MM or YYYY-MM-DD', 6, 151);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (155, true, true, false, 'Indicates what kind of article this is, for example, a research article, a commentary, a book or product review, a case report, a calendar, etc (based on JATS). ', '', false, 4, true, 'TEXT', 'journalArticleType', false, 'Type of Article', NULL, NULL, '', 6, NULL);

INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (795, 0, '', 'abstract', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (796, 1, '', 'addendum', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (797, 2, '', 'announcement', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (798, 3, '', 'article-commentary', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (799, 4, '', 'book review', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (800, 5, '', 'books received', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (801, 6, '', 'brief report', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (802, 7, '', 'calendar', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (803, 8, '', 'case report', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (804, 9, '', 'collection', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (805, 10, '', 'correction', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (806, 11, '', 'data paper', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (807, 12, '', 'discussion', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (808, 13, '', 'dissertation', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (809, 14, '', 'editorial', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (810, 15, '', 'in brief', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (811, 16, '', 'introduction', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (812, 17, '', 'letter', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (813, 18, '', 'meeting report', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (814, 19, '', 'news', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (815, 20, '', 'obituary', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (816, 21, '', 'oration', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (817, 22, '', 'partial retraction', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (818, 23, '', 'product review', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (819, 24, '', 'rapid communication', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (820, 25, '', 'reply', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (821, 26, '', 'reprint', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (822, 27, '', 'research article', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (823, 28, '', 'retraction', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (824, 29, '', 'review article', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (825, 30, '', 'translation', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (826, 31, '', 'other', 155);

SELECT setval('metadatablock_id_seq', COALESCE((SELECT MAX(id)+1 FROM metadatablock), 1), false);
SELECT setval('datasetfieldtype_id_seq', COALESCE((SELECT MAX(id)+1 FROM datasetfieldtype), 1), false);
SELECT setval('controlledvocabularyvalue_id_seq', COALESCE((SELECT MAX(id)+1 FROM controlledvocabularyvalue), 1), false);
SELECT setval('controlledvocabalternate_id_seq', COALESCE((SELECT MAX(id)+1 FROM controlledvocabalternate), 1), false);


INSERT INTO dataverserole (id, alias, description, name, permissionbits, owner_id) VALUES (1, 'admin', 'A person who has all permissions for dataverses, datasets, and files.', 'Admin', 8191, NULL);
INSERT INTO dataverserole (id, alias, description, name, permissionbits, owner_id) VALUES (2, 'fileDownloader', 'A person who can download a published file.', 'File Downloader', 16, NULL);
INSERT INTO dataverserole (id, alias, description, name, permissionbits, owner_id) VALUES (3, 'fullContributor', 'A person who can add subdataverses and datasets within a dataverse.', 'Dataverse + Dataset Creator', 3, NULL);
INSERT INTO dataverserole (id, alias, description, name, permissionbits, owner_id) VALUES (4, 'dvContributor', 'A person who can add subdataverses within a dataverse.', 'Dataverse Creator', 1, NULL);
INSERT INTO dataverserole (id, alias, description, name, permissionbits, owner_id) VALUES (5, 'dsContributor', 'A person who can add datasets within a dataverse.', 'Dataset Creator', 2, NULL);
INSERT INTO dataverserole (id, alias, description, name, permissionbits, owner_id) VALUES (6, 'editor', 'For datasets, a person who can edit License + Terms, and then submit them for review.', 'Contributor', 4184, NULL);
INSERT INTO dataverserole (id, alias, description, name, permissionbits, owner_id) VALUES (7, 'curator', 'For datasets, a person who can edit License + Terms, edit Permissions, and publish datasets.', 'Curator', 5471, NULL);
INSERT INTO dataverserole (id, alias, description, name, permissionbits, owner_id) VALUES (8, 'member', 'A person who can view both unpublished dataverses and datasets.', 'Member', 28, NULL);

SELECT setval('dataverserole_id_seq', COALESCE((SELECT MAX(id)+1 FROM dataverserole), 1), false);


INSERT INTO authenticationproviderrow (id, enabled, factoryalias, factorydata, subtitle, title) VALUES ('builtin', true, 'BuiltinAuthenticationProvider', '', 'Datavers'' Internal Authentication provider', 'Dataverse Local');


INSERT INTO builtinuser (id, encryptedpassword, passwordencryptionversion, username) VALUES (1, '$2a$10$AW8rVR2emcYruz6g13oQ5uti4CkKcH7HBpzXXEpSdipIwyQaT0sWm', 1, 'dataverseAdmin'); -- default password: admin
INSERT INTO authenticateduser (id, affiliation, createdtime, email, emailconfirmed, firstname, lastapiusetime, lastlogintime, lastname, "position", superuser, useridentifier) VALUES (1, 'Dataverse.org', '2019-02-04 19:43:34.906', 'dataverse@mailinator.com', NULL, 'Dataverse', '2019-02-04 19:43:35.712', '2019-02-04 19:43:34.906', 'Admin', 'Admin', true, 'dataverseAdmin');
INSERT INTO authenticateduserlookup (id, authenticationproviderid, persistentuserid, authenticateduser_id) VALUES (1, 'builtin', 'dataverseAdmin', 1);

SELECT setval('builtinuser_id_seq', COALESCE((SELECT MAX(id)+1 FROM builtinuser), 1), false);
SELECT setval('authenticateduser_id_seq', COALESCE((SELECT MAX(id)+1 FROM authenticateduser), 1), false);
SELECT setval('authenticateduserlookup_id_seq', COALESCE((SELECT MAX(id)+1 FROM authenticateduserlookup), 1), false);


INSERT INTO dvobject (id, dtype, authority, createdate, globalidcreatetime, identifier, identifierregistered, indextime, modificationtime, permissionindextime, permissionmodificationtime, previewimageavailable, protocol, publicationdate, storageidentifier, creator_id, owner_id, releaseuser_id)
    VALUES (1, 'Dataverse', NULL, current_timestamp, NULL, NULL, false, NULL, current_timestamp, NULL, current_timestamp, false, NULL, NULL, NULL, 1, NULL, NULL);
INSERT INTO dataverse (id, affiliation, alias, allowmessagesbanners, dataversetype, description, facetroot, guestbookroot, metadatablockroot, name, permissionroot, templateroot, themeroot, defaultcontributorrole_id, defaulttemplate_id) 
    VALUES (1, NULL, 'root', false, 'UNCATEGORIZED', 'The root dataverse.', true, false, true, 'Root', true, false, true, 6, NULL);

INSERT INTO dataversecontact (id, contactemail, displayorder, dataverse_id) VALUES (1, 'root@mailinator.com', 0, 1);
INSERT INTO dataverse_metadatablock (dataverse_id, metadatablocks_id) VALUES (1, 1);
INSERT INTO dataversefacet (id, displayorder, datasetfieldtype_id, dataverse_id) VALUES (5, 0, 9, 1);
INSERT INTO dataversefacet (id, displayorder, datasetfieldtype_id, dataverse_id) VALUES (6, 3, 58, 1);
INSERT INTO dataversefacet (id, displayorder, datasetfieldtype_id, dataverse_id) VALUES (7, 1, 20, 1);
INSERT INTO dataversefacet (id, displayorder, datasetfieldtype_id, dataverse_id) VALUES (8, 2, 22, 1);

SELECT setval('dvobject_id_seq', COALESCE((SELECT MAX(id)+1 FROM dvobject), 1), false);
SELECT setval('dataversecontact_id_seq', COALESCE((SELECT MAX(id)+1 FROM dataversecontact), 1), false);
SELECT setval('dataversefacet_id_seq', COALESCE((SELECT MAX(id)+1 FROM dataversefacet), 1), false);


INSERT INTO guestbook (id, createtime, emailrequired, enabled, institutionrequired, name, namerequired, positionrequired, dataverse_id) VALUES (1, current_timestamp, false, true, false, 'Default', false, false, NULL);

SELECT setval('guestbook_id_seq', COALESCE((SELECT MAX(id)+1 FROM guestbook), 1), false);
