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

package edu.harvard.iq.dataverse.pidproviders;

import edu.harvard.iq.dataverse.AbstractGlobalIdServiceBean;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.HandlenetServiceBean;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import org.apache.commons.lang3.NotImplementedException;

/**
 *
 * @author Leonid Andreev
 * 
 *         This is a *partial* implementation of the Handles global id service.
 *         As of now, it only does the registration updates, to accommodate the
 *         modifyRegistration datasets API sub-command.
 */
@Stateless
public class UnmanagedHandlenetServiceBean extends AbstractGlobalIdServiceBean {

    private static final Logger logger = Logger.getLogger(UnmanagedHandlenetServiceBean.class.getCanonicalName());

    public UnmanagedHandlenetServiceBean() {
        logger.log(Level.FINE, "Constructor");
    }

    @Override
    public boolean canManagePID() {
        return false;
    }

    @Override
    public boolean registerWhenPublished() {
        throw new NotImplementedException();
    }

    public void reRegisterHandle(DvObject dvObject) {
        throw new NotImplementedException();
    }

    public Throwable registerNewHandle(DvObject dvObject) {
        throw new NotImplementedException();
    }

    @Override
    public boolean alreadyExists(GlobalId pid) throws Exception {
        throw new NotImplementedException();
    }

    @Override
    public Map<String, String> getIdentifierMetadata(DvObject dvObject) {
        throw new NotImplementedException();
    }

    @Override
    public String modifyIdentifierTargetURL(DvObject dvObject) throws Exception {
        throw new NotImplementedException();
    }

    @Override
    public void deleteIdentifier(DvObject dvObject) throws Exception {
        throw new NotImplementedException();
    }

    @Override
    public List<String> getProviderInformation() {
        ArrayList<String> providerInfo = new ArrayList<>();
        String providerName = "UnmanagedHandle";
        String providerLink = "";
        providerInfo.add(providerName);
        providerInfo.add(providerLink);
        return providerInfo;
    }

    @Override
    public String createIdentifier(DvObject dvObject) throws Throwable {
        throw new NotImplementedException();
    }

    @Override
    public boolean publicizeIdentifier(DvObject dvObject) {
        throw new NotImplementedException();
    }

    @Override
    public GlobalId parsePersistentId(String pidString) {
        if (pidString.startsWith(HandlenetServiceBean.HDL_RESOLVER_URL)) {
            pidString = pidString.replace(HandlenetServiceBean.HDL_RESOLVER_URL,
                    (HandlenetServiceBean.HDL_PROTOCOL + ":"));
        } else if (pidString.startsWith(HandlenetServiceBean.HTTP_HDL_RESOLVER_URL)) {
            pidString = pidString.replace(HandlenetServiceBean.HTTP_HDL_RESOLVER_URL,
                    (HandlenetServiceBean.HDL_PROTOCOL + ":"));
        }
        return super.parsePersistentId(pidString);
    }

    public GlobalId parsePersistentId(String protocol, String identifierString) {
        if (!HandlenetServiceBean.HDL_PROTOCOL.equals(protocol)) {
            return null;
        }
        GlobalId globalId = super.parsePersistentId(protocol, identifierString);
        return globalId;
    }

    public GlobalId parsePersistentId(String protocol, String authority, String identifier) {
        if (!HandlenetServiceBean.HDL_PROTOCOL.equals(protocol)) {
            return null;
        }
        return super.parsePersistentId(protocol, authority, identifier);
    }

    @Override
    public String getUrlPrefix() {
        return HandlenetServiceBean.HDL_RESOLVER_URL;
    }
}
