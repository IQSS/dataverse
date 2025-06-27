package edu.harvard.iq.dataverse.pidproviders;

import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.GlobalId;
import jakarta.json.JsonObject;
import java.util.*;
import java.util.logging.Logger;

public interface PidProvider {

    static final Logger logger = Logger.getLogger(PidProvider.class.getCanonicalName());

    boolean alreadyRegistered(DvObject dvo) throws Exception;
    
    /**
     * This call reports whether a PID is registered with the external Provider
     * service. For providers like DOIs/Handles with an external service, this call
     * should accurately report whether the PID has been registered in the service.
     * For providers with no external service, the call should return true if the
     * PID is defined locally. If it isn't, these no-service providers need to know
     * whether use case of the caller requires that the returned value should
     * default to true or false - via the noProviderDefault parameter.
     * 
     * @param globalId
     * @param noProviderDefault - when there is no external service, and no local
     *                          use of the PID, this should be returned
     * @return whether the PID should be considered registered or not.
     * @throws Exception
     */
    boolean alreadyRegistered(GlobalId globalId, boolean noProviderDefault) throws Exception;
    
    boolean registerWhenPublished();
    boolean canManagePID();
    
    List<String> getProviderInformation();

    String createIdentifier(DvObject dvo) throws Throwable;

    Map<String,String> getIdentifierMetadata(DvObject dvo);

    String modifyIdentifierTargetURL(DvObject dvo) throws Exception;

    void deleteIdentifier(DvObject dvo) throws Exception;
    
    Map<String, String> getMetadataForCreateIndicator(DvObject dvObject);
    
    Map<String,String> getMetadataForTargetURL(DvObject dvObject);
    
    DvObject generatePid(DvObject dvObject);
    
    String getIdentifier(DvObject dvObject);
    
    boolean publicizeIdentifier(DvObject studyIn);
    
    boolean updateIdentifier(DvObject dvObject);
    
    boolean isGlobalIdUnique(GlobalId globalId);
    
    String getUrlPrefix();
    String getSeparator();
    
    String getProtocol();
    String getProviderType();
    String getId();
    String getLabel();
    String getAuthority();
    String getShoulder();
    String getIdentifierGenerationStyle();
    
    public static Optional<GlobalId> parse(String identifierString) {
        try {
            return Optional.of(PidUtil.parseAsGlobalID(identifierString));
        } catch ( IllegalArgumentException _iae) {
            return Optional.empty();
        }
    }
    
    /** 
     *   Parse a Persistent Id and set the protocol, authority, and identifier
     * 
     *   Example 1: doi:10.5072/FK2/BYM3IW
     *       protocol: doi
     *       authority: 10.5072
     *       identifier: FK2/BYM3IW
     * 
     *   Example 2: hdl:1902.1/111012
     *       protocol: hdl
     *       authority: 1902.1
     *       identifier: 111012
     *
     * @param identifierString
     * @param separator the string that separates the authority from the identifier.
     * @param destination the global id that will contain the parsed data.
     * @return {@code destination}, after its fields have been updated, or
     *         {@code null} if parsing failed.
     */
    public GlobalId parsePersistentId(String identifierString);
    
    public GlobalId parsePersistentId(String protocol, String authority, String identifier);

    
    
    public static boolean isValidGlobalId(String protocol, String authority, String identifier) {
        if (protocol == null || authority == null || identifier == null) {
            return false;
        }
        if(!authority.equals(PidProvider.formatIdentifierString(authority))) {
            return false;
        }
        if (PidProvider.testforNullTerminator(authority)) {
            return false;
        }
        if(!identifier.equals(PidProvider.formatIdentifierString(identifier))) {
            return false;
        }
        if (PidProvider.testforNullTerminator(identifier)) {
            return false;
        }
        return true;
    }
    
    static String formatIdentifierString(String str){
        
        if (str == null){
            return null;
        }
        // remove whitespace, single quotes, and semicolons
        return str.replaceAll("\\s+|'|;","");  
        
        /*
        <   (%3C)
>   (%3E)
{   (%7B)
}   (%7D)
^   (%5E)
[   (%5B)
]   (%5D)
`   (%60)
|   (%7C)
\   (%5C)
+
        */
        // http://www.doi.org/doi_handbook/2_Numbering.html
    }
    
    static boolean testforNullTerminator(String str){
        if(str == null) {
            return false;
        }
        return str.indexOf('\u0000') > 0;
    }
    
    static boolean checkDOIAuthority(String doiAuthority){
        
        if (doiAuthority==null){
            return false;
        }
        
        if (!(doiAuthority.startsWith("10."))){
            return false;
        }
        
        return true;
    }

    public void setPidProviderServiceBean(PidProviderFactoryBean pidProviderFactoryBean);

    String getDatafilePidFormat();

    Set<String> getManagedSet();

    Set<String> getExcludedSet();

    /** 
     * Whether related pids can be created by this pid provider
     * @see edu.harvard.iq.dataverse.pidproviders.AbstractPidProvider#canCreatePidsLike(GlobalId) more details in the abstract implementation
     * 
     * @param pid
     * @return - whether related pids can be created by this pid provider. 
     */
    boolean canCreatePidsLike(GlobalId pid);

    /**
     * Returns a JSON representation of this pid provider including it's id, label, protocol, authority, separator, and identifier.
     * @return
     */
    public JsonObject getProviderSpecification();
    
    /**
     * Returns a the Citation Style Language (CSL) JSON representation of the pid.
     * For some providers, this could be a call to the service API. For others, it
     * may involve generating a local copy.
     * 
     * @param datasetVersion
     * @return - the CSL Json for the PID
     */
    public JsonObject getCSLJson(DatasetVersion datasetVersion);

}