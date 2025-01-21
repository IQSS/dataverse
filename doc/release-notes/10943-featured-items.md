CRUD endpoints for Collection Featured Items have been implemented. In particular, the following endpoints have been implemented:

- Create a feature item (POST /api/dataverses/<dataverse_id>/featuredItems)
- Update a feature item (PUT /api/dataverseFeaturedItems/<item_id>)
- Delete a feature item (DELETE /api/dataverseFeaturedItems/<item_id>)
- List all featured items in a collection (GET /api/dataverses/<dataverse_id>/featuredItems)
- Delete all featured items in a collection (DELETE /api/dataverses/<dataverse_id>/featuredItems)
- Update all featured items in a collection (PUT /api/dataverses/<dataverse_id>/featuredItems)

See also #10943 and #11124.
