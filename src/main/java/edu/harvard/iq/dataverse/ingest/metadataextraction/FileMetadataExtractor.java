/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.ingest.metadataextraction;

import edu.harvard.iq.dataverse.ingest.metadataextraction.spi.*;
import java.io.*;
import java.util.Map; 
import java.util.Set; 

/**
 *
 * @author leonidandreev
 */
public abstract class FileMetadataExtractor {
    
    public static String INGESTER_VERSION = "4.0";


    protected FileMetadataExtractorSpi originatingProvider;

    protected FileMetadataExtractor(FileMetadataExtractorSpi originatingProvider){
        this.originatingProvider = originatingProvider;
    }

    public FileMetadataExtractorSpi getOriginatingProvider() {
        return originatingProvider;
    }
    
    public String getFormatName() throws IOException {
        return originatingProvider.getFormatNames()[0];
    }
    
    public void dispose() {
    
    }
    
    
    //public abstract Map<String, Set<String>> ingest(BufferedInputStream stream)
    public abstract FileMetadataIngest ingest(BufferedInputStream stream)
        throws IOException;

    
    // should this be an abstract method as well? 
    
    public boolean isValid(File ddiFile) throws IOException {
        return false;
    }

    // Utility methods


    public void printHexDump(byte[] buff, String hdr) {
        int counter = 0;
        if (hdr != null) {
            System.out.println(hdr);
        }
        for (int i = 0; i < buff.length; i++) {
            counter = i + 1;
            System.out.print(String.format("%02X ", buff[i]));
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

    /**
     * Returns a new null-character-free <code>String</code> object 
     * from an original <code>String</code> one that may contains
     * null characters.
     * 
     * @param rawString a<code>String</code> object
     * @return a new, null-character-free <code>String</code> object
     */
    protected String getNullStrippedString(String rawString){
        String nullRemovedString = null;
        int null_position = rawString.indexOf(0);
        if (null_position >= 0){
            // string is terminated by the null
            nullRemovedString = rawString.substring(0, null_position);
        } else {
            // not null-termiated (sometimes space-paddded, instead)
            nullRemovedString = rawString;
        }
        return nullRemovedString;
    }
}
