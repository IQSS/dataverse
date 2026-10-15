### Bulk Metadata Export via API

Metadata can now be retrieved for multiple dataset versions in a single request and delivered as **one combined export**.

A new API endpoint, `POST /api/datasets/export`, accepts a JSON body with the name of the metadata export format and a list of datasets, each identified by its persistent identifier and a version specification.
Each version may be given explicitly (for example `1.0`) or by one of the symbolic selectors `:latest`, `:latest-published`, or `:draft`; when omitted, the latest published version is used.

Please note: this is not a replacement for the OAI-PMH based harvesting protocol, which is still the recommended way to harvest metadata in larger quantities.
It reduces the number of round trips substantially for use cases around the Modern UI, where metadata for multiple user-selected datasets is requested.

**Note:** Bulk export relies on export formats that support it.
Only formats whose exporter plugin declares the new bulk export capability can be used to export multiple datasets at once.
The built-in Dataverse JSON format supports bulk export and combines the requested versions into a single JSON array.
Other formats (or bulk export for formats provided by external plugins) becomes available as plugins adopt the new capability.
Requests naming only a single dataset are handled exactly like the previous per-dataset export, so no bulk-capable format is required in that case.

#### Clear error reporting

- Request bodies that are missing or malformed are now rejected with precise, human-readable validation messages pointing at the offending field (for example an invalid version selector or a blank persistent identifier).
- If some of the requested datasets are unknown or cannot be seen by the requester, the response still lists **all** such problems in one place (an HTTP 400 response with a list of violations) instead of stopping at the first one - and deliberately without revealing which datasets exist on the instance.

#### Updates for System Administrators

- **New setting:** the maximum number of dataset versions per bulk export request is controlled by a new JVM setting `dataverse.api.export.bulk.max-request-size` (default: `32`).
- **Log tracing:** every bulk export is assigned a unique correlation identifier, returned to the client in the `X-Dataverse-Export-ID` response header and included in all log records of that batch.
  Use the identifier from a failing request to find the related log entries.
- Downloads served through bulk export are counted and reported by the MakeDataCount service like regular metadata exports (for each released version in the batch).

#### Updates for Export Plugin Developers

The Exporter Plugin API (dataverse-spi) gains a bulk export capability: plugins implementing `io.gdcc.spi.export.caps.bulk.BulkDatasetExporter` receive a `BulkDatasetContext` over a batch of dataset versions and decide how the individual exports are framed and joined.
- Exports are lazy: each one materializes on demand (read from cache, generated on the fly for drafts), in a short transaction of its own, so memory and database connections stay independent of the batch size.
- Streams handed out by the context are tracked and closed automatically when the batch finishes, even if the plugin leaves them open.
- Dataverse implements this capability for its built-in JSON exporter, and ships extensive new unit tests covering the API request validation, the pipeline, and the transaction behavior.
