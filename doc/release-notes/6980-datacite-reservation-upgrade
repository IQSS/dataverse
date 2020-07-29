## Dataverse installations using Datacite: upgrade action required.

For installations that are using Datacite, Dataverse v5.0 introduces a change in the process of registering the Persistent Identifier (DOI) for a dataset. Instead of registering it when the dataset is published for the first time, Dataverse will try to "reserve" the DOI when it's created (by registering it as a "draft", using Datacite terminology). When the user publishes the dataset, the DOI will be publicized as well (by switching the registration status to "findable"). 

This approach makes the process of publishing datasets simpler and less error-prone. One drawback is that a dataset cannot be published until the DOI has been reserved. Specifically, if your installation uses Datacite, the moment it is upgraded to v5.0, all the pre-existing unpublished drafts will become "unpublishable", until you reserve the DOIs as described below. So it is important to do that immediately after the upgrade, by using the following APIs:

`/api/pids/unreserved`  will report the ids of the datasets 
`/api/pids/:persistentId/reserve` reserves the assigned DOI with Datacite (will need to be run on every id reported by the the first API). 
(See the Native API guide for more information).

Scripted, the whole process would look as follows (adjust as needed):

```
   API_TOKEN='xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx'
   
   curl -s -H "X-Dataverse-key:$API_TOKEN" http://localhost:8080/api/pids/unreserved |
   # the API outputs JSON; note the use of jq to parse it:
   jq '.data.count[].pid' | tr -d '"' | 
   while read doi
   do
      curl -s -H "X-Dataverse-key:$API_TOKEN" -X POST http://localhost:8080/api/pids/:persistentId/reserve?persistentId=$doi
   done
```

If you have a large number of unpublished drafts in the database, reserving them one by one may take some time. So if you want to be extra nice to your users, you may warn them ahead of time that some may have trouble publishing their drafts immediately after the upgrade. 

Going forward, once all the DOIs have been reserved for the legacy drafts, some newly-created datasets may still end up with unreserved DOIs. (For example, if Datacite service was unavailable at the time of the dataset creation; we don't expect it to happen too often, but it is not impossible). To address this preemptively we recommend that you set up a cronjob to run the script above (perhaps with some extra diagnostics added) weekly or even nightly. 
