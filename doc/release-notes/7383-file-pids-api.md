## Major Use Cases

- Dataverse Administrators can now use the PID reservation API to reserve PIDs for files as well as datasets (Issue #7383, PR #7384)

### Dataverse Installations with File PIDs Enabled and Using DataCite: Upgrade Action Recommended

/* review this at release note time as this could change if this is included in the dame release as #7068 */

### Dataverse Installations Using DataCite: Upgrade Action Recommended

For installations that are using DataCite, this version introduces a change in the process of registering the Persistent Identifier (DOI) for the files in a dataset. Instead of registering file DOIs when the dataset is published for the first time, Dataverse will try to "reserve" the DOIs for files when they are added to the draft dataset (by registering the files as a "draft", using DataCite terminology). When the user publishes the dataset, the DOI for the files will be publicized as well (by switching the registration status to "findable"). This approach makes the process of publishing datasets simpler and less error-prone.

An updated API has been provided for finding any unreserved DataCite-issued file DOIs in your Dataverse, and for reserving them (see below). While not required - the user can still attempt to publish a dataset with an unreserved DOI - having all the identifiers reserved ahead of time is recommended. If you are upgrading an installation that uses DataCite, we specifically recommend that you reserve the file DOIs for all your pre-existing unpublished drafts as soon as this version of Dataverse is deployed, since none of the files were registered at create time. This can be done using the following API calls:  

- `/api/pids/unreserved`  will report the ids of the files that have unreserved PIDs
- `/api/pids/:persistentId/reserve` reserves the assigned file DOI with DataCite (will need to be run on every id reported by the the first API).

See the [Native API Guide](http://guides.dataverse.org/en/5.2/api/native-api.html) for more information.

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

Going forward, once all the DOIs have been reserved for the legacy drafts, you may still get an occasional file with an unreserved identifier. DataCite service instability would be a potential cause. There is no reason to expect that to happen often, but it is not impossible. You may consider running the script above (perhaps with some extra diagnostics added) regularly, from a cron job or otherwise, to address this preemptively.