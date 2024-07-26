### Initial Support for Dataset Types (Dataset, Software, Workflow)

Datasets now have types. By default the dataset type will be "dataset" but if you turn on support for additional types, datasets can have a type of "software" or "workflow" as well. For more details see <https://dataverse-guide--10694.org.readthedocs.build/en/10694/user/dataset-types.html> and #10517. Please note that this feature is highly experimental.

A handy query:

```
% DOCKER_CLI_HINTS=false docker exec -it postgres-1 bash -c "PGPASSWORD=secret psql -h localhost -U dataverse dataverse -c 'select dst.name, count(*) from dataset ds, datasettype dst where ds.datasettype_id = dst.id group by dst.name;'"
   name   | count
----------+-------
 dataset  |   136
 software |    14
(2 rows)
```

Most API tests are passing but we do see a few failures:

```
[ERROR] Failures:
[ERROR]   HarvestingClientsIT.testHarvestingClientRun_AllowHarvestingMissingCVV_False:187->harvestingClientRun:301 expected: <7> but was: <0>
[ERROR]   HarvestingClientsIT.testHarvestingClientRun_AllowHarvestingMissingCVV_True:191->harvestingClientRun:301 expected: <8> but was: <0>
[ERROR]   MakeDataCountApiIT.testMakeDataCountGetMetric:68 1 expectation failed.
Expected status code <200> but was <400>.
```

select dst.name, count(*) from dataset ds, datasettype dst where ds.datasettype_id = dst.id group by dst.name;
