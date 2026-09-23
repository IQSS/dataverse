## Bug ##

Fixed an issue where geographic bounding box coordinates with more than 5 decimal places could pass validation despite reversed order (due to single-precision float precision loss) and subsequently cause Solr indexing failures with `InvalidShapeException`. Bounding box coordinates are now parsed and compared using `BigDecimal`.

NOTE for the developer writing the combined 6.13 release note: Review the upgrade instructions; if we are not recommending a full reindex for other reasons, consider adding a suggestion to selective reindex datasets with bounding boxes"
