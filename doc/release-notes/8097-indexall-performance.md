### Indexing performance on datasets with large numbers of files

We discovered that whenever a full reindexing needs to be performe d, datasets with large numbers of files take exceptionally long time to index (for example, in the IQSS repository it takes several hours for a dataset that has 25,000 files). In situations where the Solr index needs to be erased and rebuilt from scratch (such as a Solr version upgrade, or a corrupt index, etc.) this can significantly delay the repopulation of the search catalog. 

We are still investigating the reasons behind this performance issue. For now, even though some improvements have been made, a dataset with thousands of files is still going to take a long time to index. But we've made a simple change to the reindexing process, where such datasets are indexed at the very end of the batch, after all the datasets with fewer files have been reindexed. This does not improve the total reindexing time, but will repopulate the bulk of the search catalog much faster for the users of the installation. 

