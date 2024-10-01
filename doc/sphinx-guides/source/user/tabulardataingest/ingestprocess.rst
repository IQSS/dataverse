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
=========================

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

Uningest and Reingest
=====================

Ingest will only work for files whose content can be interpreted as a table.
Multi-sheet spreadsheets and CSV files with a different number of entries per row are two examples where ingest will fail.
This is non-fatal. The Dataverse software will not produce a .tab version of the file and will show a warning to users
who can see the draft version of the dataset containing the file that will indicate why ingest failed. When the file is published as 
part of the dataset, there will be no indication that ingest was attempted and failed.

If the warning message is a concern, the Dataverse software includes both an API call (see :ref:`file-uningest` in the :doc:`/api/native-api` guide) 
and an Edit/Uningest menu option displayed on the file page, that allow a file to be uningested by anyone who can publish the dataset.

Uningest will remove the warning. Uningest can also be done for a file that was successfully ingested.  This is only available to superusers.
This will remove the variable-level metadata and the .tab version of the file that was generated.

If a file is a tabular format but was never ingested, .e.g. due to the ingest file size limit being lower in the past, or if ingest had failed,
e.g. in a prior Dataverse version, an reingest API (see :ref:`file-reingest` in the :doc:`/api/native-api` guide) and a file page Edit/Reingest option
in the user interface allow ingest to be tried again. As with Uningest, this fucntionality is only available to superusers. 
