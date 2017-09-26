package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.RoleAssignment;
import java.util.Collection;
import java.util.logging.Logger;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
public class IndexAsync {

    private static final Logger logger = Logger.getLogger(IndexAsync.class.getCanonicalName());

    @EJB
    SolrIndexServiceBean solrIndexService;

    @Asynchronous
    public void indexRole(RoleAssignment roleAssignment) {
        IndexResponse indexResponse = solrIndexService.indexPermissionsOnSelfAndChildren(roleAssignment.getDefinitionPoint());
        logger.fine("output from indexing operations: " + indexResponse);
    }
    
    @Asynchronous 
    public void indexRoles(Collection<DvObject> dvObjects) {
        for (DvObject dvObject : dvObjects) {
            IndexResponse indexResponse = solrIndexService.indexPermissionsOnSelfAndChildren(dvObject);
            logger.fine("output from permission indexing operations (dvobject " + dvObject.getId() + ": " + indexResponse);
        }
    }

}
