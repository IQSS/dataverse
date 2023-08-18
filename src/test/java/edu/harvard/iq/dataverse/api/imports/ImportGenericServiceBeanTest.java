package edu.harvard.iq.dataverse.api.imports;

import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
public class ImportGenericServiceBeanTest {

    @InjectMocks
    private ImportGenericServiceBean importGenericService;

    @Test
    public void testReassignIdentifierAsGlobalId() {
        // non-URL
        assertEquals("doi:10.7910/DVN/TJCLKP", importGenericService.reassignIdentifierAsGlobalId("doi:10.7910/DVN/TJCLKP", new DatasetDTO()));
        assertEquals("hdl:10.7910/DVN/TJCLKP", importGenericService.reassignIdentifierAsGlobalId("hdl:10.7910/DVN/TJCLKP", new DatasetDTO()));
        // HTTPS
        assertEquals("doi:10.7910/DVN/TJCLKP", importGenericService.reassignIdentifierAsGlobalId("https://doi.org/10.7910/DVN/TJCLKP", new DatasetDTO()));
        assertEquals("doi:10.7910/DVN/TJCLKP", importGenericService.reassignIdentifierAsGlobalId("https://dx.doi.org/10.7910/DVN/TJCLKP", new DatasetDTO()));
        assertEquals("hdl:10.7910/DVN/TJCLKP", importGenericService.reassignIdentifierAsGlobalId("https://hdl.handle.net/10.7910/DVN/TJCLKP", new DatasetDTO()));
        // HTTP (no S)
        assertEquals("doi:10.7910/DVN/TJCLKP", importGenericService.reassignIdentifierAsGlobalId("http://doi.org/10.7910/DVN/TJCLKP", new DatasetDTO()));
        assertEquals("doi:10.7910/DVN/TJCLKP", importGenericService.reassignIdentifierAsGlobalId("http://dx.doi.org/10.7910/DVN/TJCLKP", new DatasetDTO()));
        assertEquals("hdl:10.7910/DVN/TJCLKP", importGenericService.reassignIdentifierAsGlobalId("http://hdl.handle.net/10.7910/DVN/TJCLKP", new DatasetDTO()));
        // junk
        assertNull(importGenericService.reassignIdentifierAsGlobalId("junk", new DatasetDTO()));
    }

}
