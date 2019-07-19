package edu.harvard.iq.dataverse.dataset;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.license.FileTermsOfUse.RestrictType;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DatasetUtilTest {

    /**
     * Test of getThumbnailCandidates method, of class DatasetUtil.
     */
    @Test
    public void testGetThumbnailCandidates() {
        assertEquals(new ArrayList<>(), DatasetUtil.getThumbnailCandidates(null, false, new DataAccess()));

        Dataset dataset = MocksFactory.makeDataset();
        DataFile dataFile = MocksFactory.makeDataFile();
        dataFile.setContentType("image/");
        dataFile.setOwner(dataset);
        dataFile.setStorageIdentifier("file://src/test/resources/images/coffeeshop.png");

        System.out.println(ImageThumbConverter.isThumbnailAvailable(dataFile));
        DatasetVersion version = dataset.getCreateVersion();
        List<FileMetadata> fmds = new ArrayList<>();
        fmds.add(MocksFactory.addFileMetadata(dataFile));
        version.setFileMetadatas(fmds);
        assertEquals(new ArrayList<>(), DatasetUtil.getThumbnailCandidates(dataset, false, new DataAccess()));
    }

    @Test
    public void testGetThumbnailNullDataset() {
        assertNull(DatasetUtil.getThumbnail(null));
        assertNull(DatasetUtil.getThumbnail(null, null, new DataAccess()));

        Dataset dataset = MocksFactory.makeDataset();
        dataset.setStorageIdentifier("file://");
        dataset.setUseGenericThumbnail(true);

        assertNull(DatasetUtil.getThumbnail(dataset));
        assertNull(DatasetUtil.getThumbnail(dataset, new DatasetVersion(), new DataAccess()));
    }

    @Test
    public void testGetThumbnailRestricted() {
        System.out.println("testGetThumbnailRestricted");
        Dataset dataset = new Dataset();
        DataFile thumbnailFile = new DataFile();
        thumbnailFile.setId(42l);
        
        FileMetadata thumbnailFileMetadata = MocksFactory.makeFileMetadata(123l, "file.png", 0);
        thumbnailFileMetadata.getTermsOfUse().setLicense(null);
        thumbnailFileMetadata.getTermsOfUse().setRestrictType(RestrictType.ACADEMIC_PURPOSE);
        
        thumbnailFile.setFileMetadatas(Lists.newArrayList(thumbnailFileMetadata));
        dataset.setThumbnailFile(thumbnailFile);
        
        DatasetThumbnail result = DatasetUtil.getThumbnail(dataset);
        
        assertNull(result);
    }

    /**
     * Test of deleteDatasetLogo method, of class DatasetUtil.
     */
    @Test
    public void testDeleteDatasetLogo() {
        assertEquals(false, DatasetUtil.deleteDatasetLogo(null, new DataAccess()));
        assertEquals(false, DatasetUtil.deleteDatasetLogo(new Dataset(), new DataAccess()));
    }

    /**
     * Test of getDefaultThumbnailFile method, of class DatasetUtil.
     */
    @Test
    public void testGetDefaultThumbnailFile() {
        assertNull(DatasetUtil.attemptToAutomaticallySelectThumbnailFromDataFiles(null, null));
    }

    /**
     * Test of persistDatasetLogoToStorageAndCreateThumbnail method, of class
     * DatasetUtil.
     */
    @Test
    public void testPersistDatasetLogoToStorageAndCreateThumbnail() {
        assertNull(DatasetUtil.persistDatasetLogoToStorageAndCreateThumbnail(null, null, new DataAccess()));
        //Todo: a test for this that test main logic
    }

    /**
     * Test of getThumbnailAsInputStream method, of class DatasetUtil.
     */
    @Test
    public void testGetThumbnailAsInputStream() {
        assertNull(DatasetUtil.getThumbnailAsInputStream(null));
    }

    /**
     * Test of isDatasetLogoPresent method, of class DatasetUtil.
     */
    @Test
    public void testIsDatasetLogoPresent() {
        Dataset dataset = MocksFactory.makeDataset();
        assertEquals(false, DatasetUtil.isDatasetLogoPresent(dataset, new DataAccess()));
    }

}
