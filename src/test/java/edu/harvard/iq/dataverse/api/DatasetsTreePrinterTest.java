package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.datasetversiontree.DatasetVersionTreeService.FileItem;
import edu.harvard.iq.dataverse.datasetversiontree.DatasetVersionTreeService.FolderItem;
import edu.harvard.iq.dataverse.datasetversiontree.DatasetVersionTreeService.Include;
import edu.harvard.iq.dataverse.datasetversiontree.DatasetVersionTreeService.Order;
import edu.harvard.iq.dataverse.datasetversiontree.DatasetVersionTreeService.TreePage;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatasetsTreePrinterTest {

    @Test
    void printsFoldersFilesAndEnvelope() {
        FolderItem folder = new FolderItem("data", "data", 3, 1, 300, 1, 0, 2);
        FileItem file = new FileItem(5, "a.txt", "data/a.txt", 42, "text/plain", "public",
                "MD5", "abc", "/api/access/datafile/5");
        TreePage page = new TreePage("", List.of(folder, file), "CURSOR", 100, Order.NAME_ZA, Include.ALL, 2);

        JsonObject json = Datasets.jsonTreePage(page).build();

        assertEquals("", json.getString("path"));
        assertEquals("CURSOR", json.getString("nextCursor"));
        assertEquals(100, json.getInt("limit"));
        assertEquals("NameZA", json.getString("order"));
        assertEquals("all", json.getString("include"));
        assertEquals(2, json.getInt("approximateCount"));

        JsonObject f = json.getJsonArray("items").getJsonObject(0);
        assertEquals("folder", f.getString("type"));
        JsonObject counts = f.getJsonObject("counts");
        assertEquals(3, counts.getInt("files"));
        assertEquals(1, counts.getInt("folders"));
        assertEquals(300, counts.getInt("bytes"));
        assertEquals(1, counts.getInt("restricted"));
        assertEquals(2, counts.getInt("retentionExpired"));

        JsonObject i = json.getJsonArray("items").getJsonObject(1);
        assertEquals("file", i.getString("type"));
        assertEquals(5, i.getInt("id"));
        assertEquals("text/plain", i.getString("contentType"));
        assertEquals("MD5", i.getJsonObject("checksum").getString("type"));
        assertEquals("/api/access/datafile/5", i.getString("downloadUrl"));
    }

    @Test
    void omitsOptionalFileFieldsAndWritesNullCursor() {
        FileItem file = new FileItem(5, "a.txt", "a.txt", 42, null, null, null, null, "/api/access/datafile/5");
        TreePage page = new TreePage("", List.of(file), null, 10, Order.NAME_AZ, Include.FILES, 1);

        JsonObject json = Datasets.jsonTreePage(page).build();

        assertEquals(JsonValue.NULL, json.get("nextCursor"));
        assertEquals("files", json.getString("include"));
        JsonObject i = json.getJsonArray("items").getJsonObject(0);
        assertFalse(i.containsKey("contentType"));
        assertFalse(i.containsKey("access"));
        assertFalse(i.containsKey("checksum"));
        assertTrue(i.containsKey("downloadUrl"));
    }
}
