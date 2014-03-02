package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.api.SearchFields;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrInputDocument;

@Stateless
@Named
public class IndexServiceBean {

    private static final Logger logger = Logger.getLogger(IndexServiceBean.class.getCanonicalName());

    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DatasetServiceBean datasetService;

//    List<String> advancedSearchFields = new ArrayList<>();
//    List<String> notAdvancedSearchFields = new ArrayList<>();

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

        return dataverseIndexCount + " dataverses" + " and " + datasetIndexCount + " datasets indexed\n";
    }

    public String indexDataverse(Dataverse dataverse) {
        Dataverse rootDataverse = dataverseService.findRootDataverse();
        Collection<SolrInputDocument> docs = new ArrayList<>();
        SolrInputDocument solrInputDocument = new SolrInputDocument();
        solrInputDocument.addField(SearchFields.ID, "dataverse_" + dataverse.getId());
        solrInputDocument.addField(SearchFields.ENTITY_ID, dataverse.getId());
        solrInputDocument.addField(SearchFields.TYPE, "dataverses");
        solrInputDocument.addField(SearchFields.NAME, dataverse.getName());
        solrInputDocument.addField(SearchFields.HOST_DATAVERSE, dataverse.getName());
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
            solrInputDocument.addField(SearchFields.PARENT_TYPE, "dataverses");
            // important when creating root dataverse
            if (dataverse.getOwner() != null) {
                solrInputDocument.addField(SearchFields.PARENT_ID, dataverse.getOwner().getId());
                solrInputDocument.addField(SearchFields.PARENT_NAME, dataverse.getOwner().getName());
            }
        }
        List<String> dataversePathSegmentsAccumulator = new ArrayList<>();
        List<String> dataverseSegments = findPathSegments(dataverse, dataversePathSegmentsAccumulator);
        List<String> dataversePaths = getDataversePathsFromSegments(dataverseSegments);
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

        if (dataset.getEditVersion() != null) {
            for (DatasetFieldValue datasetFieldValue : dataset.getEditVersion().getDatasetFieldValues()) {
                DatasetField datasetField = datasetFieldValue.getDatasetField();
                String title = datasetField.getTitle();
                String name = datasetField.getName();
                Long id = datasetField.getId();
                String idDashTitle = id + "-" + title;
                String idDashName = id + "-" + name;
//                logger.info(idDashTitle);
//                logger.info(name + ": " + datasetFieldValue.getStrValue());
                String solrField = datasetField.getSolrField();
                if (datasetFieldValue.getStrValue() != null && !datasetFieldValue.getStrValue().isEmpty() && solrField != null) {
                    logger.info("indexing " + datasetFieldValue.getDatasetField().getName() + ":" + datasetFieldValue.getStrValue() + " into " + solrField);
                    if (solrField.endsWith("_i")) {
                        String dateAsString = datasetFieldValue.getStrValue();
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
                                solrInputDocument.addField(solrField, Integer.parseInt(datasetFieldFlaggedAsDate));
                            } catch (Exception ex) {
                                logger.info("unable to convert " + dateAsString + " into YYYY format and couldn't index it (" + datasetField.getName() + ")");
                            }
                        }
                    } else {
                        // _s (dynamic string) and all other Solr fields
                        solrInputDocument.addField(solrField, datasetFieldValue.getStrValue());
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
        solrInputDocument.addField(SearchFields.HOST_DATAVERSE, dataset.getOwner().getName());
        solrInputDocument.addField(SearchFields.PARENT_TYPE, "datasets");
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
            datafileSolrInputDocument.addField(SearchFields.FILE_TYPE_MIME, dataFile.getContentType());
            datafileSolrInputDocument.addField(SearchFields.FILE_TYPE, dataFile.getContentType().split("/")[0]);
            datafileSolrInputDocument.addField(SearchFields.SUBTREE, dataversePaths);
            datafileSolrInputDocument.addField(SearchFields.HOST_DATAVERSE, dataFile.getOwner().getOwner().getName());
            datafileSolrInputDocument.addField(SearchFields.PARENT_TYPE, "datasets");
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
}
