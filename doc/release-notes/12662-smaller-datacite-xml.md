Introduces a new JVM-option `dataverse.datacite.xml.datafile-info` that allows you to configure the number of `size` and `format` elements included in the
DataCite XML metadata. Valid options are `expanded` (default, the current behavior of including one `size` and one `format` element per datafile), `brief`
(one `size` element with the sum of all datafile sizes and one `format` element per unique datafile format), and `none` (no `size` or `format` elements included
in the DataCite XML metadata). See #12662 for details.
