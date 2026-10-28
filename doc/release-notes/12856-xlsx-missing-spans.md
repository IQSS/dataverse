## Bug ##

Tabular ingest of an XLSX file failed with "Could not parse Excel/XLSX spreadsheet" when the first row of the worksheet did not have the optional `spans` attribute. In that case the number of columns is now taken from the cells of the first row.

For more information, see #12856.
