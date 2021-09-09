package edu.harvard.iq.dataverse.export.openaire;

import edu.harvard.iq.dataverse.export.OpenAireExporter;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.Assert.*;

@ExtendWith(MockitoExtension.class)
class OpenAireExporterTest {

    @InjectMocks
    private OpenAireExporter instance;

    @Mock private SettingsServiceBean settingsService;
    @Mock private JsonPrinter jsonPrinter;

    // -------------------- TESTS --------------------

    @Test
    void testGetProviderName() {

        // when
        String result = instance.getProviderName();

        // then
        assertEquals("oai_datacite", result);
    }

    @Test
    void testGetDisplayName() {

        // when
        String result = instance.getDisplayName();

        // then
        assertEquals("OpenAIRE", result);
    }

    @Test
    void testIsXMLFormat() {

        // when
        Boolean result = instance.isXMLFormat();

        // then
        assertTrue(result);
    }

    @Test
    void testIsHarvestable() {

        // when
        Boolean result = instance.isHarvestable();

        // then
        assertTrue(result);
    }

    @Test
    void testIsAvailableToUsers() {

        // when
        Boolean result = instance.isAvailableToUsers();

        // then
        assertTrue(result);
    }

    @Test
    void testGetXMLNameSpace() {

        // when
        String result = instance.getXMLNameSpace();

        // then
        assertEquals("http://datacite.org/schema/kernel-3", result);
    }

    @Test
    void testGetXMLSchemaLocation() {

        // when
        String result = instance.getXMLSchemaLocation();

        // then
        assertEquals("http://schema.datacite.org/meta/kernel-3.1/metadata.xsd", result);
    }

    @Test
    void testGetXMLSchemaVersion() {

        // when
        String result = instance.getXMLSchemaVersion();

        // then
        assertEquals("3.1", result);
    }
}