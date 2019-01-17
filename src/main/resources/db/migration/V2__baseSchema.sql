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

