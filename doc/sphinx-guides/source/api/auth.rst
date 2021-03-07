API Tokens and Authentication 
=============================

An API token is similar to a password and allows you to authenticate to Dataverse Software APIs to perform actions as you. Many Dataverse Software APIs require the use of an API token.

.. contents:: |toctitle|
    :local:

How to Get an API Token
-----------------------

Your API token is unique to the server you are using. You cannot take your API token from one server and use it on another server.

Instructions for getting a token are described in the :doc:`/user/account` section of the User Guide.

How Your API Token Is Like a Password
-------------------------------------

Anyone who has your API Token can add and delete data as you so you should treat it with the same care as a password.

Passing Your API Token as an HTTP Header (Preferred) or a Query Parameter
-------------------------------------------------------------------------

Note: The SWORD API uses a different way of passing the API token. Please see :ref:`sword-auth` for details.

See :ref:`curl-examples-and-environment-variables` if you are unfamiliar with the use of ``export`` below.

There are two ways to pass your API token to Dataverse Software APIs. The preferred method is to send the token in the ``X-Dataverse-key`` HTTP header, as in the following curl example.

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ALIAS=root

  curl -H X-Dataverse-key:$API_TOKEN $SERVER_URL/api/dataverses/$ALIAS/contents

Here's how it looks without the environment variables:

.. code-block:: bash

  curl -H X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx https://demo.dataverse.org/api/dataverses/root/contents

The second way to pass your API token is via a query parameter called ``key`` in the URL like below.

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export ALIAS=root

  curl $SERVER_URL/api/dataverses/$ALIAS/contents?key=$API_TOKEN

Here's how it looks without the environment variables:

.. code-block:: bash

  curl https://demo.dataverse.org/api/dataverses/root/contents?key=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx

Use of the ``X-Dataverse-key`` HTTP header form is preferred to passing ``key`` in the URL because query parameters like ``key`` appear in URLs and might accidentally get shared, exposing your API token. (Again it's like a password.) Additionally, URLs are often logged on servers while it's less common to log HTTP headers.

Resetting Your API Token
------------------------

You can reset your API Token from your account page in your Dataverse installation as described in the :doc:`/user/account` section of the User Guide.
