/*
   Copyright (C) 2005-2012, by the President and Fellows of Harvard College.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   Dataverse Network - A web application to share, preserve and analyze research data.
   Developed at the Institute for Quantitative Social Science, Harvard University.
   Version 3.0.
*/

package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.DataFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;

/**
 * a 4.0 implementation of the DVN FileUtil;
 * it provides some of the functionality from the 3.6 implementation, 
 * but the old code is ported creatively on the method-by-method basis.
 * 
 * @author Leonid Andreev
 */
public class FileUtil implements java.io.Serializable  {

    public FileUtil() {
    }
    
    public static void copyFile(File inputFile, File outputFile) throws IOException {
        FileChannel in = null;
        WritableByteChannel out = null;
        
        try {
            in = new FileInputStream(inputFile).getChannel();
            out = new FileOutputStream(outputFile).getChannel();
            long bytesPerIteration = 50000;
            long start = 0;
            while ( start < in.size() ) {
                in.transferTo(start, bytesPerIteration, out);
                start += bytesPerIteration;
            }
            
        } finally {
            if (in != null) { in.close(); }
            if (out != null) { out.close(); }
        }
    }

   
    public static String getFileExtension(String fileName){
        String ext = null;
        if ( fileName.lastIndexOf(".") != -1){
            ext = (fileName.substring( fileName.lastIndexOf(".") + 1 )).toLowerCase();
        }
        return ext;
    } 

    public static String replaceExtension(String originalName) {
       return replaceExtension(originalName, "tab");
    }   
    
    public static String replaceExtension(String originalName, String newExtension) {
        int extensionIndex = originalName.lastIndexOf(".");
        if (extensionIndex != -1 ) {
            return originalName.substring(0, extensionIndex) + "."+newExtension ;
        } else {
            return originalName +"."+newExtension ;
        }
    }
    
    public static String getUserFriendlyFileType(DataFile dataFile) {
        String fileType = dataFile.getContentType();
         
        if (fileType != null) {
            if (fileType.indexOf(";") != -1) {
                fileType = fileType.substring(0, fileType.indexOf(";"));
            }

            try {
                return ResourceBundle.getBundle("MimeTypeDisplay").getString(fileType);
            } catch (MissingResourceException e) {
                return fileType;
            }
        }

        return fileType;
    }
    
    public static String getFacetFileType(DataFile dataFile) {
        String fileType = dataFile.getContentType();
        
        if (fileType != null) {
            if (fileType.indexOf(";") != -1) {
                fileType = fileType.substring(0, fileType.indexOf(";"));
            }

            try {
                return ResourceBundle.getBundle("MimeTypeFacets").getString(fileType);
            } catch (MissingResourceException e) {
                // if there's no defined "facet-friendly" form of this mime type
                // we'll truncate the available type by "/", e.g., all the 
                // unknown image/* types will become "image"; many other, quite
                // different types will all become "application" this way - 
                // but it is probably still better than to tag them all as 
                // "uknown". 
                // -- L.A. 4.0 alpha 1
                return fileType.split("/")[0];
            }
        }
        
        return "unknown"; 
    }
}
