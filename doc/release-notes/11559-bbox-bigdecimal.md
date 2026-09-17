## Bug ##

Fixed an issue where geographic bounding box coordinates with more than 5 decimal places could pass validation despite reversed order (due to single-precision float precision loss) and subsequently cause Solr indexing failures with `InvalidShapeException`. Bounding box coordinates are now parsed and compared using `BigDecimal`.
