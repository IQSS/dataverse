### More robust metadata export

The metadata export subsystem was overhauled: exports are generated more reliably, failures are reported more clearly, and cached exports are kept fresh more consistently.

### Better error reporting for failed exports

- When a dataset is (re-)exported in multiple formats, a failure of one format no longer aborts the others.
  The resulting error message now lists exactly which formats failed (and the log records more context, including the dataset and format involved).
- Requesting an export in a format that Dataverse does not recognize (for example through a typo in the `exporter=` query parameter)
  now returns a clear HTTP 400 (Bad Request) response with a readable error message, instead of an uninformative server error (HTTP 500) or forbidden (HTTP 403) response.
- Generating an export for an unreleased (draft) version no longer holds the entire export in memory; it is streamed through a temporary file instead.
  Temporary export files are now created with owner-only permissions rather than the system default.

### Updates for system administrators

- Export failures now log clearer warnings including the dataset and the format(s) affected.
  Repeatedly failing I/O operations (such as reading and deleting cached exports) escalate their log level and report recovery, making intermittent storage problems easier to spot.
- **Note:** Cached metadata exports generated with this or a previous release are stored under their legacy auxiliary object name (for example `export_ddi.cached`). 
  The new code manages the newer, version-qualified names instead (for example `export_ddi_1.0.cached`) and does not delete the legacy ones yet.
  They may linger in a dataset's auxiliary storage until manual cleanup or a full re-export removes them. Purging of legacy cache entries is tracked as a follow-up task.

### Updates for developers

The previous monolithic `ExportService` was replaced by a small set of well-separated beans (`ExportServiceBean`, `ExportPipelineBean`, `ExporterRegistryBean`).
They are built on a new `ExportCache` abstraction, with a `StorageIO`-based implementation as default.
A new `ExportSystemException` hierarchy (subclasses `InternalFailure` and `InvalidRequest`) is mapped to appropriate HTTP statuses by JAX-RS exception mappers.
Export cache entries are now keyed by the dataset's version, and the code comes with extensive new unit tests.
The old `io.gdcc.spi.export.ExportException` is no longer used on or across export subsystem boundaries.
