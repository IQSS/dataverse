package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.RoleAssignment;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

@Stateless
public class IndexAsync {

    private static final Logger logger = Logger.getLogger(IndexAsync.class.getCanonicalName());

    @EJB
    SolrIndexServiceBean solrIndexService;
    @EJB
    DvObjectServiceBean dvObjectService;
    @Inject
    Event<IndexingRequest> indexingRequests;

    /**
     * Reindexes the permissions of the definition point once the transaction that
     * assigned or revoked the role has committed, so that the background job sees
     * the change (see {@link IndexingRequest}). A revoked assignment is gone by then,
     * so only the id of its definition point is passed on.
     */
    public void indexRole(RoleAssignment roleAssignment) {
        indexingRequests.fire(new IndexingRequest.IndexPermissions(List.of(roleAssignment.getDefinitionPoint().getId())));
    }

    public void indexRoles(Collection<DvObject> dvObjects) {
        indexingRequests.fire(new IndexingRequest.IndexPermissions(dvObjects.stream().map(DvObject::getId).toList()));
    }

    @Asynchronous
    public void indexPermissionsInBackground(Collection<Long> dvObjectIds) {
        for (Long dvObjectId : dvObjectIds) {
            DvObject dvObject = dvObjectService.findDvObject(dvObjectId);
            if (dvObject == null) {
                logger.log(Level.FINE, "dvObject {0} no longer exists, no permissions to index", dvObjectId);
                continue;
            }
            IndexResponse indexResponse = solrIndexService.indexPermissionsOnSelfAndChildren(dvObject);
            logger.log(Level.FINE, "output from permission indexing operations (dvobject {0}): {1}", new Object[]{dvObjectId, indexResponse});
        }
    }

}
