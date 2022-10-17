Tabular Data, Representation, Storage and Ingest
+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

This section explains the basics of how tabular data is handled in
the application and what happens during the ingest process, as the
files uploaded by the user are processed and converted into the
archival format in the Dataverse application.

.. contents:: |toctitle|
	:local:

What Happens During this "Ingest"? 
===================================

The goal of our ingest process is to extract the data content from the
user's files and archive it in an application-neutral, easily-readable
format. What does this mean? - Commercial applications such as SPSS
and Stata use their own, proprietary formats to encode their
files. Some companies publish the specifications of their formats
(Thank you Stata - much appreciated!), some don't (SPSS - yes, we are
still frowning at you here at the Dataverse Project). Either way,
reading these specially-formatted files requires some extra knowledge
or special software. For these reasons they are not considered ideal
for the purposes of archival preservation. The Dataverse installation stores the raw data content extracted from such files in plain text, TAB-delimited
files. The metadata information that describes this content is stored
separately, in a relational database, so that it can be accessed
efficiently by the application. For the purposes of archival
preservation it can be exported, in plain text XML files, using a
standardized, open `DDI Codebook
<https://www.ddialliance.org/Specification/DDI-Codebook/2.5/>`_
format. (more info below)


Tabular Data and Metadata
==========================

Data vs. Metadata
-----------------

A simple example is a numeric data column in a user's Stata file that
contains 0s and 1s. These numeric values will be extracted and stored
in a TAB-delimited file. By themselves, if you don't know what these
values represent, these 1s and 0s are not meaningful data. So the
Stata file has some additional information that describes this data
vector: it represents the observation values of a *variable* with the
*name* "party"; with a descriptive *label* "Party Affiliation"; and
the 2 numeric values have *categorical labels* of "Democrat" for 0 and
"Republican" for 1. This extra information that adds value to the data
is *metadata*.

Tabular Metadata in the Dataverse Software
------------------------------------------

The structure of the metadata defining tabular data variables used in
the Dataverse Software was originally based on the `DDI Codebook
<https://www.ddialliance.org/Specification/DDI-Codebook/2.5/>`_ format.

You can see an example of DDI output under the :ref:`data-variable-metadata-access` section of the :doc:`/api/dataaccess` section of the API Guide.
