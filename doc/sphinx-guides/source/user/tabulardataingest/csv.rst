CSV
++++++

.. contents:: |toctitle|
	:local:

Ingest of Comma-Separated Values files as tabular data. 
-------------------------------------------------------

Dataverse will make an attempt to turn CSV files uploaded by the user into tabular data, using the `Apache CSV parser <https://commons.apache.org/proper/commons-csv/>`_. 

Main formatting requirements: 
-----------------------------

The first row in the document will be treated as the CSV's header, containing variable names for each column.

Each following row must contain the same number of cells as that header.

As of the Dataverse 4.7.1 release, we allow ingest of CSV files with commas within cells and with line breaks within cells. 

Limitations:
------------

Except for the variable names supplied in the top line, very little information describing the data can be obtained from a CSV file. We strongly recommend using one of the supported rich file formats (Stata, SPSS and R) to provide more descriptive metadata (informative labels, categorical values and labels, and more) that cannot be encoded in a CSV file. 

The application will, however, make an attempt to recognize numeric, string, and date/time values in CSV files. 

Tab-delimited Data Files:
-------------------------

Tab-delimited files can be ingested by replacing the TABs with commas. 

