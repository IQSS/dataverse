package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.ControlledVocabularyValueServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.MetadataBlockDao;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.api.annotations.ApiWriteOperation;
import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.common.NullSafeJsonBuilder;
import edu.harvard.iq.dataverse.persistence.ActionLogRecord;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabAlternate;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.search.SolrField;
import org.apache.commons.lang.StringUtils;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


@Path("admin/datasetfield")
public class DatasetFieldServiceApi extends AbstractApiBean {
    private static final Logger logger = Logger.getLogger(DatasetFieldServiceApi.class.getName());

    @EJB
    DatasetFieldServiceBean datasetFieldService;

    @EJB
    DataverseDao dataverseDao;

    @EJB
    MetadataBlockDao metadataBlockService;

    @EJB
    ControlledVocabularyValueServiceBean controlledVocabularyValueService;

    @Inject
    private ActionLogServiceBean actionLogSvc;


    @GET
    public Response getAll() {
        try {
            List<DatasetFieldType> datasetFields = datasetFieldService.findAllOrderedById();
            Map<Boolean, List<String>> byParent = datasetFields.stream().collect(
                    Collectors.partitioningBy(DatasetFieldType::isHasParent,
                            Collectors.mapping(DatasetFieldType::getName, Collectors.toList())));
            Map<Boolean, List<String>> byMultiples = datasetFields.stream().collect(
                    Collectors.partitioningBy(DatasetFieldType::isAllowMultiples,
                            Collectors.mapping(DatasetFieldType::getName, Collectors.toList())));
            List<String> requiredFieldNames = datasetFields.stream()
                    .filter(DatasetFieldType::isRequired)
                    .map(DatasetFieldType::getName)
                    .collect(Collectors.toList());
            Map<String, List<String>> dto = new LinkedHashMap<>();
            dto.put("haveParents", byParent.get(Boolean.TRUE));
            dto.put("noParents", byParent.get(Boolean.FALSE));
            dto.put("allowsMultiples", byMultiples.get(Boolean.TRUE));
            dto.put("doesNotAllowMultiples", byMultiples.get(Boolean.FALSE));
            dto.put("required", requiredFieldNames);
            return ok(dto);
        } catch (EJBException eex) {
            logger.log(Level.WARNING, "Exception encountered: ", eex);
            return error(Status.INTERNAL_SERVER_ERROR, "Exception encountered");
        }
    }

    @GET
    @Path("{name}")
    public Response getByName(@PathParam("name") String name) {
        try {
            DatasetFieldType dsf = datasetFieldService.findByName(name);
            Long id = dsf.getId();
            String title = dsf.getTitle();
            FieldType fieldType = dsf.getFieldType();
            SolrField dsfSolrField = SolrField.of(dsf.getName(),
                                                                   dsf.getFieldType(),
                                                                   dsf.isThisOrParentAllowsMultipleValues(),
                                                                   dsf.isFacetable());
            String solrFieldSearchable = dsfSolrField.getNameSearchable();
            String solrFieldFacetable = dsfSolrField.getNameFacetable();
            String metadataBlock = dsf.getMetadataBlock().getName();
            boolean hasParent = dsf.isHasParent();
            boolean allowsMultiples = dsf.isAllowMultiples();
            boolean isRequired = dsf.isRequired();
            String parentAllowsMultiplesDisplay = "N/A (no parent)";
            boolean parentAllowsMultiplesBoolean;
            if (hasParent) {
                DatasetFieldType parent = dsf.getParentDatasetFieldType();
                parentAllowsMultiplesBoolean = parent.isAllowMultiples();
                parentAllowsMultiplesDisplay = Boolean.toString(parentAllowsMultiplesBoolean);
            }
            JsonArrayBuilder controlledVocabularyValues = Json.createArrayBuilder();
            for (ControlledVocabularyValue controlledVocabularyValue : dsf.getControlledVocabularyValues()) {
                controlledVocabularyValues.add(NullSafeJsonBuilder.jsonObjectBuilder()
                                                       .add("id", controlledVocabularyValue.getId())
                                                       .add("strValue", controlledVocabularyValue.getStrValue())
                                                       .add("displayOrder", controlledVocabularyValue.getDisplayOrder())
                                                       .add("identifier", controlledVocabularyValue.getIdentifier())
                );
            }
            return ok(NullSafeJsonBuilder.jsonObjectBuilder()
                              .add("name", dsf.getName())
                              .add("id", id)
                              .add("title", title)
                              .add("metadataBlock", metadataBlock)
                              .add("fieldType", fieldType.name())
                              .add("allowsMultiples", allowsMultiples)
                              .add("hasParent", hasParent)
                              .add("controlledVocabularyValues", controlledVocabularyValues)
                              .add("parentAllowsMultiples", parentAllowsMultiplesDisplay)
                              .add("solrFieldSearchable", solrFieldSearchable)
                              .add("solrFieldFacetable", solrFieldFacetable)
                              .add("isRequired", isRequired));

        } catch (NoResultException nre) {
            return notFound(name);

        } catch (EJBException | NullPointerException ex) {
            logger.log(Level.WARNING, "Exception encountered: ", ex);
            return error(Status.INTERNAL_SERVER_ERROR, "Exception encountered");
        }

    }

    /**
     * See also http://irclog.greptilian.com/rest/2015-02-07#i_95635
     *
     * @todo is our convention camelCase? Or lisp-case? Or snake_case?
     */
    @GET
    @Path("controlledVocabulary/subject")
    public Response showControlledVocabularyForSubject() {
        DatasetFieldType subjectDatasetField = datasetFieldService.findByName(DatasetFieldConstant.subject);
        JsonArrayBuilder possibleSubjects = Json.createArrayBuilder();
        for (ControlledVocabularyValue subjectValue : controlledVocabularyValueService.findByDatasetFieldTypeId(subjectDatasetField.getId())) {
            String subject = subjectValue.getStrValue();
            if (subject != null) {
                possibleSubjects.add(subject);
            }
        }
        return ok(possibleSubjects);
    }


    // TODO consider replacing with a @Startup method on the datasetFieldServiceBean
    @GET
    @Path("loadNAControlledVocabularyValue")
    public Response loadNAControlledVocabularyValue() {
        // the find will throw a javax.persistence.NoResultException if no values are in db
//            datasetFieldService.findNAControlledVocabularyValue();
        TypedQuery<ControlledVocabularyValue> naValueFinder = em.createQuery("SELECT OBJECT(o) FROM ControlledVocabularyValue AS o WHERE o.datasetFieldType is null AND o.strValue = :strvalue", ControlledVocabularyValue.class);
        naValueFinder.setParameter("strvalue", DatasetField.NA_VALUE);

        if (naValueFinder.getResultList().isEmpty()) {
            ControlledVocabularyValue naValue = new ControlledVocabularyValue();
            naValue.setStrValue(DatasetField.NA_VALUE);
            datasetFieldService.save(naValue);
            return ok("NA value created.");

        } else {
            return ok("NA value exists.");
        }
    }

    private enum HeaderType {

        METADATABLOCK, DATASETFIELD, CONTROLLEDVOCABULARY
    }

    @POST
    @ApiWriteOperation
    @Consumes("text/tab-separated-values")
    @Path("load")
    public Response loadDatasetFields(File file) {
        ActionLogRecord alr = new ActionLogRecord(ActionLogRecord.ActionType.Admin, "loadDatasetFields");
        alr.setInfo(file.getName());
        BufferedReader br = null;
        String line;
        String splitBy = "\t";
        int lineNumber = 0;
        HeaderType header = null;
        JsonArrayBuilder responseArr = Json.createArrayBuilder();
        try {
            br = new BufferedReader(new FileReader("/" + file));
            while ((line = br.readLine()) != null) {
                lineNumber++;
                String[] values = line.split(splitBy);
                if (values[0].startsWith("#")) { // Header row
                    switch (values[0]) {
                        case "#metadataBlock":
                            header = HeaderType.METADATABLOCK;
                            break;
                        case "#datasetField":
                            header = HeaderType.DATASETFIELD;
                            break;
                        case "#controlledVocabulary":
                            header = HeaderType.CONTROLLEDVOCABULARY;
                            break;
                        default:
                            throw new IOException("Encountered unknown #header type at line lineNumber " + lineNumber);
                    }
                } else {
                    switch (header) {
                        case METADATABLOCK:
                            responseArr.add(Json.createObjectBuilder()
                                                    .add("name", parseMetadataBlock(values))
                                                    .add("type", "MetadataBlock"));
                            break;

                        case DATASETFIELD:
                            responseArr.add(Json.createObjectBuilder()
                                                    .add("name", parseDatasetField(values))
                                                    .add("type", "DatasetField"));
                            break;

                        case CONTROLLEDVOCABULARY:
                            responseArr.add(Json.createObjectBuilder()
                                                    .add("name", parseControlledVocabulary(values))
                                                    .add("type", "Controlled Vocabulary"));
                            break;

                        default:
                            throw new IOException("No #header defined in file.");

                    }
                }
            }
        } catch (FileNotFoundException e) {
            alr.setActionResult(ActionLogRecord.Result.BadRequest);
            alr.setInfo(alr.getInfo() + "// file not found");
            return error(Status.EXPECTATION_FAILED, "File not found");

        } catch (Exception e) {
            logger.log(Level.WARNING, "Error parsing dataset fields:" + e.getMessage(), e);
            alr.setActionResult(ActionLogRecord.Result.InternalError);
            alr.setInfo(alr.getInfo() + "// " + e.getMessage());
            return error(Status.INTERNAL_SERVER_ERROR, e.getMessage());

        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Error closing the reader while importing Dataset Fields.");
                }
            }
            actionLogSvc.log(alr);
        }

        return ok(Json.createObjectBuilder().add("added", responseArr));
    }

    private String parseMetadataBlock(String[] values) {
        //Test to see if it exists by name
        MetadataBlock mdb = metadataBlockService.findByName(values[1]);
        if (mdb == null) {
            mdb = new MetadataBlock();
        }
        mdb.setName(values[1]);
        if (!values[2].isEmpty()) {
            mdb.setOwner(dataverseDao.findByAlias(values[2]));
        }
        mdb.setDisplayName(values[3]);
        if (values.length > 4 && !StringUtils.isEmpty(values[4])) {
            mdb.setNamespaceUri(values[4]);
        }

        metadataBlockService.save(mdb);
        return mdb.getName();
    }

    private String parseDatasetField(String[] values) {

        //First see if it exists
        DatasetFieldType dsf = datasetFieldService.findByName(values[1]);
        if (dsf == null) {
            //if not create new
            dsf = new DatasetFieldType();
        }
        //add(update) values
        dsf.setName(values[1]);
        dsf.setTitle(values[2]);
        dsf.setDescription(values[3]);
        dsf.setWatermark(values[4]);
        dsf.setFieldType(FieldType.valueOf(values[5].toUpperCase()));
        dsf.setDisplayOrder(Integer.parseInt(values[6]));
        dsf.setDisplayFormat(values[7]);
        dsf.setAdvancedSearchFieldType(Boolean.parseBoolean(values[8]));
        dsf.setAllowControlledVocabulary(Boolean.parseBoolean(values[9]));
        dsf.setAllowMultiples(Boolean.parseBoolean(values[10]));
        dsf.setFacetable(Boolean.parseBoolean(values[11]));
        dsf.setDisplayOnCreate(Boolean.parseBoolean(values[12]));
        dsf.setRequired(Boolean.parseBoolean(values[13]));
        if (!StringUtils.isEmpty(values[14])) {
            dsf.setParentDatasetFieldType(datasetFieldService.findByName(values[14]));
        } else {
            dsf.setParentDatasetFieldType(null);
        }
        dsf.setInputRendererType(InputRendererType.valueOf(values[15]));
        dsf.setInputRendererOptions(values[16]);
        dsf.setMetadataBlock(dataverseDao.findMDBByName(values[17]));
        if (values.length > 18 && !StringUtils.isEmpty(values[18])) {
            dsf.setUri(values[18]);
        }
        if (values.length > 19 && StringUtils.isNotBlank(values[19])) {
            dsf.setValidation(values[19]);
        }
        datasetFieldService.save(dsf);
        return dsf.getName();
    }

    private String parseControlledVocabulary(String[] values) {

        DatasetFieldType dsv = datasetFieldService.findByName(values[1]);
        //See if it already exists
        /*
         Matching relies on assumption that only one cv value will exist for a given identifier or display value
        If the lookup queries return multiple matches then retval is null
        */
        //First see if cvv exists based on display name
        ControlledVocabularyValue cvv = datasetFieldService.findControlledVocabularyValueByDatasetFieldTypeAndStrValue(dsv, values[2], true);

        //then see if there's a match on identifier
        ControlledVocabularyValue cvvi = null;
        if (values[3] != null && !values[3].trim().isEmpty()) {
            cvvi = datasetFieldService.findControlledVocabularyValueByDatasetFieldTypeAndIdentifier(dsv, values[3]);
        }

        //if there's a match on identifier use it
        if (cvvi != null) {
            cvv = cvvi;
        }

        //if there's no match create a new one
        if (cvv == null) {
            cvv = new ControlledVocabularyValue();
            cvv.setDatasetFieldType(dsv);
            //Alt is only for dataload so only add to new
            for (int i = 5; i < values.length; i++) {
                ControlledVocabAlternate alt = new ControlledVocabAlternate();
                alt.setDatasetFieldType(dsv);
                alt.setControlledVocabularyValue(cvv);
                alt.setStrValue(values[i]);
                cvv.getControlledVocabAlternates().add(alt);
            }
        }
        cvv.setStrValue(values[2]);
        cvv.setIdentifier(values[3]);
        cvv.setDisplayOrder(Integer.parseInt(values[4]));
        datasetFieldService.save(cvv);
        return cvv.getStrValue();
    }
}
