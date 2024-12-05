package edu.harvard.iq.dataverse.api.imports;

import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.api.dto.DatasetVersionDTO;

import org.apache.commons.io.FileUtils;
import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;

@ExtendWith(MockitoExtension.class)
public class ImportGenericServiceBeanTest {

    @InjectMocks
    private ImportGenericServiceBean importGenericService;

    @Test
    void testIdentifierHarvestableWithOtherID() throws IOException {
        // "otherIdValue" containing the value : doi:10.7910/DVN/TJCLKP
        File file = new File("src/test/resources/json/importGenericWithOtherId.json");
        String text = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        DatasetVersionDTO dto = new Gson().fromJson(text, DatasetVersionDTO.class);
            
        assertEquals("doi:10.7910/DVN/TJCLKP", importGenericService.selectIdentifier(dto, "https://doi.org/10.7910/DVN/TJCLKP"));
        // junk or null
        assertEquals("doi:10.7910/DVN/TJCLKP", importGenericService.selectIdentifier(dto, "junk"));
        assertEquals("doi:10.7910/DVN/TJCLKP", importGenericService.selectIdentifier(dto, null));
        assertEquals("doi:10.7910/DVN/TJCLKP", importGenericService.selectIdentifier(dto, "http://www.example.com"));
        assertEquals("doi:10.7910/DVN/TJCLKP", importGenericService.selectIdentifier(dto, "https://dataverse.org"));
    }

    @Test
    void testIdentifierHarvestableWithoutOtherID() throws IOException {
        // Does not contain data of type "otherIdValue"
        File file = new File("src/test/resources/json/importGenericWithoutOtherId.json");
        String text = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        DatasetVersionDTO dto = new Gson().fromJson(text, DatasetVersionDTO.class);
                
        // non-URL
        assertEquals("doi:10.7910/DVN/TJCLKP", importGenericService.selectIdentifier(dto, "doi:10.7910/DVN/TJCLKP"));
        assertEquals("hdl:10.7910/DVN/TJCLKP", importGenericService.selectIdentifier(dto, "hdl:10.7910/DVN/TJCLKP"));
        // HTTPS
        assertEquals("https://doi.org/10.7910/DVN/TJCLKP", importGenericService.selectIdentifier(dto, "https://doi.org/10.7910/DVN/TJCLKP"));
        assertEquals("https://dx.doi.org/10.7910/DVN/TJCLKP", importGenericService.selectIdentifier(dto, "https://dx.doi.org/10.7910/DVN/TJCLKP"));
        assertEquals("https://hdl.handle.net/10.7910/DVN/TJCLKP", importGenericService.selectIdentifier(dto, "https://hdl.handle.net/10.7910/DVN/TJCLKP"));
        // HTTP (no S)
        assertEquals("http://doi.org/10.7910/DVN/TJCLKP", importGenericService.selectIdentifier(dto, "http://doi.org/10.7910/DVN/TJCLKP"));
        assertEquals("http://dx.doi.org/10.7910/DVN/TJCLKP", importGenericService.selectIdentifier(dto, "http://dx.doi.org/10.7910/DVN/TJCLKP"));
        assertEquals("http://hdl.handle.net/10.7910/DVN/TJCLKP", importGenericService.selectIdentifier(dto, "http://hdl.handle.net/10.7910/DVN/TJCLKP"));
        // junk or null
        assertNull(importGenericService.selectIdentifier(dto, "junk"));
        assertNull(importGenericService.selectIdentifier(dto, null));
        assertNull(importGenericService.selectIdentifier(dto, "http://www.example.com"));
        assertNull(importGenericService.selectIdentifier(dto, "https://dataverse.org"));
    }

    @Test
    void testReassignIdentifierAsGlobalId() {
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
        assertNull(importGenericService.reassignIdentifierAsGlobalId("http://www.example.com", new DatasetDTO()));
        assertNull(importGenericService.reassignIdentifierAsGlobalId("https://dataverse.org", new DatasetDTO()));
    }

}
