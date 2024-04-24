package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.RoleAssignment;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class IndexAsync {

    private static final Logger logger = Logger.getLogger(IndexAsync.class.getCanonicalName());

    @EJB
    SolrIndexServiceBean solrIndexService;

    @Asynchronous
    public void indexRole(RoleAssignment roleAssignment) {
        try {
            Thread.sleep(1000); //we wait to ensure a nested native query are up to date
        } catch (InterruptedException ex) {
            Logger.getLogger(IndexAsync.class.getName()).log(Level.SEVERE, null, ex);
        }
        IndexResponse indexResponse = solrIndexService.indexPermissionsOnSelfAndChildren(roleAssignment.getDefinitionPoint());
        logger.fine("output from indexing operations: " + indexResponse);
    }
    
    @Asynchronous 
    public void indexRoles(Collection<DvObject> dvObjects) {
        try {
            Thread.sleep(1000); //we wait to ensure a nested native query are up to date
        } catch (InterruptedException ex) {
            Logger.getLogger(IndexAsync.class.getName()).log(Level.SEVERE, null, ex);
        }
        for (DvObject dvObject : dvObjects) {
            IndexResponse indexResponse = solrIndexService.indexPermissionsOnSelfAndChildren(dvObject);
            logger.fine("output from permission indexing operations (dvobject " + dvObject.getId() + ": " + indexResponse);
        }
    }

}
