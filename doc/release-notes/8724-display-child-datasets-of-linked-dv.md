Datasets that are part of linked dataverse collections will now be displayed in 
their linking dataverse collections. In order to fix the display of collections 
that have already been linked you must re-index the linked collections. This 
query will provide a list of commands to re-index the effected collections:

select 'curl http://localhost:8080/api/admin/index/dataverses/' 
|| tmp.dvid  from (select distinct  dataverse_id as dvid  
from dataverselinkingdataverse)  as tmp

The result of the query will be a list of re-index commands such as:

curl http://localhost:8080/api/admin/index/dataverses/633

where '633' is the id of the linked collection.
