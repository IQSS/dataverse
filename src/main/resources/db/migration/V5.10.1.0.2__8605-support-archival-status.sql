UPDATE datasetversion SET archivalCopyLocation = CONCAT('{"status":"success", "message":"', archivalCopyLocation,'"}') where archivalCopyLocation is not null and not archivalCopyLocation='Attempted';
UPDATE datasetversion SET archivalCopyLocation = CONCAT('{"status":"failure", "message":"Attempted"}') where archivalCopyLocation='Attempted';
