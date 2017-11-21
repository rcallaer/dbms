create database university;


use university;

create table x(id integer PRIMARY KEY , name text);


create table student(a

text );

describe student;

delete

student

;

create database db3;

use db3;

create table st(id integer PRIMARY KEY, name text, gpa float);

create table prof

(pname text 

,

ssn integer);

insert into st values (

1, "Abe", 3.5);

insert into st values (2, "B", 3.0); insert into st values (3, "C", 4.0);
insert into st values (4, "D", 2.0); insert into st values (2, "B", 3.0);

insert into prof values ("ABE", 11);

insert into prof values ("C", 33);

select name, gpa, ssn from st,prof where pname=name;

select name,ssn from prof,st where gpa=2.0;


select ssn, gpa from

prof, st;


create table third (x integer);

insert into third values(1);

insert into 
third values(2);


select name, ssn, x from third, st, prof where name=pname and id=x;


delete st where name="Abe";

delete prof where ssn>=33;

select ssn from prof;

select name, gpa, ssn from st,prof where pname=name;

select name,ssn from prof,st where gpa=2.0;

describe prof;

describe all;


update st set gpa=0.00, name="X" where gpa != 4.0;


rename st (x,y,z);


describe st;


let st2 KEY ssn select name, gpa, ssn from st,prof where pname=name;

select name, gpa, ssn from st2;

exit;