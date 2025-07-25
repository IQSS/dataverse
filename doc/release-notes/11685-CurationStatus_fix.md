The updates to support keeping the history of curation status labels added in #11268
will incorrectly show curation statuses added prior to v6.7 as the current one, regardless of 
whether newer statuses exist. This PR corrects the problem.

(As a work-around for 6.7 admins can add createtime dates (must be prior to when 6.7 was installed) to the curationstatus table
 for entries that have null createtimes. The code fix in this version properly handles null dates as indicating older/pre v6.7 curationstatuses.)