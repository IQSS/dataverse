## Feature ##

Updated the following APIs to add ability to filter using `searchTerm` and added optional pagination parameters `offset` and `pageSize` to limit the results with each GET.

GET `/api/users/$USERNAME/allowedCollections/$PERMISSION?pageSize=10&offset=0&searchTerm=bio`

GET `/api/mydata/retrieve/collectionList?userIdentifier=anotherUser&pageSize=10&offset=11&searchTerm=bio`
