# Performance Testing

```{contents} Contents:
:local: 
:depth: 3
```

## Introduction

The final testing activity before producing a release is performance testing. This could be done throughout the release cycle but since it is time-consuming, it is done once near the end. Using a load-generating tool named {ref}`Locust <locust>`, our scripts load the statistically most-loaded pages (according to Google Analytics): 50% homepage and 50% some type of dataset page. 

Since dataset page weight also varies by the number of files, a selection of about 10 datasets with varying file counts is used. The pages are called randomly as a guest user with increasing levels of user load, from 1 user to 250 users. Typical daily loads in production are around the 50-user level. Though the simulated user level does have a modest amount of random think time before repeated calls, from 5-20 seconds, it is not a real-world load so direct comparisons to production are not reliable. Instead, we compare performance to prior versions of the product, and based on how that performed in production we have some idea whether this might be similar in performance or whether there is some undetected issue that appears under load, such as inefficient or too many DB queries per page.

## Testing Environment

To run performance tests, we have a performance test cluster on AWS that employs web, database, and Solr. The database contains a copy of production that is updated weekly on Sundays. To ensure the homepage content is consistent between test runs across releases, two scripts set the datasets that will appear on the homepage. There is a script on the web server in the default CentOS user dir and one on the database server in the default CentOS user dir. Run these scripts before conducting the tests. 

Once the performance has been tested and recorded in a [Google spreadsheet](https://docs.google.com/spreadsheets/d/1lwPlifvgu3-X_6xLwq6Zr6sCOervr1mV_InHIWjh5KA/edit?usp=sharing) for this proposed version, the release will be prepared and posted.

## Access

Access to performance cluster instances requires ssh keys. The cluster itself is normally not running to reduce costs. To turn on the cluster, log on to the demo server and run the perfenv scripts from the centos default user dir.

## Special Notes ⚠️ 

Please note the performance database is also used occasionally by members of the Curation team to generate prod reports so a courtesy check with them would be good before taking over the env.


Executing the Performance Script
--------------------------------
To execute the performance test script, you need to install a local copy of the database-helper-scripts project at <https://github.com/IQSS/dataverse-helper-scripts>. We have since produced a stripped-down script that calls just the collection and dataset pages and works with Python 3.
