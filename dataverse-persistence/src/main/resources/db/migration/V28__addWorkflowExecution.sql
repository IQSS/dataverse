create table workflow_execution
(
	id serial not null
		constraint workflow_execution_pkey
			primary key,
	workflow_id bigint not null,
	trigger_type varchar(255) not null,
	invocation_id varchar(255) not null
		constraint workflow_execution_invocation_id_key
			unique,
	dataset_id bigint not null
		constraint fk_workflow_execution_dataset_id
			references dvobject,
	major_version_number bigint not null,
	minor_version_number bigint not null,
	dataset_externally_released boolean not null,
	description varchar(255) not null,
	started_at timestamp,
	user_id varchar(255),
	ip_address varchar(255),
	finished_at timestamp
);

create table workflow_execution_step
(
	id serial not null
		constraint workflow_execution_step_pkey
			primary key,
	workflow_execution_id bigint not null,
	index integer not null,
	provider_id varchar(255) not null,
	step_type varchar(255) not null,
	description varchar(255) not null,
	started_at timestamp,
	paused_at timestamp,
	resumed_at timestamp,
	resumed_data varchar(255),
	finished_at timestamp,
	finished_successfully boolean,
	rolled_back_at timestamp
);

create table workflow_execution_step_input_params
(
	workflow_execution_step_id bigint not null
		constraint fk_workflow_execution_step_input_params_execution_step_id
			references workflow_execution_step,
	param_key varchar(255) not null,
	param_value varchar(255) not null
);

create table workflow_execution_step_paused_data
(
	workflow_execution_step_id bigint not null
		constraint fk_workflow_execution_step_paused_data_execution_step_id
			references workflow_execution_step,
	param_key varchar(255) not null,
	param_value varchar(255) not null
);

create table workflow_execution_step_output_params
(
	workflow_execution_step_id bigint not null
		constraint fk_workflow_execution_step_output_params_execution_step_id
			references workflow_execution_step,
	param_key varchar(255) not null,
	param_value varchar(255) not null
);

drop table pendingworkflowinvocation_localdata;
drop table pendingworkflowinvocation;
