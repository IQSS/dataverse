package edu.harvard.iq.dataverse.api.datadeposit;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.UserRequestMetadata;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;
import org.apache.abdera.Abdera;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.CollectionListManager;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;
import org.swordapp.server.UriRegistry;

public class CollectionListManagerImpl implements CollectionListManager {

    private static final Logger logger = Logger.getLogger(CollectionListManagerImpl.class.getCanonicalName());
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DatasetServiceBean datasetService;
    @Inject
    SwordAuth swordAuth;
    @Inject
    UrlManager urlManager;

    private HttpServletRequest request;

    @Override
    public Feed listCollectionContents(IRI iri, AuthCredentials authCredentials, SwordConfiguration swordConfiguration) throws SwordServerException, SwordAuthException, SwordError {
        AuthenticatedUser user = swordAuth.auth(authCredentials);
        user.setRequestMetadata(new UserRequestMetadata(request));
        urlManager.processUrl(iri.toString());
        String dvAlias = urlManager.getTargetIdentifier();
        if (urlManager.getTargetType().equals("dataverse") && dvAlias != null) {

            Dataverse dv = dataverseService.findByAlias(dvAlias);

            if (dv != null) {
                if (swordAuth.hasAccessToModifyDataverse(user, dv)) {
                    Abdera abdera = new Abdera();
                    Feed feed = abdera.newFeed();
                    feed.setTitle(dv.getName());
                    /**
                     * @todo For the supplied dataverse, should we should only
                     * the datasets that are *owned* by the user? Probably not!
                     * We should be using the permission system? Show the
                     * equivalent of datasets the user is "admin" on? What
                     * permission should we check?
                     *
                     * And should we only show datasets at the current level or
                     * should we show datasets that are in sub-dataverses as
                     * well?
                     */
                    List childDvObjects = dataverseService.findByOwnerId(dv.getId());
                    childDvObjects.addAll(datasetService.findByOwnerId(dv.getId()));
                    List<Dataset> datasets = new ArrayList<>();
                    for (Object object : childDvObjects) {
                        if (object instanceof Dataset) {
                            datasets.add((Dataset) object);
                        }
                    }
                    String baseUrl = urlManager.getHostnamePlusBaseUrlPath(iri.toString());
                    for (Dataset dataset : datasets) {
                        String editUri = baseUrl + "/edit/study/" + dataset.getGlobalId();
                        String editMediaUri = baseUrl + "/edit-media/study/" + dataset.getGlobalId();
                        Entry entry = feed.addEntry();
                        entry.setId(editUri);
                        entry.setTitle(dataset.getLatestVersion().getTitle());
                        entry.setBaseUri(new IRI(editUri));
                        entry.addLink(editMediaUri, "edit-media");
                        feed.addEntry(entry);
                    }
                    Boolean dvHasBeenReleased = dv.isReleased();
                    feed.addSimpleExtension(new QName(UriRegistry.SWORD_STATE, "dataverseHasBeenReleased"), dvHasBeenReleased.toString());
                    return feed;
                } else {
                    throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "user " + user.getDisplayInfo().getTitle() + " is not authorized to list datasets in dataverse " + dv.getAlias());
                }

            } else {
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Could not find dataverse: " + dvAlias);
            }
        } else {
            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Couldn't determine target type or identifer from URL: " + iri);
        }
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

}
