Performance Testing
===================

.. contents:: |toctitle|
    :local:

Introduction
------------
To run performance tests, we have a performance test cluster on AWS that employs web, database, and Solr. The database contains a copy of production that is updated weekly on Sundays. To ensure the homepage content is consistent between test runs across releases, two scripts set the datasets that will appear on the homepage. There is a script on the web server in the default CentOS user dir and one on the database server in the default CentOS user dir. Run these scripts before conducting the tests. 

Access
------
Access to performance cluster instances requires ssh keys, see Leonid. The cluster itself is normally not running to reduce costs. To turn on the cluster, log on to the demo server and run the perfenv scripts from the centos default user dir. Access to the demo requires an ssh key, see Leonid. 

Special Notes ⚠️
-----------------
Please note the performance database is also used occasionally by Julian and the Curation team to generate prod reports so a courtesy check with Julian would be good before taking over the env.

Executing the Performance Script
--------------------------------
To execute the performance test script, you need to install a local copy of the database-helper-scripts project (https://github.com/IQSS/dataverse-helper-scripts), written by Raman. I have since produced a stripped-down script that calls just the DB and ds and works with python3. 

The automated integration test runs happen on each commit to a PR on an AWS instance and should be reviewed to be passing before merging into development. Their status can be seen on the PR page near the bottom, above the merge button. See Don Sizemore or Phil for questions.
