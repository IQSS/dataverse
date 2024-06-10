### Number of Concurrent Heavy Solr Operations Now Configurable

A new MicroProfile setting called `dataverse.solr.concurrency.max-heavy-operations` has been added that controls the maximum number of simultaneously running heavy Solr operations (defaults to 1).

For more information, see #10469.

### Number of Maximum Open Solr Connections Now Configurable

A new MicroProfile setting called `dataverse.solr.concurrency.max-solr-connections` has been added that controls the maximum number of open connections to Solr back-end (defaults to 10000).

For more information, see #10469.