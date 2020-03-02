package edu.harvard.iq.dataverse.util.json;

import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.persistence.workflow.Workflow;
import org.junit.Test;

import javax.json.JsonObject;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

/**
 * @author michael
 */
public class BriefJsonPrinterTest {

    public BriefJsonPrinterTest() {
    }

    /**
     * Test of json method, of class BriefJsonPrinter.
     */
    @Test
    public void testJson_DatasetVersion() {
        //given
        Dataset ds = MocksFactory.makeDataset();
        DatasetVersion dsv = MocksFactory.makeDatasetVersion(ds.getCategories());

        dsv.setId(1L);
        dsv.setVersion(2l);
        dsv.setVersionState(DatasetVersion.VersionState.DEACCESSIONED);

        DatasetField titleFld = new DatasetField();
        titleFld.setDatasetFieldType(new DatasetFieldType(DatasetFieldConstant.title, FieldType.TEXT, false));
        titleFld.setFieldValue("Dataset Title");
        dsv.setDatasetFields(Collections.singletonList(titleFld));
        BriefJsonPrinter sut = new BriefJsonPrinter();

        //when
        JsonObject res = sut.json(dsv).build();

        //then
        assertEquals("Dataset Title", res.getString("title"));
        assertEquals(DatasetVersion.VersionState.DEACCESSIONED.name(), res.getString("versionState"));
        assertEquals(1, res.getInt("id"));
    }

    /**
     * Test of json method, of class BriefJsonPrinter.
     */
    @Test
    public void testJson_MetadataBlock() {
        //given
        MetadataBlock mtb = new MetadataBlock();
        mtb.setId(1L);
        mtb.setName("metadata_block_name");
        mtb.setDisplayName("Metadata Block Name");
        BriefJsonPrinter sut = new BriefJsonPrinter();

        //when
        JsonObject res = sut.json(mtb).build();

        //then
        assertEquals("Metadata Block Name", res.getString("displayName"));
        assertEquals("metadata_block_name", res.getString("name"));
        assertEquals(1, res.getInt("id"));
        assertEquals(3, res.keySet().size());
    }

    /**
     * Test of json method, of class BriefJsonPrinter.
     */
    @Test
    public void testJson_Workflow() {
        //given
        Workflow wf = new Workflow();
        wf.setId(1l);
        wf.setName("Name");
        BriefJsonPrinter sut = new BriefJsonPrinter();

        //when
        JsonObject res = sut.json(wf).build();

        //then
        assertEquals("Name", res.getString("name"));
        assertEquals(1, res.getInt("id"));
        assertEquals(2, res.keySet().size());

    }

}
