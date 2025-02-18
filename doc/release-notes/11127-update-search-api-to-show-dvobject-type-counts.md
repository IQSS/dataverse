### show-type-counts Behavior changed in Search API

In the Search API if you set show_type_counts=true the response will include all object types (Dataverses, Datasets, and Files) even if the search result for any given type is 0.

See also the [guides](https://preview.guides.gdcc.io/en/develop/api/search.html#parameters), #11127 and #11138.
