The API endpoint `api/admin/makeDataCount/{yearMonth}/processingState` has been added to Get, Create/Update(POST), and Delete a State for processing Make Data Count logged metrics
For Create/Update the 'state' is passed in through a query parameter.
Example 
- `curl POST http://localhost:8080/api/admin/makeDataCount/2024-03/processingState?state=Skip`

Valid values for state are [New, Done, Skip, Processing, and Failed]
'New' can be used to re-trigger the processing of the data for the year-month specified.
'Skip' will prevent the file from being processed.
'Processing' shows the state where the file is currently being processed.
'Failed' shows the state where the file has failed and will be re-processed in the next run. If you don't want the file to be re-processed set the state to 'Skip'.
'Done' is the state where the file has been successfully processed.
