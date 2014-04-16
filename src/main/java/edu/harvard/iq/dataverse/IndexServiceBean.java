package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.api.SearchFields;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import edu.harvard.iq.dataverse.util.FileUtil;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;

@Stateless
@Named
public class IndexServiceBean {

    private static final Logger logger = Logger.getLogger(IndexServiceBean.class.getCanonicalName());

    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DataverseUserServiceBean dataverseUserServiceBean;

    private final String solrDocIdentifierDataverse = "dataverse_";
    private static final String npeGetCreator = "creatorNPE";
    private static final String groupPrefix = "group_";
    private static final String groupPerUserPrefix = "group_user";
    private static final Long publicGroupId = 1L;
    private static final String publicGroupString = groupPrefix + "public";
    /**
     * @todo: remove this fake "has access to all data" group
     */
    private static final Long tmpNsaGroupId = 2L;

    public String indexAll() {
        /**
         * @todo allow for configuration of hostname and port
         */
        SolrServer server = new HttpSolrServer("http://localhost:8983/solr/");
        logger.info("deleting all Solr documents before a complete re-index");
        try {
            server.deleteByQuery("*:*");// CAUTION: deletes everything!
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }
        try {
            server.commit();
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }

        List<Dataverse> dataverses = dataverseService.findAll();
        int dataverseIndexCount = 0;
        for (Dataverse dataverse : dataverses) {
            logger.info("indexing dataverse " + dataverseIndexCount + " of " + dataverses.size() + ": " + indexDataverse(dataverse));
            dataverseIndexCount++;
        }

        int datasetIndexCount = 0;
        List<Dataset> datasets = datasetService.findAll();
        for (Dataset dataset : datasets) {
            datasetIndexCount++;
            logger.info("indexing dataset " + datasetIndexCount + " of " + datasets.size() + ": " + indexDataset(dataset));
        }
//        logger.info("advanced search fields: " + advancedSearchFields);
//        logger.info("not advanced search fields: " + notAdvancedSearchFields);
        logger.info("done iterating through all datasets");

        /**
         * @todo: replace hard-coded groups with real groups
         */
        Map<Long, String> groups = new HashMap<>();
        groups.put(publicGroupId, publicGroupString);
        groups.put(tmpNsaGroupId, "nsa");
        groups.put(tmpNsaGroupId + 1, "flappybird");
        groups.put(tmpNsaGroupId + 2, "2048");

        int groupIndexCount = 0;
        for (Map.Entry<Long, String> group : groups.entrySet()) {
            groupIndexCount++;
            logger.info("indexing group " + groupIndexCount + " of " + groups.size() + ": " + indexGroup(group));
        }

        int userIndexCount = 0;
        for (DataverseUser user : dataverseUserServiceBean.findAll()) {
            userIndexCount++;
            logger.info("indexing user " + userIndexCount + " of several: " + indexUser(user));
        }

        return dataverseIndexCount + " dataverses, " + datasetIndexCount + " datasets, " + groupIndexCount + " groups, and " + userIndexCount + " users indexed\n";
    }

    public String indexDataverse(Dataverse dataverse) {
        Dataverse rootDataverse = dataverseService.findRootDataverse();
        if (dataverse.getId() == rootDataverse.getId()) {
            /**
             * @todo: replace hard-coded groups with real groups
             */
            Map<Long, String> groups = new HashMap<>();
            groups.put(publicGroupId, publicGroupString);
            groups.put(tmpNsaGroupId, "nsa");
            groups.put(tmpNsaGroupId + 1, "flappybird");
            groups.put(tmpNsaGroupId + 2, "2048");

            int groupIndexCount = 0;
            for (Map.Entry<Long, String> group : groups.entrySet()) {
                groupIndexCount++;
                logger.info("indexing group " + groupIndexCount + " of " + groups.size() + ": " + indexGroup(group));
            }
            return "The root dataverse shoud not be indexed. Indexed temporary groups instead.";
        }
        Collection<SolrInputDocument> docs = new ArrayList<>();
        SolrInputDocument solrInputDocument = new SolrInputDocument();
        solrInputDocument.addField(SearchFields.ID, solrDocIdentifierDataverse + dataverse.getId());
        solrInputDocument.addField(SearchFields.ENTITY_ID, dataverse.getId());
        solrInputDocument.addField(SearchFields.TYPE, "dataverses");
        solrInputDocument.addField(SearchFields.NAME, dataverse.getName());
        solrInputDocument.addField(SearchFields.NAME_SORT, dataverse.getName());
        if (dataverse.isReleased()) {
            solrInputDocument.addField(SearchFields.RELEASE_OR_CREATE_DATE, dataverse.getPublicationDate());
            solrInputDocument.addField(SearchFields.PERMS, publicGroupString);
        } else if (dataverse.getCreator() != null) {
            solrInputDocument.addField(SearchFields.RELEASE_OR_CREATE_DATE, dataverse.getCreateDate());
            solrInputDocument.addField(SearchFields.PERMS, groupPerUserPrefix + dataverse.getCreator().getId());
            /**
             * @todo: replace this fake version of granting users access to
             * dataverses with the real thing, when it's available in the app
             */
            if (dataverse.getCreator().getUserName().equals("pete")) {
                // figure out if cathy is around
                DataverseUser cathy = dataverseUserServiceBean.findByUserName("cathy");
                if (cathy != null) {
                    // let cathy see all of pete's dataverses
                    solrInputDocument.addField(SearchFields.PERMS, groupPerUserPrefix + cathy.getId());
                }
            }
        } else {
            solrInputDocument.addField(SearchFields.RELEASE_OR_CREATE_DATE, dataverse.getCreateDate());
            /**
             * @todo: remove this once everyone has dropped their database and
             * won't get NPE's from dataverse.getCreator
             */
            solrInputDocument.addField(SearchFields.PERMS, npeGetCreator);
        }

        /**
         * @todo: remove this fake "has access to all data" group
         */
        solrInputDocument.addField(SearchFields.PERMS, groupPrefix + tmpNsaGroupId);

        addDataverseReleaseDateToSolrDoc(solrInputDocument, dataverse);
//        if (dataverse.getOwner() != null) {
//            solrInputDocument.addField(SearchFields.HOST_DATAVERSE, dataverse.getOwner().getName());
//        }
        solrInputDocument.addField(SearchFields.DESCRIPTION, dataverse.getDescription());
//        logger.info("dataverse affiliation: " + dataverse.getAffiliation());
        if (dataverse.getAffiliation() != null && !dataverse.getAffiliation().isEmpty()) {
            /**
             * @todo: stop using affiliation as category
             */
//            solrInputDocument.addField(SearchFields.CATEGORY, dataverse.getAffiliation());
            solrInputDocument.addField(SearchFields.AFFILIATION, dataverse.getAffiliation());
        }
        // checking for NPE is important so we can create the root dataverse
        if (rootDataverse != null && !dataverse.equals(rootDataverse)) {
            // important when creating root dataverse
            if (dataverse.getOwner() != null) {
                solrInputDocument.addField(SearchFields.PARENT_ID, dataverse.getOwner().getId());
                solrInputDocument.addField(SearchFields.PARENT_NAME, dataverse.getOwner().getName());
            }
        }
        List<String> dataversePathSegmentsAccumulator = new ArrayList<>();
        List<String> dataverseSegments = findPathSegments(dataverse, dataversePathSegmentsAccumulator);
        List<String> dataversePaths = getDataversePathsFromSegments(dataverseSegments);
        if (dataversePaths.size() > 0) {
            // don't show yourself while indexing or in search results: https://redmine.hmdc.harvard.edu/issues/3613
//            logger.info(dataverse.getName() + " size " + dataversePaths.size());
            dataversePaths.remove(dataversePaths.size() - 1);
        }
        solrInputDocument.addField(SearchFields.SUBTREE, dataversePaths);
        docs.add(solrInputDocument);

        /**
         * @todo allow for configuration of hostname and port
         */
        SolrServer server = new HttpSolrServer("http://localhost:8983/solr/");

        try {
            if (dataverse.getId() != null) {
                server.add(docs);
            } else {
                logger.info("WARNING: indexing of a dataverse with no id attempted");
            }
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }
        try {
            server.commit();
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }

        return "indexed dataverse " + dataverse.getId() + ":" + dataverse.getAlias();

    }

    public String indexDataset(Dataset dataset) {
        logger.info("indexing dataset " + dataset.getId());
        String solrIdDraftStudy = "dataset_" + dataset.getId() + "_draft";
        String solrIdPublishedStudy = "dataset_" + dataset.getId();
        StringBuilder sb = new StringBuilder();
        sb.append("rationale:\n");
        List<DatasetVersion> versions = dataset.getVersions();
        for (DatasetVersion datasetVersion : versions) {
            Long versionDatabaseId = datasetVersion.getId();
            String versionTitle = datasetVersion.getTitle();
            String semanticVersion = datasetVersion.getSemanticVersion();
            String versionState = datasetVersion.getVersionState().name();
            boolean versionIsReleased = datasetVersion.isReleased();
            boolean versionIsWorkingCopy = datasetVersion.isWorkingCopy();
            sb.append("version found with database id " + versionDatabaseId + "\n");
            sb.append("- title: " + versionTitle + "\n");
            sb.append("- semanticVersion-STATE: " + semanticVersion + "-" + versionState + "\n");
            sb.append("- isWorkingCopy: " + versionIsWorkingCopy + "\n");
            sb.append("- isReleased: " + versionIsReleased + "\n");
        }
        DatasetVersion latestVersion = dataset.getLatestVersion();
        String latestVersionState = latestVersion.getVersionState().name();
        DatasetVersion releasedVersion = dataset.getReleasedVersion();
        if (latestVersion.isWorkingCopy()) {
            sb.append("The latest version is a working copy (latestVersionState: " + latestVersionState + ") and will be indexed as " + solrIdDraftStudy + " (only visible by creator)\n");
            if (releasedVersion != null) {
                String releasedVersionState = releasedVersion.getVersionState().name();
                String semanticVersion = releasedVersion.getSemanticVersion();
                sb.append("The released version is " + semanticVersion + " (releasedVersionState: " + releasedVersionState + ") and will be indexed as " + solrIdPublishedStudy + " (visible by anonymous)");
                /**
                 * The latest version is a working copy (latestVersionState:
                 * DRAFT) and will be indexed as dataset_17_draft (only visible
                 * by creator)
                 *
                 * The released version is 1.0 (releasedVersionState: RELEASED)
                 * and will be indexed as dataset_17 (visible by anonymous)
                 */
                logger.info(sb.toString());
                String indexDraftResult = indexDatasetAddOrUpdate(dataset);
                String indexReleasedVersionResult = indexDatasetAddOrUpdate(dataset);
                return "indexDraftResult:" + indexDraftResult + ", indexReleasedVersionResult:" + indexReleasedVersionResult + ", " + sb.toString();
            } else {
                sb.append("There is no released version yet so nothing will be indexed as " + solrIdPublishedStudy);
                /**
                 * The latest version is a working copy (latestVersionState:
                 * DRAFT) and will be indexed as dataset_33_draft (only visible
                 * by creator)
                 *
                 * There is no released version yet so nothing will be indexed
                 * as dataset_33
                 */
                logger.info(sb.toString());
                String indexDraftResult = indexDatasetAddOrUpdate(dataset);
                return "indexDraftResult:" + indexDraftResult + ", " + sb.toString();
            }
        } else {
            sb.append("The latest version is not a working copy (latestVersionState: " + latestVersionState + ") and will be indexed as " + solrIdPublishedStudy + " (visible by anonymous) and we will be deleting " + solrIdDraftStudy + "\n");
            if (releasedVersion != null) {
                String releasedVersionState = releasedVersion.getVersionState().name();
                String semanticVersion = releasedVersion.getSemanticVersion();
                sb.append("The released version is " + semanticVersion + " (releasedVersionState: " + releasedVersionState + ") and will be (again) indexed as " + solrIdPublishedStudy + " (visible by anonymous)");
                /**
                 * The latest version is not a working copy (latestVersionState:
                 * RELEASED) and will be indexed as dataset_34 (visible by
                 * anonymous) and we will be deleting dataset_34_draft
                 *
                 * The released version is 1.0 (releasedVersionState: RELEASED)
                 * and will be  (again) indexed as dataset_34 (visible by anonymous)
                 */
                logger.info(sb.toString());
                String deleteDraftVersionResult = removeDatasetDraftFromIndex(solrIdDraftStudy);
                String indexReleasedVersionResult = indexDatasetAddOrUpdate(dataset);
                return "deleteDraftVersionResult: " + deleteDraftVersionResult + ", indexReleasedVersionResult:" + indexReleasedVersionResult + ", " + sb.toString();
            } else {
                sb.append("We don't ever expect to ever get here. Why is there no released version if the latest version is not a working copy? The latestVersionState is " + latestVersionState + " and we don't know what to do with it. Nothing will be added or deleted from the index.");
                logger.info(sb.toString());
                return sb.toString();
            }
        }
    }

    public String indexDatasetAddOrUpdate(Dataset dataset) {
        logger.info("adding or updating Solr document for dataset id " + dataset.getId());
        Collection<SolrInputDocument> docs = new ArrayList<>();
        List<String> dataversePathSegmentsAccumulator = new ArrayList<>();
        List<String> dataverseSegments = new ArrayList<>();
        try {
            dataverseSegments = findPathSegments(dataset.getOwner(), dataversePathSegmentsAccumulator);
        } catch (Exception ex) {
            logger.info("failed to find dataverseSegments for dataversePaths for " + SearchFields.SUBTREE + ": " + ex);
        }
        List<String> dataversePaths = getDataversePathsFromSegments(dataverseSegments);
        SolrInputDocument solrInputDocument = new SolrInputDocument();
        solrInputDocument.addField(SearchFields.ID, "dataset_" + dataset.getId());
        solrInputDocument.addField(SearchFields.ENTITY_ID, dataset.getId());
        solrInputDocument.addField(SearchFields.TYPE, "datasets");
        if (dataset.isReleased()) {
            solrInputDocument.addField(SearchFields.RELEASE_OR_CREATE_DATE, dataset.getPublicationDate());
            solrInputDocument.addField(SearchFields.PERMS, publicGroupString);
        } else if (dataset.getOwner().getCreator() != null) {
            /**
             * todo why is dataset.getCreateDate() null? For now I guess we'll
             * use the createDate of it's parent dataverse?! https://redmine.hmdc.harvard.edu/issues/3806
             */
//            solrInputDocument.addField(SearchFields.RELEASE_OR_CREATE_DATE, dataset.getCreateDate());
            solrInputDocument.addField(SearchFields.RELEASE_OR_CREATE_DATE, dataset.getOwner().getCreateDate());
            solrInputDocument.addField(SearchFields.PERMS, groupPerUserPrefix + dataset.getOwner().getCreator().getId());
            /**
             * @todo: replace this fake version of granting users access to
             * dataverses with the real thing, when it's available in the app
             */
            if (dataset.getOwner().getCreator().getUserName().equals("pete")) {
                // figure out if cathy is around
                DataverseUser cathy = dataverseUserServiceBean.findByUserName("cathy");
                if (cathy != null) {
                    // let cathy see all of pete's dataverses
                    solrInputDocument.addField(SearchFields.PERMS, groupPerUserPrefix + cathy.getId());
                }
            }
        } else {
            /**
             * todo why is dataset.getCreateDate() null? For now I guess we'll
             * use the createDate of it's parent dataverse?! https://redmine.hmdc.harvard.edu/issues/3806
             */
//            solrInputDocument.addField(SearchFields.RELEASE_OR_CREATE_DATE, dataset.getCreateDate());
            solrInputDocument.addField(SearchFields.RELEASE_OR_CREATE_DATE, dataset.getOwner().getCreateDate());
            /**
             * @todo: remove this once everyone has dropped their database and
             * won't get NPE's from dataverse.getCreator
             */
            solrInputDocument.addField(SearchFields.PERMS, npeGetCreator);
        }

        /**
         * @todo: remove this fake "has access to all data" group
         */
        solrInputDocument.addField(SearchFields.PERMS, groupPrefix + tmpNsaGroupId);

        addDatasetReleaseDateToSolrDoc(solrInputDocument, dataset);

        if (dataset.getLatestVersion() != null) {

            DatasetVersionUI datasetVersionUI = null;
            try {
                datasetVersionUI = new DatasetVersionUI(dataset.getLatestVersion());
            } catch (NullPointerException ex) {
                logger.info("Caught exception trying to instantiate DatasetVersionUI for dataset " + dataset.getId() + ". : " + ex);
            }
            if (datasetVersionUI != null) {
                String citation = null;
                try {
                    citation = datasetVersionUI.getCitation();
                    if (citation != null) {
                        solrInputDocument.addField(SearchFields.CITATION, citation);
                    }

                } catch (NullPointerException ex) {
                    logger.info("Caught exception trying to get citation for dataset " + dataset.getId() + ". : " + ex);
                }
            }

            for (DatasetField dsf : dataset.getLatestVersion().getFlatDatasetFields()) {

                DatasetFieldType dsfType = dsf.getDatasetFieldType();
                String solrFieldSearchable = dsfType.getSolrField().getNameSearchable();
                String solrFieldFacetable = dsfType.getSolrField().getNameFacetable();

                if (dsf.getValues() != null && !dsf.getValues().isEmpty() && dsf.getValues().get(0) != null && solrFieldSearchable != null) {
                    logger.info("indexing " + dsf.getDatasetFieldType().getName() + ":" + dsf.getValues() + " into " + solrFieldSearchable + " and maybe " + solrFieldFacetable);
                    if (dsfType.getSolrField().getSolrType().equals(SolrField.SolrType.INTEGER)) {
                        String dateAsString = dsf.getValues().get(0);
                        logger.info("date as string: " + dateAsString);
                        if (dateAsString != null && !dateAsString.isEmpty()) {
                            SimpleDateFormat inputDateyyyy = new SimpleDateFormat("yyyy", Locale.ENGLISH);
                            try {
                                /**
                                 * @todo when bean validation is working we
                                 * won't have to convert strings into dates
                                 */
                                logger.info("Trying to convert " + dateAsString + " to a YYYY date from dataset " + dataset.getId());
                                Date dateAsDate = inputDateyyyy.parse(dateAsString);
                                SimpleDateFormat yearOnly = new SimpleDateFormat("yyyy");
                                String datasetFieldFlaggedAsDate = yearOnly.format(dateAsDate);
                                logger.info("YYYY only: " + datasetFieldFlaggedAsDate);
                                solrInputDocument.addField(solrFieldSearchable, Integer.parseInt(datasetFieldFlaggedAsDate));
                                if (dsfType.getSolrField().isFacetable()) {
                                    solrInputDocument.addField(solrFieldFacetable, Integer.parseInt(datasetFieldFlaggedAsDate));
                                }
                            } catch (Exception ex) {
                                logger.info("unable to convert " + dateAsString + " into YYYY format and couldn't index it (" + dsfType.getName() + ")");
                            }
                        }
                    } else {
                        // _s (dynamic string) and all other Solr fields

                        if (dsf.getDatasetFieldType().getName().equals("authorAffiliation")) {
                            /**
                             * @todo think about how to tie the fact that this
                             * needs to be multivalued (_ss) because a
                             * multivalued facet (authorAffilition_ss) is being
                             * collapsed into here at index time. The business
                             * logic to determine if a data-driven metadata
                             * field should be indexed into Solr as a single or
                             * multiple value lives in the getSolrField() method
                             * of DatasetField.java
                             */
                            solrInputDocument.addField(SearchFields.AFFILIATION, dsf.getValues());
                        } else if (dsf.getDatasetFieldType().getName().equals("title")) {
                            // datasets have titles not names but index title under name as well so we can sort datasets by name along dataverses and files
                            solrInputDocument.addField(SearchFields.NAME_SORT, dsf.getValues());
                        }
                        if (dsfType.isControlledVocabulary()) {
                            for (ControlledVocabularyValue controlledVocabularyValue : dsf.getControlledVocabularyValues()) {
                                solrInputDocument.addField(solrFieldSearchable, controlledVocabularyValue.getStrValue());
                                if (dsfType.getSolrField().isFacetable()) {
                                    solrInputDocument.addField(solrFieldFacetable, controlledVocabularyValue.getStrValue());
                                }
                            }
                        } else {
                            solrInputDocument.addField(solrFieldSearchable, dsf.getValues());
                            if (dsfType.getSolrField().isFacetable()) {
                                solrInputDocument.addField(solrFieldFacetable, dsf.getValues());
                            }
                        }
                    }
                }
                /**
                 * @todo: review all code below... commented out old indexing of
                 * hard coded fields. Also, should we respect the
                 * isAdvancedSearchField boolean?
                 */
//                if (datasetField.isAdvancedSearchField()) {
//                    advancedSearchFields.add(idDashName);
//                    logger.info(idDashName + " is an advanced search field (" + title + ")");
//                    if (name.equals(DatasetFieldConstant.title)) {
//                        String toIndexTitle = datasetFieldValue.getStrValue();
//                        if (toIndexTitle != null && !toIndexTitle.isEmpty()) {
//                            solrInputDocument.addField(SearchFields.TITLE, toIndexTitle);
//                        }
//                    } else if (name.equals(DatasetFieldConstant.authorName)) {
//                        String toIndexAuthor = datasetFieldValue.getStrValue();
//                        if (toIndexAuthor != null && !toIndexAuthor.isEmpty()) {
//                            logger.info("index this author: " + toIndexAuthor);
//                            solrInputDocument.addField(SearchFields.AUTHOR_STRING, toIndexAuthor);
//                        }
//                    } else if (name.equals(DatasetFieldConstant.productionDate)) {
//                        String toIndexProductionDateString = datasetFieldValue.getStrValue();
//                        logger.info("production date: " + toIndexProductionDateString);
//                        if (toIndexProductionDateString != null && !toIndexProductionDateString.isEmpty()) {
//                            SimpleDateFormat inputDateyyyy = new SimpleDateFormat("yyyy", Locale.ENGLISH);
//                            try {
//                                logger.info("Trying to convert " + toIndexProductionDateString + " to a YYYY date from dataset " + dataset.getId());
//                                Date productionDate = inputDateyyyy.parse(toIndexProductionDateString);
//                                SimpleDateFormat yearOnly = new SimpleDateFormat("yyyy");
//                                String productionYear = yearOnly.format(productionDate);
//                                logger.info("YYYY only: " + productionYear);
//                                solrInputDocument.addField(SearchFields.PRODUCTION_DATE_YEAR_ONLY, Integer.parseInt(productionYear));
//                                solrInputDocument.addField(SearchFields.PRODUCTION_DATE_ORIGINAL, productionDate);
//                            } catch (Exception ex) {
//                                logger.info("unable to convert " + toIndexProductionDateString + " into YYYY format");
//                            }
//                        }
//                        /**
//                         * @todo: DRY! this is the same as above!
//                         */
//                    } else if (name.equals(DatasetFieldConstant.distributionDate)) {
//                        String toIndexdistributionDateString = datasetFieldValue.getStrValue();
//                        logger.info("distribution date: " + toIndexdistributionDateString);
//                        if (toIndexdistributionDateString != null && !toIndexdistributionDateString.isEmpty()) {
//                            SimpleDateFormat inputDateyyyy = new SimpleDateFormat("yyyy", Locale.ENGLISH);
//                            try {
//                                logger.info("Trying to convert " + toIndexdistributionDateString + " to a YYYY date from dataset " + dataset.getId());
//                                Date distributionDate = inputDateyyyy.parse(toIndexdistributionDateString);
//                                SimpleDateFormat yearOnly = new SimpleDateFormat("yyyy");
//                                String distributionYear = yearOnly.format(distributionDate);
//                                logger.info("YYYY only: " + distributionYear);
//                                solrInputDocument.addField(SearchFields.DISTRIBUTION_DATE_YEAR_ONLY, Integer.parseInt(distributionYear));
//                                solrInputDocument.addField(SearchFields.DISTRIBUTION_DATE_ORIGINAL, distributionDate);
//                            } catch (Exception ex) {
//                                logger.info("unable to convert " + toIndexdistributionDateString + " into YYYY format");
//                            }
//                        }
//                    } else if (name.equals(DatasetFieldConstant.keywordValue)) {
//                        String toIndexKeyword = datasetFieldValue.getStrValue();
//                        if (toIndexKeyword != null && !toIndexKeyword.isEmpty()) {
//                            solrInputDocument.addField(SearchFields.KEYWORD, toIndexKeyword);
//                        }
//                    } else if (name.equals(DatasetFieldConstant.distributorName)) {
//                        String toIndexDistributor = datasetFieldValue.getStrValue();
//                        if (toIndexDistributor != null && !toIndexDistributor.isEmpty()) {
//                            solrInputDocument.addField(SearchFields.DISTRIBUTOR, toIndexDistributor);
//                        }
//                    } else if (name.equals(DatasetFieldConstant.description)) {
//                        String toIndexDescription = datasetFieldValue.getStrValue();
//                        if (toIndexDescription != null && !toIndexDescription.isEmpty()) {
//                            solrInputDocument.addField(SearchFields.DESCRIPTION, toIndexDescription);
//                        }
//                    }
//                } else {
//                    notAdvancedSearchFields.add(idDashName);
//                    logger.info(idDashName + " is not an advanced search field (" + title + ")");
//                }
            }
        }

        solrInputDocument.addField(SearchFields.SUBTREE, dataversePaths);
//        solrInputDocument.addField(SearchFields.HOST_DATAVERSE, dataset.getOwner().getName());
        solrInputDocument.addField(SearchFields.PARENT_ID, dataset.getOwner().getId());
        solrInputDocument.addField(SearchFields.PARENT_NAME, dataset.getOwner().getName());

        docs.add(solrInputDocument);

        List<DataFile> files = dataset.getFiles();
        for (DataFile dataFile : files) {
            SolrInputDocument datafileSolrInputDocument = new SolrInputDocument();
            datafileSolrInputDocument.addField(SearchFields.ID, "datafile_" + dataFile.getId());
            datafileSolrInputDocument.addField(SearchFields.ENTITY_ID, dataFile.getId());
            datafileSolrInputDocument.addField(SearchFields.TYPE, "files");
            datafileSolrInputDocument.addField(SearchFields.NAME, dataFile.getName());
            datafileSolrInputDocument.addField(SearchFields.NAME_SORT, dataFile.getName());
            if (dataset.isReleased()) {
                /**
                 * @todo: are datafiles supposed to have release dates? It's
                 * null. For now just set something: https://redmine.hmdc.harvard.edu/issues/3806
                 */
//                datafileSolrInputDocument.addField(SearchFields.RELEASE_OR_CREATE_DATE, dataFile.getReleaseDate());
                datafileSolrInputDocument.addField(SearchFields.RELEASE_OR_CREATE_DATE, dataFile.getOwner().getOwner().getCreateDate());
                datafileSolrInputDocument.addField(SearchFields.PERMS, publicGroupString);
            } else if (dataset.getOwner().getCreator() != null) {
                /**
                 * todo why is dataFile.getCreateDate() null? For now I guess
                 * we'll use the createDate of its parent datase's dataverset?! https://redmine.hmdc.harvard.edu/issues/3806
                 */
//                datafileSolrInputDocument.addField(SearchFields.RELEASE_OR_CREATE_DATE, dataFile.getCreateDate());
                datafileSolrInputDocument.addField(SearchFields.RELEASE_OR_CREATE_DATE, dataFile.getOwner().getOwner().getCreateDate());
                datafileSolrInputDocument.addField(SearchFields.PERMS, groupPerUserPrefix + dataset.getOwner().getCreator().getId());
                /**
                 * @todo: replace this fake version of granting users access to
                 * dataverses with the real thing, when it's available in the
                 * app
                 */
                if (dataset.getOwner().getCreator().getUserName().equals("pete")) {
                    // figure out if cathy is around
                    DataverseUser cathy = dataverseUserServiceBean.findByUserName("cathy");
                    if (cathy != null) {
                        // let cathy see all of pete's dataverses
                        datafileSolrInputDocument.addField(SearchFields.PERMS, groupPerUserPrefix + cathy.getId());
                    }
                }
            } else {
                /**
                 * @todo: remove this once everyone has dropped their database
                 * and won't get NPE's from dataverse.getCreator
                 */
                /**
                 * todo why is dataFile.getCreateDate() null? For now I guess
                 * we'll use the createDate of its parent dataset's dataverse?! https://redmine.hmdc.harvard.edu/issues/3806
                 */
//                datafileSolrInputDocument.addField(SearchFields.RELEASE_OR_CREATE_DATE, dataFile.getCreateDate());
                datafileSolrInputDocument.addField(SearchFields.RELEASE_OR_CREATE_DATE, dataFile.getOwner().getOwner().getCreateDate());
                datafileSolrInputDocument.addField(SearchFields.PERMS, npeGetCreator);
            }

            /**
             * @todo: remove this fake "has access to all data" group
             */
            datafileSolrInputDocument.addField(SearchFields.PERMS, groupPrefix + tmpNsaGroupId);

            // For the mime type, we are going to index the "friendly" version, e.g., 
            // "PDF File" instead of "application/pdf", "MS Excel" instead of 
            // "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" (!), etc., 
            // if available:
            datafileSolrInputDocument.addField(SearchFields.FILE_TYPE_MIME, dataFile.getFriendlyType());
            // For the file type facets, we have a property file that maps mime types 
            // to facet-friendly names; "application/fits" should become "FITS", etc.:
            datafileSolrInputDocument.addField(SearchFields.FILE_TYPE, FileUtil.getFacetFileType(dataFile));
            datafileSolrInputDocument.addField(SearchFields.DESCRIPTION, dataFile.getDescription());
            datafileSolrInputDocument.addField(SearchFields.SUBTREE, dataversePaths);
//            datafileSolrInputDocument.addField(SearchFields.HOST_DATAVERSE, dataFile.getOwner().getOwner().getName());
           // datafileSolrInputDocument.addField(SearchFields.PARENT_NAME, dataFile.getDataset().getTitle());
            datafileSolrInputDocument.addField(SearchFields.PARENT_ID, dataFile.getOwner().getId());
            if (!dataFile.getOwner().getLatestVersion().getTitle().isEmpty()) {
                datafileSolrInputDocument.addField(SearchFields.PARENT_NAME, dataFile.getOwner().getLatestVersion().getTitle());
            }
            
            // If this is a tabular data file -- i.e., if there are data
            // variables associated with this file, we index the variable 
            // names and labels: 
            
            if (dataFile.isTabularData()) {
                List<DataVariable> variables = dataFile.getDataTable().getDataVariables();
                String variableNamesToIndex = null;
                String variableLabelsToIndex = null; 
                for (DataVariable var : variables) {
                    // Hard-coded search fields, for now: 
                    // TODO: immediately: define these as constants in SearchFields;
                    // TODO: eventually: review, decide how datavariables should
                    // be handled for indexing purposes. (should it be a fixed
                    // setup, defined in the code? should it be flexible? unlikely
                    // that this needs to be domain-specific... since these data
                    // variables are quite specific to tabular data, which in turn
                    // is something social science-specific...
                    // anyway -- needs to be reviewed. -- L.A. 4.0alpha1 
                    
                    if (var.getName() != null && !var.getName().equals("")) {
                        if (variableNamesToIndex == null) {
                            variableNamesToIndex = var.getName();
                        } else {
                            variableNamesToIndex = variableNamesToIndex + " " + var.getName();
                        }
                    }
                    if (var.getLabel() != null && !var.getLabel().equals("")) {
                        if (variableLabelsToIndex == null) {
                            variableLabelsToIndex = var.getLabel();
                        } else {
                            variableLabelsToIndex = variableLabelsToIndex + " " + var.getLabel();
                        }
                    }
                }
                if (variableNamesToIndex != null) {
                    logger.info("indexing  " + variableNamesToIndex.length() + " bytes");
                    datafileSolrInputDocument.addField("varname_s", variableNamesToIndex);
                }
                if (variableLabelsToIndex != null) {
                    logger.info("indexing  " + variableLabelsToIndex.length() + " bytes");
                    datafileSolrInputDocument.addField("varlabel_s", variableLabelsToIndex);
                }
            }
            
            // And if the file has indexable file-level metadata associated
            // with it, we'll index that too:
            
            List<FileMetadataFieldValue> fileMetadataFieldValues = dataFile.getFileMetadataFieldValues();
            if (fileMetadataFieldValues != null && fileMetadataFieldValues.size() > 0) {
                for (int j = 0; j < fileMetadataFieldValues.size(); j++) {

                    String fieldValue = fileMetadataFieldValues.get(j).getStrValue();

                    FileMetadataField fmf = fileMetadataFieldValues.get(j).getFileMetadataField();
                    String fileMetadataFieldName = fmf.getName();
                    String fileMetadataFieldFormatName = fmf.getFileFormatName();
                    String fieldName = fileMetadataFieldFormatName + "-" + fileMetadataFieldName  + "_s";

                    datafileSolrInputDocument.addField(fieldName, fieldValue);

                }
            }
            
            docs.add(datafileSolrInputDocument);
        }

        /**
         * @todo allow for configuration of hostname and port
         */
        SolrServer server = new HttpSolrServer("http://localhost:8983/solr/");

        try {
            server.add(docs);
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }
        try {
            server.commit();
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }

        return "indexed dataset " + dataset.getId(); // + ":" + dataset.getTitle();
    }

    public String indexGroup(Map.Entry<Long, String> group) {

        Collection<SolrInputDocument> docs = new ArrayList<>();
        SolrInputDocument solrInputDocument = new SolrInputDocument();

        String id = groupPrefix + group.getKey();

        solrInputDocument.addField(SearchFields.TYPE, "groups");
        solrInputDocument.addField(SearchFields.ID, id);
        solrInputDocument.addField(SearchFields.ENTITY_ID, group.getKey());
        solrInputDocument.addField(SearchFields.NAME_SORT, group.getValue());
        solrInputDocument.addField(SearchFields.GROUPS, id);

        docs.add(solrInputDocument);
        /**
         * @todo allow for configuration of hostname and port
         */
        SolrServer server = new HttpSolrServer("http://localhost:8983/solr/");

        try {
            server.add(docs);
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }
        try {
            server.commit();
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }

        return "indexed group " + group;
    }

    public String indexUser(DataverseUser user) {

        Collection<SolrInputDocument> docs = new ArrayList<>();
        SolrInputDocument solrInputDocument = new SolrInputDocument();

        String userid = groupPerUserPrefix + user.getId();
        if (user.isGuest()) {
            userid = publicGroupString;
        }

        solrInputDocument.addField(SearchFields.TYPE, "groups");
        solrInputDocument.addField(SearchFields.ID, userid);
        solrInputDocument.addField(SearchFields.ENTITY_ID, user.getId());
        solrInputDocument.addField(SearchFields.NAME_SORT, user.getUserName());
        solrInputDocument.addField(SearchFields.GROUPS, userid);

        docs.add(solrInputDocument);
        /**
         * @todo allow for configuration of hostname and port
         */
        SolrServer server = new HttpSolrServer("http://localhost:8983/solr/");

        try {
            server.add(docs);
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }
        try {
            server.commit();
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }

        return "indexed user " + user.getId() + ":" + user.getUserName();
    }

    public List<String> findPathSegments(Dataverse dataverse, List<String> segments) {
        if (!dataverseService.findRootDataverse().equals(dataverse)) {
            // important when creating root dataverse
            if (dataverse.getOwner() != null) {
                findPathSegments(dataverse.getOwner(), segments);
            }
            segments.add(dataverse.getAlias());
            return segments;
        } else {
            // base case
            return segments;
        }
    }

    List<String> getDataversePathsFromSegments(List<String> dataversePathSegments) {
        List<String> subtrees = new ArrayList<>();
        for (int i = 0; i < dataversePathSegments.size(); i++) {
            StringBuilder pathBuilder = new StringBuilder();
            int numSegments = dataversePathSegments.size();
            for (int j = 0; j < numSegments; j++) {
                if (j <= i) {
                    pathBuilder.append("/" + dataversePathSegments.get(j));
                }
            }
            subtrees.add(pathBuilder.toString());
        }
        return subtrees;
    }

    private void addDataverseReleaseDateToSolrDoc(SolrInputDocument solrInputDocument, Dataverse dataverse) {
        if (dataverse.getPublicationDate() != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(dataverse.getPublicationDate().getTime());
            int YYYY = calendar.get(Calendar.YEAR);
            solrInputDocument.addField(SearchFields.RELEASE_DATE, YYYY);
        }
    }

    private void addDatasetReleaseDateToSolrDoc(SolrInputDocument solrInputDocument, Dataset dataset) {
        if (dataset.getPublicationDate() != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(dataset.getPublicationDate().getTime());
            int YYYY = calendar.get(Calendar.YEAR);
            solrInputDocument.addField(SearchFields.RELEASE_DATE, YYYY);
        }
    }

    public static String getGroupPrefix() {
        return groupPrefix;
    }

    public static String getGroupPerUserPrefix() {
        return groupPerUserPrefix;
    }

    public static String getPublicGroupString() {
        return publicGroupString;
    }

    public static Long getTmpNsaGroupId() {
        return tmpNsaGroupId;
    }

    public String delete(Dataverse doomed) {
        /**
         * @todo allow for configuration of hostname and port
         */
        SolrServer server = new HttpSolrServer("http://localhost:8983/solr/");
        logger.info("deleting Solr document for dataverse " + doomed.getId());
        UpdateResponse updateResponse;
        try {
            updateResponse = server.deleteById(solrDocIdentifierDataverse + doomed.getId());
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }
        try {
            server.commit();
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }
        String response = "Successfully deleted dataverse " + doomed.getId() + " from Solr index. updateReponse was: " + updateResponse.toString();
        logger.info(response);
        return response;
    }

    public String removeDatasetDraftFromIndex(String doomed) {
        if (true) {
            return "FIXME: remove this 'if true' to actually delete dataset draft " + doomed;
        }
        /**
         * @todo allow for configuration of hostname and port
         */
        SolrServer server = new HttpSolrServer("http://localhost:8983/solr/");
        logger.info("deleting Solr document for dataset draft: " + doomed);
        UpdateResponse updateResponse;
        try {
            updateResponse = server.deleteById(doomed);
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }
        try {
            server.commit();
        } catch (SolrServerException | IOException ex) {
            return ex.toString();
        }
        String response = "Successfully deleted dataset draft " + doomed + " from Solr index. updateReponse was: " + updateResponse.toString();
        logger.info(response);
        return response;
    }

}
