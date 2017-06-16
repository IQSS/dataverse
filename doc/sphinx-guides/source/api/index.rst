.. Dataverse API Documentation master file, created by
   sphinx-quickstart on Wed Aug 28 17:54:16 2013.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.

API Guide
=========

We encourage anyone interested in building tools to interoperate with the Dataverse to utilize our APIs. Some API calls do not require authentication. Calls that require authentication require the user's API key. That key can be passed either via an extra query parameter, ``key``, as in ``ENPOINT?key=API_KEY``, or via the HTTP header ``X-Dataverse-key``. Note that while the header option normally requires more work on client side, it is considered safer, as the API key is not logged in the server access logs.

Rather than using a production installation of Dataverse, API users are welcome to use http://demo.dataverse.org for testing.

Please note that the APIs in this guide are shipped with the Dataverse software itself but additional APIs are available if you install the "miniverse" application from https://github.com/IQSS/miniverse and give it read only access to your production Dataverse database. http://dataverse.org/metrics is powered by miniverse.

**Contents:**

.. toctree::

   sword
   search
   dataaccess
   native-api
   client-libraries
   apps
