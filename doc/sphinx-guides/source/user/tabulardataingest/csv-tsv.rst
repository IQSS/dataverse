CSV/TSV
++++++++

.. contents:: |toctitle|
	:local:

Ingest of Comma-Separated Values and Tab-Separated Values files as tabular data
--------------------------------------------------------------------------------

The Dataverse installation will make an attempt to turn CSV and TSV files uploaded by the user into tabular data, using the `Apache CSV parser <https://commons.apache.org/proper/commons-csv/>`_. 

Main formatting requirements
-----------------------------

The first row in the document will be treated as the CSV's header, containing variable names for each column.

Each following row must contain the same number of comma-separated values ("cells") as that header.

As of the Dataverse Software 4.8 release, we allow ingest of CSV files with commas and line breaks within cells. A string with any number of commas and line breaks enclosed within double quotes is recognized as a single cell. Double quotes can be encoded as two double quotes in a row (``""``). 

For example, the following lines:

.. code-block:: none

    a,b,"c,d
    efgh""ijk""l",m,n

are recognized as a **single** row with **5** comma-separated values (cells):

.. code-block:: none

    a
    b 
    c,d\nefgh"ijk"l
    m
    n 

(where ``\n`` is a new line character)

Limitations
------------

Compared to other formats, relatively little information about the data ("variable-level metadata") can be extracted from a CSV file. Aside from the variable names supplied in the top line, the ingest will make an educated guess about the data type of each comma-separated column. One of the supported rich file formats (Stata, SPSS and R) should be used if you need to provide more descriptive variable-level metadata (variable labels, categorical values and labels, explicitly defined data types, etc.). 

Recognized data types and formatting
-------------------------------------

The application will attempt to recognize numeric, string, and date/time values in the individual columns.

For dates, the ``yyyy-MM-dd`` format is recognized. 

For date-time values, the following 2 formats are recognized: 

``yyyy-MM-dd HH:mm:ss``

``yyyy-MM-dd HH:mm:ss z`` (same format as the above, with the time zone specified)

For numeric variables, the following special values are recognized:

``inf``, ``+inf`` - as a special IEEE 754 "positive infinity" value;

``NaN`` - as a special IEEE 754 "not a number" value; 

An empty value (i.e., a comma followed immediately by another comma, or the line end), or ``NA`` - as a *missing value*.

``null`` - as a numeric *zero*. 

(any combinations of lower and upper cases are allowed in the notations above). 

In character strings, an empty value (a comma followed by another comma, or the line end) is treated as an empty string (NOT as a *missing value*). 

Any non-Latin characters are allowed in character string values, **as long as the encoding is UTF8**. 

**Note:** When the ingest recognizes a CSV or TSV column as a numeric vector, or as a date/time value, this information is reflected and saved in the database as the *data variable metadata*. To inspect that metadata, select *Variable Metadata* listed as a download option for the tabular file. This will export the variable records in the DDI XML format. (Alternatively, this metadata fragment can be downloaded via the Data Access API; for example: ``http://localhost:8080/api/access/datafile/<FILEID>/metadata/ddi``). 

The most immediate implication is in the calculation of the UNF signatures for the data vectors, as different normalization rules are applied to numeric, character, and date/time values. (see the :doc:`/developers/unf/index` section for more information). If it is important to you that the UNF checksums of your data are accurately calculated, check that the numeric and date/time columns in your file were recognized as such (as ``type=numeric`` and ``type=character, category=date(time)``, respectively). If, for example, a column that was supposed to be numeric is recognized as a vector of character values (strings), double-check that the formatting of the values is consistent. Remember, a single improperly-formatted value in the column will turn it into a vector of character strings, and result in a different UNF. Fix any formatting errors you find, delete the file from the dataset, and try to ingest it again.
