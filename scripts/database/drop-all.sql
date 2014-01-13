-- Drop all tables and constraints
-- Useful way of deleting the database before recreating it.

drop table datafile CASCADE;
drop table dataset CASCADE;
drop table datatable CASCADE;
drop table datavariable CASCADE;
drop table dataverse_dataverserole CASCADE;
drop table dataverserole_userdataverseassignedrole CASCADE;
drop table dataverseuser_userdataverseassignedrole CASCADE;
drop table dataverse CASCADE;
drop table dataverserole CASCADE;
drop table dataverseuser CASCADE;
drop table dataverseuserrole CASCADE;
drop table summarystatistic CASCADE;
drop table summarystatistictype CASCADE;
drop table userdataverseassignedrole CASCADE;
drop table userdataverserole CASCADE;
drop table variablecategory CASCADE;
drop table variableformattype CASCADE;
drop table variableintervaltype CASCADE;
drop table variablerange CASCADE;
drop table variablerangeitem CASCADE;
drop table variablerangetype CASCADE;
