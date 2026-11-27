### Fix metadata updates through the API

The `editMetadata` API now compares metadata values directly instead of using substring matches on display text, preventing distinct values from being skipped. Compound field comparisons correctly handle nested values and empty child fields, and replacing metadata correctly clears existing values.
