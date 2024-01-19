/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.ingest.metadataextraction.spi;

import edu.harvard.iq.dataverse.ingest.metadataextraction.*;
import java.util.logging.*;
import java.io.*;

import edu.harvard.iq.dataverse.ingest.plugin.spi.IngestServiceProvider;
import java.nio.MappedByteBuffer;
import java.util.Locale;

/**
 *
 * @author Leonid Andreev
 */

public abstract class FileMetadataExtractorSpi extends IngestServiceProvider {
    private static Logger dbgLog = 
    Logger.getLogger(FileMetadataExtractorSpi.class.getPackage().getName());

    
    protected FileMetadataExtractorSpi() {
    }

    
    protected String vendorName;
    protected String version;

    public FileMetadataExtractorSpi(String vendorName, String version) {
        if (vendorName == null){
            throw new IllegalArgumentException("vendorName is null!");
        }
        if (version == null){
            throw new IllegalArgumentException("version string is null");
        }
        this.vendorName = vendorName;
        this.version = version;
    }

    public abstract String getDescription(Locale locale);
    
    protected String[] names = null;

    public String[] getFormatNames() {
        return (String[])names.clone();
    }

    protected String[] suffixes = null;
    
    public String[] getFileSuffixes() {
        return suffixes == null ? null : (String[])suffixes.clone();
    }
    
    
    protected String[] MIMETypes = null;
    
    public String[] getMIMETypes() {
        return MIMETypes == null ? null : (String[])MIMETypes.clone();
    }
    
    protected String pluginClassName = null;

    public String getPluginClassName() {
        return pluginClassName;
    }

   
    public FileMetadataExtractorSpi(
            String vendorName,
            String version,
            String[] names,
            String[] suffixes,
            String[] MIMETypes,
            String pluginClassName
            ) {
        
        this(vendorName, version);

        if (names == null) {
            throw new IllegalArgumentException("names is null!");
        }

        if (names.length == 0) {
            throw new IllegalArgumentException("names.length is 0!");
        }
        this.names = (String[])names.clone();
        if (pluginClassName == null) {
            throw new IllegalArgumentException("pluginClassName is null!");
        }

        if (suffixes != null && suffixes.length > 0) {
            this.suffixes = (String[])suffixes.clone();
        }

        if (MIMETypes != null && MIMETypes.length > 0) {
            this.MIMETypes = (String[])MIMETypes.clone();
        }

        this.pluginClassName = pluginClassName;
    }

    public void printHexDump(MappedByteBuffer buff, String hdr) {
        int counter = 0;
        if (hdr != null) {
            System.out.println(hdr);
        }
        for (int i = 0; i < buff.capacity(); i++) {
            counter = i + 1;
            System.out.println(String.format("%02X ", buff.get()));
            if (counter % 16 == 0) {
                System.out.println();
            } else {
                if (counter % 8 == 0) {
                    System.out.print(" ");
                }
            }
        }
        System.out.println();
        buff.rewind();
    }
    
    public void printHexDump(byte[] buff, String hdr) {
        int counter = 0;
        if (hdr != null) {
            System.out.println(hdr);
        }
        for (int i = 0; i < buff.length; i++) {
            counter = i + 1;
            System.out.println(String.format("%02X ", buff[i]));
            if (counter % 16 == 0) {
                System.out.println();
            } else {
                if (counter % 8 == 0) {
                    System.out.print(" ");
                }
            }
        }
        System.out.println();
    }

    public abstract boolean canDecodeInput(Object source) throws IOException;
    
    
    public abstract boolean canDecodeInput(File file) throws IOException;
    public abstract boolean canDecodeInput(BufferedInputStream stream) throws IOException;
    
    public abstract FileMetadataExtractor createIngesterInstance(Object extension)
        throws IOException;
    
    public FileMetadataExtractor createIngesterInstance() throws IOException{
        return createIngesterInstance(null);
    }
    
    public boolean isOwnReader(FileMetadataExtractor reader) {
        if (reader == null) {
            throw new IllegalArgumentException("reader == null!");
        }
        String name = reader.getClass().getName();
        return name.equals(pluginClassName);
    }

    
}
