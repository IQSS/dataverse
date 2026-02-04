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

package edu.harvard.iq.dataverse.pidproviders.handle;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.pidproviders.AbstractPidProvider;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.PrivateKey;

/* Handlenet imports: */
import edu.harvard.iq.dataverse.util.SystemConfig;
import net.handle.hdllib.AbstractMessage;
import net.handle.hdllib.AbstractResponse;
import net.handle.hdllib.AdminRecord;
import net.handle.hdllib.ClientSessionTracker;
import net.handle.hdllib.CreateHandleRequest;
import net.handle.hdllib.DeleteHandleRequest;
import net.handle.hdllib.Encoder;
import net.handle.hdllib.HandleException;
import net.handle.hdllib.HandleResolver;
import net.handle.hdllib.HandleValue;
import net.handle.hdllib.ModifyValueRequest;
import net.handle.hdllib.PublicKeyAuthenticationInfo;
import net.handle.hdllib.ResolutionRequest;
import net.handle.hdllib.Util;
import org.apache.commons.lang3.NotImplementedException;

/**
 *
 * @author Leonid Andreev
 * 
 * This is a *partial* implementation of the Handles global id 
 * service. 
 * As of now, it only does the registration updates, to accommodate 
 * the modifyRegistration datasets API sub-command.
 * 
 * Note that while Handles are nominally case sensitive, handle.net is
 * configured to be case-insensitive and Dataverse makes case-insensitve
 * database look-ups to find Handles (See #11003). That said, database
 * entries are stored in the case matching the configuration of the provider.
 */
public class HandlePidProvider extends AbstractPidProvider {

    private static final Logger logger = Logger.getLogger(HandlePidProvider.class.getCanonicalName());
    
    public static final String HDL_PROTOCOL = "hdl";
    public static final String TYPE = "hdl";
    public static final String HTTP_HDL_RESOLVER_URL = "http://hdl.handle.net/";
    public static final String HDL_RESOLVER_URL = "https://hdl.handle.net/";

    
    
    int handlenetIndex;
    private boolean isIndependentHandleService;
    private String authHandle;
    private String keyPath;
    private String keyPassphrase;
    
    public HandlePidProvider(String id, String label, String authority, String shoulder, String identifierGenerationStyle,
            String datafilePidFormat, String managedList, String excludedList, int index, boolean isIndependentService, String authHandle, String path, String passphrase) {
        super(id, label, HDL_PROTOCOL, authority, shoulder, identifierGenerationStyle, datafilePidFormat, managedList, excludedList);
        this.handlenetIndex = index;
        this.isIndependentHandleService = isIndependentService;
        this.authHandle = authHandle;
        this.keyPath = path;
        this.keyPassphrase = passphrase;

    }

    @Override
    public boolean registerWhenPublished() {
        return false; // TODO current value plays safe, can we loosen up?
    }

    public void reRegisterHandle(DvObject dvObject) {
        logger.log(Level.FINE,"reRegisterHandle");
        if (!HDL_PROTOCOL.equals(dvObject.getProtocol())) {
            logger.log(Level.WARNING, "reRegisterHandle called on a dvObject with the non-handle global id: {0}", dvObject.getId());
        }
        
        String handle = getDvObjectHandle(dvObject);

        boolean handleRegistered = isHandleRegistered(handle);
        
        if (handleRegistered) {
            // Rebuild/Modify an existing handle
            
            logger.log(Level.INFO, "Re-registering an existing handle id {0}", handle);
            
            String authHandle = getAuthenticationHandle(dvObject);

            HandleResolver resolver = new HandleResolver();

            String datasetUrl = getRegistrationUrl(dvObject);
            
            logger.log(Level.INFO, "New registration URL: {0}", datasetUrl);

            PublicKeyAuthenticationInfo auth = getAuthInfo(dvObject.getAuthority());
            
            try {

                AdminRecord admin = new AdminRecord(authHandle.getBytes(StandardCharsets.UTF_8), handlenetIndex,
                        true, true, true, true, true, true,
                        true, true, true, true, true, true);

                int timestamp = (int) (System.currentTimeMillis() / 1000);

                HandleValue[] val = {new HandleValue(100, "HS_ADMIN".getBytes(StandardCharsets.UTF_8),
                    Encoder.encodeAdminRecord(admin),
                    HandleValue.TTL_TYPE_RELATIVE, 86400,
                    timestamp, null, true, true, true, false), new HandleValue(1, "URL".getBytes(StandardCharsets.UTF_8),
                    datasetUrl.getBytes(),
                    HandleValue.TTL_TYPE_RELATIVE, 86400,
                    timestamp, null, true, true, true, false)};

                ModifyValueRequest req = new ModifyValueRequest(handle.getBytes(StandardCharsets.UTF_8), val, auth);

                resolver.traceMessages = true;
                AbstractResponse response = resolver.processRequest(req);
                if (response.responseCode == AbstractMessage.RC_SUCCESS) {
                    logger.info("\nGot Response: \n" + response);
                } else {
                    logger.info("\nGot Error: \n" + response);
                }
            } catch (Throwable t) {
                logger.fine("\nError: " + t);
            }
        } else {
            // Create a new handle from scratch:
            logger.log(Level.INFO, "Handle {0} not registered. Registering (creating) from scratch.", handle);
            registerNewHandle(dvObject);
        }
    }
    
    public Throwable registerNewHandle(DvObject dvObject) {
        logger.log(Level.FINE,"registerNewHandle");
        String handlePrefix = dvObject.getAuthority();
        String handle = getDvObjectHandle(dvObject);
        String datasetUrl = getRegistrationUrl(dvObject);

        logger.log(Level.INFO, "Creating NEW handle {0}", handle);

        String authHandle = getAuthenticationHandle(dvObject);

        PublicKeyAuthenticationInfo auth = getAuthInfo(handlePrefix);
        HandleResolver resolver = new HandleResolver();

        try {

            AdminRecord admin = new AdminRecord(authHandle.getBytes(StandardCharsets.UTF_8), handlenetIndex,
                    true, true, true, true, true, true,
                    true, true, true, true, true, true);

            int timestamp = (int) (System.currentTimeMillis() / 1000);

            HandleValue[] val = {new HandleValue(100, "HS_ADMIN".getBytes(StandardCharsets.UTF_8),
                Encoder.encodeAdminRecord(admin),
                HandleValue.TTL_TYPE_RELATIVE, 86400,
                timestamp, null, true, true, true, false), new HandleValue(1, "URL".getBytes(StandardCharsets.UTF_8),
                datasetUrl.getBytes(),
                HandleValue.TTL_TYPE_RELATIVE, 86400,
                timestamp, null, true, true, true, false)};

            CreateHandleRequest req
                    = new CreateHandleRequest(handle.getBytes(StandardCharsets.UTF_8), val, auth);

            resolver.traceMessages = true;
            AbstractResponse response = resolver.processRequest(req);
            if (response.responseCode == AbstractMessage.RC_SUCCESS) {
                logger.log(Level.INFO, "Success! Response: \n{0}", response);
                return null;
            } else {
                logger.log(Level.WARNING, "RegisterNewHandle failed. Error response: {0}", response);
                return new Exception("registerNewHandle failed: " + response);
            }
        } catch (Throwable t) {
            logger.log(Level.WARNING, "registerNewHandle failed", t);
            return t;
        }
    }
    
    public boolean isHandleRegistered(String handle){
        logger.log(Level.FINE,"isHandleRegistered");
        boolean handleRegistered = false;
        ResolutionRequest req = buildResolutionRequest(handle);
        AbstractResponse response = null;
        HandleResolver resolver = new HandleResolver();
        try {
            response = resolver.processRequest(req);
        } catch (HandleException ex) {
            logger.log(Level.WARNING, "Caught exception trying to process lookup request", ex);
        }
        if((response!=null && response.responseCode==AbstractMessage.RC_SUCCESS)) {
            logger.log(Level.INFO, "Handle {0} registered.", handle);
            handleRegistered = true;
        } 
        return handleRegistered;
    }
    
    private ResolutionRequest buildResolutionRequest(final String handle) {
        logger.log(Level.FINE,"buildResolutionRequest");
        String handlePrefix = handle.substring(0,handle.indexOf("/"));
        
        PublicKeyAuthenticationInfo auth = getAuthInfo(handlePrefix);
        
        byte[][] types = null;
        int[] indexes = null;
        ResolutionRequest req =
                new ResolutionRequest(Util.encodeString(handle),
                types, indexes,
                auth);
        req.certify = false;
        req.cacheCertify = true;
        req.authoritative = false;
        req.ignoreRestrictedValues = true;
        return req;
    }
    
    private PublicKeyAuthenticationInfo getAuthInfo(String handlePrefix) {
        logger.log(Level.FINE,"getAuthInfo");
        byte[] key = null;
        String adminCredFile = getKeyPath();
       
        key = readKey(adminCredFile);
        PrivateKey privkey = null;
        privkey = readPrivKey(key, adminCredFile);
        String authHandle =  getAuthenticationHandle(handlePrefix);
        PublicKeyAuthenticationInfo auth =
                new PublicKeyAuthenticationInfo(Util.encodeString(authHandle), handlenetIndex, privkey);
        return auth;
    }
    private String getRegistrationUrl(DvObject dvObject) {
        logger.log(Level.FINE,"getRegistrationUrl");
        String siteUrl = SystemConfig.getDataverseSiteUrlStatic();
        String targetUrl = siteUrl + dvObject.getTargetUrl() + "hdl:" + dvObject.getAuthority()
                + "/" + dvObject.getIdentifier();         
        return targetUrl;
    }
 
    public String getSiteUrl() {
        return SystemConfig.getDataverseSiteUrlStatic();
    }
    
    private byte[] readKey(final String file) {
        logger.log(Level.FINE,"readKey");
        byte[] key = null;
        try {
            File f = new File(file);
            FileInputStream fs = new FileInputStream(f);
            key = new byte[(int)f.length()];
            int n=0;
            while(n<key.length) {
                key[n++] = (byte)fs.read();
            }
        } catch (Throwable t){
            logger.log(Level.SEVERE, "Cannot read private key {0}: {1}", new Object[]{file, t});
        }
        return key;
    }
    
    private PrivateKey readPrivKey(byte[] key, final String file) {
        logger.log(Level.FINE,"readPrivKey");
        PrivateKey privkey = null;
        
        try {
            byte[] secKey = null;
            if ( Util.requiresSecretKey(key) ) {
                secKey = getKeyPassphrase().getBytes(StandardCharsets.UTF_8);
            }
            key = Util.decrypt(key, secKey);
            privkey = Util.getPrivateKeyFromBytes(key, 0);
        } catch (Throwable t){
            logger.log(Level.SEVERE, "Can''t load private key in {0}: {1}", new Object[]{file, t});
        }
        return privkey;
    }
    
    private String getDvObjectHandle(DvObject dvObject) {
        /* 
         * This is different from dataset.getGlobalId() in that we don't 
         * need the "hdl:" prefix.
         */
        String handle = dvObject.getAuthority() + "/" + dvObject.getIdentifier();
        return handle;
    }
    
    private String getAuthenticationHandle(DvObject dvObject){
        return getAuthenticationHandle(dvObject.getAuthority());
    }
    
    private String getAuthenticationHandle(String handlePrefix) {
        logger.log(Level.FINE,"getAuthenticationHandle");
        if (getHandleAuthHandle()!=null) {
            return getHandleAuthHandle();
        } else if (isIndependentHandleService()) {
            return handlePrefix + "/ADMIN";
        } else {
            return "0.NA/" + handlePrefix;
        }
    }

    @Override
    public boolean alreadyRegistered(DvObject dvObject) {
        String handle = getDvObjectHandle(dvObject);
        return isHandleRegistered(handle);
    }
    
    @Override
    public boolean alreadyRegistered(GlobalId pid, boolean noProviderDefault) throws Exception {
        String handle = pid.getAuthority() + "/" + pid.getIdentifier();
        return isHandleRegistered(handle);
    }
    
    @Override
    public Map<String,String> getIdentifierMetadata(DvObject dvObject) {
        throw new NotImplementedException();
    }

    @Override
    public String modifyIdentifierTargetURL(DvObject dvObject) throws Exception  {
        logger.log(Level.FINE,"modifyIdentifier");
        reRegisterHandle(dvObject);
        if(dvObject instanceof Dataset){
            Dataset dataset = (Dataset) dvObject;
            dataset.getFiles().forEach((df) -> {
                reRegisterHandle(df);
            });            
        }
        return getIdentifier(dvObject);
    }

    @Override
    public void deleteIdentifier(DvObject dvObject) throws Exception  {
        String handle = getDvObjectHandle(dvObject);
        String authHandle = getAuthenticationHandle(dvObject);
    
        String adminCredFile = getKeyPath();
        
        byte[] key = readKey(adminCredFile);
        PrivateKey privkey = readPrivKey(key, adminCredFile);

        HandleResolver resolver = new HandleResolver();
        resolver.setSessionTracker(new ClientSessionTracker());

        PublicKeyAuthenticationInfo auth =
                new PublicKeyAuthenticationInfo(Util.encodeString(authHandle), handlenetIndex, privkey);

        DeleteHandleRequest req =
                new DeleteHandleRequest(Util.encodeString(handle), auth);
        AbstractResponse response=null;
        try {
            response = resolver.processRequest(req);
        } catch (HandleException ex) {
            ex.printStackTrace();
        }
        if(response==null || response.responseCode!=AbstractMessage.RC_SUCCESS) {
            logger.fine("error deleting '"+handle+"': "+response);
        } else {
            logger.fine("deleted "+handle);
        }
    }

    private boolean updateIdentifierStatus(DvObject dvObject, String statusIn) {
        logger.log(Level.FINE,"updateIdentifierStatus");
        reRegisterHandle(dvObject); // No Need to register new - this is only called when a handle exists
        return true;
    }
    
    @Override
    public List<String> getProviderInformation(){
        return List.of(getId(), HDL_RESOLVER_URL);
    }


    @Override
    public String createIdentifier(DvObject dvObject) throws Throwable {
        Throwable result = registerNewHandle(dvObject);
        if (result != null)
            throw result;
        // TODO get exceptions from under the carpet
        return getDvObjectHandle(dvObject);

    }


    @Override
    public boolean publicizeIdentifier(DvObject dvObject) {
        if (dvObject.getIdentifier() == null || dvObject.getIdentifier().isEmpty()){
            generatePid(dvObject);
        }
        return updateIdentifierStatus(dvObject, "public");

    }

    @Override
    public GlobalId parsePersistentId(String pidString) {
        if (pidString.startsWith(HDL_RESOLVER_URL)) {
            pidString = pidString.replace(HDL_RESOLVER_URL, (HDL_PROTOCOL + ":"));
        } else if (pidString.startsWith(HTTP_HDL_RESOLVER_URL)) {
            pidString = pidString.replace(HTTP_HDL_RESOLVER_URL, (HDL_PROTOCOL + ":"));
        }
        return super.parsePersistentId(pidString);
    }

    @Override
    public GlobalId parsePersistentId(String protocol, String identifierString) {
        if (!HDL_PROTOCOL.equals(protocol)) {
            return null;
        }
        GlobalId globalId = super.parsePersistentId(protocol, identifierString);
        return globalId;
    }
    
    @Override
    public GlobalId parsePersistentId(String protocol, String authority, String identifier) {
        if (!HDL_PROTOCOL.equals(protocol)) {
            return null;
        }
        return super.parsePersistentId(protocol, authority, identifier);
    }

    @Override
    public String getUrlPrefix() {
        return HDL_RESOLVER_URL;
    }

    @Override
    public String getProtocol() {
        return HDL_PROTOCOL;
    }

    @Override
    public String getProviderType() {
        return TYPE;
    }

    public String getKeyPath() {
        return keyPath;
    }

    public String getKeyPassphrase() {
        return keyPassphrase;
    }

    public boolean isIndependentHandleService() {
        return isIndependentHandleService;
    }
    
    public String getHandleAuthHandle() {
        return authHandle;
    }
}


