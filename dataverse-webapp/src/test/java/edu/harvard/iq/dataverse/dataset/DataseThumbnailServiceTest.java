package edu.harvard.iq.dataverse.dataset;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse.RestrictType;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@ExtendWith(MockitoExtension.class)
public class DataseThumbnailServiceTest {

    @InjectMocks
    private DatasetThumbnailService datasetThumbnailService;
    
    @Mock
    private ImageThumbConverter imageThumbConverter;
    
    /**
     * Test of getThumbnailCandidates method, of class DatasetUtil.
     */
    @Test
    public void testGetThumbnailCandidates() {
        assertEquals(new ArrayList<>(), datasetThumbnailService.getThumbnailCandidates(null, false));

        Dataset dataset = MocksFactory.makeDataset();
        DataFile dataFile = MocksFactory.makeDataFile();
        dataFile.setContentType("image/");
        dataFile.setOwner(dataset);
        dataFile.setStorageIdentifier("file://src/test/resources/images/coffeeshop.png");

        DatasetVersion version = dataset.getLatestVersion();
        List<FileMetadata> fmds = new ArrayList<>();
        fmds.add(MocksFactory.addFileMetadata(dataFile));
        version.setFileMetadatas(fmds);
        assertEquals(new ArrayList<>(), datasetThumbnailService.getThumbnailCandidates(dataset, false));
    }

    @Test
    public void testGetThumbnailNullDataset() {
        assertNull(datasetThumbnailService.getThumbnail(null));

        Dataset dataset = MocksFactory.makeDataset();
        dataset.setStorageIdentifier("file://");
        dataset.setUseGenericThumbnail(true);

        assertNull(datasetThumbnailService.getThumbnail(dataset));
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
        
        DatasetThumbnail result = datasetThumbnailService.getThumbnail(dataset);
        
        assertNull(result);
    }

    /**
     * Test of deleteDatasetLogo method, of class DatasetUtil.
     */
    @Test
    public void testDeleteDatasetLogo() {
        assertEquals(false, datasetThumbnailService.deleteDatasetLogo(null));
        assertEquals(false, datasetThumbnailService.deleteDatasetLogo(new Dataset()));
    }

    /**
     * Test of persistDatasetLogoToStorageAndCreateThumbnail method, of class
     * DatasetUtil.
     */
    @Test
    public void testPersistDatasetLogoToStorageAndCreateThumbnail() {
        assertNull(datasetThumbnailService.persistDatasetLogoToStorageAndCreateThumbnail(null, null));
        //Todo: a test for this that test main logic
    }

    /**
     * Test of getThumbnailAsInputStream method, of class DatasetUtil.
     */
    @Test
    public void testGetThumbnailAsInputStream() {
        assertNull(datasetThumbnailService.getThumbnailAsInputStream(null));
    }

    /**
     * Test of isDatasetLogoPresent method, of class DatasetUtil.
     */
    @Test
    public void testIsDatasetLogoPresent() {
        Dataset dataset = MocksFactory.makeDataset();
        assertEquals(false, datasetThumbnailService.isDatasetLogoPresent(dataset));
    }

}
