package edu.harvard.iq.dataverse.datasetversiontree;

import edu.harvard.iq.dataverse.DatasetVersion;
import jakarta.ejb.Stateless;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
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

        public FolderItem(String name, String path, long fileCount, long folderCount) {
            super("folder", name, path);
            this.fileCount = fileCount;
            this.folderCount = folderCount;
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
     * the {@code (lower(label), datafile_id)} pair from the previous page.
     */
    record TreeCursor(Phase phase, String lastFolderName, String lastFileLabelLower, Long lastFileId) {
        enum Phase { FOLDERS, FILES }

        static TreeCursor folders(String lastFolderName) {
            return new TreeCursor(Phase.FOLDERS, lastFolderName, null, null);
        }

        static TreeCursor files(String lastFileLabelLower, long lastFileId) {
            return new TreeCursor(Phase.FILES, null, lastFileLabelLower, lastFileId);
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
        List<FolderItem> folderRows = new ArrayList<>();
        List<FileItem> fileRows = new ArrayList<>();
        String nextCursor = null;

        // ---- folders ----------------------------------------------------
        if (include != Include.FILES && (cursor == null || cursor.phase() == TreeCursor.Phase.FOLDERS)) {
            String afterFolderName = cursor == null ? null : cursor.lastFolderName();
            List<FolderItem> rows = runFolderQuery(versionId, path, remaining + 1, afterFolderName, order);
            int take = Math.min(remaining, rows.size());
            hasMoreFolders = rows.size() > remaining;
            folderRows = rows.subList(0, take);
            remaining -= take;
            if (hasMoreFolders) {
                nextCursor = encodeCursor(TreeCursor.folders(folderRows.get(take - 1).name));
            }
        }

        // ---- files (same page if folders left room) ---------------------
        if (remaining > 0 && include != Include.FOLDERS && !hasMoreFolders) {
            String afterLabelLower = null;
            Long afterId = null;
            if (cursor != null && cursor.phase() == TreeCursor.Phase.FILES) {
                afterLabelLower = cursor.lastFileLabelLower();
                afterId = cursor.lastFileId();
            }
            List<FileItem> rows = runFileQuery(versionId, path, remaining + 1, afterLabelLower, afterId, order, originals);
            int take = Math.min(remaining, rows.size());
            boolean hasMoreFiles = rows.size() > remaining;
            fileRows = rows.subList(0, take);
            if (hasMoreFiles) {
                FileItem last = fileRows.get(take - 1);
                nextCursor = encodeCursor(TreeCursor.files(last.name.toLowerCase(Locale.ROOT), last.id));
            }
        }

        List<TreeItem> out = new ArrayList<>(folderRows.size() + fileRows.size());
        out.addAll(folderRows);
        out.addAll(fileRows);

        int approximateCount = countFolders(versionId, path) + countFiles(versionId, path);

        return new TreePage(path, out, nextCursor, limit, order, include, approximateCount);
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
        sql.append("           FILTER (WHERE position('/' IN substring(fm.directorylabel FROM ").append(substringFrom).append(")) > 0) AS subfolder_count ");
        sql.append("FROM filemetadata fm ");
        sql.append("WHERE fm.datasetversion_id = ?").append(params.size() + 1).append(" ");
        params.add(versionId);
        if (root) {
            sql.append("  AND fm.directorylabel IS NOT NULL AND fm.directorylabel <> '' ");
        } else {
            sql.append("  AND fm.directorylabel LIKE ?").append(params.size() + 1).append(" ");
            params.add(path + "/%");
        }
        if (afterFolderName != null) {
            String cmp = order == Order.NAME_ZA ? "<" : ">";
            sql.append("  AND lower(split_part(substring(fm.directorylabel FROM ").append(substringFrom).append("), '/', 1)) ").append(cmp).append(" ?").append(params.size() + 1).append(" ");
            params.add(afterFolderName.toLowerCase(Locale.ROOT));
        }
        // Repeat the expression in GROUP BY / ORDER BY rather than referring
        // to the `folder_name` SELECT alias. PostgreSQL allows the bare
        // alias in GROUP BY (extension) but does NOT allow it inside an
        // expression in ORDER BY — `ORDER BY lower(folder_name)` errors
        // with `column "folder_name" does not exist`. Repeating the
        // expression is also SQL-spec-clean.
        sql.append("GROUP BY split_part(substring(fm.directorylabel FROM ").append(substringFrom).append("), '/', 1) ");
        sql.append("ORDER BY lower(split_part(substring(fm.directorylabel FROM ").append(substringFrom).append("), '/', 1)) ").append(order == Order.NAME_ZA ? "DESC" : "ASC").append(" ");
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
            String folderPath = root ? folderName : path + "/" + folderName;
            out.add(new FolderItem(folderName, folderPath, filesUnder, subfolderCount));
        }
        return out;
    }

    // ---------- File query ----------------------------------------------

    @SuppressWarnings("unchecked")
    private List<FileItem> runFileQuery(long versionId, String path, int limit,
                                         String afterLabelLower, Long afterId,
                                         Order order, boolean originals) {
        boolean root = path.isEmpty();

        // Positional ?N parameters per the EclipseLink native-query convention;
        // see the comment in `runFolderQuery` for why named parameters cannot
        // be used here.
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        sql.append("SELECT fm.label, df.id, df.filesize, df.contenttype, df.restricted, df.embargo_id, df.checksumtype, df.checksumvalue ");
        sql.append("FROM filemetadata fm JOIN datafile df ON fm.datafile_id = df.id ");
        sql.append("WHERE fm.datasetversion_id = ?").append(params.size() + 1).append(" ");
        params.add(versionId);
        if (root) {
            sql.append("  AND (fm.directorylabel IS NULL OR fm.directorylabel = '') ");
        } else {
            sql.append("  AND fm.directorylabel = ?").append(params.size() + 1).append(" ");
            params.add(path);
        }
        if (afterLabelLower != null && afterId != null) {
            String cmp = order == Order.NAME_ZA ? "<" : ">";
            // Standard tuple-keyset pattern: (lower(label), datafile_id) compared lexicographically.
            // afterLabel needs two positional slots — the value is the same in both, but EclipseLink's
            // native-query binding wants each occurrence as its own ?N.
            int afterLabelLeft = params.size() + 1;
            params.add(afterLabelLower);
            int afterLabelRight = params.size() + 1;
            params.add(afterLabelLower);
            int afterIdSlot = params.size() + 1;
            params.add(afterId);
            sql.append("  AND (lower(fm.label) ").append(cmp).append(" ?").append(afterLabelLeft).append(" ");
            sql.append("       OR (lower(fm.label) = ?").append(afterLabelRight).append(" AND df.id ").append(cmp).append(" ?").append(afterIdSlot).append(")) ");
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
            Boolean restricted = (Boolean) row[4];
            Object embargoId = row[5];
            String checksumTypeRaw = (String) row[6];
            String checksumValue = (String) row[7];

            String access;
            if (Boolean.TRUE.equals(restricted)) {
                access = "restricted";
            } else if (embargoId != null) {
                access = "embargoed";
            } else {
                access = "public";
            }
            String checksumType = checksumTypeRaw;  // stored as the enum name already
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
            sql.append("  AND fm.directorylabel LIKE ?2 ");
        }

        Query q = em.createNativeQuery(sql.toString());
        q.setParameter(1, versionId);
        if (!root) {
            q.setParameter(2, path + "/%");
        }
        Number n = (Number) q.getSingleResult();
        return n == null ? 0 : n.intValue();
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

    public static String normalizePath(String raw) {
        if (raw == null) {
            return "";
        }
        String collapsed = raw.replaceAll("/+", "/");
        if (collapsed.startsWith("/")) {
            collapsed = collapsed.substring(1);
        }
        if (collapsed.endsWith("/")) {
            collapsed = collapsed.substring(0, collapsed.length() - 1);
        }
        return collapsed;
    }

    private static int clampLimit(Integer requested) {
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

    private static String encodeCursor(TreeCursor cursor) {
        // Tiny hand-rolled JSON; pulling javax.json or Jackson here would be
        // overkill for two strings + a long.
        StringBuilder json = new StringBuilder();
        json.append("{\"p\":\"");
        json.append(cursor.phase() == TreeCursor.Phase.FOLDERS ? "F" : "f");
        json.append("\",\"k\":[");
        if (cursor.phase() == TreeCursor.Phase.FOLDERS) {
            json.append('"').append(escapeJson(cursor.lastFolderName())).append('"');
        } else {
            json.append('"').append(escapeJson(cursor.lastFileLabelLower())).append('"');
            json.append(',').append(cursor.lastFileId());
        }
        json.append("]}");
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static TreeCursor decodeCursor(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(raw), StandardCharsets.UTF_8);
            try (JsonReader reader = Json.createReader(new StringReader(decoded))) {
                jakarta.json.JsonObject obj = reader.readObject();
                String phaseStr = obj.getString("p", null);
                JsonArray keys = obj.getJsonArray("k");
                if (phaseStr == null || keys == null) {
                    throw new InvalidQueryException("invalid cursor");
                }
                if ("F".equals(phaseStr) && keys.size() == 1) {
                    return TreeCursor.folders(((JsonString) keys.get(0)).getString());
                }
                if ("f".equals(phaseStr) && keys.size() == 2) {
                    String label = ((JsonString) keys.get(0)).getString();
                    long id = ((JsonValue) keys.get(1)) instanceof jakarta.json.JsonNumber n
                            ? n.longValueExact()
                            : Long.parseLong(keys.get(1).toString());
                    return TreeCursor.files(label, id);
                }
                throw new InvalidQueryException("invalid cursor");
            }
        } catch (IllegalArgumentException | jakarta.json.JsonException ex) {
            throw new InvalidQueryException("invalid cursor");
        }
    }

    private static String escapeJson(String s) {
        // Minimal JSON-string escape. directorylabel / label come from
        // user-controlled text so we do need this to be correct.
        StringBuilder out = new StringBuilder(s.length() + 4);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                case '\\':
                    out.append('\\').append(c);
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        out.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        return out.toString();
    }
}
