UPDATE datasetversion SET archivalCopyLocation = CONCAT('{"status":"success", "Message":"', archivalCopyLocation,'"}') where archivalCopyLocation is not null and not archivalCopyLocation='Attempted';
UPDATE datasetversion SET archivalCopyLocation = CONCAT('{"status":"failure", "Message":"Attempted"}') where archivalCopyLocation is not null;
