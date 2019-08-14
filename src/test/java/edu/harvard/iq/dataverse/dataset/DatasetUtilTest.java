package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileCategory;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.DatasetFieldType.FieldType;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class DatasetUtilTest {

    /**
     * Test of getThumbnailCandidates method, of class DatasetUtil.
     */
    @Test
    public void testGetThumbnailCandidates() {
        assertEquals(new ArrayList<>(), DatasetUtil.getThumbnailCandidates(null, false));

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
        assertEquals(new ArrayList<>(), DatasetUtil.getThumbnailCandidates(dataset, false));
    }

    @Test
    public void testGetThumbnailNullDataset() {
        assertNull(DatasetUtil.getThumbnail(null));
        assertNull(DatasetUtil.getThumbnail(null, null));

        Dataset dataset = MocksFactory.makeDataset();
        dataset.setStorageIdentifier("file://");
        dataset.setUseGenericThumbnail(true);

        assertNull(DatasetUtil.getThumbnail(dataset));
        assertNull(DatasetUtil.getThumbnail(dataset, new DatasetVersion()));
    }

    @Test
    public void testGetThumbnailRestricted() {
        System.out.println("testGetThumbnailRestricted");
        Dataset dataset = new Dataset();
        DataFile thumbnailFile = new DataFile();
        thumbnailFile.setId(42l);
        thumbnailFile.setRestricted(true);
        dataset.setThumbnailFile(thumbnailFile);
        DatasetThumbnail result = DatasetUtil.getThumbnail(dataset);
        assertNull(result);
    }
    /**
     * Test of deleteDatasetLogo method, of class DatasetUtil.
     */
    @Test
    public void testDeleteDatasetLogo() {
        assertEquals(false, DatasetUtil.deleteDatasetLogo(null));
        assertEquals(false, DatasetUtil.deleteDatasetLogo(new Dataset()));
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
        assertNull(DatasetUtil.persistDatasetLogoToStorageAndCreateThumbnail(null, null));
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
        assertEquals(false, DatasetUtil.isDatasetLogoPresent(dataset));
    }

    @Test
    public void testGetDatasetSummaryField_defaultSelectionWithAndWithoutMatches() {
        DatasetVersion version = new DatasetVersion();
        List<DatasetField> fields = new ArrayList<DatasetField>();

        String[] fieldNames = {"subject", "keyword", "random-notInDefault"};
        for (String fieldName : fieldNames) {
            DatasetField field = DatasetField.createNewEmptyDatasetField(new DatasetFieldType(fieldName, FieldType.TEXT, false), version);
            field.setId(1l);
            fields.add(field);
        }
        version.setDatasetFields(fields);

        assertEquals(2, DatasetUtil.getDatasetSummaryFields(version, null).size());
        assertEquals(2, DatasetUtil.getDatasetSummaryFields(version, "").size());
    }

    @Test
    public void testGetDatasetSummaryField_defaultSelectionWithoutDatasetFields() {
        DatasetVersion version = new DatasetVersion();
        List<DatasetField> fields = new ArrayList<DatasetField>();
        version.setDatasetFields(fields);

        assertEquals(0, DatasetUtil.getDatasetSummaryFields(version, null).size());
        assertEquals(0, DatasetUtil.getDatasetSummaryFields(version, "").size());
    }

    @Test
    public void testGetDatasetSummaryField_withSelectionWithoutDatasetFields() {
        DatasetVersion version = new DatasetVersion();
        List<DatasetField> fields = new ArrayList<DatasetField>();
        version.setDatasetFields(fields);

        assertEquals(0, DatasetUtil.getDatasetSummaryFields(version, "subject,randomSelector").size());
    }

    @Test
    public void testGetDatasetSummaryField_withSelectionWithoutMatches() {
        DatasetVersion version = new DatasetVersion();
        List<DatasetField> fields = new ArrayList<DatasetField>();

        String[] fieldNames = {"subject"};
        for (String fieldName : fieldNames) {
            DatasetField field = DatasetField.createNewEmptyDatasetField(new DatasetFieldType(fieldName, FieldType.TEXT, false), version);
            field.setId(1l);
            fields.add(field);
        }
        version.setDatasetFields(fields);

        assertEquals(0, DatasetUtil.getDatasetSummaryFields(version, "object").size());
    }
}
