select alias from dataverse where lower(alias) in (select lower(alias) from dataverse group by lower(alias) having count(*) >1);
