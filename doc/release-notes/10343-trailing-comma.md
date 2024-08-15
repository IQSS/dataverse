### Trailing commas in author name now permitted

When an author name ends on a comma (e.g. "Smith,") a dataset cannot be properly loaded when using json-ld. A null check fixes this.

For more information, see #10343.