CSV
++++++

.. contents:: |toctitle|
	:local:

Ingest of Comma-Separated Values files as tabular data. 
-------------------------------------------------------

Dataverse will make an attempt to turn CSV files uploaded by the user into tabular data. 

Main formatting requirements: 
-----------------------------

The first line must contain a comma-separated list of the variable names; 

All the lines that follow must contain the same number of comma-separated values as the first, variable name line. 

Limitations:
------------

Except for the variable names supplied in the top line, very little information describing the data can be obtained from a CSV file. We strongly recommend using one of the supported rich files formats (Stata, SPSS and R) to provide more descriptive metadata (informatinve lables, categorical values and labels, and more) that cannot be encoded in a CSV file. 

The application will however make an attempt to recognize numeric, string and date/time values in CSV files. 

Tab-delimited Data Files:
-------------------------

Tab-delimited files could be ingested by replacing the TABs with commas. 

