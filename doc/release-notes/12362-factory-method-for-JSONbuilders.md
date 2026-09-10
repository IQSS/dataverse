# Improved JSON Performance

A Jakarta JSON-P provider is now reused for every builder or value creation.
This improves the performance of large JSON operations, especially large exports and API responses, without changing the existing behavior or the JSON output.
