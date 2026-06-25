## Bug ##

API GET /api/datasets/{id}/metadata now returns all multivalued fields as arrays instead of String for 1 entry and Array for more than 1 entry. This consistancy makes parsing the JSON easier for users of the API.
