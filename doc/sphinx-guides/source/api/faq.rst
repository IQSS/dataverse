Frequently Asked Questions
==========================

APIs are less intuitive than graphical user interfaces (GUIs) so questions are expected!

.. contents:: |toctitle|
    :local:

What is an API?
---------------

See "What is an API?" in the :doc:`intro` section.

What Are Common Use Cases for Dataverse Software APIs?
----------------------------------------------------------

See the :doc:`getting-started` section for common use cases for researchers and curators. Other types of API users should find starting points at :ref:`types-of-api-users`.

Where Can I Find Examples of Using Dataverse Software APIs?
-----------------------------------------------------------

See the :doc:`getting-started` section links to examples using curl.

For examples in Javascript, Python, R, and Java, and PHP, see the :doc:`apps` and :doc:`client-libraries` sections.

When Should I Use the Native API vs. the SWORD API?
---------------------------------------------------

The :doc:`sword` is based on a standard, works fine, and is fully supported, but much more development effort has been going into the :doc:`native-api`, which is not based on a standard. It is specific to the Dataverse Software.

SWORD uses XML. The Native API uses JSON.

SWORD only supports a dozen or so operations. The Native API supports many more.

To Operate on a Dataset Should I Use Its DOI (or Handle) or Its Database ID?
----------------------------------------------------------------------------

It is fine to target a dataset using either its Persistent ID (PID such as DOI or Handle) or its database id.

Here's an example from :ref:`publish-dataset-api` of targeting a dataset using its DOI:

.. code-block:: bash

  curl -H X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx -X POST "https://demo.dataverse.org/api/datasets/:persistentId/actions/:publish?persistentId=doi:10.5072/FK2/J8SJZB&type=major"

You can target the same dataset with its database ID ("42" in the example below), like this:

.. code-block:: bash

  curl -H X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx -X POST "https://demo.dataverse.org/api/datasets/42/actions/:publish?type=major"

Note that when multiple query parameters are used (such as ``persistentId`` and ``type`` above) there is a question mark (``?``) before the first query parameter and ampersands (``&``) before each of the subsequent query parameters. Also, ``&`` has special meaning in Unix shells such as Bash so you must put quotes around the entire URL.

Where is the Comprehensive List of All API Functionality?
---------------------------------------------------------

There are so many Dataverse Software APIs that a single page in this guide would probably be overwhelming. See :ref:`list-of-dataverse-apis` for links to various pages.

It is possible to get a complete list of API functionality in Swagger/OpenAPI format if you deploy Dataverse Software 5.x. For details, see https://github.com/IQSS/dataverse/issues/5794

Is There a Changelog of API Functionality That Has Been Added Over Time?
------------------------------------------------------------------------

No, but there probably should be. If you have suggestions for how it should look, please create an issue at https://github.com/IQSS/dataverse/issues

.. _no-api:

What Functionality is GUI Only and Not Available Via API
--------------------------------------------------------

The following tasks cannot currently be automated via API because no API exists for them. The web interface should be used instead for these GUI-only features:

- Setting a logo image, URL, and tagline when creating a Dataverse collection.
- Editing properties of an existing Dataverse collection.
- Set "Enable Access Request" for Terms of Use: https://groups.google.com/d/msg/dataverse-community/oKdesT9rFGc/qM6wrsnnBAAJ
- Downloading a guestbook.
- Set guestbook_id for a dataset: https://groups.google.com/d/msg/dataverse-community/oKdesT9rFGc/qM6wrsnnBAAJ
- Filling out a guestbook. See also https://groups.google.com/d/msg/dataverse-dev/G9FNGP_bT0w/dgE2Fk4iBQAJ
- Seeing why a file failed ingest.
- Dataset templates.
- Deaccessioning datasets.

If you would like APIs for any of the features above, please open a GitHub issue at https://github.com/IQSS/dataverse/issues

You are also welcome to open an issue to add to the list above. Or you are welcome to make a pull request. Please see the :doc:`/developers/documentation` section of the Developer Guide for instructions.

Why Are the Return Values (HTTP Status Codes) Not Documented?
-------------------------------------------------------------

They should be. Please consider making a pull request to help. The :doc:`/developers/documentation` section of the Developer Guide should help you get started. :ref:`create-dataverse-api` has an example you can follow or you can come up with a better way.

What If My Question Is Not Answered Here?
-----------------------------------------

Please ask! For information on where to ask, please see :ref:`getting-help-with-apis`.
