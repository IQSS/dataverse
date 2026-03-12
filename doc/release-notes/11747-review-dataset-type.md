## Highlights

### Review Datasets

Dataverse now supports review datasets, a type of dataset that can be used to review resources such as other datasets in the Dataverse installation itself or various resources in external data repositories. APIs and a new "review" metadata block (with an "Item Reviewed" field) are in place but the UI for this feature will only available in a future version of the new React-based [Dataverse Frontend](https://github.com/IQSS/dataverse-frontend). See also the [guides](https://dataverse-guide--11753.org.readthedocs.build/en/11753/api/native-api.html#add-dataset-type), #11747, #12015, #11887, #12115, and #11753.

## Other Features Added

- Citation Style Language (CSL) output now includes "type:software" or "type:review" when those dataset types are used. See the [guides](https://dataverse-guide--11753.org.readthedocs.build/en/11753/api/native-api.html#get-citation-in-other-formats) and #11753.

## Updated APIs

- The Change Collection Attributes API now supports `allowedDatasetTypes`. See the [guides](https://dataverse-guide--11753.org.readthedocs.build/en/11753/api/native-api.html#change-collection-attributes), #12115, and #11753.

## Bugs Fixed

- 500 error when deleting dataset type by name. See #11833 and #11753.
- Dataset Type facet works in JSF but not the SPA. See #11758 and #11753.

## Backward Incompatible Changes

### Dataset Types Must Be Allowed, Per-Collection, Before Use

In previous releases of Dataverse, as soon as additional dataset types were added (such as "software", "workflow", etc.), they could be used by all users when creating datasets (via API only). As of this release, on a per-collection basis, superusers must allow these dataset types to be used. See #12115 and #11753.
