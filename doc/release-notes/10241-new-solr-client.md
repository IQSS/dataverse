[HttpSolrClient](https://solr.apache.org/docs/9_4_1/solrj/org/apache/solr/client/solrj/impl/HttpSolrClient.html) is deprecated as of Solr 9, and which will be removed in a future major release of Solr. It's recommended to use [Http2SolrClient](https://solr.apache.org/docs/9_4_1/solrj/org/apache/solr/client/solrj/impl/Http2SolrClient.html) instead.

[Solr documentation](https://solr.apache.org/guide/solr/latest/deployment-guide/solrj.html#types-of-solrclients) describe it as a _async, non-blocking and general-purpose client that leverage HTTP/2 using the Jetty Http library_.

With Solr 9.4.1, the Http2SolrClient is indicate as experimental. But since the 9.6 version of Solr, this mention is no longer maintained.

The ConcurrentUpdateHttp2SolrClient is now also used in some cases, which is supposed to be more efficient for indexing.

For more information, see issue [#10161](https://github.com/IQSS/dataverse/issues/10161) and pull request [#10241](https://github.com/IQSS/dataverse/pull/10241)
