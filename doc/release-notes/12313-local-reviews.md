### Local Reviews

Datasets can have local reviews, listable via API. A local review is a review dataset ("review" for short) that points at the URL form of a persistent ID of a dataset (e.g. itemReviewedUrl:https://doi.org/10.5072/FK2/ABCDEF) that is in the same Dataverse installation. Local reviews of a dataset can be listed via API (and we plan to build a UI for it some day).

A new metadata block called "Trusted Data Dimensions and Intensities" has been added for testing. This is described in the setup instructions for review datasets.

If you set `dataverse.feature.croissant-with-local-reviews` to true, local reviews will appear in the croissant and croissantSlim metadata export formats for any dataset that has local reviews. This feature is experiemental, which is why it is hidden behind a feature flag.

See the guides for the new [list reviews](https://preview.guides.gdcc.io/en/develop/api/native-api.html#list-reviews) API endpoint, #12313, #12314, and #12425.

## New Settings

- dataverse.feature.croissant-with-local-reviews
