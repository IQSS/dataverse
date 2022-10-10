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

package edu.harvard.iq.dataverse.dataaccess;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.util.FileUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
/**
 *
 * @author Leonid Andreev
 */


public class DataAccess {

	private static final Logger logger = Logger.getLogger(DataAccess.class.getCanonicalName());
	
    public DataAccess() {

    };

    public static final String FILE = "file";
    public static final String S3 = "s3";
    static final String SWIFT = "swift";
    static final String REMOTE = "remote";
    static final String TMP = "tmp";
    public static final String SEPARATOR = "://";
    //Default to "file" is for tests only
    public static final String DEFAULT_STORAGE_DRIVER_IDENTIFIER = System.getProperty("dataverse.files.storage-driver-id", FILE);
    public static final String UNDEFINED_STORAGE_DRIVER_IDENTIFIER = "undefined"; //Used in dataverse.xhtml as a non-null selection option value (indicating a null driver/inheriting the default)
     
    
    // The getStorageIO() methods initialize StorageIO objects for
    // datafiles that are already saved using one of the supported Dataverse
    // DataAccess IO drivers.
    public static <T extends DvObject> StorageIO<T> getStorageIO(T dvObject) throws IOException {
        return getStorageIO(dvObject, null);
    }

    

    public static String getStorageDriverFromIdentifier(String storageIdentifier) {
        
        int separatorIndex = storageIdentifier.indexOf(SEPARATOR);
        String driverId = DEFAULT_STORAGE_DRIVER_IDENTIFIER; // default
        if (separatorIndex > 0) {
            driverId = storageIdentifier.substring(0, separatorIndex);
        }
        return driverId;
    }
    
    //passing DVObject instead of a datafile to accomodate for use of datafiles as well as datasets
	public static <T extends DvObject> StorageIO<T> getStorageIO(T dvObject, DataAccessRequest req) throws IOException {

		if (dvObject == null || dvObject.getStorageIdentifier() == null || dvObject.getStorageIdentifier().isEmpty()) {
			throw new IOException("getDataAccessObject: null or invalid datafile.");
		}

        String storageDriverId = getStorageDriverFromIdentifier(dvObject.getStorageIdentifier());

		return getStorageIO(dvObject, req, storageDriverId);
	}

	protected static <T extends DvObject> StorageIO<T> getStorageIO(T dvObject, DataAccessRequest req,
			String storageDriverId) throws IOException {
		String storageType = getDriverType(storageDriverId);
		switch (storageType) {
		case FILE:
			return new FileAccessIO<>(dvObject, req, storageDriverId);
		case S3:
			return new S3AccessIO<>(dvObject, req, storageDriverId);
		case SWIFT:
			return new SwiftAccessIO<>(dvObject, req, storageDriverId);
		case REMOTE:
			return new RemoteOverlayAccessIO<>(dvObject, req, storageDriverId);
		case TMP:
			throw new IOException(
					"DataAccess IO attempted on a temporary file that hasn't been permanently saved yet.");
		}
		// TODO:
		// This code will need to be extended with a system of looking up
		// available storage plugins by the storage tag embedded in the
		// "storage identifier".
		// -- L.A. 4.0.2

		logger.warning("Could not find storage driver for: " + storageDriverId);
		throw new IOException("getDataAccessObject: Unsupported storage method.");
	}

    // Experimental extension of the StorageIO system allowing direct access to
    // stored physical files that may not be associated with any DvObjects

    public static StorageIO<DvObject> getDirectStorageIO(String fullStorageLocation) throws IOException {
    	String[] response = getDriverIdAndStorageLocation(fullStorageLocation);
    	String storageDriverId = response[0];
    	String storageLocation=response[1];
        String storageType = getDriverType(storageDriverId);
        switch(storageType) {
        case FILE:
            return new FileAccessIO<>(storageLocation, storageDriverId);
        case S3:
            return new S3AccessIO<>(storageLocation, storageDriverId);
        case SWIFT:
            return new SwiftAccessIO<>(storageLocation, storageDriverId);
        case REMOTE:
            return new RemoteOverlayAccessIO<>(storageLocation, storageDriverId);
        default:
        	logger.warning("Could not find storage driver for: " + fullStorageLocation);
        	throw new IOException("getDirectStorageIO: Unsupported storage method.");
        }
    }
    
    public static String[] getDriverIdAndStorageLocation(String storageLocation) {
    	//default if no prefix
    	String storageIdentifier=storageLocation;
        int separatorIndex = storageLocation.indexOf(SEPARATOR);
    	String storageDriverId = ""; //default
        if(separatorIndex>0) {
        	storageDriverId = storageLocation.substring(0,separatorIndex);
        	storageIdentifier = storageLocation.substring(separatorIndex + 3);
        }
		return new String[]{storageDriverId, storageIdentifier};
    }
    
    public static String getStorageIdFromLocation(String location) {
    	if(location.contains(SEPARATOR)) {
    		//It's a full location with a driverId, so strip and reapply the driver id
    		//NOte that this will strip the bucketname out (which s3 uses) but the S3IOStorage class knows to look at re-insert it
    		return location.substring(0,location.indexOf(SEPARATOR) +3) + location.substring(location.lastIndexOf('/')+1); 
    	}
    	return location.substring(location.lastIndexOf('/')+1);
    }
    
    public static String getDriverType(String driverId) {
    	if(driverId.isEmpty() || driverId.equals("tmp")) {
    		return "tmp";
    	}
    	return System.getProperty("dataverse.files." + driverId + ".type", "Undefined");
    }
    
    //This 
    public static String getDriverPrefix(String driverId) throws IOException {
        if(driverId.isEmpty() || driverId.equals("tmp")) {
            return "tmp" + SEPARATOR;
        }
        String storageType = System.getProperty("dataverse.files." + driverId + ".type", "Undefined");
        switch(storageType) {
        case FILE:
            return FileAccessIO.getDriverPrefix(driverId);
        case S3:
            return S3AccessIO.getDriverPrefix(driverId);
        case SWIFT:
            return SwiftAccessIO.getDriverPrefix(driverId);
        default:
            logger.warning("Could not find storage driver for id: " + driverId);
            throw new IOException("getDriverPrefix: Unsupported storage method.");
        }
        

    }

    // createDataAccessObject() methods create a *new*, empty DataAccess objects,
    // for saving new, not yet saved datafiles.
    public static <T extends DvObject> StorageIO<T> createNewStorageIO(T dvObject, String storageTag) throws IOException {
        if (dvObject == null
        		|| dvObject.getDataverseContext()==null
                || storageTag == null
                || storageTag.isEmpty()) {
            throw new IOException("getDataAccessObject: null or invalid datafile.");
        }
                
        if (dvObject instanceof Dataset) {
            return createNewStorageIO(dvObject, storageTag, ((Dataset)dvObject).getEffectiveStorageDriverId());
        } 
        // it's a DataFile:
        return createNewStorageIO(dvObject, storageTag, dvObject.getOwner().getEffectiveStorageDriverId());
    }

    public static <T extends DvObject> StorageIO<T> createNewStorageIO(T dvObject, String storageTag, String storageDriverId) throws IOException {
        if (dvObject == null
                || storageTag == null
                || storageTag.isEmpty()) {
            throw new IOException("getDataAccessObject: null or invalid datafile.");
        }
        
        /* Prior versions sometimes called createNewStorageIO(object, "placeholder") with an existing object to get a ~clone for use in storing/reading Aux files
         * Since PR #6488 for multi-store - this can return a clone using a different store than the original (e.g. if the default store changes) which causes errors
         * This if will catch any cases where that's attempted.
         */
        // Tests send objects with no storageIdentifier set
        if((dvObject.getStorageIdentifier()!=null) && dvObject.getStorageIdentifier().contains(SEPARATOR)) {
        	throw new IOException("Attempt to create new StorageIO for already stored object: " + dvObject.getStorageIdentifier());
        }

        StorageIO<T> storageIO = null;
        
        dvObject.setStorageIdentifier(storageTag);

        if (StringUtils.isBlank(storageDriverId)) {
        	storageDriverId = DEFAULT_STORAGE_DRIVER_IDENTIFIER;
        }
        String storageType = getDriverType(storageDriverId);
        switch(storageType) {
        case FILE:
        	storageIO = new FileAccessIO<>(dvObject, null, storageDriverId);
        	break;
        case SWIFT:
        	storageIO = new SwiftAccessIO<>(dvObject, null, storageDriverId);
        	break;
        case S3:
        	storageIO = new S3AccessIO<>(dvObject, null, storageDriverId);
        	break;
        case REMOTE:
            storageIO = createNewStorageIO(dvObject, storageTag, RemoteOverlayAccessIO.getBaseStoreIdFor(storageDriverId)) ;
            break;
        default:
        	logger.warning("Could not find storage driver for: " + storageTag);
        	throw new IOException("createDataAccessObject: Unsupported storage method " + storageDriverId);
        }
        // Note: All storageIO classes must assure that dvObject instances' storageIdentifiers are prepended with 
        // the <driverId>:// + any additional storageIO type information required (e.g. the bucketname for s3/swift)
        // This currently happens when the storageIO is opened for write access
        storageIO.open(DataAccessOption.WRITE_ACCESS);
        return storageIO;
    }

    static HashMap<String, String> drivers = null;
    
    public static String getStorageDriverId(String driverLabel) {
    	if (drivers==null) {
    		populateDrivers();
    	}
    	if(!StringUtils.isBlank(driverLabel) && drivers.containsKey(driverLabel)) {
    		return drivers.get(driverLabel);
    	} 
    	return DEFAULT_STORAGE_DRIVER_IDENTIFIER;
    }

    public static HashMap<String, String> getStorageDriverLabels() {
    	if (drivers==null) {
    		populateDrivers();
    	}
    	return drivers;
    }

    private static void populateDrivers() {
    	drivers = new HashMap<String, String>();
    	Properties p = System.getProperties();
    	for(String property: p.stringPropertyNames()) {
    		if(property.startsWith("dataverse.files.") && property.endsWith(".label")) {
    			String driverId = property.substring(16); // "dataverse.files.".length
    			driverId=driverId.substring(0,driverId.indexOf('.'));
    			logger.info("Found Storage Driver: " + driverId + " for " + p.get(property).toString());
    			drivers.put(p.get(property).toString(), driverId);
    		}
    	}
    }

    public static String getStorageDriverLabelFor(String storageDriverId) {
    	String label = null;
    	if(!StringUtils.isEmpty(storageDriverId)) {
    		if (drivers==null) {
    			populateDrivers();
    		}

    		if(drivers.containsValue(storageDriverId)) {
    			for(String key: drivers.keySet()) {
    				if(drivers.get(key).equals(storageDriverId)) {
    					label = key;
    					break;
    				}
    			}
    		}
    	}
    	return label;
    }
    
    /**
     * This method checks to see if an overlay store is being used and, if so,
     * defines a base storage identifier for use with auxiliary files, and adds it
     * into the returned value
     * 
     * @param newStorageIdentifier
     * @return - the newStorageIdentifier (for file, S3, swift stores) - the
     *         newStorageIdentifier with a new base store identifier inserted (for
     *         an overlay store)
     */
    public static String expandStorageIdentifierIfNeeded(String newStorageIdentifier) {
        logger.fine("found: " + newStorageIdentifier);
        String driverType = DataAccess
                .getDriverType(newStorageIdentifier.substring(0, newStorageIdentifier.indexOf(":")));
        logger.fine("drivertype: " + driverType);
        if (driverType.equals(REMOTE)) {
            // Add a generated identifier for the aux files
            logger.fine("in: " + newStorageIdentifier);
            int lastColon = newStorageIdentifier.lastIndexOf(SEPARATOR);
            newStorageIdentifier = newStorageIdentifier.substring(0, lastColon + 3)
                    + FileUtil.generateStorageIdentifier() + "//" + newStorageIdentifier.substring(lastColon + 3);
            logger.fine("out: " + newStorageIdentifier);
        }
        return newStorageIdentifier;
    }
    
    public static boolean uploadToDatasetAllowed(Dataset d, String storageIdentifier) {
        boolean allowed=true;
        String driverId = DataAccess.getStorageDriverFromIdentifier(storageIdentifier);
        String effectiveDriverId = d.getEffectiveStorageDriverId();
        if(!effectiveDriverId.equals(driverId)) {
            //Not allowed unless this is a remote store and you're uploading to the basestore
            if(getDriverType(driverId).equals(REMOTE)) {
                String baseDriverId = RemoteOverlayAccessIO.getBaseStoreIdFor(driverId);
                if(!effectiveDriverId.equals(baseDriverId)) {
                    //Not allowed - wrong base driver
                    allowed = false;
                } else {
                    //Only allowed if baseStore allows it
                    allowed = StorageIO.isDirectUploadEnabled(baseDriverId);
                }
            } else {
                //Not allowed - wrong main driver
                allowed=false;
            }
        } else {
            //Only allowed if main store allows it
            allowed = StorageIO.isDirectUploadEnabled(driverId);
        }
        return allowed;
    }


    //Method to verify that a submitted storageIdentifier (i.e. in direct/remote uploads) is consistent with the store's configuration.
    public static boolean isValidDirectStorageIdentifier(String storageId) {
        String driverId = DataAccess.getStorageDriverFromIdentifier(storageId);
        String storageType = DataAccess.getDriverType(driverId);
        if (storageType.equals("tmp") || storageType.equals("Undefined")) {
            return false;
        }
        switch (storageType) {
        case FILE:
            return FileAccessIO.isValidIdentifier(driverId, storageId);
        case SWIFT:
            return SwiftAccessIO.isValidIdentifier(driverId, storageId);
        case S3:
            return S3AccessIO.isValidIdentifier(driverId, storageId);
        case REMOTE:
            return RemoteOverlayAccessIO.isValidIdentifier(driverId, storageId);
        default:
            logger.warning("Request to validate for storage driver: " + driverId);
        }
        return false;
    }
}
