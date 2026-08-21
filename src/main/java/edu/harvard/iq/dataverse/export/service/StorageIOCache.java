package edu.harvard.iq.dataverse.export.service;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.util.SecureTempFiles;
import edu.harvard.iq.dataverse.util.logging.FailureEscalation;
import io.gdcc.spi.export.ExportException;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link ExportCache} backed by Dataverse's {@link StorageIO} layer, storing exports as auxiliary objects alongside the dataset.
 * <p>
 * <b>Naming Schema:</b> The canonical "aux tag" is version-qualified ({@code export_<format>_<versionNumber>.cached},
 * see {@link ExportCacheKey#auxTag()}) and is the only name ever written.
 * <p>
 * The legacy, unqualified name ({@code export_<format>.cached}) predates version qualification and only ever described
 * the <em>latest released</em> version. It is ignored by this cache implementation for read/write cycles but may
 * be purged using {@link #evictAll(Dataset)}.
 * <p>
 * <b>Write Atomicity:</b> Exports are always rendered to a local temp file first.
 * Then it gets persisted via {@link StorageIO#savePathAsAux(Path, String)} as an auxiliary dataset file.
 * To make it thread-safe end-to-end, the underlying storage drivers <em>must</em> support atomic writes.
 * <p>
 * Note: This class replaces the former {@code ExportService.cacheExport()} method, mostly written by qqmyers.
 * Instead of its "try openAuxChannel, fall back to temp file for S3/Swift" branching, there now is one code path for all drivers.
 * Readers can never observe a half-written export under the cache key. The cost is one extra local write per export,
 * which is negligible next to export generation itself.
 * <p>
 * Note 2: This class is an application-scoped CDI bean (single instance). The cache itself is stateless,
 * and every operation operates on their own {@code StorageIO}. But: if we add a write lock later on to avoid race
 * conditions during writes, we will require an instance wide single map to store these locks, which CDI gives us for free.
 * In addition, one might use a Hazelcast-backed map to acquire multi-instance wide locks!
 * And lastly, making this an injectable CDI bean makes mocking it in tests very easy.
 */
@ApplicationScoped
public final class StorageIOCache implements ExportCache {
    
    private static final Logger logger = Logger.getLogger(StorageIOCache.class.getCanonicalName());
    
    // TODO: these hard coded thresholds are arbitrarily high and should be configurable via JvmSettings
    private static final FailureEscalation quietDeleteFails = new FailureEscalation(256);
    private static final FailureEscalation tryReadFails = new FailureEscalation(256);
    
    /**
     * Reads an input stream associated with the given export cache key.
     *
     * @param dataset The dataset associated with the export cache key, used to determine storage access.
     * @param key The export cache key containing dataset, format, and versioning information.
     * @return an {@code Optional} containing the input stream if available, otherwise an empty {@code Optional}.
     * @throws IOException if an I/O error occurs while attempting to read the data.
     */
    @Override
    public Optional<InputStream> read(Dataset dataset, ExportCacheKey key) throws IOException {
        StorageIO<Dataset> storage = storageFor(dataset);
        return tryRead(storage, key.auxTag());
    }
    
    /**
     * Writes the export cache data to a temporary file and ensures it is properly persisted to the dataset's storage.
     * Handles file cleanup to maintain system integrity.
     *
     * @param dataset The dataset associated with the export cache key, used to determine storage access.
     * @param key The {@code ExportCacheKey} representing the metadata export about to be cached.
     * @param writer The {@code ExportStreamWriter} functional interface implementation responsible for writing data
     *              to the output stream. This wraps the underlying exporter, writing the actual data format.
     * @throws ExportException If an error occurs during the export process.
     * @throws IOException If an I/O error occurs while creating, writing, or managing the temporary file.
     */
    @Override
    public void write(Dataset dataset, ExportCacheKey key, ExportStreamWriter writer) throws ExportException, IOException {
        Path tempFile = SecureTempFiles.createOwnerOnlyTempFile("dataverse-export-", ".tmp");
        try {
            // No catch here (checked exception), but closing the stream after use, avoiding leaks.
            try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(tempFile))) {
                writer.writeTo(out);
            }
            // Persist to storage only after the metadata export has been fully and successfully rendered.
            // A failure above leaves the cache untouched.
            // TODO: verify for all storage drivers that they support atomic writes.
            storageFor(dataset).savePathAsAux(tempFile, key.auxTag());
            logger.log(Level.FINE, dataset.getId() + ": Cached export written: {0}", key.auxTag());
        } finally {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException e) {
                // Warn, but do not fail if the temp file could not be deleted. (The main operation was a success)
                logger.log(Level.WARNING, e, () -> dataset.getId() + ": could not delete export temp file " + tempFile);
            }
        }
    }
    
    @Override
    public void evict(Dataset dataset, ExportCacheKey key) throws IOException {
        deleteQuietly(storageFor(dataset), key.auxTag());
    }
    
    @Override
    public void evictAll(Dataset dataset) throws IOException {
        StorageIO<Dataset> storage = storageFor(dataset);
        List<String> auxTags = storage.listAuxObjects();
        for (String tag : auxTags) {
            if (tag.startsWith(ExportCacheKey.TAG_PREFIX) && tag.endsWith(ExportCacheKey.TAG_SUFFIX)) {
                deleteQuietly(storage, tag);
            }
        }
    }
    
    /**
     * Try reading a cached metadata export via StorageIO. Cache miss results in empty {@code Optional}.
     */
    private static Optional<InputStream> tryRead(StorageIO<Dataset> storage, String auxTag) {
        // Distinguish "not cached" (normal, frequent) from actual failures: only read if the aux object exists.
        try {
            if (!storage.isAuxObjectCached(auxTag)) {
                return Optional.empty();
            }
            tryReadFails.recordSuccess().ifPresent(n -> logger.warning("Trying to read cached export recovered after " + n + " consecutive failures"));
        } catch (IOException e) {
            // Treat as a "cache miss" so the pipeline regenerates rather than failing over a cache IO issue.
            // Note: if necessary, elevate recording the failures per storage or even more fine-grained, including the tag.
            logger.log(tryReadFails.incrementAndGetLevel(), e,
                       () -> "Existence check failed for " + auxTag + " (consecutive failures: " + tryReadFails.currentStreak() + ")");
            return Optional.empty();
        }
        try {
            return Optional.of(storage.getAuxFileAsInputStream(auxTag));
        } catch (IOException e) {
            // Maybe an exists-then-vanished race, or a genuine storage problem.
            // Treated as a "cache miss" so the pipeline regenerates rather than failing over a cache IO issue.
            logger.log(Level.WARNING, e, () -> "Could not open cached export " + auxTag);
            return Optional.empty();
        }
    }
    
    /**
     * Try to delete, but do not fail on errors. Logging a warning instead.
     */
    private static void deleteQuietly(StorageIO<Dataset> storage, String auxTag) {
        try {
            storage.deleteAuxObject(auxTag);
            quietDeleteFails.recordSuccess().ifPresent(n -> logger.log(Level.FINE, "Quiet deletes from the cache recovered after " + n + " consecutive failures."));
        } catch (IOException e) {
            // Absence is the common case here and not an error.
            // Real failures are logged but non-fatal, as the entry will be overwritten or ignored on the next pipeline run.
            // Note: if necessary, elevate recording the failures per storage or even more fine-grained, including the tag.
            logger.log(quietDeleteFails.incrementAndGetLevel(), e, () -> "Could not delete aux object " + auxTag);
        }
    }
    
    /**
     * Retrieve the storage interface for a given dataset.
     * <p>
     * Extracted to a static method to avoid repeating it in multiple places, allowing substitution
     * and extension to a StorageProvider functional interface (which is mockable on its own).
     */
    private static StorageIO<Dataset> storageFor(Dataset dataset) throws IOException {
        return DataAccess.getStorageIO(dataset);
    }
}
