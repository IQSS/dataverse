package edu.harvard.iq.dataverse.api.imports;

import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ImportGenericServiceBeanTest {

    @InjectMocks
    private ImportGenericServiceBean importGenericService;

    @Test
    public void testReassignIdentifierAsGlobalId() {
        // non-URL
        Assert.assertEquals("doi:10.7910/DVN/TJCLKP", importGenericService.reassignIdentifierAsGlobalId("doi:10.7910/DVN/TJCLKP", new DatasetDTO()));
        Assert.assertEquals("hdl:10.7910/DVN/TJCLKP", importGenericService.reassignIdentifierAsGlobalId("hdl:10.7910/DVN/TJCLKP", new DatasetDTO()));
        // HTTPS
        Assert.assertEquals("doi:10.7910/DVN/TJCLKP", importGenericService.reassignIdentifierAsGlobalId("https://doi.org/10.7910/DVN/TJCLKP", new DatasetDTO()));
        Assert.assertEquals("doi:10.7910/DVN/TJCLKP", importGenericService.reassignIdentifierAsGlobalId("https://dx.doi.org/10.7910/DVN/TJCLKP", new DatasetDTO()));
        Assert.assertEquals("hdl:10.7910/DVN/TJCLKP", importGenericService.reassignIdentifierAsGlobalId("https://hdl.handle.net/10.7910/DVN/TJCLKP", new DatasetDTO()));
        // HTTP (no S)
        Assert.assertEquals("doi:10.7910/DVN/TJCLKP", importGenericService.reassignIdentifierAsGlobalId("http://doi.org/10.7910/DVN/TJCLKP", new DatasetDTO()));
        Assert.assertEquals("doi:10.7910/DVN/TJCLKP", importGenericService.reassignIdentifierAsGlobalId("http://dx.doi.org/10.7910/DVN/TJCLKP", new DatasetDTO()));
        Assert.assertEquals("hdl:10.7910/DVN/TJCLKP", importGenericService.reassignIdentifierAsGlobalId("http://hdl.handle.net/10.7910/DVN/TJCLKP", new DatasetDTO()));
        // junk
        Assert.assertEquals(null, importGenericService.reassignIdentifierAsGlobalId("junk", new DatasetDTO()));
    }

}
