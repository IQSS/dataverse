Excel 
+++++++

Microsoft Excel files (XLSX format).

.. contents:: |toctitle|
	:local:

Supported Formats
=================

.. note:: Only the "new", XLSX Excel files are supported. We are not planning to add support for the old-style, binary XLS files. XLS files will *not* be ingested.

.. warning:: We strongly encourage NOT using the old, binary XLS files. Apart from concerns with its strictly proprietary nature, the format simply wasn't designed for storing large scientific data, and that is often misunderstood. One such file can only store up to 65535 rows of data and **doesn't prompt user in any way** upon saving a file if this limit is exceeded. When the file is saved and the editor closed, the data is silently truncated and lost.

Limitations
===========

If an Excel file has multiple sheets, only the first sheet of the file will be ingested. The other sheets will be available when a user downloads the original Excel file. To have all sheets of an Excel file ingested and searchable at the variable level, upload each sheet as an individual file in your dataset.

Ingest Errors
=============

You may encounter ingest errors after uploading an Excel file if the file is formatted in a way that can’t be ingested by the Dataverse software. Ingest errors can be caused by a variety of formatting inconsistencies, including:

- line breaks in a cell
- blank cells
- single cells that span multiple rows or columns (i.e. merged cells)
- missing headers

Example Data
============

`An example of an Excel file that successfully ingests <https://github.com/IQSS/dataverse-sample-data/blob/master/data/dataverses/dataverseno/datasets/tabular-sample-data/files/Tabular_Sample_Data.xlsx>`_ is available in the Dataverse Sample Data GitHub repository.