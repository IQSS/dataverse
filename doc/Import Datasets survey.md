# Import Dataset Survey

## Dataset-related commands - current
This is a list of commands, what they actually do, and some recommendations about what to do with them, in the context of the current refactoring.

* `PublishDatasetCommand` verifies that the dataset version publication is legal from a business logic POV. Either kicks off a workflow (aynsc), or  goes directly to `FinalizeDatasetPublicationCommand`.
* `FinalizeDatasetPublicationCommand` Does most of the heavy lifting: Permissions, notifications, file permissions, etc. **Handles DOI creation**. Kicks off the post-publish workflow, if any.
* `UpdateDatasetVersionCommand` updates a dataset version, if it is the edit version (otherwise throws `IllegalCommandException`). Validates and updates related fields, but generally takes the updated Java Dataset object and puts it into the DB.
* `UpdateDatasetThumbnailCommand` Creates/return thumbnails for Dataset. Not relevant to this effort.
* `UpdateDatasetTargetURLCommand` **Super-user only** Sets the persistent identifier of a dataset using its metadata. This action affects both internal database and the external ID provider.
* `DestroyDatasetCommand` **Super-user only**. Deletes datasets, even if they are published.
* `DeleteDatasetVersionCommand` Deletes the *draft* version of a dataset. May destroy the dataset, if all it has is a draft (so, not published and no more versions left after the draft is deleted).
    - TODO: Rename command to `DeleteDatasetDraftVersionCommand`.
* `DeleteDatasetCommand` Referred to in multiple places, but actually has a TODO: REMOVE in code. Currently calls `DeleteDatasetVersionCommand`, which only deletes a dataset if it only has a draft version (which is the correct business logic BTW, so maybe no need to remove).
    - TODO: Maybe not remove, but test that the dataset to be removed only has a draft version, and based on that either remove, or throw an `IllegalCommandException`.
* `CreateDatasetVersionCommand` Creates a new version of a dataset. Has some complications to support migration. Shares a lot of code with `UpdateDatasetVersionCommand`.
    - TODO: Clean up migration stuff
    - consolidate functionality with `UpdateDatasetVersionCommand`.
* `CreateDatasetCommand` Creates a dataset and a dataset version. Functionality supports harvest and migration as well as normal creation, so the logic is quite complex and long. Serious code duplication with `CreateDatasetVersionCommand`. Might be able to re-use it altogether here (pending JPA issues with created ids etc. But should be able to work.)
    - Has a more detailed constraint violation report.
    -  TODO: break, consolidate, tidy, remove duplications.
* `UpdateDatasetCommand` Updates the edit version of a dataset. May delete files as well. Can create a persistent ID. Seems rather outdated, as it mentions only EZID.
* Also related: `SubmitDatasetForReviewCommand`, `ReturnDatasetToAuthorCommand`
* `DataFile`, `Dataset`, `DatasetVersion`: Cleanup as we go (some deprecated methods can be removed).

## DOI / Persistent Identifier

* Registration Performed by `FinalizeDatasetPublicationCommand`, in methods `registerExternalIdentifier` and `publicizeExternalIdentifier`.
* `IdServiceBean` an interface for service beans with a static dispatcher for getting an actual bean based on the protocol and DOI provider. All these service beans extend `AbstractIdServiceBean`, which implements `IdSeviceBean`. Aforementioned methods use it.
    - TODO: Rename to `PersistenIdentifierServiceBean`, use better code for dispatch.

## Migration
* Can be refactored out, thanks to Ellen's ImportUtil.MIGRATION flag. :heart_eyes_cat:

## Questions
* Why does the `Dataset` has a publication date? Can't this be inferred from the dataset versions it has? (probably not part of this refactor, though.)
* Exporting formats - can this be done asynchronously? Why wait for this possibly long process? Note that we explicitly ignore any failures of this (`FinalizeDatasetPublicatinoCommand#exportMetadata`, line 134).
    - *Decision* Use async (but alidate with related programmers first).
* Are we OK with this:
    - Import: POST a JSON with dataset data and a *single* dataset version
    - Migrate: Start same as import, with the dataset version being the first dataset version. Then POST additional versions, one at a time, to a different endpoint.
    - Both endpoints accept parameters for triggering workflows (yes/no) and publishing (yes/no). If no publishing, the version stays at DRAFT mode.
        + Also, no publish effectively means no workflow (since no reason to run the workflow, event if there is one).
* Refactor out migration?
    *  *Decision* YES.
* Creation of new datasets: We can either have a single command with multiple modes (native new, harvest, import), or three different commands with as much code reuse as possible. Need to decide on this.
    - *Decision* Separate commands
* Difference between `UpdateDatasetCommand` and `UpdateDatasetVersionCommand`?
    - Seems like the `XXDatasetVersionCommand`s are redundant. They should be removed.
* What about the package `edu.harvard.iq.dataverse.batch.jobs.importer`? Can this go away now?
    - no, used by rsync
* Same question about `ImportServiceBean`
    - no, used by batch that's used by rsync

## Common activities
* Creating DOIs
* Validating a dataset
* Saving a dataset version
    - compare `UpdateDatasetCommand` to `UpdateDatasetVersionCommand`
    -

## Import Behavior
### CreateDatasetCommand
<pre>
Item         | null | NEW         | MIGRATION | HARVEST | HARVEST_WITH_FILES
-------------+------+-------------+-----------+---------+--------------------
validate     |  +   |   +         |    x      |     x   |  + (bug?)
Unique PID   |      |             |           |         |
-------------+------+-------------+-----------+---------+--------------------
persisted    | edit | latest      |             latest   
version      |      | (bug?)      |                       
-------------+------+-------------+-----------+---------+--------------------
PID          | create if          |  use existing if latest is released
             | globalIdCreateTime |
             | is null            |    
-------------+------+-------------+-----------+---------+--------------------
Set default  |  +   |   +         |    x      |    x    |  x
contributor  |      |             |           |         |   
role         |      |             |           |         |   
</pre>

### Other places

* BatchImport Service bean (`MIGRATE`, `HARVEST`)
* ImportDDIServiceBean (all types, including `MIGRATE`)
* ImportServiceBean (all types, including `MIGRATE`)
* BatchServiceBean (passthrough to ImportServiceBean)

_Note: no other commands use it_

## Open issues for



## Added to scope (if possible)
* https://github.com/IQSS/dataverse/issues/4561


## Done log
* Removed deprecated `name` field from `DataFile` (including related methods which were not used).
* `IdServiceBean`: Code cleanup for dispatch and code-to-interface (rather than implementing classes).
* `IdServiceBean` -> `PersistentIdentifierServiceBean`
* Code clean-up for the DOI/EZId service beans.
* Unit tests for DOI service bean dispatching.
* Added SettingsServiceBean mock for testing.
* Fixed integration tests
* Created `AbstractDatasaetCommand` and consolidated code to it.
* (During publication) Moved the update/create of the DatasetVersionUser from the finalize stage to the publication kickoff stage. This reflects better that fact that this class represents the last time a user have touched a dataset (note that workflows can take a long time)
* DatasetVersion validation consolidation
* Dataset: Remove unused methods.
* Created an async method to index datasets.
* CreateDatasetCommand calls the async indexation method.
    - Also, not failing when indexation fails
