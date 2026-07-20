package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.util.SystemConfig;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class EditDatafilesPageTest {
    
    @InjectMocks
    private EditDatafilesPage editDatafilesPage;
    
    @Mock
    private SystemConfig systemConfig;
    
    public EditDatafilesPageTest() {
        MockitoAnnotations.openMocks(this);
    }
    
    @Test
    void testPopulateHumanPerFormatTabularLimits_WithEmptyLimits() {
        Map<String, Long> tabularLimits = new HashMap<>();
        when(systemConfig.getTabularIngestSizeLimits()).thenReturn(tabularLimits);
        
        String result = editDatafilesPage.populateHumanPerFormatTabularLimits();
        
        assertEquals("", result, "Expected no formatted limits when the map is empty");
    }
    
    @Test
    void testPopulateHumanPerFormatTabularLimits_WithNonDefaultLimits() {
        Map<String, Long> tabularLimits = new HashMap<>();
        tabularLimits.put("csv", 10485760L); // 10MB
        tabularLimits.put("tsv", 5242880L);  // 5MB
        when(systemConfig.getTabularIngestSizeLimits()).thenReturn(tabularLimits);
        
        String result = editDatafilesPage.populateHumanPerFormatTabularLimits();
        
        assertTrue(result.contains("csv: 10.0 MB"), "Expected CSV limit in human-readable format, but got: " + result);
        assertTrue(result.contains("tsv: 5.0 MB"), "Expected TSV limit in human-readable format, but got: " + result);
    }
    
    @Test
    void testPopulateHumanPerFormatTabularLimits_WithDefaultKey() {
        Map<String, Long> tabularLimits = new HashMap<>();
        tabularLimits.put(SystemConfig.TABULAR_INGEST_SIZE_LIMITS_DEFAULT_KEY, 2097152L); // 2MB
        tabularLimits.put("csv", 10485760L); // 10MB
        when(systemConfig.getTabularIngestSizeLimits()).thenReturn(tabularLimits);
        
        String result = editDatafilesPage.populateHumanPerFormatTabularLimits();
        
        assertTrue(result.contains("csv: 10.0 MB"), "Expected CSV limit in human-readable format, but got: " + result);
        assertFalse(result.contains("default"), "Default key should be excluded from the output");
    }
}