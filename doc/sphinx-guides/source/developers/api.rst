API development
===============

All features of Dataverse are mostly "API too", if not "API first". Making APIs great again helps building integrations
for Dataverse with third party systems, automation and many other use cases.

Dataverse API is based on `Jersey <https://eclipse-ee4j.github.io/jersey>`_ using JSON all over for both requests and
responses.

Response Creation
-----------------

To provide a consistent experience for developers, integrators and users, it's important to streamline what is sent
over the wire as responses.

.. warning:: To be extended and changed with refactoring of API code to align code paths.

`JSONResponseBuilder`
^^^^^^^^^^^^^^^^^^^^^

To make response building easier and aligned throught the API code base, a factory and decorator pattern based helper
utility has been introduced in `edu.harvard.iq.dataverse.api.util.JSONResponseBuilder`_.

*Hint: Right now, only the exception handlers listed below use it.*

Create a response builder by using ``JSONResponseBuilder.error()``.

1. Add decorations as you see fit (messages, incident identifiers, ...). For current options, see the classes extensive Javadoc.
2. You can also use the same builder to log your response for the admins by using ``.log()`` before you ``.build()``.
3. When writing a filter for the servlet container, you may use ``.apply()`` to sent a response looking and feeling like
   when used from a JAX-RS endpoint method.

.. code-block:: java
     :caption: A full fledged example (copied from the ``ThrowableHandler`` described below)

     // Create an instance (contains a human friendly message by default, but may be overridden)
     return JSONResponseBuilder.error(Response.Status.INTERNAL_SERVER_ERROR)
            // Add an identifier, so admins can find exceptions easier in the logs when users report
            .randomIncidentId()
            // Hint what went wrong internally by adding exception name & cause
            .internalError(ex)
            // Add the complete URL & HTTP method the client tried to use
            .request(request)
            // Write all this to the logs, including the stack trace from the exception
            .log(logger, Level.SEVERE, Optional.of(ex))
            // Build the JAX-RS response. Done!
            .build();

Exception Handling
------------------

We have to differ between three types of exceptions thrown during API usage:

1. Servlet exceptions, based on `javax.servlet.ServletException <https://jakarta.ee/specifications/platform/8/apidocs/javax/servlet/ServletException.html>`_
2. JAX-RS-native exceptions, based on `javax.ws.rs.WebApplicationException <https://jakarta.ee/specifications/platform/8/apidocs/javax/ws/rs/WebApplicationException.html>`_
3. Any other Java exception

When accessing the API, requests are handled by the servlet container before handing over to Jersey and from there to
the lower layers of bean validation, database connections and all other stuff you might use through the API:

.. graphviz::

    digraph G {
      rankdir = LR

      node [shape=plain] Client;
      node [shape=egg] Network;

      subgraph cluster_0 {
          node [shape=box,label="Filters, ..."] F;
          node [shape=box,label="JAX-RS/Jersey"] J;
          node [shape=box,label="Services/Persistence/..."] D;

          label = "Servlet Container";
          color = black;
      }
      Client -> Network -> F -> J -> D;
    }

Exceptions before handing over to JAX-RS
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The servlet container usually offers limited options to handle errors in elegant ways for an API, which is why you want
to avoid throwing exceptions there. However, it is possible: `see examples by Eugen Baeldung <https://www.baeldung.com/servlet-exceptions>`_

You better avoid throwing exceptions from places like
`Filter <https://jakarta.ee/specifications/platform/8/apidocs/javax/servlet/Filter.html>`_\ s or similar, as those
require specialized code handling instead of going with the below JAX-RS exception handling.

Exceptions after handing over to JAX-RS
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Cases 2) and 3) above are both happening in code easy to control in API code without fiddling with ``web.xml`` or similar.

Easy to follow code examples and explanations:

- https://www.baeldung.com/jersey-rest-api-with-spring#using-exceptionmapper (never mind this is about Spring, the technology is the same!)
- https://www.baeldung.com/jersey-bean-validation#custom-exception-handler

For Dataverse, all API exception handlers live in package `edu.harvard.iq.dataverse.api.errorhandlers`_.
*Remember, this does not handle exceptions popping via web UI, thus not served by Jersey JAX-RS!*

1. ``ThrowableHandler`` catches any Java exception thrown and uncatched in business logic (either by accident or on purpose)
   or not having a more specialized ``ExceptionMapper``. It allows for handing out nice JSON error messages to users and
   detailed logging for admins.
2. ``WebApplicationExceptionHandler`` catches all JAX-RS typed exceptions, which usually depict HTTP statuses.
   Those "native" exceptions are commonly used for redirection, client and server errors, so better watch out what you
   catch and what you do when the fish bit.
3. ``ConstraintViolationExceptionHandler`` allows for catching and formating bean validation exception from any
   layer of validation (JAX-RS itself, persistence, ...)
4. ``JsonParseExceptionHandler`` catches and formats error messages when a user or admin send an invalid JSON document
   to and endpoint and validation failed. (Usually contains hints about what is wrong...)

.. _edu.harvard.iq.dataverse.api.errorhandlers: https://github.com/IQSS/dataverse/tree/develop/src/main/java/edu/harvard/iq/dataverse/api/errorhandlers
.. _edu.harvard.iq.dataverse.api.util.JSONResponseBuilder: https://github.com/IQSS/dataverse/tree/develop/src/main/java/edu/harvard/iq/dataverse/api/util/JSONResponseBuilder.java