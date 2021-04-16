Please note that these scripts are not used. Take a look at `dataverse_tasks.py`, `creds_template.json`, and `basic_test_04_mydata.py` in https://github.com/IQSS/dataverse-helper-scripts/tree/master/src/stress_tests

First, gather data:

```
cp settings.json.template settings.json
$EDITOR settings.json
./locust
```

Then, analyze the data:

```
./analyze
```
