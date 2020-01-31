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

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.util.StringUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
/**
 *
 * @author Leonid Andreev
 */


public class DataAccess {

	private static final Logger logger = Logger.getLogger(DataAccess.class.getCanonicalName());
	
    public DataAccess() {

    };


    public static final String DEFAULT_STORAGE_DRIVER_IDENTIFIER = System.getProperty("dataverse.files.storage-driver-id");

    // The getStorageIO() methods initialize StorageIO objects for
    // datafiles that are already saved using one of the supported Dataverse
    // DataAccess IO drivers.
    public static <T extends DvObject> StorageIO<T> getStorageIO(T dvObject) throws IOException {
        return getStorageIO(dvObject, null);
    }

    //passing DVObject instead of a datafile to accomodate for use of datafiles as well as datasets
    public static <T extends DvObject> StorageIO<T> getStorageIO(T dvObject, DataAccessRequest req) throws IOException {

        if (dvObject == null
                || dvObject.getStorageIdentifier() == null
            || dvObject.getStorageIdentifier().isEmpty()) {
            throw new IOException("getDataAccessObject: null or invalid datafile.");
        }
        String storageIdentifier = dvObject.getStorageIdentifier();
        int separatorIndex = storageIdentifier.indexOf("://");
    	String storageDriverId = "file"; //default
        if(separatorIndex>0) {
        	storageDriverId = storageIdentifier.substring(0,separatorIndex);
        }
        String storageType = getDriverType(storageDriverId);
        switch(storageType) {
        case "file":
            return new FileAccessIO<>(dvObject, req, storageDriverId);
        case "s3":
            return new S3AccessIO<>(dvObject, req, storageDriverId);
        case "swift":
            return new SwiftAccessIO<>(dvObject, req, storageDriverId);
        case "tmp":
        	throw new IOException("DataAccess IO attempted on a temporary file that hasn't been permanently saved yet.");
        }

        // TODO:
        // This code will need to be extended with a system of looking up
        // available storage plugins by the storage tag embedded in the
        // "storage identifier".
        // -- L.A. 4.0.2


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
        case "file":
            return new FileAccessIO<>(storageLocation, storageDriverId);
        case "s3":
            return new S3AccessIO<>(storageLocation, storageDriverId);
        case "swift":
            return new SwiftAccessIO<>(storageLocation, storageDriverId);
        default:
        	throw new IOException("getDirectStorageIO: Unsupported storage method.");
        }
    }
    
    public static String[] getDriverIdAndStorageLocation(String storageLocation) {
    	//default if no prefix
    	String storageIdentifier=storageLocation;
        int separatorIndex = storageLocation.indexOf("://");
    	String storageDriverId = "file"; //default
        if(separatorIndex>0) {
        	storageDriverId = storageLocation.substring(0,separatorIndex);
        	storageIdentifier = storageLocation.substring(separatorIndex + 3);
        }
		return new String[]{storageDriverId, storageIdentifier};
    }
    
    public static String getStorarageIdFromLocation(String location) {
    	return location.substring(location.lastIndexOf('/')+1);
    }
    
    public static String getDriverType(String driverId) {
    	return System.getProperty("dataverse.files." + driverId + ".type", "Undefined");
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
        return createNewStorageIO(dvObject, storageTag, dvObject.getDataverseContext().getStorageDriverId());
    }

    public static <T extends DvObject> StorageIO<T> createNewStorageIO(T dvObject, String storageTag, String storageDriverId) throws IOException {
        if (dvObject == null
                || storageTag == null
                || storageTag.isEmpty()) {
            throw new IOException("getDataAccessObject: null or invalid datafile.");
        }

        StorageIO<T> storageIO = null;

        dvObject.setStorageIdentifier(storageTag);

        if (StringUtils.isEmpty(storageDriverId)) {
        	storageDriverId = "file";
        }
        String storageType = getDriverType(storageDriverId);
        switch(storageType) {
        case "file":
        	storageIO = new FileAccessIO<>(dvObject, null, storageDriverId);
        	break;
        case "swift":
        	storageIO = new SwiftAccessIO<>(dvObject, null, storageDriverId);
        	break;
        case "s3":
        	storageIO = new S3AccessIO<>(dvObject, null, storageDriverId);
        	break;
        default:
        	throw new IOException("createDataAccessObject: Unsupported storage method " + storageDriverId);
        }

        storageIO.open(DataAccessOption.WRITE_ACCESS);
        return storageIO;
    }

    static HashMap<String, String> drivers = null;
    
    public static String getStorageDriverId(String driverLabel) {
    	if (drivers==null) {
    		populateDrivers();
    	}
    	if(StringUtil.nonEmpty(driverLabel) && drivers.containsKey(driverLabel)) {
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
}
