## Bug fix to Search API. Now includes all type totals (Dataverses, Dataset, and Files) regardless of the list of types requested

None requested types were returned with total count set to 0.
&type=dataverse&type=dataset would result in "Files" : 0 since type=file was not requested

Now all counts show the correct totals.
Note: This is only true for the first page requested. Subsequent pages could have 0 counts and should not be used. This is due to the need for speed. Getting the totals is an additional search call in the background.
