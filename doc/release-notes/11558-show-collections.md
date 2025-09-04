The Search API now supports a `show_collections` parameter for dataset results.
When the parameter is set, each result includes a `collections` array showing the dataset’s parent and linked collections. Each entry includes `id`, `name`, and `alias`, for example:

```json
"collections": [
  {
    "id": 11,
    "name": "My cool collection",
    "alias": "dvcb50a190"
  }
]
```