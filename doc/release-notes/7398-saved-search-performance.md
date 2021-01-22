## Release Highlights

### Saved Search Performance Improvements

A refactoring has greatly improved Saved Search performance in the application. If your installation has multiple, potentially long-running Saved Searches in place, this greatly improves the probability that those search jobs will complete without timing out.

## Notes for Dataverse Installation Administrators

### DB Cleanup for Saved Searches

A previous version of dataverse changed the indexing logic so that when a user links a dataverse, its children are also indexed as linked. This means that the children do not need to be separately linked, and in this version we removed the logic that creates a saved search to create those links when a dataverse is linked.

We recommend cleaning up the db to a) remove these saved searches and b) remove the links for the objects. We can do this via a few queries, which are available in the folder here:

https://github.com/IQSS/dataverse/raw/develop/scripts/issues/7398/

There are four sets of queries available, and they should be run in this order:

- ss_for_deletion.txt to identify the Saved Searches to be deleted
- delete_ss.txt to delete the Saved Searches identified in the previous query
- dld_for_deletion.txt to identify the linked datasets and dataverses to be deleted
- delete_dld.txt to delete the linked datasets and dataverses identified in the previous queries

Note: removing these saved searches and links should not affect what users will see as linked due to the aforementioned indexing change. Similarly, not removing these saved searches and links should not affect anything, but is a cleanup of unnecessary rows in the database.

## Additional Upgrade Instructions

X\. (Optional, but recommended) DB Cleanup

Perform the DB Cleanup for Saved Searches and Linked Objects, summarized in the "Notes for Dataverse Installation Administrators" section above.
