Getting Started with APIs
=========================

If you are a researcher or curator who wants to automate parts of your workflow, this section should help you get started. The :doc:`intro` section lists resources for other groups who may be interested in Dataverse Software APIs such as developers of integrations and support teams.

.. contents:: |toctitle|
    :local:

Servers You Can Test With
-------------------------

Rather than using a production Dataverse installation, API users are welcome to use http://demo.dataverse.org for testing. You can email support@dataverse.org if you have any trouble with this server.  

If you would rather have full control over your own test server, deployments to AWS, Docker, and more are covered in the :doc:`/developers/index` and the :doc:`/installation/index`.

Getting an API Token
--------------------

Many Dataverse Software APIs require an API token.

Once you have identified a server to test with, create an account, click on your name, and get your API token. For more details, see the :doc:`auth` section.

.. _curl-examples-and-environment-variables:

curl Examples and Environment Variables
---------------------------------------

The examples in this guide use `curl`_ for the following reasons:

- curl commands are succinct.
- curl commands can be copied and pasted into a terminal.
- This guide is programming language agnostic. It doesn't prefer any particular programming language.

You'll find curl examples that look like this:

.. code-block:: bash

  export SERVER_URL=https://demo.dataverse.org
  export QUERY=data

  curl $SERVER_URL/api/search?q=$QUERY

What's going on above is the declaration of "environment variables" that are substituted into a curl command. You should run the "export" commands but change the value for the server URL or the query (or whatever options the command supports). Then you should be able to copy and paste the curl command and it should "just work", substituting the variables like this:

.. code-block:: bash

  curl https://demo.dataverse.org/api/search?q=data

If you ever want to check an environment variable, you can "echo" it like this:

.. code-block:: bash

  echo $SERVER_URL

With curl version 7.56.0 and higher, it is recommended to use --form-string with outer quote rather than -F flag without outer quote.

For example, curl command parameter below might cause error such as ``warning: garbage at end of field specification: ,"categories":["Data"]}``.

.. code-block:: bash

  -F jsonData={\"description\":\"My description.\",\"categories\":[\"Data\"]}

Instead, use --form-string with outer quote. See https://github.com/curl/curl/issues/2022

.. code-block:: bash

  --form-string 'jsonData={"description":"My description.","categories":["Data"]}'

If you don't like curl, don't have curl, or want to use a different programming language, you are encouraged to check out the Python, Javascript, R, and Java options in the :doc:`client-libraries` section.

.. _curl: https://curl.haxx.se

Depositing Data
---------------

Creating a Dataverse Collection
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

See :ref:`create-dataverse-api`.

Creating a Dataset
~~~~~~~~~~~~~~~~~~

See :ref:`create-dataset-command`.

Uploading Files
~~~~~~~~~~~~~~~

See :ref:`add-file-api`.

Publishing a Dataverse Collection
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

See :ref:`publish-dataverse-api`.

Publishing a Dataset
~~~~~~~~~~~~~~~~~~~~

See :ref:`publish-dataset-api`.

Finding and Downloading Data
----------------------------

Finding Datasets
~~~~~~~~~~~~~~~~

A quick example search for the word "data" is https://demo.dataverse.org/api/search?q=data

See the :doc:`search` section for details.

Finding Recently Published Dataverse Collections, Datasets, and Files
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

See :ref:`search-date-range`.

It's also possible to find recently published datasets via OAI-PMH.

Downloading Files
~~~~~~~~~~~~~~~~~

The :doc:`dataaccess` section explains how to download files.

To download all the files in a dataset, see :ref:`download-by-dataset-api`.

In order to download individual files, you must know their database IDs which you can get from the ``dataverse_json`` metadata at the dataset level. See :ref:`export-dataset-metadata-api`.

Downloading Metadata
~~~~~~~~~~~~~~~~~~~~

Dataset metadata is available in a variety of formats listed at :ref:`metadata-export-formats`.

See :ref:`export-dataset-metadata-api`.

Listing the Contents of a Dataverse Collection
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

See :ref:`show-contents-of-a-dataverse-api`.

Managing Permissions
--------------------

Granting Permission
~~~~~~~~~~~~~~~~~~~

See :ref:`assign-role-on-a-dataverse-api`.

Revoking Permission
~~~~~~~~~~~~~~~~~~~

See :ref:`revoke-role-on-a-dataverse-api`.

Listing Permissions (Role Assignments)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

See :ref:`list-role-assignments-on-a-dataverse-api`.

Beyond "Getting Started" Tasks
------------------------------

In addition to the tasks listed above, your Dataverse installation supports many other operations via API.

See :ref:`list-of-dataverse-apis` and :ref:`types-of-api-users` to get oriented.

If you're looking for some inspiration for how you can use the Dataverse Software APIs, there are open source projects that integrate with the Dataverse Software listed in the :doc:`apps` section.

Getting Help
-------------

See :ref:`getting-help-with-apis`.
