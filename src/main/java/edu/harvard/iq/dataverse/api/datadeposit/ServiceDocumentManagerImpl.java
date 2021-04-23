package edu.harvard.iq.dataverse.api.datadeposit;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.ServiceDocument;
import org.swordapp.server.ServiceDocumentManager;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordCollection;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;
import org.swordapp.server.SwordWorkspace;
import org.swordapp.server.UriRegistry;

public class ServiceDocumentManagerImpl implements ServiceDocumentManager {

    private static final Logger logger = Logger.getLogger(ServiceDocumentManagerImpl.class.getCanonicalName());
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    PermissionServiceBean permissionService;
    @EJB
    SystemConfig systemConfig;
    @Inject
    SwordAuth swordAuth;
    @Inject
    UrlManager urlManager;

    @Override
    public ServiceDocument getServiceDocument(String sdUri, AuthCredentials authCredentials, SwordConfiguration config)
            throws SwordError, SwordServerException, SwordAuthException {

        AuthenticatedUser user = swordAuth.auth(authCredentials);
        String warning = urlManager.processUrl(sdUri);
        ServiceDocument service = new ServiceDocument();
        SwordWorkspace swordWorkspace = new SwordWorkspace();
        String name = dataverseService.getRootDataverseName();
        if (!StringUtils.isEmpty(name)) {
            swordWorkspace.setTitle(name);
        }
        if (warning != null) {
            swordWorkspace.getWrappedWorkspace().setAttributeValue("warning", warning);
        }
        service.setMaxUploadSize(config.getMaxUploadSize());
        String hostnamePlusBaseUrl = urlManager.getHostnamePlusBaseUrlPath(sdUri);
        if (hostnamePlusBaseUrl == null) {
            ServiceDocument serviceDocument = new ServiceDocument();
            return serviceDocument;
        }

        /**
         * We don't expect this to support Shibboleth groups because even though
         * a Shibboleth user can have an API token the transient
         * shibIdentityProvider String on AuthenticatedUser is only set when a
         * SAML assertion is made at runtime via the browser.
         */
        List<Dataverse> dataverses = permissionService.getDataversesUserHasPermissionOn(user, Permission.AddDataset);
        for (Dataverse dataverse : dataverses) {
            String dvAlias = dataverse.getAlias();
            if (dvAlias != null && !dvAlias.isEmpty()) {
                SwordCollection swordCollection = new SwordCollection();
                swordCollection.setTitle(dataverse.getName());
                swordCollection.setHref(hostnamePlusBaseUrl + "/collection/dataverse/" + dvAlias);
                swordCollection.addAcceptPackaging(UriRegistry.PACKAGE_SIMPLE_ZIP);
                swordCollection.setCollectionPolicy(systemConfig.getApiTermsOfUse());
                swordWorkspace.addCollection(swordCollection);
            }
        }
        service.addWorkspace(swordWorkspace);

        return service;
    }

}
