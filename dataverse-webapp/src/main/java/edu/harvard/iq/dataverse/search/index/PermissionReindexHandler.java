package edu.harvard.iq.dataverse.search.index;

import edu.harvard.iq.dataverse.persistence.DvObject;

import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;

import java.util.logging.Logger;

/**
 * Handler of {@link PermissionReindexEvent} events.
 * It assures that changes in permissions will be reflected
 * in solr index.
 *
 * @author madryk
 */
@ApplicationScoped
public class PermissionReindexHandler {

    private static final Logger logger = Logger.getLogger(PermissionReindexHandler.class.getCanonicalName());

    @EJB
    SolrIndexServiceBean solrIndexService;

    // -------------------- LOGIC --------------------

    public void reindexPermission(@Observes(during = TransactionPhase.AFTER_SUCCESS) PermissionReindexEvent reindexEvent) {
        for (DvObject dvObject : reindexEvent.getDvObjects()) {
            IndexResponse indexResponse = solrIndexService.indexPermissionsOnSelfAndChildren(dvObject);
            logger.fine("output from permission indexing operations (dvobject " + dvObject.getId() + ": " + indexResponse);
        }
    }
}
