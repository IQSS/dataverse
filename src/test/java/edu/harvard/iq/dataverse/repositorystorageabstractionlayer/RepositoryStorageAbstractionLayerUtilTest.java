package edu.harvard.iq.dataverse.repositorystorageabstractionlayer;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.EssentialTests;
import edu.harvard.iq.dataverse.locality.StorageSite;
import java.util.ArrayList;
import java.util.List;
import javax.json.JsonArray;
import javax.json.JsonObject;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class RepositoryStorageAbstractionLayerUtilTest {

    @Category(EssentialTests.class)
    @Test
    public void testGetRsalSites_3args() {
        System.out.println("getRsalSites");
        Dataset dataset = new Dataset();
        dataset.setIdentifier("identifierPartOfPersistentID");
        dataset.setAuthority("10.5072/FK2");
        List<StorageSite> storageLocations = new ArrayList<>();
        StorageSite sbgrid = new StorageSite();
        sbgrid.setHostname("dv.sbgrid.org");
        sbgrid.setName("Harvard Medical School, USA");
        storageLocations.add(sbgrid);
        JsonArray myList = RepositoryStorageAbstractionLayerUtil.getStorageSitesAsJson(storageLocations);
        List<RsyncSite> result = RepositoryStorageAbstractionLayerUtil.getRsyncSites(dataset, myList);
        System.out.println(result.get(0).getName());
        assertEquals("Harvard Medical School, USA", result.get(0).getName());
        assertEquals("dv.sbgrid.org", result.get(0).getFqdn());
        assertEquals("10.5072/FK2/identifierPartOfPersistentID", result.get(0).getFullRemotePathToDirectory());
        assertEquals("rsync -av rsync://dv.sbgrid.org/10.5072/FK2/identifierPartOfPersistentID", result.get(0).getRsyncDownloadcommand());
    }

    @Category(EssentialTests.class)
    @Test
    public void testGetRsalSites_String() {
        System.out.println("getRsalSites");
        List<StorageSite> storageLocations = new ArrayList<>();
        StorageSite sbgrid = new StorageSite();
        sbgrid.setHostname("dv.sbgrid.org");
        sbgrid.setName("Harvard Medical School, USA");
        storageLocations.add(sbgrid);
        // Expect a warning here because there are no primary sites.
        JsonArray result = RepositoryStorageAbstractionLayerUtil.getStorageSitesAsJson(storageLocations);
        JsonObject first = (JsonObject) result.get(0);
        System.out.println(result);
        assertEquals("Harvard Medical School, USA", first.getString("name"));
    }

    @Category(EssentialTests.class)
    @Test
    public void testGetLocalDataAccessDirectory() {
        System.out.println("getLocalDataAccessDirectory");
        String localDataAccessParentDir = "/opt/data";
        Dataset dataset = new Dataset();
        dataset.setIdentifier("identifierPartOfPersistentID");
        dataset.setAuthority("10.5072/FK2");
        String result = RepositoryStorageAbstractionLayerUtil.getLocalDataAccessDirectory(localDataAccessParentDir, dataset);
        assertEquals("/opt/data/10.5072/FK2/identifierPartOfPersistentID", result);
    }

    @Category(EssentialTests.class)
    @Test
    public void testGetVerifyDataCommand() {
        System.out.println("getVerifyDataCommand");
        Dataset dataset = new Dataset();
        dataset.setIdentifier("identifierPartOfPersistentID");
        String result = RepositoryStorageAbstractionLayerUtil.getVerifyDataCommand(dataset);
        assertEquals("cd identifierPartOfPersistentID ; shasum -c files.sha", result);
    }


}
