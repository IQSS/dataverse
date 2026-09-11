package edu.harvard.iq.dataverse.datasetversiontree;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DatasetVersion;
import jakarta.ejb.Stateless;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
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
 * One page of the folders and files directly inside a folder of a dataset
 * version. Two native queries over {@code filemetadata}, both served by the
 * covering index {@code ix_filemetadata_tree} (migration {@code V6.10.1.2}):
 * a {@code GROUP BY} on the first path segment for folders, then a keyset
 * scan for files. Folders sort before files; {@code nextCursor} is opaque.
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
     * Malformed client input; the endpoint maps it to a 400. Declared an
     * application exception so the container does not wrap it in
     * {@code EJBException}, which would turn a bad cursor into a 500.
     */
    @jakarta.ejb.ApplicationException(rollback = false)
    public static class InvalidQueryException extends RuntimeException {
        public InvalidQueryException(String message) {
            super(message);
        }
    }

    /** The client's query, as received; {@code path} is normalized by the service. */
    public record TreeQuery(String path, Integer limit, String cursor,
                            Include include, Order order, boolean originals) {
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
        /** Subtree bytes as served by the default {@code downloadUrl}; for ingested tabular files that is the TSV. */
        public final long bytes;
        /** Subtree file counts per access bucket; public is {@code fileCount} minus the three. */
        public final long restrictedCount;
        public final long embargoedCount;
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

    /**
     * Keyset cursor. Keys hold the raw name or label; the SQL lowers both
     * sides itself, so ordering follows the database collation rather than
     * Java's. {@code approximateCount} is snapshotted on the first page.
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

    private record Slice<T>(List<T> rows, boolean hasMore) {
        static <T> Slice<T> of(List<T> fetched, int wanted) {
            return new Slice<>(fetched.subList(0, Math.min(wanted, fetched.size())), fetched.size() > wanted);
        }

        static <T> Slice<T> empty() {
            return new Slice<>(List.of(), false);
        }

        T last() {
            return rows.get(rows.size() - 1);
        }
    }

    public TreePage listChildren(DatasetVersion version, TreeQuery query) {
        Objects.requireNonNull(version);
        String path = normalizePath(query.path());
        int limit = clampLimit(query.limit());
        TreeCursor cursor = decodeCursor(query.cursor());
        long versionId = version.getId();

        Slice<FolderItem> folders = Slice.empty();
        if (query.include() != Include.FILES && (cursor == null || cursor.phase() == TreeCursor.Phase.FOLDERS)) {
            String after = cursor == null ? null : cursor.lastFolderName();
            folders = Slice.of(runFolderQuery(versionId, path, limit + 1, after, query.order()), limit);
        }

        // The file query also runs with no room left on the page: its LIMIT 1
        // probe is what decides whether a cursor is needed at the boundary.
        int remaining = limit - folders.rows().size();
        Slice<FileItem> files = Slice.empty();
        if (query.include() != Include.FOLDERS && !folders.hasMore()) {
            boolean inFiles = cursor != null && cursor.phase() == TreeCursor.Phase.FILES;
            String afterLabel = inFiles ? cursor.lastFileLabel() : null;
            Long afterId = inFiles ? cursor.lastFileId() : null;
            files = Slice.of(runFileQuery(versionId, path, remaining + 1, afterLabel, afterId,
                    query.order(), query.originals()), remaining);
        }

        int approximateCount = cursor != null
                ? cursor.approximateCount()
                : countAll(versionId, path, query.include(), folders, files);
        TreeCursor next = nextCursor(folders, files);
        String nextCursor = next == null ? null : encodeCursor(next.withApproximateCount(approximateCount));

        List<TreeItem> out = new ArrayList<>(folders.rows().size() + files.rows().size());
        out.addAll(folders.rows());
        out.addAll(files.rows());
        return new TreePage(path, out, nextCursor, limit, query.order(), query.include(), approximateCount);
    }

    /**
     * When folders fill the page exactly and files remain, the folder cursor
     * is re-issued: the next page's folder query returns nothing and falls
     * through to the files, so the cursor never needs a third state.
     */
    private static TreeCursor nextCursor(Slice<FolderItem> folders, Slice<FileItem> files) {
        if (folders.hasMore()) {
            return TreeCursor.folders(folders.last().name);
        }
        if (files.hasMore()) {
            return files.rows().isEmpty()
                    ? TreeCursor.folders(folders.last().name)
                    : TreeCursor.files(files.last().name, files.last().id);
        }
        return null;
    }

    /** A phase that completed on this page is its own exact count; only truncated or skipped phases need a query. */
    private int countAll(long versionId, String path, Include include,
                         Slice<FolderItem> folders, Slice<FileItem> files) {
        int folderCount = include != Include.FILES && !folders.hasMore()
                ? folders.rows().size()
                : countFolders(versionId, path);
        int fileCount = include != Include.FOLDERS && !folders.hasMore() && !files.hasMore()
                ? files.rows().size()
                : countFiles(versionId, path);
        return folderCount + fileCount;
    }

    // Wire values of the per-file access marker, fixed by the dv-tree-view bundle's contract.
    static final String ACCESS_RETENTION_EXPIRED = "retentionExpired";
    static final String ACCESS_RESTRICTED = "restricted";
    static final String ACCESS_EMBARGOED = "embargoed";
    static final String ACCESS_PUBLIC = "public";

    /**
     * Per-row access classification shared by both queries: retention
     * expired wins, then restricted, then embargoed, else public. The date
     * predicates mirror download enforcement in {@code FileUtil}.
     */
    private static final String ACCESS_CLASSIFIER_SQL =
            "CROSS JOIN LATERAL (SELECT CASE "
            + "WHEN r.dateunavailable < current_date THEN '" + ACCESS_RETENTION_EXPIRED + "' "
            + "WHEN df.restricted THEN '" + ACCESS_RESTRICTED + "' "
            + "WHEN e.dateavailable > current_date THEN '" + ACCESS_EMBARGOED + "' "
            + "ELSE '" + ACCESS_PUBLIC + "' END AS access) x ";

    private static final String ACCESS_JOINS =
            "                     LEFT JOIN embargo e ON df.embargo_id = e.id "
            + "                     LEFT JOIN retention r ON df.retention_id = r.id ";

    /**
     * The clock every date predicate in this service's SQL uses. Cache
     * validators over this output must be stamped with it, not the JVM's.
     */
    public LocalDate currentDbDate() {
        java.sql.Date d = (java.sql.Date) em.createNativeQuery("SELECT current_date").getSingleResult();
        return d.toLocalDate();
    }

    /** The first path segment of {@code directorylabel} below the listed folder. */
    private static String folderNameExpr(int substringFrom) {
        return "split_part(substring(fm.directorylabel FROM " + substringFrom + "), '/', 1)";
    }

    private static String accessCountColumn(String bucket, String alias) {
        return "       COUNT(*) FILTER (WHERE x.access = '" + bucket + "') AS " + alias;
    }

    // Native queries use positional ?N parameters: EclipseLink does not
    // substitute named parameters in native SQL.
    @SuppressWarnings("unchecked")
    private List<FolderItem> runFolderQuery(long versionId, String path, int limit,
                                             String afterFolderName, Order order) {
        boolean root = path.isEmpty();
        int substringFrom = root ? 1 : path.length() + 2;
        String folderName = folderNameExpr(substringFrom);
        String dir = order == Order.NAME_ZA ? "DESC" : "ASC";
        String cmp = order == Order.NAME_ZA ? "<" : ">";

        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        sql.append("SELECT ").append(folderName).append(" AS folder_name, ");
        sql.append("       COUNT(*) AS files_under, ");
        sql.append("       COUNT(DISTINCT split_part(substring(fm.directorylabel FROM ").append(substringFrom).append("), '/', 2)) ");
        sql.append("           FILTER (WHERE position('/' IN substring(fm.directorylabel FROM ").append(substringFrom).append(")) > 0) AS subfolder_count, ");
        sql.append("       COALESCE(SUM(df.filesize), 0) AS bytes_under, ");
        sql.append(accessCountColumn(ACCESS_RESTRICTED, "restricted_count")).append(", ");
        sql.append(accessCountColumn(ACCESS_EMBARGOED, "embargoed_count")).append(", ");
        sql.append(accessCountColumn(ACCESS_RETENTION_EXPIRED, "retentionexpired_count")).append(" ");
        sql.append("FROM filemetadata fm JOIN datafile df ON df.id = fm.datafile_id ");
        sql.append(ACCESS_JOINS);
        sql.append(ACCESS_CLASSIFIER_SQL);
        sql.append("WHERE fm.datasetversion_id = ?").append(params.size() + 1).append(" ");
        params.add(versionId);
        if (root) {
            sql.append("  AND fm.directorylabel IS NOT NULL AND fm.directorylabel <> '' ");
        } else {
            sql.append("  AND fm.directorylabel LIKE ?").append(params.size() + 1).append(" ESCAPE '\\' ");
            params.add(escapeLikePattern(path) + "/%");
        }
        if (afterFolderName != null) {
            // Case-insensitive primary key with the raw name as tiebreak, so
            // siblings differing only in case cannot collide at a page boundary.
            int p1 = params.size() + 1;
            int p2 = params.size() + 2;
            sql.append("  AND (lower(").append(folderName).append(") ").append(cmp).append(" lower(?").append(p1).append(") ")
                    .append("       OR (lower(").append(folderName).append(") = lower(?").append(p1).append(") ")
                    .append("           AND ").append(folderName).append(" ").append(cmp).append(" ?").append(p2).append(")) ");
            params.add(afterFolderName);
            params.add(afterFolderName);
        }
        // Repeated rather than aliased: PostgreSQL rejects an alias inside an ORDER BY expression.
        sql.append("GROUP BY ").append(folderName).append(" ");
        sql.append("ORDER BY lower(").append(folderName).append(") ").append(dir).append(", ")
                .append(folderName).append(" ").append(dir).append(" ");
        sql.append("LIMIT ?").append(params.size() + 1);
        params.add(limit);

        List<Object[]> rows = bind(sql, params).getResultList();
        List<FolderItem> out = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            out.add(toFolderItem(row, path));
        }
        return out;
    }

    private static FolderItem toFolderItem(Object[] row, String path) {
        String folderName = (String) row[0];
        return new FolderItem(folderName, joinPath(path, folderName),
                longAt(row, 1), longAt(row, 2), longAt(row, 3),
                longAt(row, 4), longAt(row, 5), longAt(row, 6));
    }

    @SuppressWarnings("unchecked")
    private List<FileItem> runFileQuery(long versionId, String path, int limit,
                                         String afterLabel, Long afterId,
                                         Order order, boolean originals) {
        boolean root = path.isEmpty();
        String dir = order == Order.NAME_ZA ? "DESC" : "ASC";
        String cmp = order == Order.NAME_ZA ? "<" : ">";

        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        int originalsSlot = params.size() + 1;
        params.add(originals);
        // For ingested tabular files the stored checksum belongs to the
        // original upload, not the converted TSV the default download serves,
        // so it is only reported when originals=true switches the URL to the
        // original. Size follows the same rule.
        sql.append("SELECT fm.label, df.id, ");
        sql.append("       CASE WHEN dt.id IS NOT NULL AND ?").append(originalsSlot)
           .append(" THEN COALESCE(dt.originalfilesize, df.filesize) ELSE df.filesize END, ");
        sql.append("       df.contenttype, x.access, ");
        sql.append("       CASE WHEN dt.id IS NOT NULL AND NOT ?").append(originalsSlot).append(" THEN NULL ELSE df.checksumtype  END, ");
        sql.append("       CASE WHEN dt.id IS NOT NULL AND NOT ?").append(originalsSlot).append(" THEN NULL ELSE df.checksumvalue END ");
        sql.append("FROM filemetadata fm JOIN datafile df ON fm.datafile_id = df.id ");
        sql.append(ACCESS_JOINS);
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
            int p1 = params.size() + 1;
            int p2 = params.size() + 2;
            int p3 = params.size() + 3;
            params.add(afterLabel);
            params.add(afterLabel);
            params.add(afterId);
            sql.append("  AND (lower(fm.label) ").append(cmp).append(" lower(?").append(p1).append(") ");
            sql.append("       OR (lower(fm.label) = lower(?").append(p2).append(") AND df.id ").append(cmp).append(" ?").append(p3).append(")) ");
        }
        sql.append("ORDER BY lower(fm.label) ").append(dir).append(", df.id ").append(dir).append(" ");
        sql.append("LIMIT ?").append(params.size() + 1);
        params.add(limit);

        List<Object[]> rows = bind(sql, params).getResultList();
        List<FileItem> out = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            out.add(toFileItem(row, path, originals));
        }
        return out;
    }

    private static FileItem toFileItem(Object[] row, String path, boolean originals) {
        String label = (String) row[0];
        long id = longAt(row, 1);
        String checksumTypeName = (String) row[5];
        // The column holds the enum name ("SHA1"); the API emits the label ("SHA-1").
        String checksumType = checksumTypeName == null ? null
                : DataFile.ChecksumType.valueOf(checksumTypeName).toString();
        String downloadUrl = "/api/access/datafile/" + id + (originals ? "?format=original" : "");
        return new FileItem(id, label, joinPath(path, label), longAt(row, 2),
                (String) row[3], (String) row[4], checksumType, (String) row[6], downloadUrl);
    }

    private Query bind(StringBuilder sql, List<Object> params) {
        Query q = em.createNativeQuery(sql.toString());
        for (int i = 0; i < params.size(); i++) {
            q.setParameter(i + 1, params.get(i));
        }
        return q;
    }

    private static long longAt(Object[] row, int index) {
        return row[index] == null ? 0L : ((Number) row[index]).longValue();
    }

    private static String joinPath(String parent, String name) {
        return parent.isEmpty() ? name : parent + '/' + name;
    }

    private int countFolders(long versionId, String path) {
        boolean root = path.isEmpty();
        int substringFrom = root ? 1 : path.length() + 2;

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(DISTINCT ").append(folderNameExpr(substringFrom)).append(") ");
        sql.append("FROM filemetadata fm ");
        sql.append("WHERE fm.datasetversion_id = ?1 ");
        if (root) {
            sql.append("  AND fm.directorylabel IS NOT NULL AND fm.directorylabel <> '' ");
        } else {
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
     * Folder names are user input used as the literal prefix of a LIKE
     * pattern; unescaped {@code \}, {@code %} or {@code _} would match
     * sibling subtrees.
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

    /**
     * Matches what the write side stores: slash runs collapse, and the
     * leading junk that {@code StringUtil.sanitizeFileDirectory} strips is
     * stripped here too. Only trailing slashes are dropped from the end,
     * since a folder path is a prefix and inner segments may end in '.',
     * ' ' or '-'. Non-blank input that normalizes to nothing is rejected.
     *
     * @throws InvalidQueryException for non-blank input that normalizes away
     */
    public static String normalizePath(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String p = raw.replaceAll("[\\\\/]+", "/");
        if (p.chars().allMatch(c -> c == '/')) {
            return "";
        }
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

    private static final String CURSOR_PHASE_FOLDERS = "FOLDERS";
    private static final String CURSOR_PHASE_FILES = "FILES";

    static String encodeCursor(TreeCursor cursor) {
        JsonArrayBuilder keys = JsonUtil.createArrayBuilder();
        if (cursor.phase() == TreeCursor.Phase.FOLDERS) {
            keys.add(cursor.lastFolderName());
        } else {
            keys.add(cursor.lastFileLabel());
            keys.add(cursor.lastFileId());
        }
        String json = JsonUtil.createObjectBuilder()
                .add("p", cursor.phase() == TreeCursor.Phase.FOLDERS ? CURSOR_PHASE_FOLDERS : CURSOR_PHASE_FILES)
                .add("k", keys)
                .add("c", cursor.approximateCount())
                .build().toString();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    /** Anything that is not a cursor this server minted is a 400, never a 500. */
    static TreeCursor decodeCursor(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(raw), StandardCharsets.UTF_8);
            try (JsonReader reader = JsonUtil.createReader(new StringReader(decoded))) {
                JsonObject obj = reader.readObject();
                String phaseStr = obj.getString("p", null);
                JsonArray keys = obj.getJsonArray("k");
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
            throw new InvalidQueryException("invalid cursor");
        }
    }
}
