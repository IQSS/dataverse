package edu.harvard.iq.dataverse.util.json;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import edu.harvard.iq.dataverse.workflow.Workflow;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
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
        Dataset ds = MocksFactory.makeDataset();
        DatasetVersion dsv = MocksFactory.makeDatasetVersion( ds.getCategories() );
        
        dsv.setId(1L);
        dsv.setVersion(2l);
        dsv.setVersionState(DatasetVersion.VersionState.DEACCESSIONED);
        
        DatasetField titleFld = new DatasetField();
        titleFld.setDatasetFieldType( new DatasetFieldType(DatasetFieldConstant.title, DatasetFieldType.FieldType.TEXT, false) );
        titleFld.setSingleValue("Dataset Title");
        dsv.setDatasetFields( Collections.singletonList(titleFld) );
        
        BriefJsonPrinter sut = new BriefJsonPrinter();
        JsonObject res = sut.json(dsv).build();
        
        assertEquals("Dataset Title", res.getString("title"));
        assertEquals(DatasetVersion.VersionState.DEACCESSIONED.name(), res.getString("versionState"));
        assertEquals(1, res.getInt("id"));        
    }

    /**
     * Test of json method, of class BriefJsonPrinter.
     */
    @Test
    public void testJson_MetadataBlock() {
        MetadataBlock mtb = new MetadataBlock();
        mtb.setId(1L);
        mtb.setName("metadata_block_name");
        mtb.setDisplayName("Metadata Block Name");
        
        BriefJsonPrinter sut = new BriefJsonPrinter();
        JsonObject res = sut.json(mtb).build();
        
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
        Workflow wf = new Workflow();
        wf.setId(1l);
        wf.setName("Name");
        
        BriefJsonPrinter sut = new BriefJsonPrinter();
        JsonObject res = sut.json(wf).build();
        
        assertEquals("Name", res.getString("name"));        
        assertEquals(1, res.getInt("id"));        
        assertEquals(2, res.keySet().size());
        
    }
    
}
