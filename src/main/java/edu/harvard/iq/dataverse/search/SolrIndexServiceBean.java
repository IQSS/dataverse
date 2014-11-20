package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.IndexServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;

@Named
@Stateless
public class SolrIndexServiceBean {

    private static final Logger logger = Logger.getLogger(SolrIndexServiceBean.class.getCanonicalName());

    @EJB
    SystemConfig systemConfig;
    @EJB
    DvObjectServiceBean dvObjectServiceBean;
    @EJB
    SearchPermissionsServiceBean searchPermissionsService;
    @EJB
    SearchDebugServiceBean searchDebugService;

    public IndexResponse indexAllPermissions() {
        Collection<SolrInputDocument> docs = new ArrayList<>();

        List<DvObjectSolrDoc> definitionPoints = new ArrayList<>();
        for (DvObject dvObject : dvObjectServiceBean.findAll()) {
            definitionPoints.addAll(searchDebugService.determineSolrDocs(dvObject.getId()));
        }

        for (DvObjectSolrDoc dvObjectSolrDoc : definitionPoints) {
            SolrInputDocument solrInputDocument = createSolrDoc(dvObjectSolrDoc);
            docs.add(solrInputDocument);
        }
        try {
            persistToSolr(docs);
            /**
             * @todo Do we need a separate permissionIndexTime timestamp?
             * Probably. Update it here.
             */
            return new IndexResponse("indexed all permissions");
        } catch (SolrServerException | IOException ex) {
            return new IndexResponse("problem indexing");
        }

    }

    public IndexResponse indexPermissionsForOneDvObject(long dvObjectId) {
        Collection<SolrInputDocument> docs = new ArrayList<>();

        List<DvObjectSolrDoc> definitionPoints = searchDebugService.determineSolrDocs(dvObjectId);

        for (DvObjectSolrDoc dvObjectSolrDoc : definitionPoints) {
            SolrInputDocument solrInputDocument = createSolrDoc(dvObjectSolrDoc);
            docs.add(solrInputDocument);
        }
        try {
            persistToSolr(docs);
            /**
             * @todo Do we need a separate permissionIndexTime timestamp?
             * Probably. Update it here.
             */
            return new IndexResponse("attempted to index permissions for DvObject " + dvObjectId);
        } catch (SolrServerException | IOException ex) {
            return new IndexResponse("problem indexing");
        }

    }

    private SolrInputDocument createSolrDoc(DvObjectSolrDoc dvObjectSolrDoc) {
        SolrInputDocument solrInputDocument = new SolrInputDocument();
        solrInputDocument.addField(SearchFields.ID, dvObjectSolrDoc.getSolrId() + IndexServiceBean.discoverabilityPermissionSuffix);
        solrInputDocument.addField(SearchFields.DEFINITION_POINT, dvObjectSolrDoc.getSolrId());
        solrInputDocument.addField(SearchFields.DISCOVERABLE_BY, dvObjectSolrDoc.getPermissions());
        return solrInputDocument;
    }

    private void persistToSolr(Collection<SolrInputDocument> docs) throws SolrServerException, IOException {
        if (docs.isEmpty()) {
            /**
             * @todo Throw an exception here? "DvObject id 9999 does not exist."
             */
            return;
        }
        SolrServer solrServer = new HttpSolrServer("http://" + systemConfig.getSolrHostColonPort() + "/solr");
        /**
         * @todo Do something with these responses from Solr.
         */
        UpdateResponse addResponse = solrServer.add(docs);
        UpdateResponse commitResponse = solrServer.commit();
    }
}
