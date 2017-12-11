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

package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

import java.io.File;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.security.PrivateKey;

/* Handlenet imports: */
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
import org.apache.commons.lang.NotImplementedException;

/**
 *
 * @author Leonid Andreev
 * 
 * This is a *partial* implementation of the Handles global id 
 * service. 
 * As of now, it only does the registration updates, to accommodate 
 * the modifyRegistration datasets API sub-command.
 */
@Stateless
public class HandlenetServiceBean extends AbstractIdServiceBean {

    @EJB
    DataverseServiceBean dataverseService;
    @EJB 
    SettingsServiceBean settingsService;    
    private static final Logger logger = Logger.getLogger(HandlenetServiceBean.class.getCanonicalName());
    
    private static final String HANDLE_PROTOCOL_TAG = "hdl";
    
    public HandlenetServiceBean() {
        logger.log(Level.FINE,"Constructor");
    }

    @Override
    public boolean registerWhenPublished() {
        return false; // TODO current value plays safe, can we loosen up?
    }

    public void reRegisterHandle(DvObject dvObject) {
        logger.log(Level.FINE,"reRegisterHandle");
        if (!HANDLE_PROTOCOL_TAG.equals(dvObject.getProtocol())) {
            logger.warning("reRegisterHandle called on a dvObject with the non-handle global id: "+dvObject.getId());
        }
        
        String handle = getDvObjectHandle(dvObject);

        boolean handleRegistered = isHandleRegistered(handle);
        
        if (handleRegistered) {
            // Rebuild/Modify an existing handle
            
            logger.info("Re-registering an existing handle id "+handle);
            
            String authHandle = getHandleAuthority(dvObject);

            HandleResolver resolver = new HandleResolver();

            String datasetUrl = getRegistrationUrl(dvObject);
            
            logger.info("New registration URL: "+datasetUrl);

            PublicKeyAuthenticationInfo auth = getAuthInfo(dvObject.getAuthority());
            
            try {

                AdminRecord admin = new AdminRecord(authHandle.getBytes("UTF8"), 300,
                        true, true, true, true, true, true,
                        true, true, true, true, true, true);

                int timestamp = (int) (System.currentTimeMillis() / 1000);

                HandleValue[] val = {new HandleValue(100, "HS_ADMIN".getBytes("UTF8"),
                    Encoder.encodeAdminRecord(admin),
                    HandleValue.TTL_TYPE_RELATIVE, 86400,
                    timestamp, null, true, true, true, false), new HandleValue(1, "URL".getBytes("UTF8"),
                    datasetUrl.getBytes(),
                    HandleValue.TTL_TYPE_RELATIVE, 86400,
                    timestamp, null, true, true, true, false)};

                ModifyValueRequest req = new ModifyValueRequest(handle.getBytes("UTF8"), val, auth);

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
            logger.info("Handle " + handle + " not registered. Registering (creating) from scratch.");
            registerNewHandle(dvObject);
        }
    }
    
    public Throwable registerNewHandle(DvObject dvObject) {
        logger.log(Level.FINE,"registerNewHandle");
        String handlePrefix = dvObject.getAuthority();
        String handle = getDvObjectHandle(dvObject);
        String datasetUrl = getRegistrationUrl(dvObject);

        logger.info("Creating NEW handle " + handle);

        String authHandle = getHandleAuthority(dvObject);

        PublicKeyAuthenticationInfo auth = getAuthInfo(handlePrefix);
        HandleResolver resolver = new HandleResolver();

        try {

            AdminRecord admin = new AdminRecord(authHandle.getBytes("UTF8"), 300,
                    true, true, true, true, true, true,
                    true, true, true, true, true, true);

            int timestamp = (int) (System.currentTimeMillis() / 1000);

            HandleValue[] val = {new HandleValue(100, "HS_ADMIN".getBytes("UTF8"),
                Encoder.encodeAdminRecord(admin),
                HandleValue.TTL_TYPE_RELATIVE, 86400,
                timestamp, null, true, true, true, false), new HandleValue(1, "URL".getBytes("UTF8"),
                datasetUrl.getBytes(),
                HandleValue.TTL_TYPE_RELATIVE, 86400,
                timestamp, null, true, true, true, false)};

            CreateHandleRequest req
                    = new CreateHandleRequest(handle.getBytes("UTF8"), val, auth);

            resolver.traceMessages = true;
            AbstractResponse response = resolver.processRequest(req);
            if (response.responseCode == AbstractMessage.RC_SUCCESS) {
                logger.info("Success! Response: \n" + response);
                return null;
            } else {
                logger.log(Level.WARNING, "registerNewHandle failed");
                logger.warning("Error response: \n" + response);
                return new Exception("registerNewHandle failed: " + response);
            }
        } catch (Throwable t) {
            logger.log(Level.WARNING, "registerNewHandle failed");
            logger.log(Level.WARNING, "String {0}", t.toString());
            logger.log(Level.WARNING, "localized message {0}", t.getLocalizedMessage());
            logger.log(Level.WARNING, "cause", t.getCause());
            logger.log(Level.WARNING, "message {0}", t.getMessage());
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
            logger.info("Caught exception trying to process lookup request");
            ex.printStackTrace();
        }
        if((response!=null && response.responseCode==AbstractMessage.RC_SUCCESS)) {
            logger.info("Handle "+handle+" registered.");
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
        String adminCredFile = System.getProperty("dataverse.handlenet.admcredfile");

        key = readKey(adminCredFile);        
        PrivateKey privkey = null;
        privkey = readPrivKey(key, adminCredFile);
        String authHandle =  getHandleAuthority(handlePrefix);
        PublicKeyAuthenticationInfo auth =
                new PublicKeyAuthenticationInfo(Util.encodeString(authHandle), 300, privkey);
        return auth;
    }
    private String getRegistrationUrl(DvObject dvObject) {
        logger.log(Level.FINE,"getRegistrationUrl");
        String siteUrl = systemConfig.getDataverseSiteUrl();
        String targetUrl = siteUrl + dvObject.getTargetUrl() + "hdl:" + dvObject.getAuthority()         
                + "/" + dvObject.getIdentifier();         
        return targetUrl;
    }
 
    public String getSiteUrl() {
        logger.log(Level.FINE,"getSiteUrl");
        String hostUrl = System.getProperty("dataverse.siteUrl");
        if (hostUrl != null && !"".equals(hostUrl)) {
            return hostUrl;
        }
        String hostName = System.getProperty("dataverse.fqdn");
        if (hostName == null) {
            try {
                hostName = InetAddress.getLocalHost().getCanonicalHostName();
            } catch (UnknownHostException e) {
                return null;
            }
        }
        hostUrl = "https://" + hostName;
        return hostUrl;
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
            logger.severe("Cannot read private key " + file +": " + t);
        }
        return key;
    }
    
    private PrivateKey readPrivKey(byte[] key, final String file) {
        logger.log(Level.FINE,"readPrivKey");
        PrivateKey privkey=null;
        
        String secret = System.getProperty("dataverse.handlenet.admprivphrase");
        byte secKey[] = null;
        try {
            if(Util.requiresSecretKey(key)){
                secKey = secret.getBytes();
            }
            key = Util.decrypt(key, secKey);
            privkey = Util.getPrivateKeyFromBytes(key, 0);
        } catch (Throwable t){
            logger.severe("Can't load private key in " + file +": " + t);
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
    
    private String getHandleAuthority(DvObject dvObject){
        return getHandleAuthority(dvObject.getAuthority());
    }
    
    private String getHandleAuthority(String handlePrefix) {
        logger.log(Level.FINE,"getHandleAuthority");
        return "0.NA/" + handlePrefix;
    }

    @Override
    public boolean alreadyExists(DvObject dvObject) throws Exception {
        String handle = getDvObjectHandle(dvObject);
        return isHandleRegistered(handle);
    }
    
    @Override
    public HashMap getIdentifierMetadata(DvObject dvObject) {
        throw new NotImplementedException();
    }

    @Override
    public HashMap lookupMetadataFromIdentifier(String protocol, String authority, String separator, String identifier)  {
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
        String authHandle = getAuthHandle(dvObject);

        String adminCredFile = System.getProperty("dataverse.handlenet.admcredfile");

        byte[] key = readKey(adminCredFile);
        PrivateKey privkey = readPrivKey(key, adminCredFile);

        HandleResolver resolver = new HandleResolver();
        resolver.setSessionTracker(new ClientSessionTracker());

        PublicKeyAuthenticationInfo auth =
                new PublicKeyAuthenticationInfo(Util.encodeString(authHandle), 300, privkey);

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

    private String getAuthHandle(DvObject dvObject) {
        // TODO hack: GNRSServiceBean retrieved this from vdcNetworkService
        return "0.NA/" + dvObject.getAuthority();
    }
    
    @Override
    public List<String> getProviderInformation(){
        ArrayList <String> providerInfo = new ArrayList<>();
        String providerName = "Handle";
        String providerLink = "https://hdl.handle.net";
        providerInfo.add(providerName);
        providerInfo.add(providerLink);
        return providerInfo;
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
        
        return updateIdentifierStatus(dvObject, "public");

    }

}



