package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldType.FieldType;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import edu.harvard.iq.dataverse.util.testing.SystemProperty;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DatasetUtilTest {

    /**
     * Test of getThumbnailCandidates method, of class DatasetUtil.
     */
    @Test
    @SystemProperty(key = "dataverse.files.testfile.type", value = "file")
    void testGetThumbnailCandidates() {
        assertEquals(new ArrayList<>(), DatasetUtil.getThumbnailCandidates(null, false, ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE));

        Dataset dataset = MocksFactory.makeDataset();
        DataFile dataFile = MocksFactory.makeDataFile();
        dataFile.setContentType("image/");
        dataFile.setOwner(dataset);
        dataFile.setStorageIdentifier("testfile://src/test/resources/images/coffeeshop.png");

        //System.out.println(ImageThumbConverter.isThumbnailAvailable(dataFile));
        DatasetVersion version = dataset.getCreateVersion(null);
        List<FileMetadata> fmds = new ArrayList<>();
        fmds.add(MocksFactory.addFileMetadata(dataFile));
        version.setFileMetadatas(fmds);
        assertEquals(new ArrayList<>(), DatasetUtil.getThumbnailCandidates(dataset, false, ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE));
    }

    @Test
    @SystemProperty(key = "dataverse.files.testfile.type", value = "file")
    void testGetThumbnailNullDataset() {
        assertNull(DatasetUtil.getThumbnail(null, ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE));
        assertNull(DatasetUtil.getThumbnail(null, null, ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE));

        Dataset dataset = MocksFactory.makeDataset();
        dataset.setStorageIdentifier("testfile://");
        dataset.setUseGenericThumbnail(true);

        assertNull(DatasetUtil.getThumbnail(dataset, ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE));
        assertNull(DatasetUtil.getThumbnail(dataset, new DatasetVersion(), ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE));
    }

    @Test
    void testGetThumbnailRestricted() {
        //System.out.println("testGetThumbnailRestricted");
        Dataset dataset = new Dataset();
        DataFile thumbnailFile = new DataFile();
        thumbnailFile.setId(42l);
        thumbnailFile.setRestricted(true);
        dataset.setThumbnailFile(thumbnailFile);
        DatasetThumbnail result = DatasetUtil.getThumbnail(dataset, ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE);
        assertNull(result);
    }
    /**
     * Test of deleteDatasetLogo method, of class DatasetUtil.
     */
    @Test
    void testDeleteDatasetLogo() {
        assertEquals(false, DatasetUtil.deleteDatasetLogo(null));
        assertEquals(false, DatasetUtil.deleteDatasetLogo(new Dataset()));
    }

    /**
     * Test of getDefaultThumbnailFile method, of class DatasetUtil.
     */
    @Test
    void testGetDefaultThumbnailFile() {
        assertNull(DatasetUtil.attemptToAutomaticallySelectThumbnailFromDataFiles(null, null));
    }

    /**
     * Test of persistDatasetLogoToStorageAndCreateThumbnail method, of class
     * DatasetUtil.
     */
    @Test
    void testPersistDatasetLogoToStorageAndCreateThumbnail() {
        assertNull(DatasetUtil.persistDatasetLogoToStorageAndCreateThumbnails(null, null));
        //Todo: a test for this that test main logic
    }

    /**
     * Test of getThumbnailAsInputStream method, of class DatasetUtil.
     */
    @Test
    void testGetThumbnailAsInputStream() {
        assertNull(DatasetUtil.getThumbnailAsInputStream(null, ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE));
    }

    /**
     * Test of isDatasetLogoPresent method, of class DatasetUtil.
     */
    @Test
    void testIsDatasetLogoPresent() {
        Dataset dataset = MocksFactory.makeDataset();
        assertEquals(false, DatasetUtil.isDatasetLogoPresent(dataset, ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE));
    }

    @Test
    void testGetDatasetSummaryField_defaultSelectionWithAndWithoutMatches() {
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
    void testGetDatasetSummaryField_defaultSelectionWithoutDatasetFields() {
        DatasetVersion version = new DatasetVersion();
        List<DatasetField> fields = new ArrayList<DatasetField>();
        version.setDatasetFields(fields);

        assertEquals(0, DatasetUtil.getDatasetSummaryFields(version, null).size());
        assertEquals(0, DatasetUtil.getDatasetSummaryFields(version, "").size());
    }

    @Test
    void testGetDatasetSummaryField_withSelectionWithoutDatasetFields() {
        DatasetVersion version = new DatasetVersion();
        List<DatasetField> fields = new ArrayList<DatasetField>();
        version.setDatasetFields(fields);

        assertEquals(0, DatasetUtil.getDatasetSummaryFields(version, "subject,randomSelector").size());
    }

    @Test
    void testGetDatasetSummaryField_withSelectionWithoutMatches() {
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
