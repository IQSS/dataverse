package edu.harvard.iq.dataverse.api.datadeposit;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DataverseUser;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.inject.Inject;
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

    @Override
    public Feed listCollectionContents(IRI iri, AuthCredentials authCredentials, SwordConfiguration swordConfiguration) throws SwordServerException, SwordAuthException, SwordError {
        DataverseUser dataverseUser = swordAuth.auth(authCredentials);

        urlManager.processUrl(iri.toString());
        String dvAlias = urlManager.getTargetIdentifier();
        if (urlManager.getTargetType().equals("dataverse") && dvAlias != null) {

            Dataverse dv = dataverseService.findByAlias(dvAlias);

            if (dv != null) {
                if (swordAuth.hasAccessToModifyDataverse(dataverseUser, dv)) {
                    Abdera abdera = new Abdera();
                    Feed feed = abdera.newFeed();
                    feed.setTitle(dv.getName());
                    /**
                     * @todo how do I get a list of datasets belonging to a
                     * user?
                     */
//                    Collection<Study> studies = dv.getOwnedStudies();
                    List childDvObjects = dataverseService.findByOwnerId(dv.getId());
                    childDvObjects.addAll(datasetService.findByOwnerId(dv.getId()));
                    List<Dataset> studies = new ArrayList<>();
                    for (Object object : childDvObjects) {
                        if (object instanceof Dataset) {
                            studies.add((Dataset) object);
                        }
                    }
                    String baseUrl = urlManager.getHostnamePlusBaseUrlPath(iri.toString());
                    for (Dataset study : studies) {
                        String editUri = baseUrl + "/edit/study/" + study.getGlobalId();
                        String editMediaUri = baseUrl + "/edit-media/study/" + study.getGlobalId();
                        Entry entry = feed.addEntry();
                        entry.setId(editUri);
                        entry.setTitle(study.getLatestVersion().getTitle());
                        entry.setBaseUri(new IRI(editUri));
                        entry.addLink(editMediaUri, "edit-media");
                        feed.addEntry(entry);
                    }
                    Boolean dvHasBeenReleased = dv.isReleased();
                    feed.addSimpleExtension(new QName(UriRegistry.SWORD_STATE, "dataverseHasBeenReleased"), dvHasBeenReleased.toString());
                    return feed;
                } else {
                    throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "user " + dataverseUser.getUserName() + " is not authorized to list datasets in dataverse " + dv.getAlias());
                }

            } else {
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Could not find dataverse: " + dvAlias);
            }
        } else {
            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Couldn't determine target type or identifer from URL: " + iri);
        }
    }

}
