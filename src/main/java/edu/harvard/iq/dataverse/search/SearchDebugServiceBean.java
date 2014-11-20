package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.DataFile;
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
     * @todo Since we're starting to call this method from the new
     * SolrIndexServiceBean, this method should be moved to that bean or
     * somewhere else appropriate.
     */
    public List<DvObjectSolrDoc> determineSolrDocs(Long dvObjectId) {
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
            List<DvObjectSolrDoc> fileSolrDocs = constructDatafileSolrDocs((DataFile) dvObject);
            solrDocs.addAll(fileSolrDocs);
        } else {
            logger.info("Unexpected DvObject: " + dvObject.getClass().getName());
        }
        return solrDocs;
    }

    /**
     * @todo should this method return a List? The equivalent methods for
     * datasets and files return lists.
     */
    private DvObjectSolrDoc constructDataverseSolrDoc(Dataverse dataverse) {
        List<String> perms = searchPermissionsService.findDataversePerms(dataverse);
        DvObjectSolrDoc dvDoc = new DvObjectSolrDoc(IndexServiceBean.solrDocIdentifierDataverse + dataverse.getId(), dataverse.getName(), perms);
        return dvDoc;
    }

    private List<DvObjectSolrDoc> constructDatasetSolrDocs(Dataset dataset) {
        List<DvObjectSolrDoc> emptyList = new ArrayList<>();
        List<DvObjectSolrDoc> solrDocs = emptyList;
        Map<DatasetVersion.VersionState, Boolean> desiredCards = searchPermissionsService.getDesiredCards(dataset);
        for (DatasetVersion version : datasetVersionsToBuildCardsFor(dataset)) {
            boolean cardShouldExist = desiredCards.get(version.getVersionState());
            if (cardShouldExist) {
                DvObjectSolrDoc datasetSolrDoc = makeDatasetSolrDoc(version);
                solrDocs.add(datasetSolrDoc);
            }
        }
        return solrDocs;
    }

    /**
     * @todo In this method should we really piggyback off the output of
     * constructDatasetSolrDocs like this? It was the easiest thing to get
     * working quickly.
     */
    private List<DvObjectSolrDoc> constructDatafileSolrDocs(DataFile dataFile) {
        List<DvObjectSolrDoc> datafileSolrDocs = new ArrayList<>();
        List<DvObjectSolrDoc> datasetSolrDocs = constructDatasetSolrDocs(dataFile.getOwner());
        for (DvObjectSolrDoc dataset : datasetSolrDocs) {
            logger.info(dataset.toString());
            String datasetSolrId = dataset.getSolrId();
            /**
             * @todo We should probably get away from the assumption that
             * endings always end with underscore such as "_draft".
             */
            String indicatorOfPublishedSolrId = ".*_[0-9]+$";
            String ending = "";
            if (!datasetSolrId.matches(indicatorOfPublishedSolrId)) {
                ending = datasetSolrId.substring(datasetSolrId.lastIndexOf('_'));
            }
            String fileSolrId = IndexServiceBean.solrDocIdentifierFile + dataFile.getId() + ending;
            /**
             * @todo We should show the filename for this version of the file.
             * Also, go look at all the complicated logic about
             * filenameCompleteFinal in IndexServiceBean!
             */
            String name = dataFile.getDisplayName();
            DvObjectSolrDoc dataFileSolrDoc = new DvObjectSolrDoc(fileSolrId, name, dataset.getPermissions());
            datafileSolrDocs.add(dataFileSolrDoc);
        }
        return datafileSolrDocs;
    }

    private List<DatasetVersion> datasetVersionsToBuildCardsFor(Dataset dataset) {
        List<DatasetVersion> datasetVersions = new ArrayList<>();
        DatasetVersion latest = dataset.getLatestVersion();
        if (latest != null) {
            datasetVersions.add(latest);
        }
        DatasetVersion released = dataset.getReleasedVersion();
        if (released != null) {
            datasetVersions.add(released);
        }
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
