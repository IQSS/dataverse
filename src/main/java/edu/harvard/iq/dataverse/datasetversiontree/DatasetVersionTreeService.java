package edu.harvard.iq.dataverse.datasetversiontree;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DatasetVersion;
import jakarta.ejb.Stateless;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonException;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Computes a single page of folders+files inside a folder of a dataset version.
 *
 * Uses two native SQL queries against the {@code filemetadata} table — a
 * {@code GROUP BY} on the prefix-stripped {@code directorylabel} for the
 * folder rows and a keyset-ordered scan of the rows whose
 * {@code directorylabel} matches the requested path for the file rows.
 * Both queries are driven by the same covering index
 * {@code ix_filemetadata_tree(datasetversion_id, directorylabel,
 * lower(label), datafile_id)} added in Flyway migration {@code V6.10.1.2}.
 *
 * Wire format and cursor *behaviour* are stable across the in-memory and
 * SQL implementations: {@code nextCursor} stays opaque to clients (clients
 * echo it back), and {@code Order} / {@code Include} keep the same wire
 * values.
 *
 * Folders come first (ascending or descending by name), then files (same
 * ordering with a stable {@code datafile_id} tie-break).
 *
 * Per-file {@code access} is one of {@code retentionExpired},
 * {@code restricted}, {@code embargoed}, {@code public} — precedence and
 * date-boundary semantics are defined once, in
 * {@link #ACCESS_CLASSIFIER_SQL}, shared by the file listing and the
 * folder rollup counts.
 */
@Stateless
public class DatasetVersionTreeService {

    public static final int DEFAULT_LIMIT = 100;
    public static final int MAX_LIMIT = 1000;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public enum Include {
        ALL, FOLDERS, FILES;

        public static Include fromQuery(String value) {
            if (value == null) {
                return ALL;
            }
            try {
                return Include.valueOf(value.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new InvalidQueryException("invalid include: " + value);
            }
        }
    }

    public enum Order {
        NAME_AZ, NAME_ZA;

        public static Order fromQuery(String value) {
            if (value == null) {
                return NAME_AZ;
            }
            switch (value) {
                case "NameAZ":
                    return NAME_AZ;
                case "NameZA":
                    return NAME_ZA;
                default:
                    throw new InvalidQueryException("invalid order: " + value);
            }
        }

        public String wireValue() {
            return this == NAME_AZ ? "NameAZ" : "NameZA";
        }
    }

    /**
     * Malformed client input (bad cursor, junk path, unknown enum value) —
     * the endpoint maps it to a 400. {@code @ApplicationException} is
     * load-bearing: without it the EJB container treats this unchecked
     * exception as a system exception when it escapes a business method of
     * this {@code @Stateless} bean, wraps it in {@code EJBException}, and
     * the endpoint's {@code catch (InvalidQueryException)} never matches —
     * turning every malformed cursor into a 500. No rollback: the service
     * only reads.
     */
    @jakarta.ejb.ApplicationException(rollback = false)
    public static class InvalidQueryException extends RuntimeException {
        public InvalidQueryException(String message) {
            super(message);
        }
    }

    public static class TreePage {
        public final String path;
        public final List<TreeItem> items;
        public final String nextCursor;
        public final int limit;
        public final Order order;
        public final Include include;
        public final int approximateCount;

        public TreePage(String path, List<TreeItem> items, String nextCursor,
                        int limit, Order order, Include include, int approximateCount) {
            this.path = path;
            this.items = items;
            this.nextCursor = nextCursor;
            this.limit = limit;
            this.order = order;
            this.include = include;
            this.approximateCount = approximateCount;
        }
    }

    public abstract static class TreeItem {
        public final String type;
        public final String name;
        public final String path;

        protected TreeItem(String type, String name, String path) {
            this.type = type;
            this.name = name;
            this.path = path;
        }
    }

    public static class FolderItem extends TreeItem {
        public final long fileCount;
        public final long folderCount;
        /**
         * Total bytes of all files in this folder's subtree (recursive,
         * matching {@link #fileCount}). Computed from {@code df.filesize},
         * i.e. the size of the bytes the default {@code downloadUrl} would
         * serve — for ingested tabular files that's the converted TSV, not
         * the original upload. Useful as a "downloading this folder = N GB"
         * UX hint; not authoritative under {@code originals=true}.
         */
        public final long bytes;
        /**
         * Subtree file counts per access bucket. The buckets are the
         * per-file {@code access} marker aggregated — mutually exclusive
         * by construction, precedence and date semantics defined once on
         * {@link #ACCESS_CLASSIFIER_SQL}. "Public" is implied:
         * {@code fileCount - restrictedCount - embargoedCount
         * - retentionExpiredCount}.
         */
        public final long restrictedCount;
        /** See {@link #restrictedCount}. */
        public final long embargoedCount;
        /** See {@link #restrictedCount}. */
        public final long retentionExpiredCount;

        public FolderItem(String name, String path,
                          long fileCount, long folderCount, long bytes,
                          long restrictedCount, long embargoedCount,
                          long retentionExpiredCount) {
            super("folder", name, path);
            this.fileCount = fileCount;
            this.folderCount = folderCount;
            this.bytes = bytes;
            this.restrictedCount = restrictedCount;
            this.embargoedCount = embargoedCount;
            this.retentionExpiredCount = retentionExpiredCount;
        }
    }

    public static class FileItem extends TreeItem {
        public final long id;
        public final long size;
        public final String contentType;
        public final String access;
        public final String checksumType;
        public final String checksumValue;
        public final String downloadUrl;

        public FileItem(long id, String name, String path, long size,
                        String contentType, String access,
                        String checksumType, String checksumValue,
                        String downloadUrl) {
            super("file", name, path);
            this.id = id;
            this.size = size;
            this.contentType = contentType;
            this.access = access;
            this.checksumType = checksumType;
            this.checksumValue = checksumValue;
            this.downloadUrl = downloadUrl;
        }
    }

    // ---------- Cursor ---------------------------------------------------

    /**
     * Opaque keyset cursor. Encoded as Base64-URL'd JSON; clients echo
     * back the raw string. The {@code phase} marker tells the server
     * whether the next page continues mid-folder-list or has crossed into
     * the file list; {@code keys} carries either the last folder name or
     * the {@code (label, datafile_id)} pair from the previous page. Keys
     * hold the <em>raw</em> name/label, never a lowercased copy: the SQL
     * applies its own {@code lower()} to both sides of the keyset
     * comparison, so ordering always follows the database's collation
     * rules rather than Java's (which disagree on non-ASCII input under
     * e.g. a C-collation database). {@code approximateCount} snapshots the
     * folder+file counts computed on the first page so later pages don't
     * re-aggregate a value that is constant across the walk; every minted
     * cursor carries it, and decoding rejects cursors without it (this
     * format has never shipped, so there is no legacy form to accept).
     */
    record TreeCursor(Phase phase, String lastFolderName, String lastFileLabel, Long lastFileId,
                      int approximateCount) {
        enum Phase { FOLDERS, FILES }

        static TreeCursor folders(String lastFolderName) {
            return new TreeCursor(Phase.FOLDERS, lastFolderName, null, null, -1);
        }

        static TreeCursor files(String lastFileLabel, long lastFileId) {
            return new TreeCursor(Phase.FILES, null, lastFileLabel, lastFileId, -1);
        }

        TreeCursor withApproximateCount(int count) {
            return new TreeCursor(phase, lastFolderName, lastFileLabel, lastFileId, count);
        }
    }

    // ---------- Public entry point --------------------------------------

    public TreePage listChildren(DatasetVersion version, String rawPath,
                                 Integer limitParam, String cursorParam,
                                 Include include, Order order, boolean originals) {
        Objects.requireNonNull(version);
        String path = normalizePath(rawPath);
        int limit = clampLimit(limitParam);
        TreeCursor cursor = decodeCursor(cursorParam);

        long versionId = version.getId();

        int remaining = limit;
        boolean hasMoreFolders = false;
        boolean hasMoreFiles = false;
        List<FolderItem> folderRows = new ArrayList<>();
        List<FileItem> fileRows = new ArrayList<>();
        TreeCursor next = null;

        // ---- folders ----------------------------------------------------
        if (include != Include.FILES && (cursor == null || cursor.phase() == TreeCursor.Phase.FOLDERS)) {
            String afterFolderName = cursor == null ? null : cursor.lastFolderName();
            List<FolderItem> rows = runFolderQuery(versionId, path, remaining + 1, afterFolderName, order);
            int take = Math.min(remaining, rows.size());
            hasMoreFolders = rows.size() > remaining;
            folderRows = rows.subList(0, take);
            remaining -= take;
            if (hasMoreFolders) {
                next = TreeCursor.folders(folderRows.get(take - 1).name);
            }
        }

        // ---- files ------------------------------------------------------
        // Runs whenever the folder listing is exhausted — even with no room
        // left on the page (remaining == 0, the folders-exactly-fill-a-page
        // boundary). The LIMIT-1 fetch is then the "is anything behind this
        // boundary" probe, asked through the exact same query the next page
        // will run, so the probe can never disagree with the listing.
        if (include != Include.FOLDERS && !hasMoreFolders) {
            String afterLabel = null;
            Long afterId = null;
            if (cursor != null && cursor.phase() == TreeCursor.Phase.FILES) {
                afterLabel = cursor.lastFileLabel();
                afterId = cursor.lastFileId();
            }
            List<FileItem> rows = runFileQuery(versionId, path, remaining + 1, afterLabel, afterId, order, originals);
            int take = Math.min(remaining, rows.size());
            hasMoreFiles = rows.size() > remaining;
            fileRows = rows.subList(0, take);
            if (hasMoreFiles) {
                // take == 0 is the boundary case: files exist but the page
                // was filled exactly by folders. Re-issue the folder cursor
                // — the next page's folder keyset query returns nothing and
                // falls into the files phase from the start. This keeps the
                // cursor wire format at exactly two states (FOLDERS/FILES
                // with their keys); no dedicated "files from the start"
                // state to mint, decode, or maintain.
                next = take > 0
                        ? TreeCursor.files(fileRows.get(take - 1).name, fileRows.get(take - 1).id)
                        : TreeCursor.folders(folderRows.get(folderRows.size() - 1).name);
            }
        }

        // ---- counts ------------------------------------------------------
        // approximateCount is a snapshot taken on the first page of a walk
        // and carried inside the cursor — it is constant across the walk by
        // contract. A phase that completed inside this page already IS its
        // exact count; only truncated or skipped phases need a COUNT query,
        // so the common single-page listing runs no aggregate queries at all.
        int approximateCount;
        if (cursor != null) {
            approximateCount = cursor.approximateCount();
        } else {
            int folderCount = include != Include.FILES && !hasMoreFolders
                    ? folderRows.size()
                    : countFolders(versionId, path);
            int fileCount = include != Include.FOLDERS && !hasMoreFolders && !hasMoreFiles
                    ? fileRows.size()
                    : countFiles(versionId, path);
            approximateCount = folderCount + fileCount;
        }

        String nextCursor = next == null ? null
                : encodeCursor(next.withApproximateCount(approximateCount));

        List<TreeItem> out = new ArrayList<>(folderRows.size() + fileRows.size());
        out.addAll(folderRows);
        out.addAll(fileRows);

        return new TreePage(path, out, nextCursor, limit, order, include, approximateCount);
    }

    // ---------- Access classification ------------------------------------

    // Wire values of the per-file access marker (also used as folder-count
    // bucket names). Fixed by the shipped dv-tree-view bundle's contract;
    // /files and search spell the equivalent states differently
    // (FileAccessStatus.RetentionPeriodExpired etc.).
    static final String ACCESS_RETENTION_EXPIRED = "retentionExpired";
    static final String ACCESS_RESTRICTED = "restricted";
    static final String ACCESS_EMBARGOED = "embargoed";
    static final String ACCESS_PUBLIC = "public";

    /**
     * Row-wise access classification, shared by the folder rollup (which
     * aggregates it into the per-bucket counts) and the file listing (which
     * selects it as the per-file {@code access} marker) — the single
     * encoding of the precedence contract: retention-expired wins (the file
     * cannot be served at all), then restricted, then actively embargoed,
     * else public. The buckets are therefore mutually exclusive by
     * construction and {@code public = files - restricted - embargoed -
     * retentionExpired} holds for every folder.
     *
     * Joined as {@code CROSS JOIN LATERAL} after the {@code embargo} and
     * {@code retention} LEFT JOINs it references. The date predicates
     * mirror the actual download enforcement in {@code FileUtil}
     * ({@code isRetentionExpired}: {@code dateunavailable < current_date};
     * {@code isActivelyEmbargoed}: {@code dateavailable > current_date}).
     * {@code /files}' access-status <em>filter</em> uses {@code >=} for the
     * embargo — an off-by-one against enforcement the tree deliberately
     * does not copy. NULL dates (no embargo/retention row behind the LEFT
     * JOIN) make their WHEN false and fall through — no IS NOT NULL guards
     * needed. NULL never reaches the output: the ELSE arm catches
     * everything.
     */
    private static final String ACCESS_CLASSIFIER_SQL =
            "CROSS JOIN LATERAL (SELECT CASE "
            + "WHEN r.dateunavailable < current_date THEN '" + ACCESS_RETENTION_EXPIRED + "' "
            + "WHEN df.restricted THEN '" + ACCESS_RESTRICTED + "' "
            + "WHEN e.dateavailable > current_date THEN '" + ACCESS_EMBARGOED + "' "
            + "ELSE '" + ACCESS_PUBLIC + "' END AS access) x ";

    /**
     * The database's current date — the clock every date predicate in this
     * service's SQL uses ({@code current_date}). Callers building cache
     * validators over this service's output must stamp them with this date,
     * not the JVM's: the two clocks can sit in different timezones, and a
     * validator on the wrong clock keeps confirming (304) a body whose
     * embargo/retention markers the SQL has already flipped.
     */
    public LocalDate currentDbDate() {
        java.sql.Date d = (java.sql.Date) em.createNativeQuery("SELECT current_date").getSingleResult();
        return d.toLocalDate();
    }

    // ---------- Folder query --------------------------------------------

    @SuppressWarnings("unchecked")
    private List<FolderItem> runFolderQuery(long versionId, String path, int limit,
                                             String afterFolderName, Order order) {
        // For the root case we strip nothing off the front of directorylabel;
        // for a nested path we strip "<path>/". The substring offset is
        // 1-indexed in Postgres, so the +1 is for the index and the "/" is
        // implicit in the LIKE pattern below.
        //
        // We use positional ?N parameters (not :name) because EclipseLink's
        // native-query parameter substitution requires the JPA positional
        // form; named parameters reach Postgres unsubstituted and produce
        // "syntax error at or near ':'". This matches the convention used
        // by every other native query in the codebase.
        boolean root = path.isEmpty();
        int substringFrom = root ? 1 : path.length() + 2; // 1-indexed; +1 for index, +1 for '/'

        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        sql.append("SELECT split_part(substring(fm.directorylabel FROM ").append(substringFrom).append("), '/', 1) AS folder_name, ");
        sql.append("       COUNT(*) AS files_under, ");
        sql.append("       COUNT(DISTINCT split_part(substring(fm.directorylabel FROM ").append(substringFrom).append("), '/', 2)) ");
        sql.append("           FILTER (WHERE position('/' IN substring(fm.directorylabel FROM ").append(substringFrom).append(")) > 0) AS subfolder_count, ");
        // SUM forces a heap visit on `datafile` for `filesize` (the covering
        // index `ix_filemetadata_tree` carries `datafile_id` but not
        // `filesize`). For typical version sizes this is sub-second; the
        // planner usually picks a hash join on `datafile_id`. If a future
        // perf issue surfaces on very large versions, consider an
        // INCLUDE(filesize) index on `datafile(id)` so the join becomes
        // index-only.
        sql.append("       COALESCE(SUM(df.filesize), 0) AS bytes_under, ");
        // Per-access counts aggregate the shared row classifier (see
        // ACCESS_CLASSIFIER_SQL for the precedence contract), so the folder
        // buckets are the same expression the per-file `access` marker
        // selects — they cannot drift apart.
        sql.append("       COUNT(*) FILTER (WHERE x.access = '").append(ACCESS_RESTRICTED).append("') AS restricted_count, ");
        sql.append("       COUNT(*) FILTER (WHERE x.access = '").append(ACCESS_EMBARGOED).append("') AS embargoed_count, ");
        sql.append("       COUNT(*) FILTER (WHERE x.access = '").append(ACCESS_RETENTION_EXPIRED).append("') AS retentionexpired_count ");
        sql.append("FROM filemetadata fm JOIN datafile df ON df.id = fm.datafile_id ");
        // Small, FK-indexed; hash joins in practice.
        sql.append("                     LEFT JOIN embargo e ON df.embargo_id = e.id ");
        sql.append("                     LEFT JOIN retention r ON df.retention_id = r.id ");
        sql.append(ACCESS_CLASSIFIER_SQL);
        sql.append("WHERE fm.datasetversion_id = ?").append(params.size() + 1).append(" ");
        params.add(versionId);
        if (root) {
            sql.append("  AND fm.directorylabel IS NOT NULL AND fm.directorylabel <> '' ");
        } else {
            // LIKE with the user-provided path needs to escape SQL wildcard
            // characters, otherwise a folder named `a_b` would also match
            // `axb/...`, and `%foo` would match arbitrary prefixes — both
            // crossing into sibling subtrees. Escape `\`, `%`, `_` and add
            // an explicit ESCAPE clause; PostgreSQL's default escape char is
            // already `\`, but stating it makes the contract local.
            sql.append("  AND fm.directorylabel LIKE ?").append(params.size() + 1).append(" ESCAPE '\\' ");
            params.add(escapeLikePattern(path) + "/%");
        }
        if (afterFolderName != null) {
            // Two-key keyset comparison. Folder names are aggregations, so
            // there's no per-row id to break ties on; siblings whose names
            // differ only in case (`Data/` vs `data/`) collide on the
            // primary `lower(name)` ordering. We add the case-sensitive
            // name as a deterministic secondary key. The matching ORDER BY
            // below carries the same `(lower, cs)` tuple so the keyset
            // walk is stable. The cursor carries the raw folder name and
            // the lowering happens in SQL — Java's toLowerCase and
            // Postgres's lower() disagree on non-ASCII input under some
            // collations (e.g. C), and a mismatch here skips or repeats
            // rows at page boundaries.
            String cmp = order == Order.NAME_ZA ? "<" : ">";
            sql.append("  AND (lower(split_part(substring(fm.directorylabel FROM ").append(substringFrom).append("), '/', 1)) ").append(cmp).append(" lower(?").append(params.size() + 1).append(") ")
                    .append("       OR (lower(split_part(substring(fm.directorylabel FROM ").append(substringFrom).append("), '/', 1)) = lower(?").append(params.size() + 1).append(") ")
                    .append("           AND split_part(substring(fm.directorylabel FROM ").append(substringFrom).append("), '/', 1) ").append(cmp).append(" ?").append(params.size() + 2).append(")) ");
            params.add(afterFolderName);
            params.add(afterFolderName);
        }
        // Repeat the expression in GROUP BY / ORDER BY rather than referring
        // to the `folder_name` SELECT alias. PostgreSQL allows the bare
        // alias in GROUP BY (extension) but does NOT allow it inside an
        // expression in ORDER BY — `ORDER BY lower(folder_name)` errors
        // with `column "folder_name" does not exist`. Repeating the
        // expression is also SQL-spec-clean.
        sql.append("GROUP BY split_part(substring(fm.directorylabel FROM ").append(substringFrom).append("), '/', 1) ");
        String dir = order == Order.NAME_ZA ? "DESC" : "ASC";
        sql.append("ORDER BY lower(split_part(substring(fm.directorylabel FROM ").append(substringFrom).append("), '/', 1)) ").append(dir).append(", ");
        sql.append("         split_part(substring(fm.directorylabel FROM ").append(substringFrom).append("), '/', 1) ").append(dir).append(" ");
        sql.append("LIMIT ?").append(params.size() + 1);
        params.add(limit);

        Query q = em.createNativeQuery(sql.toString());
        for (int i = 0; i < params.size(); i++) {
            q.setParameter(i + 1, params.get(i));
        }

        List<Object[]> rows = q.getResultList();
        List<FolderItem> out = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            String folderName = (String) row[0];
            long filesUnder = ((Number) row[1]).longValue();
            long subfolderCount = ((Number) row[2]).longValue();
            // COALESCE in the SUM guarantees this is non-null, but the
            // null-safe read keeps us robust if a driver ever surfaces 0
            // rows in a way that bypasses the COALESCE.
            long bytesUnder = row[3] == null ? 0L : ((Number) row[3]).longValue();
            long restrictedUnder = row[4] == null ? 0L : ((Number) row[4]).longValue();
            long embargoedUnder = row[5] == null ? 0L : ((Number) row[5]).longValue();
            long retentionExpiredUnder = row[6] == null ? 0L : ((Number) row[6]).longValue();
            String folderPath = root ? folderName : path + "/" + folderName;
            out.add(new FolderItem(folderName, folderPath,
                    filesUnder, subfolderCount, bytesUnder,
                    restrictedUnder, embargoedUnder, retentionExpiredUnder));
        }
        return out;
    }

    // ---------- File query ----------------------------------------------

    @SuppressWarnings("unchecked")
    private List<FileItem> runFileQuery(long versionId, String path, int limit,
                                         String afterLabel, Long afterId,
                                         Order order, boolean originals) {
        boolean root = path.isEmpty();

        // Positional ?N parameters per the EclipseLink native-query convention;
        // see the comment in `runFolderQuery` for why named parameters cannot
        // be used here.
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        // The `originals` flag is bound first so the CASE expression in the
        // SELECT clause can refer to it as ?1 without depending on the rest of
        // the parameter assembly. We use it twice to gate both checksum
        // columns symmetrically — see the comment on the CASE expressions.
        int originalsSlot = params.size() + 1;
        params.add(originals);
        // The per-file `access` marker is the shared row classifier's label
        // (see ACCESS_CLASSIFIER_SQL for the precedence and date-boundary
        // contract) — the same expression the folder rollup aggregates.
        // size: served-form (df.filesize) by default. For ingested tabular files
        // under originals=true, return the saved-original's size (dt.originalfilesize)
        // so the reported size matches the bytes the ?format=original downloadUrl
        // serves — clients (e.g. rclone) verify transfer size against this.
        sql.append("SELECT fm.label, df.id, ");
        sql.append("       CASE WHEN dt.id IS NOT NULL AND ?").append(originalsSlot)
           .append(" THEN COALESCE(dt.originalfilesize, df.filesize) ELSE df.filesize END, ");
        sql.append("       df.contenttype, x.access, ");
        // For ingested tabular files (those with an associated `datatable` row),
        // `df.checksumvalue` is the digest of the *original upload* — Dataverse
        // never persists a digest of the converted TSV that the default
        // `downloadUrl` actually serves. Returning that digest as `checksum`
        // would silently lie to clients that try to verify the bytes they
        // receive. To keep the contract honest, both checksum fields are
        // NULLed for ingested-tabular rows whose `downloadUrl` resolves to
        // the converted form. When `originals=true` the URL switches to
        // `?format=original`, which serves the saved-original auxiliary blob;
        // that file's bytes match `df.checksumvalue` so we keep the digest.
        sql.append("       CASE WHEN dt.id IS NOT NULL AND NOT ?").append(originalsSlot).append(" THEN NULL ELSE df.checksumtype  END, ");
        sql.append("       CASE WHEN dt.id IS NOT NULL AND NOT ?").append(originalsSlot).append(" THEN NULL ELSE df.checksumvalue END ");
        sql.append("FROM filemetadata fm JOIN datafile df ON fm.datafile_id = df.id ");
        sql.append("                     LEFT JOIN embargo e ON df.embargo_id = e.id ");
        sql.append("                     LEFT JOIN retention r ON df.retention_id = r.id ");
        // Single LEFT JOIN on `datatable.datafile_id` (already FK-indexed).
        // The join exists on the file's row regardless of cursor / ordering,
        // so it does not break the keyset's index plan.
        sql.append("                     LEFT JOIN datatable dt ON dt.datafile_id = df.id ");
        sql.append(ACCESS_CLASSIFIER_SQL);
        sql.append("WHERE fm.datasetversion_id = ?").append(params.size() + 1).append(" ");
        params.add(versionId);
        if (root) {
            sql.append("  AND (fm.directorylabel IS NULL OR fm.directorylabel = '') ");
        } else {
            sql.append("  AND fm.directorylabel = ?").append(params.size() + 1).append(" ");
            params.add(path);
        }
        if (afterLabel != null && afterId != null) {
            String cmp = order == Order.NAME_ZA ? "<" : ">";
            // Standard tuple-keyset pattern: (lower(label), datafile_id) compared lexicographically.
            // afterLabel needs two positional slots — the value is the same in both, but EclipseLink's
            // native-query binding wants each occurrence as its own ?N. The cursor carries the raw
            // label and both sides are lowered by Postgres itself (`lower(?)`), so the comparison
            // can never disagree with the `ORDER BY lower(fm.label)` collation the walk relies on —
            // Java's toLowerCase produces different results for non-ASCII input under e.g. a
            // C-collation database, which would skip or repeat rows at page boundaries.
            int afterLabelLeft = params.size() + 1;
            params.add(afterLabel);
            int afterLabelRight = params.size() + 1;
            params.add(afterLabel);
            int afterIdSlot = params.size() + 1;
            params.add(afterId);
            sql.append("  AND (lower(fm.label) ").append(cmp).append(" lower(?").append(afterLabelLeft).append(") ");
            sql.append("       OR (lower(fm.label) = lower(?").append(afterLabelRight).append(") AND df.id ").append(cmp).append(" ?").append(afterIdSlot).append(")) ");
        }
        sql.append("ORDER BY lower(fm.label) ").append(order == Order.NAME_ZA ? "DESC" : "ASC")
           .append(", df.id ").append(order == Order.NAME_ZA ? "DESC" : "ASC").append(" ");
        sql.append("LIMIT ?").append(params.size() + 1);
        params.add(limit);

        Query q = em.createNativeQuery(sql.toString());
        for (int i = 0; i < params.size(); i++) {
            q.setParameter(i + 1, params.get(i));
        }

        List<Object[]> rows = q.getResultList();
        List<FileItem> out = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            String label = (String) row[0];
            long id = ((Number) row[1]).longValue();
            long size = row[2] == null ? 0L : ((Number) row[2]).longValue();
            String contentType = (String) row[3];
            String access = (String) row[4];
            String checksumTypeRaw = (String) row[5];
            String checksumValue = (String) row[6];

            // The column stores the enum *name* ("SHA1"); every other API
            // response emits the display label ("SHA-1") via
            // ChecksumType#toString, so map before exposing.
            String checksumType = checksumTypeRaw == null ? null
                    : DataFile.ChecksumType.valueOf(checksumTypeRaw).toString();
            String filePath = root ? label : path + "/" + label;
            String downloadUrl = "/api/access/datafile/" + id + (originals ? "?format=original" : "");

            out.add(new FileItem(id, label, filePath, size, contentType, access, checksumType, checksumValue, downloadUrl));
        }
        return out;
    }

    // ---------- Counts (for approximateCount on the page envelope) -------

    private int countFolders(long versionId, String path) {
        boolean root = path.isEmpty();
        int substringFrom = root ? 1 : path.length() + 2;

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(DISTINCT split_part(substring(fm.directorylabel FROM ").append(substringFrom).append("), '/', 1)) ");
        sql.append("FROM filemetadata fm ");
        sql.append("WHERE fm.datasetversion_id = ?1 ");
        if (root) {
            sql.append("  AND fm.directorylabel IS NOT NULL AND fm.directorylabel <> '' ");
        } else {
            // See `runFolderQuery` — LIKE wildcards in the path must be escaped
            // or `a_b` cross-matches sibling `axb/...` etc.
            sql.append("  AND fm.directorylabel LIKE ?2 ESCAPE '\\' ");
        }

        Query q = em.createNativeQuery(sql.toString());
        q.setParameter(1, versionId);
        if (!root) {
            q.setParameter(2, escapeLikePattern(path) + "/%");
        }
        Number n = (Number) q.getSingleResult();
        return n == null ? 0 : n.intValue();
    }

    /**
     * Escapes the three characters PostgreSQL's `LIKE` operator treats
     * specially when the default escape character (`\`) is in effect: `\`,
     * `%`, `_`. Folder names in `directorylabel` are user-controlled,
     * and we use them as the literal prefix of a `LIKE ? + '/%'` pattern,
     * so any of those characters would otherwise expand the matched
     * subtree past what the cursor describes.
     */
    static String escapeLikePattern(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        StringBuilder out = new StringBuilder(s.length() + 4);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' || c == '%' || c == '_') {
                out.append('\\');
            }
            out.append(c);
        }
        return out.toString();
    }

    private int countFiles(long versionId, String path) {
        boolean root = path.isEmpty();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM filemetadata fm ");
        sql.append("WHERE fm.datasetversion_id = ?1 ");
        if (root) {
            sql.append("  AND (fm.directorylabel IS NULL OR fm.directorylabel = '') ");
        } else {
            sql.append("  AND fm.directorylabel = ?2 ");
        }

        Query q = em.createNativeQuery(sql.toString());
        q.setParameter(1, versionId);
        if (!root) {
            q.setParameter(2, path);
        }
        Number n = (Number) q.getSingleResult();
        return n == null ? 0 : n.intValue();
    }

    // ---------- Path normalisation --------------------------------------

    /**
     * Normalizes a requested path so it matches what the write side stores.
     * FileMetadata#setDirectoryLabel runs every stored directorylabel through
     * StringUtil.sanitizeFileDirectory: slash/backslash runs collapse into
     * "/", and '/', '-', '.', ' ' are stripped from the two ENDS of the whole
     * label. That means a stored label's first segment never starts with
     * those characters — so stripping them from the front of a query aligns
     * it with storage (".hidden" was stored as "hidden"). The TAIL is
     * different: a folder path names a PREFIX of stored labels, and interior
     * segments legitimately end with '.', ' ' or '-' ("data./sub" is
     * storable, and the tree emits its parent folder as path "data."), so a
     * trailing strip would break the round-trip of the server's own emitted
     * paths. Only trailing slashes (impossible in storage) are dropped.
     *
     * A non-blank path that normalizes to nothing ("..", "-", ". ") names
     * nothing storable and is rejected — mapping it to the root listing
     * would alias junk onto real content.
     *
     * @throws InvalidQueryException for non-blank input that normalizes away
     */
    public static String normalizePath(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String p = raw.replaceAll("[\\\\/]+", "/");
        if (p.chars().allMatch(c -> c == '/')) {
            return ""; // "/", "//" — explicit spellings of the root
        }
        // Strip the leading run over the FULL junk set in one loop, exactly
        // like the write-side sanitizer: stripping one class can expose
        // another ("./data" → "data", "../." → nothing), and a partial
        // strip would make this function non-idempotent — the endpoint
        // normalizes once up front and the service normalizes again, and
        // the two results must be identical.
        int start = 0;
        while (start < p.length()) {
            char c = p.charAt(start);
            if (c == '/' || c == '.' || c == '-' || c == ' ') {
                start++;
            } else {
                break;
            }
        }
        p = p.substring(start);
        // Post-collapse there is at most one trailing slash.
        if (p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        if (p.isEmpty()) {
            throw new InvalidQueryException("invalid path: " + raw);
        }
        return p;
    }

    public static int clampLimit(Integer requested) {
        if (requested == null || requested <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(requested, MAX_LIMIT);
    }

    // ---------- Cursor encoding -----------------------------------------
    //
    // Wire shape: Base64-URL'd JSON. The format is opaque to clients —
    // they only echo it back. Round-trip stability is asserted by the
    // keyset IT.

    // Wire-format markers for the cursor's phase. Spelled out in full so a
    // proxy that case-folds query strings (or someone hand-editing a cursor
    // for debugging) can't silently swap one phase for the other; an
    // unrecognised value falls through to the generic "invalid cursor" path.
    private static final String CURSOR_PHASE_FOLDERS = "FOLDERS";
    private static final String CURSOR_PHASE_FILES = "FILES";

    static String encodeCursor(TreeCursor cursor) {
        JsonArrayBuilder keys = Json.createArrayBuilder();
        if (cursor.phase() == TreeCursor.Phase.FOLDERS) {
            keys.add(cursor.lastFolderName());
        } else {
            keys.add(cursor.lastFileLabel());
            keys.add(cursor.lastFileId());
        }
        String json = Json.createObjectBuilder()
                .add("p", cursor.phase() == TreeCursor.Phase.FOLDERS ? CURSOR_PHASE_FOLDERS : CURSOR_PHASE_FILES)
                .add("k", keys)
                .add("c", cursor.approximateCount())
                .build().toString();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    static TreeCursor decodeCursor(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(raw), StandardCharsets.UTF_8);
            try (JsonReader reader = Json.createReader(new StringReader(decoded))) {
                JsonObject obj = reader.readObject();
                String phaseStr = obj.getString("p", null);
                JsonArray keys = obj.getJsonArray("k");
                // Every minted cursor carries the count snapshot; a cursor
                // without one (or with a negative one) was not produced by
                // this server. The format has never shipped, so there is no
                // legacy cursor shape to grandfather in — reject as invalid
                // ("invalid or stale cursors yield 400" is the documented
                // contract).
                int approximateCount = obj.getInt("c", -1);
                if (phaseStr == null || keys == null || approximateCount < 0) {
                    throw new InvalidQueryException("invalid cursor");
                }
                if (CURSOR_PHASE_FOLDERS.equals(phaseStr) && keys.size() == 1) {
                    return new TreeCursor(TreeCursor.Phase.FOLDERS,
                            ((JsonString) keys.get(0)).getString(), null, null, approximateCount);
                }
                if (CURSOR_PHASE_FILES.equals(phaseStr) && keys.size() == 2) {
                    String label = ((JsonString) keys.get(0)).getString();
                    long id = ((JsonNumber) keys.get(1)).longValueExact();
                    return new TreeCursor(TreeCursor.Phase.FILES, null, label, id, approximateCount);
                }
                throw new InvalidQueryException("invalid cursor");
            }
        } catch (IllegalArgumentException | JsonException | ClassCastException | ArithmeticException ex) {
            // IllegalArgumentException: not Base64. JsonException: not JSON,
            // or not a JSON object. ClassCastException: right shape but wrong
            // member types (e.g. {"k":[1]} — a number where the label string
            // belongs). ArithmeticException: fractional or overflowing id.
            // All of these are malformed client input and must surface as a
            // 400 via InvalidQueryException, never as a 500.
            throw new InvalidQueryException("invalid cursor");
        }
    }
}
