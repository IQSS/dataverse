===================
Windows Development
===================

Historically, development on Windows is `not well supported <https://groups.google.com/d/msg/dataverse-community/Hs9j5rIxqPI/-q54751aAgAJ>`_ but as of 2023 a container-based approach is recommended.

.. contents:: |toctitle|
	:local:

Running Dataverse in Docker on Windows
--------------------------------------

See the `post <https://groups.google.com/g/dataverse-dev/c/utqkZ7gYsf4/m/4IDtsvKSAwAJ>`_ by Akio Sone for additional details, but please observe the following:

- In git, the line-ending setting should be set to always LF (line feed, ``core.autocrlf=input``)
- You must have jq installed: https://jqlang.github.io/jq/download/

One the above is all set you can move on to :doc:`/container/dev-usage` in the Container Guide.
