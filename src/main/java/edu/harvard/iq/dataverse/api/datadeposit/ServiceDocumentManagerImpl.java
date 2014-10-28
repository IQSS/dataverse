package edu.harvard.iq.dataverse.api.datadeposit;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.inject.Inject;
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
        Dataverse rootDataverse = dataverseService.findRootDataverse();
        if (rootDataverse != null) {
            String name = rootDataverse.getName();
            if (name != null) {
                swordWorkspace.setTitle(name);
            }
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

        List<Dataverse> dataverses = permissionService.getDataversesUserHasPermissionOn(user, Permission.AddDataset);
        for (Dataverse dataverse : dataverses) {
            String dvAlias = dataverse.getAlias();
            if (dvAlias != null && !dvAlias.isEmpty()) {
                SwordCollection swordCollection = new SwordCollection();
                swordCollection.setTitle(dataverse.getName());
                swordCollection.setHref(hostnamePlusBaseUrl + "/collection/dataverse/" + dvAlias);
                swordCollection.addAcceptPackaging(UriRegistry.PACKAGE_SIMPLE_ZIP);
                /**
                 * @todo for backwards-compatibility with DVN 3.x, display terms
                 * of uses for root dataverse and the dataverse we are iterating
                 * over. What if the root dataverse is not the direct parent of
                 * the dataverse we're iterating over? Show the terms of use
                 * each generation back to the root?
                 *
                 * See also https://github.com/IQSS/dataverse/issues/551
                 */
                swordCollection.setCollectionPolicy(dataverseService.findRootDataverse().getName() + " deposit terms of use: " + dataverseService.findRootDataverse().getDepositTermsOfUse() + "\n---\n" + dataverse.getName() + " deposit terms of use: " + dataverse.getDepositTermsOfUse());
                swordWorkspace.addCollection(swordCollection);
            }
        }
        service.addWorkspace(swordWorkspace);

        return service;
    }

}
