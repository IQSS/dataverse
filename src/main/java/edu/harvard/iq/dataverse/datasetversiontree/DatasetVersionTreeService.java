package edu.harvard.iq.dataverse.datasetversiontree;

import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import jakarta.ejb.Stateless;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Computes a single page of folders+files inside a folder of a dataset version.
 *
 * The first cut walks {@code DatasetVersion.fileMetadatas} once, groups by the
 * first segment relative to the requested {@code path}, and applies an opaque
 * keyset cursor in memory. This keeps the API contract stable and the diff
 * small; we can promote to SQL keyset queries (folder native query + JPA
 * Criteria) in a follow-up without changing the wire format.
 *
 * Folders come first (ascending or descending by name), then files (same
 * ordering with a stable {@code id} tie-break).
 */
@Stateless
public class DatasetVersionTreeService {

    public static final int DEFAULT_LIMIT = 100;
    public static final int MAX_LIMIT = 1000;

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

    public TreePage listChildren(DatasetVersion version, String rawPath,
                                 Integer limitParam, String cursorParam,
                                 Include include, Order order, boolean originals) {
        Objects.requireNonNull(version);
        String path = normalizePath(rawPath);
        int limit = clampLimit(limitParam);
        List<TreeItem> all = collectImmediateChildren(version, path, include, order, originals);
        int offset = parseCursor(cursorParam);
        if (offset > all.size()) {
            offset = all.size();
        }
        int end = Math.min(offset + limit, all.size());
        List<TreeItem> slice = new ArrayList<>(all.subList(offset, end));
        String nextCursor = end < all.size() ? encodeCursor(end) : null;
        return new TreePage(path, slice, nextCursor, limit, order, include, all.size());
    }

    private List<TreeItem> collectImmediateChildren(DatasetVersion version, String path,
                                                    Include include, Order order,
                                                    boolean originals) {
        Map<String, FolderAccumulator> folders = new LinkedHashMap<>();
        List<FileItem> files = new ArrayList<>();
        String prefix = path.isEmpty() ? "" : path + "/";
        for (FileMetadata fileMetadata : version.getFileMetadatas()) {
            String dir = normalizeDirectory(fileMetadata.getDirectoryLabel());
            if (!path.isEmpty() && !dir.equals(path) && !dir.startsWith(prefix)) {
                continue;
            }
            if (dir.equals(path)) {
                files.add(toFileItem(fileMetadata, path, originals));
                continue;
            }
            String remainder = dir.substring(prefix.length());
            int slashIdx = remainder.indexOf('/');
            String segment = slashIdx == -1 ? remainder : remainder.substring(0, slashIdx);
            if (segment.isEmpty()) {
                continue;
            }
            String folderPath = prefix + segment;
            FolderAccumulator entry = folders.computeIfAbsent(folderPath,
                    p -> new FolderAccumulator(segment, p));
            entry.fileCount += 1;
            if (slashIdx != -1) {
                // Track distinct subfolder names (the next path segment after this folder).
                int nextSlash = remainder.indexOf('/', slashIdx + 1);
                String subSegment = nextSlash == -1
                        ? remainder.substring(slashIdx + 1)
                        : remainder.substring(slashIdx + 1, nextSlash);
                if (!subSegment.isEmpty()) {
                    entry.subfolderNames.add(subSegment);
                }
            }
        }
        Comparator<String> nameComparator = order == Order.NAME_ZA
                ? Comparator.<String, String>comparing(String::toLowerCase).reversed()
                : Comparator.comparing(String::toLowerCase);

        List<FolderItem> folderItems = new ArrayList<>();
        for (FolderAccumulator acc : folders.values()) {
            folderItems.add(new FolderItem(acc.name, acc.path, acc.fileCount, acc.subfolderNames.size()));
        }
        folderItems.sort((a, b) -> nameComparator.compare(a.name, b.name));

        files.sort((a, b) -> {
            int cmp = nameComparator.compare(a.name, b.name);
            if (cmp != 0) {
                return cmp;
            }
            return Long.compare(a.id, b.id);
        });

        List<TreeItem> out = new ArrayList<>();
        if (include != Include.FILES) {
            out.addAll(folderItems);
        }
        if (include != Include.FOLDERS) {
            out.addAll(files);
        }
        return out;
    }

    private static FileItem toFileItem(FileMetadata fm, String parentPath, boolean originals) {
        long fileId = fm.getDataFile().getId();
        String contentType = fm.getDataFile().getContentType();
        String access;
        if (fm.getDataFile().isRestricted()) {
            access = "restricted";
        } else if (fm.getDataFile().getEmbargo() != null) {
            access = "embargoed";
        } else {
            access = "public";
        }
        String checksumType = fm.getDataFile().getChecksumType() != null
                ? fm.getDataFile().getChecksumType().name() : null;
        String checksumValue = fm.getDataFile().getChecksumValue();
        String downloadUrl = "/api/access/datafile/" + fileId;
        if (originals) {
            downloadUrl = downloadUrl + "?format=original";
        }
        long size = fm.getDataFile().getFilesize();
        String name = fm.getLabel();
        String path = parentPath.isEmpty() ? name : parentPath + "/" + name;
        return new FileItem(fileId, name, path, size, contentType, access,
                checksumType, checksumValue, downloadUrl);
    }

    private static String normalizeDirectory(String raw) {
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

    public static String normalizePath(String raw) {
        return normalizeDirectory(raw);
    }

    private static int clampLimit(Integer requested) {
        if (requested == null || requested <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(requested, MAX_LIMIT);
    }

    private static int parseCursor(String cursor) {
        if (cursor == null || cursor.isEmpty()) {
            return 0;
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            if (!decoded.startsWith("offset=")) {
                throw new InvalidQueryException("invalid cursor");
            }
            int offset = Integer.parseInt(decoded.substring("offset=".length()));
            if (offset < 0) {
                throw new InvalidQueryException("invalid cursor");
            }
            return offset;
        } catch (IllegalArgumentException ex) {
            throw new InvalidQueryException("invalid cursor");
        }
    }

    private static String encodeCursor(int offset) {
        String value = "offset=" + offset;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static final class FolderAccumulator {
        private final String name;
        private final String path;
        private long fileCount;
        private final Set<String> subfolderNames = new HashSet<>();

        FolderAccumulator(String name, String path) {
            this.name = name;
            this.path = path;
        }
    }
}
