package edu.harvard.iq.dataverse.ingest.metadataextraction.impl.plugins.netcdf;

import edu.harvard.iq.dataverse.ingest.metadataextraction.FileMetadataExtractor;
import edu.harvard.iq.dataverse.ingest.metadataextraction.spi.FileMetadataExtractorSpi;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class NetcdfFileMetadataExtractorSpi extends FileMetadataExtractorSpi {

    @Override
    public String getDescription(Locale locale) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean canDecodeInput(Object source) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean canDecodeInput(File file) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean canDecodeInput(BufferedInputStream stream) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public FileMetadataExtractor createIngesterInstance(Object extension) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

}
