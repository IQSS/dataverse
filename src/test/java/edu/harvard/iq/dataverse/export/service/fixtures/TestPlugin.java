package edu.harvard.iq.dataverse.export.service.fixtures;

import io.gdcc.spi.export.ExportDataProvider;
import io.gdcc.spi.export.ExportException;
import io.gdcc.spi.export.Exporter;
import io.gdcc.spi.meta.plugin.Plugin;

import java.io.OutputStream;
import java.util.Locale;

public interface TestPlugin extends Plugin {
    class TestExporter implements TestPlugin, Exporter {
        @Override
        public String identity() {
            return "test";
        }
        
        @Override
        public void exportDataset(ExportDataProvider exportDataProvider, OutputStream outputStream) throws ExportException {
        }
        
        @Override
        public String getFormatName() {
            return identity();
        }
        
        @Override
        public String getDisplayName(Locale locale) {
            return "test";
        }
        
        @Override
        public Boolean isHarvestable() {
            return true;
        }
        
        @Override
        public Boolean isAvailableToUsers() {
            return true;
        }
        
        @Override
        public String getMediaType() {
            return "test/test";
        }
    }
}
