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
import edu.harvard.iq.dataverse.ingest.IngestableDataChecker;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.activation.MimetypesFileTypeMap;
import javax.ejb.EJBException;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.zip.GZIPInputStream;

/**
 * a 4.0 implementation of the DVN FileUtil;
 * it provides some of the functionality from the 3.6 implementation, 
 * but the old code is ported creatively on the method-by-method basis.
 * 
 * @author Leonid Andreev
 */
public class FileUtil implements java.io.Serializable  {
    private static final Logger logger = Logger.getLogger(FileUtil.class.getCanonicalName());
    
    private static final String[] TABULAR_DATA_FORMAT_SET = {"POR", "SAV", "DTA", "RDA"};
    
    private static Map<String, String> STATISTICAL_SYNTAX_FILE_EXTENSION = new HashMap<String, String>();
    
    static {
        STATISTICAL_SYNTAX_FILE_EXTENSION.put("do",  "x-stata-syntax");
        STATISTICAL_SYNTAX_FILE_EXTENSION.put("sas", "x-sas-syntax");
        STATISTICAL_SYNTAX_FILE_EXTENSION.put("sps", "x-spss-syntax");
        STATISTICAL_SYNTAX_FILE_EXTENSION.put("rdat", "x-rdata-syntax");
    }
    
    private static MimetypesFileTypeMap MIME_TYPE_MAP = new MimetypesFileTypeMap();

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
            if (fileType.equalsIgnoreCase(ShapefileHandler.SHAPEFILE_FILE_TYPE)){
                return ShapefileHandler.SHAPEFILE_FILE_TYPE_FRIENDLY_NAME;
            }
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
    
    public static String determineFileType(File f, String fileName) throws IOException{
        String fileType = null;
        String fileExtension = getFileExtension(fileName);

        
        
        // step 1: 
        // Apply our custom methods to try and recognize data files that can be 
        // converted to tabular data, or can be parsed for extra metadata 
        // (such as FITS).
        logger.fine("Attempting to identify potential tabular data files;");
        IngestableDataChecker tabChk = new IngestableDataChecker(TABULAR_DATA_FORMAT_SET);
        
        fileType = tabChk.detectTabularDataFormat(f);
        
        logger.fine("determineFileType: tabular data checker found "+fileType);
                
        // step 2: If not found, check if graphml or FITS
        if (fileType==null) {
            if (isGraphMLFile(f))  {
                fileType = "text/xml-graphml";
            } else // Check for FITS:
            // our check is fairly weak (it appears to be hard to really
            // really recognize a FITS file without reading the entire 
            // stream...), so in version 3.* we used to nsist on *both* 
            // the ".fits" extension and the header check;
            // in 4.0, we'll accept either the extension, or the valid 
            // magic header:
            if (isFITSFile(f) || (fileExtension != null
                    && fileExtension.equalsIgnoreCase("fits"))) {
                fileType = "application/fits";
            }
        }
       
        // step 3: check the mime type of this file with Jhove
        if (fileType == null){
            JhoveFileType jw = new JhoveFileType();
            fileType = jw.getFileMimeType(f);
        }
        
        // step 4: 
        // Additional processing; if we haven't gotten much useful information 
        // back from Jhove, we'll try and make an educated guess based on 
        // the file extension:
        
        if ( fileExtension != null) {
            logger.fine("fileExtension="+fileExtension);

            if (fileType != null && fileType.startsWith("text/plain")){
                if (( fileExtension != null) && (STATISTICAL_SYNTAX_FILE_EXTENSION.containsKey(fileExtension))) {
                    // replace the mime type with the value of the HashMap
                    fileType = STATISTICAL_SYNTAX_FILE_EXTENSION.get(fileExtension);
                }
            } else if ("application/octet-stream".equals(fileType)) {
                fileType = determineFileType(fileName);
                logger.fine("mime type recognized by extension: "+fileType);
            }
        } else {
            logger.fine("fileExtension is null");
        }
        
        // step 5: 
        // if this is a compressed file - zip or gzip - we'll check the 
        // file(s) inside the compressed stream and see if it's one of our
        // recognized formats that we want to support compressed:

        if ("application/x-gzip".equals(fileType)) {
            logger.fine("we'll run additional checks on this gzipped file.");
            // We want to be able to support gzipped FITS files, same way as
            // if they were just regular FITS files:
            FileInputStream gzippedIn = new FileInputStream(f);
            // (new FileInputStream() can throw a "filen not found" exception;
            // however, if we've made it this far, it really means that the 
            // file does exist and can be opened)
            InputStream uncompressedIn = null; 
            try {
                uncompressedIn = new GZIPInputStream(gzippedIn);
                if (isFITSFile(uncompressedIn)) {
                    fileType = "application/fits-gzipped";
                }
            } catch (IOException ioex) {
                if (uncompressedIn != null) {
                    try {uncompressedIn.close();} catch (IOException e) {}
                }
            }
        } 
        if ("application/zip".equals(fileType)) {
            
            // Is this a zipped Shapefile?
            // Check for shapefile extensions as described here: http://en.wikipedia.org/wiki/Shapefile
            //logger.info("Checking for shapefile");

            ShapefileHandler shp_handler = new ShapefileHandler(new FileInputStream(f));
             if (shp_handler.containsShapefile()){
              //  logger.info("------- shapefile FOUND ----------");
                 fileType = ShapefileHandler.SHAPEFILE_FILE_TYPE; //"application/zipped-shapefile";
             }
        } 
        
        logger.fine("returning fileType "+fileType);
        return fileType;
    }
    
    public static String determineFileType(String fileName) {
        return MIME_TYPE_MAP.getContentType(fileName);
    }
    
    
    /* 
     * Custom method for identifying FITS files: 
     * TODO: 
     * the existing check for the "magic header" is very weak (see below); 
     * it should probably be replaced by attempting to parse and read at 
     * least the primary HDU, using the NOM fits parser. 
     * -- L.A. 4.0 alpha
    */
    private static boolean isFITSFile(File file) {
        BufferedInputStream ins = null;

        try {
            ins = new BufferedInputStream(new FileInputStream(file));
            return isFITSFile(ins);
        } catch (IOException ex) {
        } 
        
        return false;
    }
     
    private static boolean isFITSFile(InputStream ins) {
        boolean isFITS = false;

        // number of header bytes read for identification: 
        int magicWordLength = 6;
        String magicWord = "SIMPLE";

        try {
            byte[] b = new byte[magicWordLength];
            logger.fine("attempting to read "+magicWordLength+" bytes from the FITS format candidate stream.");
            if (ins.read(b, 0, magicWordLength) != magicWordLength) {
                throw new IOException();
            }

            if (magicWord.equals(new String(b))) {
                logger.fine("yes, this is FITS file!");
                isFITS = true;
            }
        } catch (IOException ex) {
            isFITS = false; 
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (Exception e) {
                }
            }
        }
    
        return isFITS;
    }
    
    private static boolean isGraphMLFile(File file) {
        boolean isGraphML = false;
        logger.fine("begin isGraphMLFile()");
        try{
            FileReader fileReader = new FileReader(file);
            javax.xml.stream.XMLInputFactory xmlif = javax.xml.stream.XMLInputFactory.newInstance();
            xmlif.setProperty("javax.xml.stream.isCoalescing", java.lang.Boolean.TRUE);

            XMLStreamReader xmlr = xmlif.createXMLStreamReader(fileReader);
            for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
                if (event == XMLStreamConstants.START_ELEMENT) {
                    if (xmlr.getLocalName().equals("graphml")) {
                        String schema = xmlr.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance", "schemaLocation");
                        logger.fine("schema = "+schema);
                        if (schema!=null && schema.indexOf("http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd")!=-1){
                            logger.fine("graphML is true");
                            isGraphML = true;
                        }
                    }
                    break;
                }
            }
        } catch(XMLStreamException e) {
            logger.fine("XML error - this is not a valid graphML file.");
            isGraphML = false;
        } catch(IOException e) {
            throw new EJBException(e);
        }
        logger.fine("end isGraphML()");
        return isGraphML;
    }
    
}
