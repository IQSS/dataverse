package edu.harvard.iq.dataverse.dataaccess;

public interface GlobusAccessibleStore {

    static final String MANAGED = "managed";
    static final String GLOBUS_TRANSFER_ENDPOINT_WITH_BASEPATH = "globus-transfer-endpoint-with-basepath";
    static final String GLOBUS_REFERENCE_ENDPOINTS_WITH_BASEPATHS = "globus-reference-endpoints-with-basepaths";
    static final String GLOBUS_TOKEN = "globus-token";
    
    public static boolean isDataverseManaged(String driverId) {
        return Boolean.parseBoolean(StorageIO.getConfigParamForDriver(driverId, MANAGED));
    }
    
    public static String getEndpointId(String driverId) {
        String baseUrl = StorageIO.getConfigParamForDriver(driverId, GLOBUS_TRANSFER_ENDPOINT_WITH_BASEPATH);
        String endpointWithBasePath = baseUrl.substring(baseUrl.lastIndexOf(DataAccess.SEPARATOR) + 3);
        int pathStart = endpointWithBasePath.indexOf("/");
        return pathStart > 0 ? endpointWithBasePath.substring(0, pathStart) : endpointWithBasePath;
        
    }
    
    public static boolean acceptsGlobusTransfers(String storeId) {
        if(StorageIO.getConfigParamForDriver(storeId, GLOBUS_TRANSFER_ENDPOINT_WITH_BASEPATH) != null) {
            return true;
        }
        return false;
    }

    public static boolean allowsGlobusReferences(String storeId) {
        if(StorageIO.getConfigParamForDriver(storeId, GLOBUS_REFERENCE_ENDPOINTS_WITH_BASEPATHS) != null) {
            return true;
        }
        return false;
    }
    
}
