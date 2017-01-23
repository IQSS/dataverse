.. Dataverse API Documentation master file, created by
   sphinx-quickstart on Wed Aug 28 17:54:16 2013.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.

API Guide
=========

We encourage anyone interested in building tools to
interoperate with the Dataverse to utilize our 
APIs. In 4.0, we require to get a token, by simply registering for a Dataverse account, before using our APIs 
(We are considering making some of the APIs completely public in the future - no token required - if you use it only a few times).

Rather than using a production installation of Dataverse, API users are welcome to use http://demo.dataverse.org for testing.

Please note that the APIs in this guide are shipped with the Dataverse software itself but additional APIs are available if you install the "miniverse" application from https://github.com/IQSS/miniverse and give it read only access to your production Dataverse database. http://dataverse.org/metrics is powered by miniverse.

Contents:

.. toctree::

   sword
   search
   dataaccess
   native-api
   client-libraries
   apps
