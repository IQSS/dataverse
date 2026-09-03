Added the ability to set and view the guestbookRoot setting of a collection via these APIs

Any GET call that returns a Collection or List of Collections will include "guestbookRoot": true or false
`Create Dataverse: POST /api/dataverse/{id}`
`Update Dataverse: PUT /api/dataverse/{id}`
(by adding "guestbookRoot" the Json body)
