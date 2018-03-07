Introduction
============

We encourage anyone interested in building tools that interoperate with Dataverse to utilize our APIs. The Dataverse community has supplied :doc:`client-libraries` for Python, R, and Java and we are always interested in helping the community develop libraries for additional languages. The :doc:`apps` section links to open source Javascript, PHP, Python, and Java code that you can learn from while developing against Dataverse APIs.

.. contents:: |toctitle|
    :local:

How This Guide is Organized
---------------------------

We document the Dataverse API in four sections:

- :doc:`sword`: For depositing data using a standards-based approach rather than the :doc:`native-api`.
- :doc:`search`: For searching dataverses, datasets, and files.
- :doc:`dataaccess`: For downloading and subsetting data.
- :doc:`native-api`: For performing most tasks that are possible in the GUI.

We use the term "native" to mean that the API is not based on any standard. For this reason, the :doc:`search` and :doc:`dataaccess` could also be considered "native" and in the future we may reorganize the API Guide to split the :doc:`native-api` section into "Datasets API", "Files" API, etc.

Authentication
--------------

Most Dataverse APIs require the use of an API token. (In code we sometimes call it a "key" because it's shorter.) Instructions for getting a token are described in the :doc:`/user/account` section of the User Guide.

There are two ways to pass your API token to Dataverse APIs. The preferred method is to send the token in the ``X-Dataverse-key`` HTTP header, as in the following curl example::

    curl -H "X-Dataverse-key: 8b955f87-e49a-4462-945c-67d32e391e7e" https://demo.dataverse.org/api/datasets/:persistentId?persistentId=doi:TEST/12345

Throughout this guide you will often see Bash shell envionmental variables being used, like this::

    export API_TOKEN='8b955f87-e49a-4462-945c-67d32e391e7e'
    curl -H "X-Dataverse-key: $API_TOKEN" https://demo.dataverse.org/api/datasets/:persistentId?persistentId=doi:TEST/12345

The second way to pass your API token is via an extra query parameter called ``key`` in the URL like this::

    curl "https://demo.dataverse.org/api/datasets/:persistentId?persistentId=doi:TEST/12345&key=$API_TOKEN"

Use of the ``X-Dataverse-key`` HTTP header form is preferred because putting the query parameters in URLs often results in them finding their way into web server access logs. Your API token should be kept as secret as your password because it can be used to perform any action *as you* in the Dataverse application.

Testing
-------

Rather than using a production installation of Dataverse, API users are welcome to use http://demo.dataverse.org for testing.  

Support
-------

If you are using the APIs for an installation of Dataverse hosted by your institution, you may want to reach out to the team that supports it. At the top of the Dataverse installation's home page, there should be a form you can fill out by clicking the "Support" link.

If you are having trouble with http://demo.dataverse.org or have questions about the APIs, please feel free to reach out to the Dataverse community via https://groups.google.com/forum/#!forum/dataverse-community .

Metrics
-------

APIs described in this guide are shipped with the Dataverse software itself. Additional APIs are available if someone at your institution installs the "miniverse" application from https://github.com/IQSS/miniverse and gives it read only access to a production Dataverse database. http://dataverse.org/metrics is powered by miniverse.
