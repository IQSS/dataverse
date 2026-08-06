## Feature ##

Updated the following APIs to add ability to filter using `searchTerm` and added optional pagination parameters `offset` and `limit` to limit the results with each GET.

GET `/api/users/$USERNAME/allowedCollections/$PERMISSION?limit=10&offset=0&searchTerm=bio`

GET `/api/mydata/retrieve/collectionList?userIdentifier=anotherUser&limit=10&offset=11&searchTerm=bio`
