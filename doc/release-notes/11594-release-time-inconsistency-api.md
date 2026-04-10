## Bug
API inconsistency for release time between JsonParser/JsonPrinter has been addressed. For backward compatibility new "releaseTime" and original "releaseDate" will be allowed in the Json giving preferance to the new "releaseTime" field. Also, "releaseTime" field will populate the db with date/time. The original "releaseDate" field will continue to truncated to the date only.
