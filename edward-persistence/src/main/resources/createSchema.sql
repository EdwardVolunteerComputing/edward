drop all objects;


create table users(
	id bigint auto_increment primary key,
	name varchar(255) not null,
	password char(60) not null
);

insert into users values(1, 'admin', '$2a$10$IWKTJ7LhE.Me/DbZCod01uN/TjILc2sjfu4GjskavZ1pT5PiZOYwS');


create table projects(
	id bigint auto_increment primary key, 
	name varchar(255) not null,
	owner_id bigint not null, 
	foreign key(owner_id) references users(id)
);


create table data(
	id bigint auto_increment primary key,
	data clob not null
);

create table jobs(
	id bigint auto_increment primary key,
	name varchar(255) not null,
	project_id bigint not null,
	code clob,
	foreign key(project_id) references projects(id)
);


create table tasks(
	id bigint auto_increment primary key,
	priority int not null,
	concurrent_executions_count int not null,
	job_id bigint not null,
	input_data_id bigint not null,
	aborted boolean not null,
	creation_time bigint not null,
	timeout bigint not null,
	foreign key(job_id) references jobs(id),
	foreign key(input_data_id) references data(id)
);


create table volunteers(
	id bigint auto_increment primary key
);

-- default volunteer 
insert into volunteers values(1);


create table executions(
	id bigint auto_increment primary key,
	task_id bigint not null,
	volunteer_id bigint not null,
	output_data_id bigint,
	status varchar(10) not null,
	error clob,
	creation_time bigint not null,
	foreign key(task_id) references tasks(id),
	foreign key(volunteer_id) references volunteers(id),
	foreign key(output_data_id) references data(id)
);



