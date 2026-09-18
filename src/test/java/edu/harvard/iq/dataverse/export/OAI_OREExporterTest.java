package edu.harvard.iq.dataverse.export;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OAI_OREExporterTest {

    @Test
    public void testGetMediaType() {
        OAI_OREExporter exporter = new OAI_OREExporter();

        assertEquals("application/ld+json", exporter.getMediaType());
    }
}
