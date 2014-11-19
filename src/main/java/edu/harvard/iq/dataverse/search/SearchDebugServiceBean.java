package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.IndexServiceBean;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;

@Stateless
@Named
public class SearchDebugServiceBean {

    private static final Logger logger = Logger.getLogger(SearchDebugServiceBean.class.getCanonicalName());

    @EJB
    SearchPermissionsServiceBean searchPermissionsService;
    @EJB
    DvObjectServiceBean dvObjectService;

    /**
     * Method used by Search API for showing the document(s) (and their
     * permissions) we'd like to index into Solr based on the dvObjectId.
     */
    public List determineSolrDocs(Long dvObjectId) {
        List<DvObjectSolrDoc> emptyList = new ArrayList<>();
        List<DvObjectSolrDoc> solrDocs = emptyList;
        DvObject dvObject = dvObjectService.findDvObject(dvObjectId);
        if (dvObject == null) {
            return emptyList;
        }
        if (dvObject.isInstanceofDataverse()) {
            DvObjectSolrDoc dataverseSolrDoc = constructDataverseSolrDoc((Dataverse) dvObject);
            solrDocs.add(dataverseSolrDoc);
        } else if (dvObject.isInstanceofDataset()) {
            List<DvObjectSolrDoc> datasetSolrDocs = constructDatasetSolrDocs((Dataset) dvObject);
            solrDocs.addAll(datasetSolrDocs);
        } else if (dvObject.isInstanceofDataFile()) {
            /**
             * @todo Show permissions for a file.
             */
        } else {
            logger.info("Unexpected DvObject: " + dvObject.getClass().getName());
        }
        return solrDocs;
    }

    private DvObjectSolrDoc constructDataverseSolrDoc(Dataverse dataverse) {
        List<String> perms = searchPermissionsService.findDataversePerms(dataverse);
        DvObjectSolrDoc dvDoc = new DvObjectSolrDoc(IndexServiceBean.solrDocIdentifierDataverse + dataverse.getId(), dataverse.getName(), perms);
        return dvDoc;
    }

    private List constructDatasetSolrDocs(Dataset dataset) {
        List<DvObjectSolrDoc> emptyList = new ArrayList<>();
        List<DvObjectSolrDoc> solrDocs = emptyList;
        Map<DatasetVersion.VersionState, Boolean> desiredCards = searchPermissionsService.getDesiredCards(dataset);
        for (DatasetVersion version : datasetVersionsWeBuildCardsFor(dataset)) {
            if (version != null) {
                boolean cardShouldExist = desiredCards.get(version.getVersionState());
                if (cardShouldExist) {
                    DvObjectSolrDoc datasetSolrDoc = makeDatasetSolrDoc(version);
                    solrDocs.add(datasetSolrDoc);
                }
            }
        }
        return solrDocs;
    }

    private List<DatasetVersion> datasetVersionsWeBuildCardsFor(Dataset dataset) {
        List<DatasetVersion> datasetVersions = new ArrayList<>();
        DatasetVersion latest = dataset.getLatestVersion();
        DatasetVersion released = dataset.getReleasedVersion();
        datasetVersions.add(latest);
        datasetVersions.add(released);
        logger.info("versions: " + datasetVersions.toString());
        return datasetVersions;
    }

    private DvObjectSolrDoc makeDatasetSolrDoc(DatasetVersion version) {
        String solrIdStart = IndexServiceBean.solrDocIdentifierDataset + version.getDataset().getId().toString();
        String solrIdEnd = getDatasetSolrIdEnding(version.getVersionState());
        String solrId = solrIdStart + solrIdEnd;
        String name = version.getTitle();
        List<String> perms = searchPermissionsService.findDatasetVersionPerms(version);
        return new DvObjectSolrDoc(solrId, name, perms);
    }

    private String getDatasetSolrIdEnding(DatasetVersion.VersionState versionState) {
        if (versionState.equals(DatasetVersion.VersionState.RELEASED)) {
            return "";
        } else if (versionState.equals(DatasetVersion.VersionState.DRAFT)) {
            return IndexServiceBean.draftSuffix;
        } else if (versionState.equals(DatasetVersion.VersionState.DEACCESSIONED)) {
            return IndexServiceBean.deaccessionedSuffix;
        } else {
            return "_unexpectedDatasetVersion";
        }
    }

}
