# Signposting for Dataverse

This branch adds [Signposting](https://signposting.org/) support to Dataverse

There are 2 Signposting profile levels, level 1 and level 2. In this implementation, 
 * level 1 links are shown in 
HTTP header, which can be fetched by `curl -I https://domain/link-to-article`. 
 * The level 2 linkset can be fetched by visiting the dedicated linkset page for 
   that artifact. The link can be seen in level 1 links with key name `rel="linkset"`.

The configuration is stored as JSON string in the `Bundle.properties` file, key name is
`signposting.configuration.SignpostingConf`. Please see a sample configuration below with explaination for each of the
config items.

```json
{
  "describedby": {
    "doi": "https://doi.org/",
    "type": "application/vnd.citationstyles.csl+json"
  },
  "useDefaultFileType": true,
  "defaultFileTypeValue": "https://schema.org/Dataset",
  "maxItems": 5,
  "maxAuthors": 5
}
```

 * `describedby` is required by [Signposting](https://signposting.org/). It shows the link to the metadata 
which describes the resources that is the origin of the link.
 * `useDefaultFileType` and `defaultFileTypeValue` are used in combination to provide extra `Dataset` type to DV 
   datasets. `AboutPage` is required by `Signposting`, hence always present in the datasets. Whilst a second type 
   could be configured to better reflect the actual scholarly type of the dataset. 
 * `maxItems` sets the max number of items/files which will be shown in `level 1` profile. Datasets with 
   too many files will not show any file link in `level 1` profile. They will be shown in `level 2` linkset only. 
 * `maxAuthors` Same with `maxItems`, `maxAuthors` sets the max number of authors to be shown in `level 1` profile. 
If amount of authors exceeds this value, no link of authors will be shown in `level 1` profile. 

Note: Authors without author link will not be counted nor shown in any profile/linkset. 