ROR
===

.. contents:: Contents:
    :local:

About ROR
---------

ROR (Research Organization Registry) is a registry of identifiers for research
organizations. ROR identifiers help with matching affiliation data with institutions.

For more informations visit `official ROR homepage <https://ror.org/>`_


ROR in Dataverse
----------------

Dataverse uses ROR identifiers to enrich DataCite metadata format.
In dataset metadata fields configuration there is an option for suggesting ROR identifiers based on organization name (turned on by default).


Uploading ROR data
------------------

To fully profit from ROR integration in Dataverse you will need to feed your Dataverse installation with ROR data.
You can either use full ROR data dump with all available ROR identifiers or a subset of it.

Full dump
~~~~~~~~~

First you will need to obtain ROR dump by following instructions presented in `ROR data dump documentation <https://ror.readme.io/docs/data-dump>`_.

The next step is to upload obtained ROR dump into your Dataverse installation. It can be done using
the following REST api endpoint::

        curl -H "X-Dataverse-key:$API_TOKEN" -F file=@2021-03-25-ror-data.zip "http://localhost:8080/api/ror/upload"

Note that you will need superuser permissions to upload ROR data.

Partial dump
~~~~~~~~~~~~

It is possible to use the same REST api enpoint as in full dump case to upload only selected ROR identifiers.
In such cases you will need to prepare the dump data yourself. We will show an example how it can be achieved.

Dump can be prepared using ROR api:

.. literalinclude:: ../_static/util/make_ror_dump.sh

In example above we include ROR identifiers for institutions from `Great Britain` with type `Education`.
You can modify ``ROR_FILTER`` variable in the script to obtain the data you want.

Next you need to execute the same api endpoint as in full dump::

        curl -H "X-Dataverse-key:$API_TOKEN" -F file=@ror.json "http://localhost:8080/api/ror/upload"

Note that endpoint can work with zipped dump and single file json. So you don't need to prepare zip yourself

