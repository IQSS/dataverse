package edu.harvard.iq.dataverse.util.json;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileCategory;
import edu.harvard.iq.dataverse.DataFileTag;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.users.PrivateUrlUser;
import edu.harvard.iq.dataverse.privateurl.PrivateUrl;
import java.util.ArrayList;
import java.util.List;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class JsonPrinterTest {

    @Test
    public void testJson_RoleAssignment() {
        DataverseRole aRole = new DataverseRole();
        PrivateUrlUser privateUrlUserIn = new PrivateUrlUser(42);
        RoleAssignee anAssignee = privateUrlUserIn;
        Dataset dataset = new Dataset();
        dataset.setId(123l);
        String privateUrlToken = "e1d53cf6-794a-457a-9709-7c07629a8267";
        RoleAssignment ra = new RoleAssignment(aRole, anAssignee, dataset, privateUrlToken);
        JsonObjectBuilder job = JsonPrinter.json(ra);
        assertNotNull(job);
        JsonObject jsonObject = job.build();
        assertEquals("#42", jsonObject.getString("assignee"));
        assertEquals(123, jsonObject.getInt("definitionPointId"));
        assertEquals("e1d53cf6-794a-457a-9709-7c07629a8267", jsonObject.getString("privateUrlToken"));
    }

    @Test
    public void testJson_PrivateUrl() {
        DataverseRole aRole = new DataverseRole();
        PrivateUrlUser privateUrlUserIn = new PrivateUrlUser(42);
        RoleAssignee anAssignee = privateUrlUserIn;
        Dataset dataset = new Dataset();
        String privateUrlToken = "e1d53cf6-794a-457a-9709-7c07629a8267";
        RoleAssignment ra = new RoleAssignment(aRole, anAssignee, dataset, privateUrlToken);
        String dataverseSiteUrl = "https://dataverse.example.edu";
        PrivateUrl privateUrl = new PrivateUrl(ra, dataset, dataverseSiteUrl);
        JsonObjectBuilder job = JsonPrinter.json(privateUrl);
        assertNotNull(job);
        JsonObject jsonObject = job.build();
        assertEquals("e1d53cf6-794a-457a-9709-7c07629a8267", jsonObject.getString("token"));
        assertEquals("https://dataverse.example.edu/privateurl.xhtml?token=e1d53cf6-794a-457a-9709-7c07629a8267", jsonObject.getString("link"));
        assertEquals("e1d53cf6-794a-457a-9709-7c07629a8267", jsonObject.getJsonObject("roleAssignment").getString("privateUrlToken"));
        assertEquals("#42", jsonObject.getJsonObject("roleAssignment").getString("assignee"));
    }

    @Test
    public void testGetFileCategories() {
        FileMetadata fmd = new FileMetadata();
        DatasetVersion dsVersion = new DatasetVersion();
        DataFile dataFile = new DataFile();
        List<DataFileTag> dataFileTags = new ArrayList<>();
        DataFileTag tag = new DataFileTag();
        tag.setTypeByLabel("Survey");
        dataFileTags.add(tag);
        dataFile.setTags(dataFileTags);
        fmd.setDatasetVersion(dsVersion);
        fmd.setDataFile(dataFile);
        List<DataFileCategory> fileCategories = new ArrayList<>();
        DataFileCategory dataFileCategory = new DataFileCategory();
        dataFileCategory.setName("Data");
        fileCategories.add(dataFileCategory);
        fmd.setCategories(fileCategories);
        JsonObjectBuilder job = JsonPrinter.json(fmd);
        assertNotNull(job);
        JsonObject jsonObject = job.build();
        System.out.println("json: " + jsonObject);
        assertEquals("", jsonObject.getString("description"));
        assertEquals("", jsonObject.getString("label"));
        assertEquals("Data", jsonObject.getJsonArray("categories").getString(0));
        assertEquals("", jsonObject.getJsonObject("dataFile").getString("filename"));
        assertEquals(-1, jsonObject.getJsonObject("dataFile").getInt("filesize"));
        assertEquals("UNKNOWN", jsonObject.getJsonObject("dataFile").getString("originalFormatLabel"));
        assertEquals(-1, jsonObject.getJsonObject("dataFile").getInt("rootDataFileId"));
        assertEquals("Survey", jsonObject.getJsonObject("dataFile").getJsonArray("tabularTags").getString(0));
    }

}
