## Bug
Validating that an update to dataset or datafile metadata uses lastUpdateTimestamp to assure that the data wasn't edited between calling get and put/post by another user. This fix removed the assumption that the timestamp is stored as UTC since the server could be using local time zone
