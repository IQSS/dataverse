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
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Map; 
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList; 
import java.util.Date;
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
import org.apache.commons.lang.StringUtils;

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
    
    private static final String ASTROPHYSICS_BLOCK_NAME = "astrophysics";
    
    private static final int FIELD_TYPE_TEXT = 0;
    private static final int FIELD_TYPE_DATE = 1; 
    private static final int FIELD_TYPE_FLOAT = 2;
    
    private static final String ATTRIBUTE_TYPE = "astroType";
    private static final String ATTRIBUTE_FACILITY = "astroFacility";
    private static final String ATTRIBUTE_INSTRUMENT = "astroInstrument";
    private static final String ATTRIBUTE_START_TIME = "coverage.Temporal.StartTime";
    private static final String ATTRIBUTE_STOP_TIME = "coverage.Temporal.StopTime";
    
    
    static {
        
            dbgLog.fine("FITS plugin: loading the default configuration values;");
            
            
            // The following fields have been dropped from the configuration 
            // map, not because we are not interested in them anymore - but
            // because they are now *mandatory*, i.e. non-configurable. 
            // We will be checking for the "telescope" and "instrument" 
            // fields on all files and HDUs:
            // -- 4.0 beta
            
            //defaultRecognizedFitsMetadataKeys.put("TELESCOP", 0);
            //defaultRecognizedFitsMetadataKeys.put("INSTRUME", 0);
            //defaultRecognizedFitsMetadataKeys.put("NAXIS", 0);
            //defaultRecognizedFitsMetadataKeys.put("DATE-OBS", FIELD_TYPE_DATE);
            // both coverage.Temporal.StartTime and .EndTime are derived from 
            // the DATE-OBS values; extra rules apply (coded further down)
            //defaultIndexableFitsMetaKeys.put("DATE-OBS", "coverage.Temporal.StartTime");
            //defaultIndexableFitsMetaKeys.put("DATE-OBS", "coverage.Temporal.StopTime");


            //defaultIndexableFitsMetaKeys.put("NAXIS", "naxis");


            // Optional, configurable fields: 
            
            defaultRecognizedFitsMetadataKeys.put("FILTER", FIELD_TYPE_TEXT);
            defaultRecognizedFitsMetadataKeys.put("OBJECT", FIELD_TYPE_TEXT);
            defaultRecognizedFitsMetadataKeys.put("CD1_1", FIELD_TYPE_FLOAT);
            defaultRecognizedFitsMetadataKeys.put("CDELT", FIELD_TYPE_FLOAT);
            defaultRecognizedFitsMetadataKeys.put("EXPTIME", FIELD_TYPE_DATE);
            defaultRecognizedFitsMetadataKeys.put("CRVAL1", FIELD_TYPE_TEXT);
            defaultRecognizedFitsMetadataKeys.put("CRVAL2", FIELD_TYPE_TEXT);

            
            // And the mapping to the corresponding values in the 
            // metadata block:
            // (per 4.0 beta implementation, the names below must match 
            // the names of the fields in the corresponding metadata block!)
            
            defaultIndexableFitsMetaKeys.put("TELESCOP", ATTRIBUTE_FACILITY);
            defaultIndexableFitsMetaKeys.put("INSTRUME", ATTRIBUTE_INSTRUMENT);
            defaultIndexableFitsMetaKeys.put("FILTER", "coverage.Spectral.Bandpass");
            defaultIndexableFitsMetaKeys.put("OBJECT", "astroObject");
            defaultIndexableFitsMetaKeys.put("CD1_1", "resolution.Spatial");
            defaultIndexableFitsMetaKeys.put("CDELT", "resolution.Spatial");
            defaultIndexableFitsMetaKeys.put("EXPTIME", "resolution.Temporal");
            defaultIndexableFitsMetaKeys.put("CDELT", "resolution.Spatial");
            defaultIndexableFitsMetaKeys.put("CRVAL1", "coverage.Spatial");
            defaultIndexableFitsMetaKeys.put("CRVAL2", "coverage.Spatial");

            

            // The following fields have been dropped from the configuration 
            // in 4.0 beta because we are not interested in them 
            // any longer: 
            
            //defaultRecognizedFitsMetadataKeys.put("EQUINOX", 0);
            //defaultIndexableFitsMetaKeys.put("EQUINOX", "Equinox");

            //defaultRecognizedFitsMetadataKeys.put("DATE", 0);
            //defaultRecognizedFitsMetadataKeys.put("ORIGIN", 0);
            //defaultRecognizedFitsMetadataKeys.put("AUTHOR", 0);
            //defaultRecognizedFitsMetadataKeys.put("REFERENC", 0);
            //defaultRecognizedFitsMetadataKeys.put("COMMENT", 0);
            //defaultRecognizedFitsMetadataKeys.put("HISTORY", 0);
            //defaultRecognizedFitsMetadataKeys.put("OBSERVER", 0);
            //defaultRecognizedFitsMetadataKeys.put("EXTNAME", 0);
            //defaultRecognizedFitsColumnKeys.put("TTYPE", 1);
            //defaultRecognizedFitsColumnKeys.put("TCOMM", 0);
            //defaultRecognizedFitsColumnKeys.put("TUCD", 0);
            //defaultRecognizedFitsMetadataKeys.put("CUNIT", 0);
            
            

            //defaultIndexableFitsMetaKeys.put("DATE", "Date");
            //defaultIndexableFitsMetaKeys.put("ORIGIN", "Origin");
            //defaultIndexableFitsMetaKeys.put("AUTHOR", "Author");
            //defaultIndexableFitsMetaKeys.put("REFERENC", "Reference");
            //defaultIndexableFitsMetaKeys.put("COMMENT", "Comment");
            //defaultIndexableFitsMetaKeys.put("HISTORY", "History");
            //defaultIndexableFitsMetaKeys.put("OBSERVER", "Observer");
            //defaultIndexableFitsMetaKeys.put("EXTNAME", "Extension-Name");
            //defaultIndexableFitsMetaKeys.put("TTYPE", "Column-Label");
            //defaultIndexableFitsMetaKeys.put("TCOMM", "Column-Comment");
            //defaultIndexableFitsMetaKeys.put("TUCD", "Column-UCD");
            //defaultIndexableFitsMetaKeys.put("CUNIT", "cunit");
      
    }
    
    //private static final String METADATA_SUMMARY = "FILE_METADATA_SUMMARY_INFO";
    //private static final String OPTION_PREFIX_SEARCHABLE = "PREFIXSEARCH";
    
    
    private static final String HDU_TYPE_IMAGE = "Image";
    private static final String HDU_TYPE_IMAGE_CUBE = "Cube";
    private static final String HDU_TYPE_TABLE = "Table";
    private static final String HDU_TYPE_UNDEF = "Undefined";
    private static final String HDU_TYPE_UNKNOWN = "Unknown";
    
    private static final String FILE_TYPE_IMAGE = "Image";
    private static final String FILE_TYPE_MOSAIC = "Mosaic";
    private static final String FILE_TYPE_CUBE = "Cube";
    private static final String FILE_TYPE_TABLE = "Table";
    private static final String FILE_TYPE_SPECTRUM = "Spectrum";
    
    // Recognized date formats, for extracting temporal values: 
    
    private static SimpleDateFormat[] DATE_FORMATS = new SimpleDateFormat[] {
        new SimpleDateFormat("yyyy-MM-dd"),
        new SimpleDateFormat("yyyy")
    };
    
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
    
    public FileMetadataIngest ingest (BufferedInputStream stream) throws IOException{
        dbgLog.fine("Attempting to read FITS file;");
        
        Map<String, Set<String>> fitsMetaMap = new HashMap<>();
        Map<String, Set<String>> tempMetaMap = new HashMap<>(); 
        
        FileMetadataIngest ingest = new FileMetadataIngest();
        ingest.setMetadataBlockName(ASTROPHYSICS_BLOCK_NAME);
        
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

        BasicHDU hdu = null;
        int i = 0; 
                
        int nTableHDUs = 0; 
        int nImageHDUs = 0; 
        int nUndefHDUs = 0; 
        
        int nAxis = 0; 
        
        Set<String> metadataKeys = new HashSet<String>(); 
        Set<String> columnKeys = new HashSet<String>(); 
        List<String> hduTypes = new ArrayList<String>();
        List<String> hduNames = new ArrayList<String>(); 
        
        try {
            fitsMetaMap.put(ATTRIBUTE_TYPE, new HashSet<String>());

            while ((hdu = fitsFile.readHDU()) != null) {
                dbgLog.fine("reading HDU number " + i);
                hduNames.add("[UNNAMED]");
                
                Header hduHeader = hdu.getHeader();

                
                if (hdu instanceof ImageHDU) {
                    dbgLog.fine("this is an image HDU");
                    
                    nAxis = hduHeader.getIntValue("NAXIS");
                    dbgLog.fine("NAXIS (directly from header): "+nAxis);
                    
                    if (nAxis > 0) {
                        metadataKeys.add("NAXIS");

                        if (nAxis > 1) {
                        
                            nImageHDUs++;
                            if (nAxis > 2) {
                                hduTypes.add(HDU_TYPE_IMAGE_CUBE);
                             
                            } else {
                                // Check for type Spectrum: 
                            
                                hduTypes.add(HDU_TYPE_IMAGE);
                            }
                        }
                    } else {
                        hduTypes.add(HDU_TYPE_UNKNOWN);
                    }
                } else if (hdu instanceof TableHDU) {
                    dbgLog.fine("this is a table HDU");
                    nTableHDUs++;
                    hduTypes.add(HDU_TYPE_TABLE);
                } else if (hdu instanceof UndefinedHDU) {
                    dbgLog.fine("this is an undefined HDU");
                    nUndefHDUs++;
                    hduTypes.add(HDU_TYPE_UNDEF);

                } else {
                    dbgLog.fine("this is an UKNOWN HDU");
                    hduTypes.add(HDU_TYPE_UNKNOWN);
                }
                               
                i++;

                // Standard HDU attributes that we always check: 
                
                if (fitsMetaMap.get(ATTRIBUTE_FACILITY) == null) {
                    String hduTelescope = hdu.getTelescope();
                    if (hduTelescope != null) {
                        fitsMetaMap.put(ATTRIBUTE_FACILITY, new HashSet<String>());
                        fitsMetaMap.get(ATTRIBUTE_FACILITY).add(hduTelescope);
                        metadataKeys.add("TELESCOP");
                    }
                }
                
                if (fitsMetaMap.get(ATTRIBUTE_INSTRUMENT) == null) {
                    String hduInstrument = hdu.getInstrument();
                    if (hduInstrument != null) {
                        fitsMetaMap.put(ATTRIBUTE_INSTRUMENT, new HashSet<String>());
                        fitsMetaMap.get(ATTRIBUTE_INSTRUMENT).add(hduInstrument);
                        metadataKeys.add("INSTRUME");
                    }
                }

                //if (fitsMetaMap.get(ATTRIBUTE_START_TIME) == null) {
                String obsDate = hduHeader.getStringValue("DATE-OBS");
                if (obsDate != null) {
                        // The value of DATE-OBS will be used later, to determine
                    // coverage.Temporal.StarTime and .StppTime; for now
                    // we are storing the values in a temporary map. 
                    if (tempMetaMap.get("DATE-OBS") == null) {
                        tempMetaMap.put("DATE-OBS", new HashSet<String>());
                    }
                    tempMetaMap.get("DATE-OBS").add(obsDate);
                    metadataKeys.add("DATE-OBS");
                }
                //}
                
                
                /* TODO: 
                 * use the Axes values for determining if this is a spectrum:
                */
                for (int j = 0; j < hdu.getAxes().length; j++) {
                    int nAxisN = hdu.getAxes()[j];
                    metadataKeys.add("NAXIS"+j);
                    dbgLog.fine("NAXIS"+j+" value: "+nAxisN);
                }
                
                // Process individual header cards:
                
                HeaderCard headerCard = null;

                int j = 0;
                while ((headerCard = hduHeader.nextCard()) != null) {

                    String headerKey = headerCard.getKey();
                    String headerValue = headerCard.getValue();
                    String headerComment = headerCard.getComment();

                    dbgLog.fine("Processing header key: "+headerKey);
                    dbgLog.fine("Value: "+headerValue);
                    boolean recognized = false; 
                    
                    if (headerKey != null) {
                        /*
                        if (i > 1 && headerKey.equals("EXTNAME")) {
                            hduNames.set(i-2, headerValue);
                        } */
                        if (isRecognizedKey(headerKey)) {
                            dbgLog.fine("recognized key: " + headerKey);
                            recognized = true; 
                            metadataKeys.add(headerKey);
                        } /*else if (isRecognizedColumnKey(headerKey)) {
                            dbgLog.fine("recognized column key: " + headerKey);
                            recognized = true;
                            //columnKeys.add(getTrimmedColumnKey(headerKey));
                            columnKeys.add(headerKey);
                        }*/
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
        
        int n = fitsFile.getNumberOfHDUs(); 
        
        if (n != i) {
            dbgLog.fine("WARNING: mismatch between the number of cards processed and reported!");
        }
        dbgLog.fine("Total (current) number of HDUs: "+n);
        
        // Make final decisions on the "type(s)" of the file we have just
        // processed: 
        
        String imageFileType = determineImageFileType (nImageHDUs, hduTypes);
        if (imageFileType != null) {
            fitsMetaMap.get(ATTRIBUTE_TYPE).add(imageFileType);
        }
        
        if (fitsMetaMap.get(ATTRIBUTE_TYPE).isEmpty()) {
            String tableFileType = determineTableFileType (nTableHDUs, hduTypes);
            if (tableFileType != null) {
                fitsMetaMap.get(ATTRIBUTE_TYPE).add(tableFileType);
            }
        }
        
        if (n == 1 && fitsMetaMap.get(ATTRIBUTE_TYPE).isEmpty()) {
            // If there's only 1 (primary) HDU in the file, we'll make sure 
            // the file type is set to (at least) "image" - even if we skipped 
            // that HDU because it looked empty:
            fitsMetaMap.get(ATTRIBUTE_TYPE).add(FILE_TYPE_IMAGE);
        }
        
        // Final post-processing. 
        // Some values are derived from the collected fields 
        // (for example, the coverage.temporal.StopTime is the min. 
        // of all the collected OBS-DATE values). 
        // Specific rules are applied below: 
        
        // start time and and stop time: 
        
        int numObsDates = tempMetaMap.get("DATE-OBS") == null ? 0 : tempMetaMap.get("DATE-OBS").size();
        if (numObsDates > 0) {

            String[] obsDateValues = new String[numObsDates];
            obsDateValues = tempMetaMap.get("DATE-OBS").toArray(new String[0]);

            Date minDate = null; 
            Date maxDate = null; 
            
            String startObsTime = "";
            String stopObsTime = "";
            
            for (int k = 0; k < obsDateValues.length; k++) {
                Date obsDate = null;
                String obsDateString = obsDateValues[k];
                
                for (SimpleDateFormat format : DATE_FORMATS) {
                    // Strict parsing - it will throw an 
                    // exception if it doesn't parse!
                    format.setLenient(false);
                    //try {
                    ParsePosition pos = new ParsePosition(0);
                    obsDate = format.parse(obsDateString, pos);
                    if (obsDate == null) {
                        continue;
                    }
                    if (pos.getIndex() != obsDateString.length()) {
                        obsDateString = obsDateString.substring(0, pos.getIndex());
                    }
                    dbgLog.fine("Valid date: " + obsDateString + ", format: " + format.toPattern());
                    break;
                    //} catch (ParseException ex) {
                    //    obsDate = null; 
                    //}
                }
                
                if (obsDate != null) {
                     
                    if (minDate == null) {
                        minDate = obsDate;
                        startObsTime = obsDateString;
                    } else if (obsDate.before(minDate)) {
                        minDate = obsDate;
                        startObsTime = obsDateString;
                    }
                    
                    if (maxDate == null) {
                        maxDate = obsDate;
                        stopObsTime = obsDateString; 
                    } else if (obsDate.after(maxDate)) {
                        maxDate = obsDate; 
                        stopObsTime = obsDateString; 
                    }
                }
            }
            
            if (!startObsTime.equals("")) {
                fitsMetaMap.put(ATTRIBUTE_START_TIME, new HashSet<String>());
                fitsMetaMap.get(ATTRIBUTE_START_TIME).add(startObsTime);
            }

            if (!stopObsTime.equals("")) {
                fitsMetaMap.put(ATTRIBUTE_STOP_TIME, new HashSet<String>());
                fitsMetaMap.get(ATTRIBUTE_STOP_TIME).add(stopObsTime);
            }
        }
        
        String metadataSummary = createMetadataSummary (n, nTableHDUs, nImageHDUs, nUndefHDUs, metadataKeys); //, columnKeys, hduNames, fitsMetaMap.get("Column-Label"));
        
        ingest.setMetadataMap(fitsMetaMap);
        ingest.setMetadataSummary(metadataSummary);
        
        //return fitsMetaMap; 
        return ingest; 
    }
    
    private void readConfig () {
        // Initialize the field configuration. 
        // We'll attempt to read the configuration file in the domain config 
        // directory. If not available, we'll use some hard-coded default values. 
        
        Properties p = System.getProperties();
        String domainRoot = p.getProperty("com.sun.aas.instanceRoot");
        dbgLog.fine("PROPERTY: com.sun.aas.instanceRoot="+domainRoot);
        if (domainRoot == null || domainRoot.equals("")) {
            domainRoot = "/usr/local/glassfish4/glassfish/domains/domain1";
        }
        int nConfiguredKeys = 0; 

        if (domainRoot != null && !(domainRoot.equals(""))) {
            String configFileName = domainRoot + "/config/fits.conf_DONOTREAD"; 
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
                                    /*
                                    if (configTokens.length > 3 && configTokens[3] != null) {
                                        if (configTokens[3].equalsIgnoreCase(OPTION_PREFIX_SEARCHABLE)) {
                                            recognizedFitsMetadataKeys.put(configTokens[1], 1);
                                        }
                                    } 
                                    */
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
                                    /*
                                    if (configTokens.length > 3 && configTokens[3] != null) {
                                        if (configTokens[3].equalsIgnoreCase(OPTION_PREFIX_SEARCHABLE)) {
                                            recognizedFitsColumnKeys.put(configTokens[1], 1);
                                        }
                                    } */
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
    
    private String determineImageFileType (int nImageHDUs, List<String> hduTypes) {
        if (nImageHDUs > 0) {
            // At least one HDU is an image; so the whole file gets to be typed
            // as image - unless it qualifies as one of the Image sub-types:
            for (int j = 0; j < hduTypes.size(); j++) {
                if (hduTypes.get(j).equals(HDU_TYPE_IMAGE_CUBE)) {
                    return FILE_TYPE_CUBE;
                } 
            }
            
            if (nImageHDUs > 1) {
                return FILE_TYPE_MOSAIC;
            } 
            
            return FILE_TYPE_IMAGE;
        }
        
        return null; 
    }
    
    private String determineTableFileType (int nTableHDUs, List<String> hduTypes) {
        if (nTableHDUs > 0) {
            return FILE_TYPE_TABLE;
        }
        
        return null; 
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
    
    private String createMetadataSummary (int nHDU, int nTableHDUs, int nImageHDUs, int nUndefHDUs, Set<String> metadataKeys) { //, Set<String> columnKeys, List<String> hduNames, Set<String> columnNames) {
        String summary = ""; 
        
        if (nHDU > 1) {
            summary = "FITS file, "+nHDU+" HDUs total:\n";

            summary = summary.concat("The primary HDU; ");
            if (nTableHDUs > 0) {
                summary = summary.concat(nTableHDUs + " Table HDU(s) ");
                //summary = summary.concat("(column names: "+StringUtils.join(columnNames, ", ")+"); ");
            }
            if (nImageHDUs > 0) {
                summary = summary.concat(nImageHDUs + " Image HDU(s); ");
            }
            if (nUndefHDUs > 0) {
                summary = summary.concat(nUndefHDUs + " undefined HDU(s); ");
            }
            summary = summary.concat("\n");
            
            //summary = summary.concat("HDU names: "+StringUtils.join(hduNames, ", ")+"; ");
        } else {
            summary = "This is a FITS file with 1 (primary) HDU.\n"; 
        }
        
        
        if (metadataKeys != null && metadataKeys.size() > 0) {
            summary = summary.concat ("The following recognized metadata keys " + 
                    "have been found in the FITS file:\n");
            for (String key : metadataKeys) {
                summary = summary.concat(key+"; ");
            }
            summary=summary.concat("\n");
        }
        
        /*
         * Per feedback from Gus: it's not necessary to list the column keys.
         *
        if (columnKeys != null && columnKeys.size() > 0) {
            summary = summary.concat ("In addition, the following column keys "+
                    "have been found in the table HDUs: \n");
            for (String key : columnKeys) {
                summary = summary.concat(key+"; ");
            }
            summary=summary.concat("\n");
        }
        */
            
        return summary; 
    }
    
    private int typeCount (List<String> typeList, String typeToken) {
        if (typeToken == null || typeToken.equals("")) {
            return 0;
        }
        
        int count = 0;

        if (typeList != null) {
            for (int i = 0; i<typeList.size(); i++) {
                if (typeToken.equals(typeList.get(i))) {
                    count++; 
                }
            }
        }
        
        return count; 
    }
    
    @Override
    public String getFormatName() throws IOException {
        if (originatingProvider != null) {
            return originatingProvider.getFormatNames()[0];
        }
        return "fits";
    }
    
    /**
     * main() method, for testing
     * usage: java edu.harvard.iq.dataverse.ingest.metadataextraction.impl.plugins.fits.FITSFileMetadataExtractor testfile.fits
     * make sure the CLASSPATH contains fits.jar.
     * 
     */
    
    public static void main(String[] args) {
        BufferedInputStream fitsStream = null;
        
        String fitsFile = args[0]; 
        FileMetadataIngest fitsIngest = null; 
        Map<String, Set<String>> fitsMetadata = null; 
        
        try {
           fitsStream = new BufferedInputStream(new FileInputStream(fitsFile)); 
           
           FITSFileMetadataExtractor fitsIngester = new FITSFileMetadataExtractor();
           
           fitsIngest = fitsIngester.ingest(fitsStream); 
           fitsMetadata = fitsIngest.getMetadataMap();
            
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
        
        for (String mKey : fitsMetadata.keySet()) {
            //if (mKey.equals(METADATA_SUMMARY)) {
            //    continue;
            //}
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
        
        if (fitsIngest.getMetadataSummary() != null) {
            System.out.println("\nFITS Metadata summary: \n"+fitsIngest.getMetadataSummary()); 
        }
    }
}
