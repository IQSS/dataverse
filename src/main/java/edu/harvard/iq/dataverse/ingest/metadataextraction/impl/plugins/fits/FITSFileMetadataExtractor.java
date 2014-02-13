/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.ingest.metadataextraction.impl.plugins.fits;


import edu.harvard.iq.dataverse.ingest.metadataextraction.*;
import edu.harvard.iq.dataverse.ingest.metadataextraction.spi.*;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader; 
import java.io.IOException; 
import java.io.File; 
import java.util.Map; 
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Properties;
import java.util.logging.Logger;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Data;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.ImageHDU;
import nom.tam.fits.TableHDU;
import nom.tam.fits.UndefinedHDU;

/**
 *
 * @author leonidandreev
 */
public class FITSFileMetadataExtractor extends FileMetadataExtractor {
    private static Logger dbgLog = Logger.getLogger(FITSFileMetadataExtractor.class.getPackage().getName());
    
    private Map<String, Integer> recognizedFitsMetadataKeys = null;
    // the integer value in the map is reserved for the type of the metadata 
    // keyword and configuration options.  
    
    private Map<String, Integer> recognizedFitsColumnKeys = null;
    // these are the column-level metadata keys; these are defined as XXXXn in 
    // the "FITS Standard, Appendix C" document; for example, "TTYPEn", meaning 
    // that the Header section of the table HDU will contain the keys TTYPE1, 
    // TTYPE2, ... TTYPEN - where N is the number of columns. 
    
    private Map<String, String> indexableFitsMetaKeys = null; 
    // This map defines the names of the keys under which they will be indexed
    // and made searchable in the application
    
    private static final Map<String, Integer> defaultRecognizedFitsMetadataKeys = new HashMap<String, Integer>();
    // the integer value in the map is reserved for the type of the metadata 
    // keyword; it's not being used as of now. 
    
    private static final Map<String, Integer> defaultRecognizedFitsColumnKeys = new HashMap<String, Integer>();
    // these are the column-level metadata keys; these are defined as XXXXn in 
    // the "FITS Standard, Appendix C" document; for example, "TTYPEn", meaning 
    // that the Header section of the table HDU will contain the keys TTYPE1, 
    // TTYPE2, ... TTYPEN - where N is the number of columns. 
    
    private static final Map<String, String> defaultIndexableFitsMetaKeys = new HashMap<String, String>(); 
    // This map defines the names of the keys under which they will be indexed
    // and made searchable in the application
    
    
    private static final String CONFIG_TOKEN_META_KEY = "RECOGNIZED_META_KEY";
    private static final String CONFIG_TOKEN_COLUMN_KEY = "RECOGNIZED_COLUMN_KEY"; 
    
    static {
        
            dbgLog.fine("FITS plugin: loading the default configuration values;");
            
            defaultRecognizedFitsMetadataKeys.put("DATE", 0);
            defaultRecognizedFitsMetadataKeys.put("DATE-OBS", 0);
            defaultRecognizedFitsMetadataKeys.put("ORIGIN", 0);
            defaultRecognizedFitsMetadataKeys.put("AUTHOR", 0);
            defaultRecognizedFitsMetadataKeys.put("REFERENC", 0);
            defaultRecognizedFitsMetadataKeys.put("COMMENT", 0);
            defaultRecognizedFitsMetadataKeys.put("HISTORY", 0);
            defaultRecognizedFitsMetadataKeys.put("OBSERVER", 0);
            defaultRecognizedFitsMetadataKeys.put("TELESCOP", 0);
            defaultRecognizedFitsMetadataKeys.put("INSTRUME", 0);
            defaultRecognizedFitsMetadataKeys.put("EQUINOX", 0);
            defaultRecognizedFitsMetadataKeys.put("EXTNAME", 0);

            defaultRecognizedFitsColumnKeys.put("TTYPE", 1);
            defaultRecognizedFitsColumnKeys.put("TCOMM", 0);
            defaultRecognizedFitsColumnKeys.put("TUCD", 0);

            defaultIndexableFitsMetaKeys.put("DATE", "Date");
            defaultIndexableFitsMetaKeys.put("DATE-OBS", "Observation-Date");
            defaultIndexableFitsMetaKeys.put("ORIGIN", "Origin");
            defaultIndexableFitsMetaKeys.put("AUTHOR", "Author");
            defaultIndexableFitsMetaKeys.put("REFERENC", "Reference");
            defaultIndexableFitsMetaKeys.put("COMMENT", "Comment");
            defaultIndexableFitsMetaKeys.put("HISTORY", "History");
            defaultIndexableFitsMetaKeys.put("OBSERVER", "Observer");
            defaultIndexableFitsMetaKeys.put("TELESCOP", "Telescope");
            defaultIndexableFitsMetaKeys.put("INSTRUME", "Instrument");
            defaultIndexableFitsMetaKeys.put("EQUINOX", "Equinox");
            defaultIndexableFitsMetaKeys.put("EXTNAME", "Extension-Name");
            defaultIndexableFitsMetaKeys.put("TTYPE", "Column-Label");
            defaultIndexableFitsMetaKeys.put("TCOMM", "Column-Comment");
            defaultIndexableFitsMetaKeys.put("TUCD", "Column-UCD");
      
    }
    
    private static final String METADATA_SUMMARY = "FILE_METADATA_SUMMARY_INFO";
    private static final String OPTION_PREFIX_SEARCHABLE = "PREFIXSEARCH";
    /**
     * Constructs a <code>FITSFileMetadataExtractor</code> instance with a 
     * <code>FITSFileMetadataExtractorSpi</code> object.
     * 
     * @param originator a <code>FITSFileMetadataExtractorSpi</code> object.
     */
    public FITSFileMetadataExtractor(FileMetadataExtractorSpi originator) {
        super(originator);
    }
    
    public FITSFileMetadataExtractor() {
        super(null); 
    }
    
    private void readConfig () {
        // Initialize the field configuration. 
        // We'll attempt to read the configuration file in the domain config 
        // directory. If not available, we'll use some hard-coded default values. 
        
        Properties p = System.getProperties();
        String domainRoot = p.getProperty("com.sun.aas.instanceRoot");
        dbgLog.fine("PROPERTY: com.sun.aas.instanceRoot="+domainRoot);
        int nConfiguredKeys = 0; 

        if (domainRoot != null && !(domainRoot.equals(""))) {
            String configFileName = domainRoot + "/config/fits.conf"; 
            File configFile = new File (configFileName);
            BufferedReader configFileReader = null; 
            
            boolean success = true;
            
            dbgLog.fine("FITS plugin: checking for the config file: "+configFileName);
            
            if (configFile.exists()) {
                recognizedFitsMetadataKeys = new HashMap<String, Integer>();
                recognizedFitsColumnKeys = new HashMap<String, Integer>();
                indexableFitsMetaKeys = new HashMap<String, String>();
                
                

                String line;

                try {
                    dbgLog.fine("FITS plugin: attempting to read the config file: "+configFileName);
                    configFileReader = new BufferedReader(new InputStreamReader(new FileInputStream(configFile)));

                    while ((line = configFileReader.readLine()) != null) {

                        // lines that start with "#" are comments;
                        // we skip them. 
                        if (line.indexOf('#') != 0) {
                            String[] configTokens = line.split("\t", -2);

                            if (configTokens == null || configTokens.length < 2) {
                                continue;
                            }

                            if (configTokens[0].equalsIgnoreCase(CONFIG_TOKEN_META_KEY)) {
                                if (configTokens[1] != null
                                        && !(configTokens[1].equals(""))) {
                                    dbgLog.fine("FITS plugin: found metadata key config entry for " +
                                            configTokens[1]);
                                    recognizedFitsMetadataKeys.put(configTokens[1], 0);
                                    if (configTokens.length > 2 && configTokens[2] != null
                                            && !(configTokens[2].equals(""))) {
                                        indexableFitsMetaKeys.put(configTokens[1], configTokens[2]);
                                    } else {
                                        dbgLog.fine("FITS plugin: (warning) no index name specified for "+configTokens[1]);
                                        indexableFitsMetaKeys.put(configTokens[1], configTokens[1]);
                                    }
                                    // Extra field options:
                                    // (the only option currently supported is prefix-steam searching
                                    // on the field)
                                    if (configTokens.length > 3 && configTokens[3] != null) {
                                        if (configTokens[3].equalsIgnoreCase(OPTION_PREFIX_SEARCHABLE)) {
                                            recognizedFitsMetadataKeys.put(configTokens[1], 1);
                                        }
                                    } 
                                    nConfiguredKeys++;
                                } else {
                                    dbgLog.warning("FITS plugin: empty (or malformed) meta key entry in the config file.");
                                }
                            } else if (configTokens[0].equalsIgnoreCase(CONFIG_TOKEN_COLUMN_KEY)) {
                                if (configTokens[1] != null
                                        && !(configTokens[1].equals(""))) {
                                    dbgLog.fine("FITS plugin: found column key config entry for " +
                                            configTokens[1]);
                                    recognizedFitsColumnKeys.put(configTokens[1], 0);
                                    if (configTokens.length > 2 && configTokens[2] != null
                                            && !(configTokens[2].equals(""))) {
                                        indexableFitsMetaKeys.put(configTokens[1], configTokens[2]);
                                    } else {
                                        dbgLog.fine("FITS plugin: (warning) no index name specified for "+configTokens[1]);
                                        indexableFitsMetaKeys.put(configTokens[1], configTokens[1]);
                                    }
                                    // Extra field options:
                                    if (configTokens.length > 3 && configTokens[3] != null) {
                                        if (configTokens[3].equalsIgnoreCase(OPTION_PREFIX_SEARCHABLE)) {
                                            recognizedFitsColumnKeys.put(configTokens[1], 1);
                                        }
                                    } 
                                    nConfiguredKeys++;
                                } else {
                                    dbgLog.warning("FITS plugin: empty (or malformed) column key entry in the config file.");

                                }
                            }
                        }
                    }
                    
                    if (nConfiguredKeys == 0) {
                        dbgLog.warning("FITS plugin: parsed the config file successfully; " +
                                "but no metadata fields found. will proceed with the " +
                                "default configuration.");
                    }
                } catch (IOException ioex) {
                    dbgLog.warning("FITS plugin: Caught an exception reading "
                            + "the configuration file; will proceed with the "
                            + "default configuration.");
                    success = false;
                    // We may have already read some values from the config
                    // file, before the exception was encountered. We will
                    // now resort to using the hard-coded, default 
                    // configuration. What we don't want to happen is end up
                    // with a mix of that hard-coded config, and whatever
                    // partial configuration we may have read. So we 
                    // need to clear the configuration maps now:
                    
                    nConfiguredKeys=0;
                } finally {
                    try {
                        configFileReader.close();
                    } catch (Exception e) {
                    }
                }
            } else {
                dbgLog.fine("FITS plugin: no config file; will proceed with "
                        + "the default configurtion.");
            }
        } else {
            dbgLog.warning("FITS plugin: could not find domain room property. "+
                    "(default configuration will be used)"); 
        }
        
        // If no config file/no keys in the config file, this is the default 
        // configuration we'll be using: 
        
        if (nConfiguredKeys == 0) {
            recognizedFitsMetadataKeys = defaultRecognizedFitsMetadataKeys;
            recognizedFitsColumnKeys = defaultRecognizedFitsColumnKeys;
            indexableFitsMetaKeys = defaultIndexableFitsMetaKeys;
            
        }
    }
    
    public Map<String, Set<String>> ingest (BufferedInputStream stream) throws IOException{
        dbgLog.fine("Attempting to read FITS file;");
        
        Map<String, Set<String>> fitsMetaMap = new HashMap<String, Set<String>>();
        
        Fits fitsFile = null; 
        try {
            fitsFile = new Fits (stream);
        } catch (FitsException fEx) {
            throw new IOException ("Failed to open FITS stream; "+fEx.getMessage());
        }
        
        if (fitsFile == null) {
            throw new IOException ("Failed to open FITS stream; null Fits object");
        }
        
        readConfig(); 
        
        int n = fitsFile.getNumberOfHDUs(); 
        
        dbgLog.fine("Total number of HDUs: "+n);
        
        BasicHDU hdu = null;
        int i = 0; 
        
        String primaryType = ""; 
        
        int nTableHDUs = 0; 
        int nImageHDUs = 0; 
        int nUndefHDUs = 0; 
        
        Set<String> metadataKeys = new HashSet<String>(); 
        Set<String> columnKeys = new HashSet<String>(); 
        
        try {

            while ((hdu = fitsFile.readHDU()) != null) {
                dbgLog.fine("reading HDU number " + i);
                
                if (hdu instanceof TableHDU) {
                    dbgLog.fine("this is a table HDU");
                    if (i > 0) {
                        nTableHDUs++;
                    } else {
                        primaryType = "Table";
                    }
                } else if (hdu instanceof ImageHDU) {
                    dbgLog.fine("this is an image HDU");
                    if (i > 0) {
                        nImageHDUs++;
                    } else {
                        primaryType = "Image";
                    }
                } else if (hdu instanceof UndefinedHDU) {
                    dbgLog.fine("this is an undefined HDU");
                    if (i > 0) {
                        nUndefHDUs++; 
                    } else {
                        primaryType = "Undefined"; 
                    }
                } else {
                    dbgLog.fine("this is an UKNOWN HDU");
                }
                               
                i++;

                Header hduHeader = hdu.getHeader();
                HeaderCard headerCard = null;

                int j = 0;
                while ((headerCard = hduHeader.nextCard()) != null) {

                    String headerKey = headerCard.getKey();
                    String headerValue = headerCard.getValue();
                    String headerComment = headerCard.getComment();

                    boolean recognized = false; 
                    
                    if (headerKey != null) {
                        if (isRecognizedKey(headerKey)) {
                            dbgLog.fine("recognized key: " + headerKey);
                            recognized = true; 
                            metadataKeys.add(headerKey);
                        } else if (isRecognizedColumnKey(headerKey)) {
                            dbgLog.fine("recognized column key: " + headerKey);
                            recognized = true;
                            columnKeys.add(getTrimmedColumnKey(headerKey));
                        }
                    } 
                    
                    if (recognized) {

                        String indexableKey = 
                                getIndexableMetaKey(headerKey) != null ? 
                                getIndexableMetaKey(headerKey) : 
                                headerKey; 
                        
                        if (headerValue != null) {
                            dbgLog.fine("value: " + headerValue);
                            if (fitsMetaMap.get(indexableKey) == null) {
                                fitsMetaMap.put(indexableKey, new HashSet<String>());
                            } 
                            fitsMetaMap.get(indexableKey).add(headerValue); 

                        } else if (headerKey.equals("COMMENT") && headerComment != null) {
                            dbgLog.fine("comment: " + headerComment);
                            if (fitsMetaMap.get(indexableKey) == null) {
                                fitsMetaMap.put(indexableKey, new HashSet<String>());
                            } 
                            fitsMetaMap.get(indexableKey).add(headerComment);
                        } else {
                            dbgLog.fine("value is null");
                        }

                        /*
                         * TODO:
                         * decide what to do with regular key comments:
                         
                        if (headerComment != null) {
                            dbgLog.fine("comment: " + headerComment);
                        } else {
                            dbgLog.fine("comment is null");
                        }
                        * */
                    }
                    j++;
                }
                dbgLog.fine ("processed "+j+" cards total;");
                
                Data fitsData = hdu.getData(); 
                
                dbgLog.fine ("data size: "+fitsData.getSize());
                dbgLog.fine("total size of the HDU is "+hdu.getSize());
                               
            }

        } catch (FitsException fEx) {
            throw new IOException("Failed to read HDU number " + i);
        }
            
        dbgLog.fine ("processed "+i+" HDUs total;");
        
        n = fitsFile.getNumberOfHDUs(); 
        
        dbgLog.fine("Total (current) number of HDUs: "+n);
        
        String metadataSummary = createMetadataSummary (n, nTableHDUs, primaryType, nImageHDUs, nUndefHDUs, metadataKeys, columnKeys);
        
        fitsMetaMap.put(METADATA_SUMMARY, new HashSet<String>());
        fitsMetaMap.get(METADATA_SUMMARY).add(metadataSummary); 
        
        return fitsMetaMap; 
    }
    
    private boolean isRecognizedKey (String key) {
        if (recognizedFitsMetadataKeys.containsKey(key)) {
            return true;
        }
        return false; 
    }
    
    private boolean isRecognizedColumnKey (String key) {
        if (key.matches(".*[0-9]$")) {
            String trimmedKey = getTrimmedColumnKey(key);
            if (recognizedFitsColumnKeys.containsKey(trimmedKey)) {
                return true; 
            }
        }
        return false; 
    }
    
    private String getIndexableMetaKey (String key) {
        String indexableKey = null; 
        
        if (isRecognizedKey(key)) {
            indexableKey = indexableFitsMetaKeys.get(key);
        } else if (isRecognizedColumnKey(key)) {
            indexableKey = indexableFitsMetaKeys.get(getTrimmedColumnKey(key));
        }
        
        return indexableKey; 
    }
    
    private String getTrimmedColumnKey (String key) {
        if (key != null) {
            return key.replaceFirst("[0-9][0-9]*$", "");
        }
        return null;
    }    
    
    private String createMetadataSummary (int nHDU, int nTableHDUs, String primaryType, int nImageHDUs, int nUndefHDUs, Set<String> metadataKeys, Set<String> columnKeys) {
        String summary = ""; 
        
        if (nHDU > 1) {
            summary = "This is a FITS file with "+nHDU+" HDUs total.\n";

            summary = summary.concat("In addition to the primary HDU of type "+
                    primaryType + ", it contains ");
            if (nTableHDUs > 0) {
                summary = summary.concat(nTableHDUs + " Table HDU(s); ");
            }
            if (nImageHDUs > 0) {
                summary = summary.concat(nImageHDUs + " Image HDU(s); ");
            }
            if (nUndefHDUs > 0) {
                summary = summary.concat(nUndefHDUs + " undefined HDU(s); ");
            }
            summary = summary.concat("\n");
        } else {
            summary = "This is a FITS file with 1 HDU of type "+primaryType+"\n"; 
        }
                
        if (metadataKeys != null && metadataKeys.size() > 0) {
            summary = summary.concat ("The following recognized metadata keys " + 
                    "have been found in the FITS file, and their values " +
                    "will be made searchable in the DVN, once " +
                    "the study has been indexed: \n");
            for (String key : metadataKeys) {
                summary = summary.concat(key+"; ");
            }
            summary=summary.concat("\n");
        }
        
        if (columnKeys != null && columnKeys.size() > 0) {
            summary = summary.concat ("In addition, the following recognized " + 
                    "and searchable column keys have been found in the table " +
                    "HDUs: \n");
            for (String key : columnKeys) {
                summary = summary.concat(key+"; ");
            }
            summary=summary.concat("\n");
        }
            
        return summary; 
    }
    
    /**
     * main() method, for testing
     * usage: java edu.harvard.iq.dvn.ingest.specialother.impl.plugins.fits.FITSFileMetadataExtractor testfile.fits
     * make sure the CLASSPATH contains fits.jar
     * 
     */
    
    public static void main(String[] args) {
        BufferedInputStream fitsStream = null;
        
        String fitsFile = args[0]; 
        Map<String, Set<String>> fitsMetadata = null; 
        
        try {
           fitsStream = new BufferedInputStream(new FileInputStream(fitsFile)); 
           
           FITSFileMetadataExtractor fitsIngester = new FITSFileMetadataExtractor();
           
           fitsMetadata = fitsIngester.ingest(fitsStream); 
            
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
        
        for (String mKey : fitsMetadata.keySet()) {
            
            Set<String> mValues = fitsMetadata.get(mKey); 
            System.out.println("key: " + mKey);
            
            if (mValues != null) {
                for (String mValue : mValues) {
                    if (mValue != null) {
                        System.out.println("value: " + mValue);               
                    } else {
                        System.out.println("value is null");
                    }
                }   
            }
        }
    }
}
