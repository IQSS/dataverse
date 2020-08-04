## Dataverse installations using Datacite: upgrade action required.

For installations that are using Datacite, Dataverse v5.0 introduces a change in the process of registering the Persistent Identifier (DOI) for a dataset. Instead of registering it when the dataset is published for the first time, Dataverse will try to "reserve" the DOI when it's created (by registering it as a "draft", using Datacite terminology). When the user publishes the dataset, the DOI will be publicized as well (by switching the registration status to "findable"). 

This approach makes the process of publishing datasets simpler and less error-prone. If the DOI has not been reserved (for example, if Datacite was down when the dataset was created), the author can still attempt to publish. But it does help to ensure that the DOI is guaranteed to be available beforehand. New APIs have been provided for finding any unreserved Datacite-issued DOIs in your Dataverse, and for reserving them (see below). If you are upgrading an installation that uses Datacite, we specifically recommend that you reserve the DOIs for all your pre-existing unpublished drafts as soon as Dataverse v5.0 is deployed, since none of them were registered at create time. 

The following API calls should be used:
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

Going forward, once all the DOIs have been reserved for the legacy drafts, you may still get an occasional dataset with an unreserved identifier. Datacite service instability would be a potential cause. There is no reason to expect that to happen often, but it is not impossible. You may consider running the script above (perhaps with some extra diagnostics added) regularly, from a cron job or otherwise, to address this preemptively.
