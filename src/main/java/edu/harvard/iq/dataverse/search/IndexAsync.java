package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.RoleAssignment;
import java.util.Collection;
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
    IndexAsync self;
    @Inject
    Event<IndexingRequest> indexingRequests;

    /**
     * Reindexes the permissions of the definition point once the transaction that
     * assigned or revoked the role has committed, so that the background job sees
     * the change (see {@link IndexingRequest}).
     */
    public void indexRole(RoleAssignment roleAssignment) {
        indexingRequests.fire(new IndexingRequest(() -> self.indexRoleInBackground(roleAssignment)));
    }

    public void indexRoles(Collection<DvObject> dvObjects) {
        indexingRequests.fire(new IndexingRequest(() -> self.indexRolesInBackground(dvObjects)));
    }

    @Asynchronous
    public void indexRoleInBackground(RoleAssignment roleAssignment) {
        IndexResponse indexResponse = solrIndexService.indexPermissionsOnSelfAndChildren(roleAssignment.getDefinitionPoint());
        logger.fine("output from indexing operations: " + indexResponse);
    }

    @Asynchronous
    public void indexRolesInBackground(Collection<DvObject> dvObjects) {
        for (DvObject dvObject : dvObjects) {
            IndexResponse indexResponse = solrIndexService.indexPermissionsOnSelfAndChildren(dvObject);
            logger.fine("output from permission indexing operations (dvobject " + dvObject.getId() + ": " + indexResponse);
        }
    }

}
