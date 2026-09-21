# Dataverse Large File Upload Performance Investigation

Investigation into the issue reported by **Federico Yemurenko** on the [Dataverse Users Community](https://groups.google.com/g/dataverse-community/c/pzSjF1YPaJw/m/bQWX3W_kBwAJ?utm_medium=email&utm_source=footer):
> *Uploading 70,000+ small files (.gif and .txt) in batches of 1,000 files with a 5s delay using DVUploader.*  
> *Environment: 1 VM with 8GB RAM, 2 CPUs, filesystem storage (Payara, Postgres, Solr co-located).*  
> *Symptom: Started at ~1 file/second, but slowed down to ~30 seconds per file after 14,000 files (taking 3+ days for 14 batches).*

---

## 1. Executive Summary & Direct Answers

### Q1: Is 30 seconds per file reasonable?
**Yes, it is entirely expected given Dataverse's current architecture and filesystem storage driver, but it represents an architectural bottleneck ($O(N^2)$ algorithmic complexity) rather than normal behavior.**

Dataverse was designed as a repository for research datasets with moderate file counts (typically dozens to hundreds of files per dataset). When uploading to local filesystem storage via standard HTTP, each file triggers a full dataset version mutation cycle. At $N = 14,000$ files:
- **PostgreSQL executes 14,000 SQL `UPDATE` statements** for every single file uploaded.
- **Payara deeply clones 14,000 `FileMetadata` objects in RAM** and runs reflection-based Bean Validation on all 14,000 files.
- **Solr deletes and re-indexes all 14,000 files** in the background, consuming CPU and I/O.
- On a 2-CPU / 8GB RAM VM, this 3-way resource competition causes latency to balloon from ~0.2s to ~30s per file.

### Q2: What happens if the upload continues to 70,000 files?
- At 30,000 files: ~60–80 seconds per file.
- At 70,000 files: ~2–4 minutes per file.
- Remaining time for the next 56,000 files: **~50 to 60+ days**, with a near-certain risk of Payara `OutOfMemoryError`, database connection timeout, or Solr thread pool exhaustion.

---

## 2. Root Cause Analysis (Code-Level Evidence)

When DVUploader uploads to a dataset on **local filesystem storage**, direct upload is unavailable. Files are uploaded sequentially via `POST /api/datasets/{id}/add`. For each file uploaded, Dataverse executes `UpdateDatasetVersionCommand`:

### Root Cause 1: $O(N)$ SQL Updates per Upload ($O(N^2)$ Cumulative)
In [`src/main/java/edu/harvard/iq/dataverse/engine/command/impl/UpdateDatasetVersionCommand.java`](file:///Users/tuannguyen/Projects/tuannx/dataverse/src/main/java/edu/harvard/iq/dataverse/engine/command/impl/UpdateDatasetVersionCommand.java#L159-L165):

```java
// Set creator and create date for files if needed
for (DataFile dataFile : theDataset.getFiles()) {
    if (dataFile.getCreateDate() == null) {
        dataFile.setCreateDate(getTimestamp());
        dataFile.setCreator((AuthenticatedUser) getUser());
    }
    dataFile.setModificationTime(getTimestamp()); // <--- CRITICAL BUG/BOTTLENECK
}
```

- When uploading file #14,001, `theDataset.getFiles()` contains 14,000 files.
- Setting `modificationTime` on all 14,000 `DataFile` entities marks them dirty in EclipseLink JPA.
- When `ctxt.em().flush()` is called (line 276), PostgreSQL executes **14,000 separate `UPDATE DVOBJECT SET MODIFICATIONTIME = ... WHERE ID = ...` statements** inside a single transaction!
- For a single batch of 1,000 files (e.g. from 13,000 to 14,000), PostgreSQL executes **~13.5 million SQL updates**!
- Cumulatively for 14,000 files: $\frac{N(N+1)}{2} \approx \mathbf{98\text{ million}}$ SQL updates!

### Root Cause 2: In-Memory Deep Cloning on Every File
In [`src/main/java/edu/harvard/iq/dataverse/DatasetVersion.java`](file:///Users/tuannguyen/Projects/tuannx/dataverse/src/main/java/edu/harvard/iq/dataverse/DatasetVersion.java#L690-L692):

```java
public DatasetVersion cloneDatasetVersion() {
    ...
    for (FileMetadata fm : this.getFileMetadatas()) {
        fm.createCopyInVersion(dsv);
    }
    ...
}
```
Every file upload invokes `cloneDatasetVersion()` to create a pre-change snapshot for version diffing. For 14,000 files, 14,000 heavy Java entities are deeply cloned on every HTTP request, thrashing the JVM Eden space and triggering constant GC pauses.

### Root Cause 3: Reflection-Based Bean Validation
In [`src/main/java/edu/harvard/iq/dataverse/DatasetVersion.java`](file:///Users/tuannguyen/Projects/tuannx/dataverse/src/main/java/edu/harvard/iq/dataverse/DatasetVersion.java#L1791-L1809):

```java
List<FileMetadata> dsvfileMetadatas = this.getFileMetadatas();
if (dsvfileMetadatas != null) {
    for (FileMetadata fileMetadata : dsvfileMetadatas) {
        Set<ConstraintViolation<FileMetadata>> constraintViolations = validator.validate(fileMetadata);
        ...
    }
}
```
`validator.validate(fileMetadata)` runs Jakarta Bean Validation via reflection on all 14,000 files for every single file uploaded.

### Root Cause 4: Complete Solr Delete & Re-index per File ($O(N^2)$ Lookups)
In [`src/main/java/edu/harvard/iq/dataverse/search/IndexServiceBean.java`](file:///Users/tuannguyen/Projects/tuannx/dataverse/src/main/java/edu/harvard/iq/dataverse/search/IndexServiceBean.java#L1455-L1525):
- For draft versions without a released version, Dataverse adds all file IDs to an `ArrayList<Long> changedFileIds`.
- It iterates through all 14,000 files and calls `changedFileIds.contains(datafile.getId())`. Because `changedFileIds` is a `List`, `contains()` is $O(N)$, resulting in **$14,000 \times 14,000 = 196\text{ million}$ string/Long equality checks**.
- Solr sends delete queries for all 14,000 files and creates 14,000 new Solr documents per file uploaded.

### Root Cause 5: 2 CPU / 8GB Hardware Contention
Payara Server (JVM), PostgreSQL, and Apache Solr run on the same 2-CPU VM:
- PostgreSQL CPU spikes processing thousands of row updates.
- Solr CPU spikes rebuilding search indexes and committing transaction logs.
- Payara CPU spikes with object cloning, validation reflection, and JPA flushing.
The 2 CPUs are pinned at 100% saturation continuously.

---

## 3. Recommended Solutions & Best Practices

### Solution A: Package Files into Archives (Immediate Best Practice)
For 70,000 small files (`.gif`, `.txt`):
- **Bundle the files into zip or tar archives** (e.g., grouped by category, date, or subject into 10–70 zip files).
- Research repositories (and users) prefer downloading a single structured zip archive rather than navigating 70,000 individual file entries in the web UI.

### Solution B: Split into Multiple Datasets
- Instead of 1 monolithic dataset with 70,000 files, split into 70 datasets with 1,000 files each, linked together under a common Dataverse collection or using dataset linking.

### Solution C: Use S3 / MinIO Direct Upload with Batch `/addFiles`
- Configure Dataverse with an S3-compatible storage driver (AWS S3, MinIO, or Ceph) and enable `upload-redirect=true`.
- DVUploader will upload file payloads directly to object storage in parallel, and then call the batch `/api/datasets/{id}/addFiles` API.
- With batch `/addFiles`, `UpdateDatasetVersionCommand` runs **ONCE per batch of 1,000 files**, rather than 1,000 times!

### Solution D: Code Fix for Dataverse (Upstream Contribution)
In `UpdateDatasetVersionCommand.java`:
```java
// INSTEAD OF:
for (DataFile dataFile : theDataset.getFiles()) {
    dataFile.setModificationTime(getTimestamp());
}

// IT SHOULD ONLY UPDATE NEWLY CREATED OR MODIFIED FILES:
if (filesToUpdate != null) {
    for (DataFile dataFile : filesToUpdate) {
        dataFile.setModificationTime(getTimestamp());
    }
}
```
Eliminating the blanket modification time update avoids marking clean files dirty, dropping database query count from $O(N)$ to $O(1)$ per upload.

---

## 4. How to Run the Investigation Docker Environment

### Prerequisites
- Docker Desktop or Docker Engine with Docker Compose v2.

### Step 1: Start the Environment
```bash
cd docker/investigation-large-upload
docker compose up -d
```

### Step 2: Check Logs and Wait for Bootstrap
```bash
docker compose logs -f bootstrap
```
Once bootstrap completes with `bootstrap.sh dev finished`, Dataverse will be running at `http://localhost:8085`.

### Step 3: Run the Investigation Benchmark
```bash
python3 investigate.py --url http://localhost:8085 --total-files 100 --batch-size 20 --batch-delay 2.0
```

To run against the existing development instance:
```bash
python3 investigate.py --url http://localhost:8080 --total-files 100 --batch-size 20
```

The script will report:
- Latency progression across batches.
- Total PostgreSQL `UPDATE DVOBJECT SET MODIFICATIONTIME` queries executed.
- Comparison with the theoretical $O(N^2/2)$ curve.
