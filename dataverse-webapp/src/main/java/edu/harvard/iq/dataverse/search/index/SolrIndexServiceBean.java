package edu.harvard.iq.dataverse.search.index;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.search.SearchUtil;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObjectBuilder;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
public class SolrIndexServiceBean {

    private static final Logger logger = Logger.getLogger(SolrIndexServiceBean.class.getCanonicalName());

    @EJB
    DvObjectServiceBean dvObjectService;
    @EJB
    DataverseDao dataverseDao;
    @EJB
    private PermissionsSolrDocFactory solrDocFactory;
    @Inject
    private SolrClient solrServer;

    public static String numRowsClearedByClearAllIndexTimes = "numRowsClearedByClearAllIndexTimes";
    public static String messageString = "message";



    public IndexResponse indexPermissionsOnSelfAndChildren(long definitionPointId) {
        DvObject definitionPoint = dvObjectService.findDvObject(definitionPointId);
        if (definitionPoint == null) {
            logger.log(Level.WARNING, "Cannot find a DvOpbject with id of {0}", definitionPointId);
            return null;
        } else {
            return indexPermissionsOnSelfAndChildren(definitionPoint);
        }
    }

    private Set<Long> collectDvObjectIds(List<PermissionsSolrDoc> solrDocs) {
        return solrDocs.stream().map(doc -> doc.getDvObjectId()).collect(Collectors.toSet());
    }
    
    /**
     * We use the database to determine direct children since there is no
     * inheritance
     */
    public IndexResponse indexPermissionsOnSelfAndChildren(DvObject definitionPoint) {

        List<PermissionsSolrDoc> definitionPoints = solrDocFactory.determinePermissionsDocsOnSelfAndChildren(definitionPoint);

        List<String> updatePermissionTimeSuccessStatus = new ArrayList<>();
        Set<Long> affectedDvObjectIds = collectDvObjectIds(definitionPoints);
        
        try {
            persistToSolr(definitionPoints);
            
            for (long dvObjectId : affectedDvObjectIds) {
                dvObjectService.updatePermissionIndexTime(dvObjectId);
                updatePermissionTimeSuccessStatus.add(dvObjectId + ":" + "success");
            }
            
            return new IndexResponse("Number of dvObject permissions indexed for " + definitionPoint
                    + " (updatePermissionTimeSuccessful:" + updatePermissionTimeSuccessStatus
                    + "): " + affectedDvObjectIds.size());
            
        } catch (SolrServerException | IOException ex) {
            return new IndexResponse("problem indexing");
        }
        
    }

    public IndexResponse indexPermissionsForOneDvObject(DvObject dvObject) {
        if (dvObject == null) {
            return new IndexResponse("problem indexing... null DvObject passed in");
        }

        List<PermissionsSolrDoc> definitionPoints = solrDocFactory.determinePermissionsDocsOnSelfOnly(dvObject);

        try {
            persistToSolr(definitionPoints);
            dvObjectService.updatePermissionIndexTime(dvObject.getId());
            
            return new IndexResponse("attempted to index permissions for DvObject " + dvObject.getId() + " and update permission index time was sucessfull");
        } catch (SolrServerException | IOException ex) {
            return new IndexResponse("problem indexing");
        }

    }

    public IndexResponse indexAllPermissions() {

        List<PermissionsSolrDoc> definitionPoints = solrDocFactory.determinePermissionsDocsOnAll();
        Set<Long> affectedDvObjectIds = collectDvObjectIds(definitionPoints);

        try {
            persistToSolr(definitionPoints);
            
            for (Long dvObjectId : affectedDvObjectIds) {
                dvObjectService.updatePermissionIndexTime(dvObjectId);
            }
            return new IndexResponse("indexed all permissions");
        } catch (SolrServerException | IOException ex) {
            return new IndexResponse("problem indexing");
        }

    }

    private void persistToSolr(Collection<PermissionsSolrDoc> permissionDocs) throws SolrServerException, IOException {
        if (permissionDocs.isEmpty()) {
            // This method is routinely called with an empty list of docs.
            logger.fine("nothing to persist");
            return;
        }
        logger.fine("persisting to Solr...");
        List<SolrInputDocument> inputDocs = new ArrayList<>();
        permissionDocs.forEach(permissionDoc -> inputDocs.add(SearchUtil.createSolrDoc(permissionDoc)));
        /**
         * @todo Do something with these responses from Solr.
         */
        UpdateResponse addResponse = solrServer.add(inputDocs);
        UpdateResponse commitResponse = solrServer.commit();
    }

    public IndexResponse deleteMultipleSolrIds(List<String> solrIdsToDelete) {
        if (solrIdsToDelete.isEmpty()) {
            return new IndexResponse("nothing to delete");
        }
        try {
            solrServer.deleteById(solrIdsToDelete);
        } catch (SolrServerException | IOException ex) {
            /**
             * @todo mark these for re-deletion
             */
            return new IndexResponse("problem deleting the following documents from Solr: " + solrIdsToDelete);
        }
        try {
            solrServer.commit();
        } catch (SolrServerException | IOException ex) {
            return new IndexResponse("problem committing deletion of the following documents from Solr: " + solrIdsToDelete);
        }
        return new IndexResponse("no known problem deleting the following documents from Solr:" + solrIdsToDelete);
    }

    public JsonObjectBuilder deleteAllFromSolrAndResetIndexTimes() throws SolrServerException, IOException {
        JsonObjectBuilder response = Json.createObjectBuilder();
        logger.info("attempting to delete all Solr documents before a complete re-index");
        solrServer.deleteByQuery("*:*");
        solrServer.commit();
        int numRowsAffected = dvObjectService.clearAllIndexTimes();
        response.add(numRowsClearedByClearAllIndexTimes, numRowsAffected);
        response.add(messageString, "Solr index and database index timestamps cleared.");
        return response;
    }

    /**
     * @return A list of dvobject ids that should have their permissions
     * re-indexed Solr was down when a permission was added. The permission
     * should be added to Solr.
     * @todo Do we want to report the root dataverse (id 1, often) in
     * permissionsInDatabaseButMissingFromSolr?
     */
    public List<Long> findPermissionsInDatabaseButStaleInOrMissingFromSolr() {
        List<Long> indexingRequired = new ArrayList<>();
        long rootDvId = dataverseDao.findRootDataverse().getId();
        for (DvObject dvObject : dvObjectService.findAll()) {
//            logger.info("examining dvObjectId " + dvObject.getId() + "...");
            Timestamp permissionModificationTime = dvObject.getPermissionModificationTime();
            Timestamp permissionIndexTime = dvObject.getPermissionIndexTime();
            if (permissionIndexTime == null) {
                if (dvObject.getId() != rootDvId) {
                    // we don't index the rootDv
                    indexingRequired.add(dvObject.getId());
                }
            } else if (permissionModificationTime == null) {
                /**
                 * @todo What should we do here? Permissions should always be
                 * there. They are assigned at create time.
                 */
                logger.info("no permission modification time for dvobject id " + dvObject.getId());
            } else if (permissionIndexTime.before(permissionModificationTime)) {
                indexingRequired.add(dvObject.getId());
            }
        }
        return indexingRequired;
    }

    /**
     * @return A list of dvobject ids that should have their permissions
     * re-indexed because Solr was down when a permission was revoked. The
     * permission should be removed from Solr.
     */
    public List<Long> findPermissionsInSolrNoLongerInDatabase() {
        /**
         * @todo Implement this!
         */
        return new ArrayList<>();
    }
}
