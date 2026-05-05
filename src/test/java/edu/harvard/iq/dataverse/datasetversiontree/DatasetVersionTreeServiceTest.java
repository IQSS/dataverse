package edu.harvard.iq.dataverse.datasetversiontree;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Embargo;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.DataFile.ChecksumType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatasetVersionTreeServiceTest {

    private final DatasetVersionTreeService svc = new DatasetVersionTreeService();

    @Test
    void rootListGroupsFilesByFirstSegmentAndKeepsRootFiles() {
        DatasetVersion version = makeVersion(List.of(
                fm(1, "top.txt", null),
                fm(2, "a.txt", "data"),
                fm(3, "b.txt", "data/raw"),
                fm(4, "c.txt", "docs")
        ));
        DatasetVersionTreeService.TreePage page = svc.listChildren(version, null, null, null,
                DatasetVersionTreeService.Include.ALL,
                DatasetVersionTreeService.Order.NAME_AZ, false);

        List<String> names = page.items.stream().map(i -> i.name).collect(Collectors.toList());
        assertEquals(List.of("data", "docs", "top.txt"), names);
        assertEquals("", page.path);
        assertNull(page.nextCursor);
    }

    @Test
    void folderItemCountsAreDistinctSubfoldersAndDescendantFiles() {
        DatasetVersion version = makeVersion(List.of(
                fm(1, "a.txt", "data/sub1"),
                fm(2, "b.txt", "data/sub1"),
                fm(3, "c.txt", "data/sub2"),
                fm(4, "d.txt", "data")
        ));
        DatasetVersionTreeService.TreePage page = svc.listChildren(version, null, null, null,
                DatasetVersionTreeService.Include.ALL,
                DatasetVersionTreeService.Order.NAME_AZ, false);
        DatasetVersionTreeService.FolderItem dataFolder = page.items.stream()
                .filter(i -> i instanceof DatasetVersionTreeService.FolderItem)
                .map(i -> (DatasetVersionTreeService.FolderItem) i)
                .findFirst()
                .orElseThrow();
        assertEquals("data", dataFolder.name);
        assertEquals(4L, dataFolder.fileCount, "fileCount should count all descendant files");
        assertEquals(2L, dataFolder.folderCount, "folderCount should count distinct subfolders");
    }

    @Test
    void folderListIncludesOnlyImmediateChildren() {
        DatasetVersion version = makeVersion(List.of(
                fm(1, "a.txt", "data"),
                fm(2, "sub.txt", "data/sub"),
                fm(3, "unrelated.txt", "docs")
        ));
        DatasetVersionTreeService.TreePage page = svc.listChildren(version, "data", null, null,
                DatasetVersionTreeService.Include.ALL,
                DatasetVersionTreeService.Order.NAME_AZ, false);

        List<String> names = page.items.stream().map(i -> i.name).collect(Collectors.toList());
        assertEquals(List.of("sub", "a.txt"), names);
    }

    @Test
    void normalizesPath() {
        assertEquals("data/sub", DatasetVersionTreeService.normalizePath("/data//sub///"));
        assertEquals("", DatasetVersionTreeService.normalizePath("/"));
        assertEquals("", DatasetVersionTreeService.normalizePath(null));
    }

    @Test
    void includeFilterRespected() {
        DatasetVersion version = makeVersion(List.of(
                fm(1, "top.txt", null),
                fm(2, "in.txt", "data")
        ));
        DatasetVersionTreeService.TreePage folderPage = svc.listChildren(version, "", null, null,
                DatasetVersionTreeService.Include.FOLDERS,
                DatasetVersionTreeService.Order.NAME_AZ, false);
        assertEquals(1, folderPage.items.size());
        assertTrue(folderPage.items.get(0) instanceof DatasetVersionTreeService.FolderItem);

        DatasetVersionTreeService.TreePage filePage = svc.listChildren(version, "", null, null,
                DatasetVersionTreeService.Include.FILES,
                DatasetVersionTreeService.Order.NAME_AZ, false);
        assertEquals(1, filePage.items.size());
        assertTrue(filePage.items.get(0) instanceof DatasetVersionTreeService.FileItem);
    }

    @Test
    void cursorPaginationStable() {
        List<FileMetadata> metas = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            metas.add(fm(i, "f" + i + ".txt", null));
        }
        DatasetVersion version = makeVersion(metas);
        DatasetVersionTreeService.TreePage first = svc.listChildren(version, null, 3, null,
                DatasetVersionTreeService.Include.ALL,
                DatasetVersionTreeService.Order.NAME_AZ, false);
        assertEquals(3, first.items.size());
        assertNotNull(first.nextCursor);

        DatasetVersionTreeService.TreePage second = svc.listChildren(version, null, 3,
                first.nextCursor,
                DatasetVersionTreeService.Include.ALL,
                DatasetVersionTreeService.Order.NAME_AZ, false);
        assertEquals(3, second.items.size());

        DatasetVersionTreeService.TreePage third = svc.listChildren(version, null, 3,
                second.nextCursor,
                DatasetVersionTreeService.Include.ALL,
                DatasetVersionTreeService.Order.NAME_AZ, false);
        assertEquals(1, third.items.size());
        assertNull(third.nextCursor);
    }

    @Test
    void invalidCursorRejected() {
        DatasetVersion version = makeVersion(List.of());
        assertThrows(DatasetVersionTreeService.InvalidQueryException.class,
                () -> svc.listChildren(version, null, null, "not-a-real-cursor",
                        DatasetVersionTreeService.Include.ALL,
                        DatasetVersionTreeService.Order.NAME_AZ, false));
    }

    @Test
    void invalidOrderRejected() {
        assertThrows(DatasetVersionTreeService.InvalidQueryException.class,
                () -> DatasetVersionTreeService.Order.fromQuery("Bogus"));
    }

    @Test
    void originalsTogglesDownloadUrl() {
        DatasetVersion version = makeVersion(List.of(fm(1, "top.txt", null)));
        DatasetVersionTreeService.TreePage page = svc.listChildren(version, null, null, null,
                DatasetVersionTreeService.Include.ALL,
                DatasetVersionTreeService.Order.NAME_AZ, true);
        DatasetVersionTreeService.FileItem file = (DatasetVersionTreeService.FileItem) page.items.get(0);
        assertEquals("/api/access/datafile/1?format=original", file.downloadUrl);
    }

    @Test
    void restrictedFileReportsRestrictedAccess() {
        FileMetadata fm = fm(1, "secret.txt", null);
        fm.getDataFile().setRestricted(true);
        DatasetVersion version = makeVersion(List.of(fm));
        DatasetVersionTreeService.TreePage page = svc.listChildren(version, null, null, null,
                DatasetVersionTreeService.Include.ALL,
                DatasetVersionTreeService.Order.NAME_AZ, false);
        DatasetVersionTreeService.FileItem file = (DatasetVersionTreeService.FileItem) page.items.get(0);
        assertEquals("restricted", file.access);
    }

    @Test
    void embargoedFileReportsEmbargoedAccess() {
        FileMetadata fm = fm(1, "future.txt", null);
        Embargo embargo = new Embargo();
        embargo.setDateAvailable(LocalDate.now().plusYears(1));
        fm.getDataFile().setEmbargo(embargo);
        DatasetVersion version = makeVersion(List.of(fm));
        DatasetVersionTreeService.TreePage page = svc.listChildren(version, null, null, null,
                DatasetVersionTreeService.Include.ALL,
                DatasetVersionTreeService.Order.NAME_AZ, false);
        DatasetVersionTreeService.FileItem file = (DatasetVersionTreeService.FileItem) page.items.get(0);
        assertEquals("embargoed", file.access);
    }

    @Test
    void descendingOrderSortsByName() {
        DatasetVersion version = makeVersion(List.of(
                fm(1, "a.txt", null),
                fm(2, "b.txt", null)
        ));
        DatasetVersionTreeService.TreePage page = svc.listChildren(version, null, null, null,
                DatasetVersionTreeService.Include.ALL,
                DatasetVersionTreeService.Order.NAME_ZA, false);
        List<String> names = page.items.stream().map(i -> i.name).collect(Collectors.toList());
        assertEquals(List.of("b.txt", "a.txt"), names);
    }

    private static DatasetVersion makeVersion(List<FileMetadata> metas) {
        DatasetVersion version = new DatasetVersion();
        version.setFileMetadatas(metas);
        return version;
    }

    private static FileMetadata fm(long id, String label, String directoryLabel) {
        DataFile dataFile = new DataFile();
        dataFile.setId(id);
        dataFile.setFilesize(1024L);
        dataFile.setContentType("text/plain");
        dataFile.setChecksumType(ChecksumType.MD5);
        dataFile.setChecksumValue("deadbeef");
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setLabel(label);
        fileMetadata.setDirectoryLabel(directoryLabel);
        fileMetadata.setDataFile(dataFile);
        return fileMetadata;
    }
}
