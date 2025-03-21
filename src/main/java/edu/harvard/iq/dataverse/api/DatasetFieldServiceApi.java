package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.ControlledVocabAlternate;
import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.ControlledVocabularyValueServiceBean;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType.FieldType;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.MetadataBlockServiceBean;
import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.ConstraintViolationUtil;
import org.apache.commons.lang3.StringUtils;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.asJsonArray;
import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;

import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response.Status;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Path("admin/datasetfield")
public class DatasetFieldServiceApi extends AbstractApiBean {

    @EJB
    DatasetFieldServiceBean datasetFieldService;

    @EJB
    DataverseServiceBean dataverseService;

    @EJB
    MetadataBlockServiceBean metadataBlockService;

    @EJB
    ControlledVocabularyValueServiceBean controlledVocabularyValueService;

    private static final Logger logger = Logger.getLogger(DatasetFieldServiceApi.class.getName());
    
    @GET
    public Response getAll() {
        try {
            List<String> listOfIsHasParentsTrue = new ArrayList<>();
            List<String> listOfIsHasParentsFalse = new ArrayList<>();
            List<String> listOfIsAllowsMultiplesTrue = new ArrayList<>();
            List<String> listOfIsAllowsMultiplesFalse = new ArrayList<>();
            for (DatasetFieldType dsf : datasetFieldService.findAllOrderedById()) {
                if (dsf.isHasParent()) {
                    listOfIsHasParentsTrue.add(dsf.getName());
                    listOfIsAllowsMultiplesTrue.add(dsf.getName());
                } else {
                    listOfIsHasParentsFalse.add(dsf.getName());
                    listOfIsAllowsMultiplesFalse.add(dsf.getName());
                }
            }
            final List<DatasetFieldType> requiredFields = datasetFieldService.findAllRequiredFields();
            final List<String> requiredFieldNames = new ArrayList<>(requiredFields.size());
            for ( DatasetFieldType dt : requiredFields ) {
                requiredFieldNames.add( dt.getName() );
            }
            return ok( Json.createObjectBuilder().add("haveParents", asJsonArray(listOfIsHasParentsTrue))
                    .add("noParents", asJsonArray(listOfIsHasParentsFalse))
                    .add("allowsMultiples", asJsonArray(listOfIsAllowsMultiplesTrue))
                    .add("allowsMultiples", asJsonArray(listOfIsAllowsMultiplesTrue))
                    .add("doesNotAllowMultiples", asJsonArray(listOfIsAllowsMultiplesFalse))
                    .add("required", asJsonArray(requiredFieldNames))
            );
            
        } catch (EJBException ex) {
            Throwable cause = ex;
            StringBuilder sb = new StringBuilder();
            sb.append(ex).append(" ");
            while (cause.getCause() != null) {
                cause = cause.getCause();
                sb.append(cause.getClass().getCanonicalName()).append(" ");
                sb.append(cause.getMessage()).append(" ");
                if (cause instanceof ConstraintViolationException) {
                    sb.append(ConstraintViolationUtil.getErrorStringForConstraintViolations(cause));
                }
            }
            return error(Status.INTERNAL_SERVER_ERROR, sb.toString());
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
            String solrFieldSearchable = dsf.getSolrField().getNameSearchable();
            String solrFieldFacetable = dsf.getSolrField().getNameFacetable();
            String metadataBlock = dsf.getMetadataBlock().getName();
            String uri = dsf.getUri();
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
                    .add("id", id )
                    .add("title", title)
                    .add( "metadataBlock", metadataBlock)
                    .add("fieldType", fieldType.name())
                    .add("allowsMultiples", allowsMultiples)
                    .add("hasParent", hasParent)
                    .add("controlledVocabularyValues", controlledVocabularyValues)
                    .add("parentAllowsMultiples", parentAllowsMultiplesDisplay)
                    .add("solrFieldSearchable", solrFieldSearchable)
                    .add("solrFieldFacetable", solrFieldFacetable)
                    .add("isRequired", isRequired)
                    .add("uri", uri));
        
        } catch ( NoResultException nre ) {
            return notFound(name);
            
        } catch (EJBException | NullPointerException ex) {
            Throwable cause = ex;
            StringBuilder sb = new StringBuilder();
            sb.append(ex).append(" ");
            while (cause.getCause() != null) {
                cause = cause.getCause();
                sb.append(cause.getClass().getCanonicalName()).append(" ");
                sb.append(cause.getMessage()).append(" ");
                if (cause instanceof ConstraintViolationException) {
                    sb.append(ConstraintViolationUtil.getErrorStringForConstraintViolations(cause));
                }
            }
            return error( Status.INTERNAL_SERVER_ERROR, sb.toString() );
        }

    }

    /**
     *
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
        // the find will throw a NoResultException if no values are in db
//            datasetFieldService.findNAControlledVocabularyValue();
        TypedQuery<ControlledVocabularyValue> naValueFinder = em.createQuery("SELECT OBJECT(o) FROM ControlledVocabularyValue AS o WHERE o.datasetFieldType is null AND o.strValue = :strvalue", ControlledVocabularyValue.class);
        naValueFinder.setParameter("strvalue", DatasetField.NA_VALUE);
        
        if ( naValueFinder.getResultList().isEmpty() ) {
            ControlledVocabularyValue naValue = new ControlledVocabularyValue();
            naValue.setStrValue(DatasetField.NA_VALUE);
            datasetFieldService.save(naValue);
            return ok("NA value created.");

        } else {
            return ok("NA value exists.");
        }
    }

    public enum HeaderType {
        METADATABLOCK, DATASETFIELD, CONTROLLEDVOCABULARY
    }

    @POST
    @Consumes("text/tab-separated-values")
    @Path("load")
    public Response loadDatasetFields(File file) {
        ActionLogRecord alr = new ActionLogRecord(ActionLogRecord.ActionType.Admin, "loadDatasetFields");
        alr.setInfo( file.getName() );
        BufferedReader br = null;
        String line;
        String splitBy = "\t";
        int lineNumber = 0;
        HeaderType header = null;
        JsonArrayBuilder responseArr = Json.createArrayBuilder();
        String[] values = null;
        try {
            br = new BufferedReader(new FileReader("/" + file));
            while ((line = br.readLine()) != null) {
                lineNumber++;
                values = Arrays.stream(line.split(splitBy))
                    .map(String::trim)
                    .toArray(String[]::new);
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
                            responseArr.add( Json.createObjectBuilder()
                                                    .add("name", parseMetadataBlock(values))
                                                    .add("type", "MetadataBlock"));
                            break;
                            
                        case DATASETFIELD:
                            responseArr.add( Json.createObjectBuilder()
                                                    .add("name", parseDatasetField(values))
                                                    .add("type", "DatasetField") );
                            break;
                            
                        case CONTROLLEDVOCABULARY:
                            responseArr.add( Json.createObjectBuilder()
                                                    .add("name", parseControlledVocabulary(values))
                                                    .add("type", "Controlled Vocabulary") );
                            break;
                            
                        default:
                            throw new IOException("No #header defined in file.");

                    }
                }
            }
        } catch (FileNotFoundException e) {
            alr.setActionResult(ActionLogRecord.Result.BadRequest);
            alr.setInfo( alr.getInfo() + "// file not found");
            return error(Status.EXPECTATION_FAILED, "File not found");

        } catch (ArrayIndexOutOfBoundsException e) {
            String message = getArrayIndexOutOfBoundMessage(header, lineNumber, values.length);
            logger.log(Level.WARNING, message, e);
            alr.setActionResult(ActionLogRecord.Result.InternalError);
            alr.setInfo(alr.getInfo() + "// " + message);
            return error(Status.INTERNAL_SERVER_ERROR, message);

        } catch (Exception e) {
            String message = getGeneralErrorMessage(header, lineNumber, e.getMessage());
            logger.log(Level.WARNING, message, e);
            alr.setActionResult(ActionLogRecord.Result.InternalError);
            alr.setInfo( alr.getInfo() + "// " + message);
            return error(Status.INTERNAL_SERVER_ERROR, message);
            
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

        return ok( Json.createObjectBuilder().add("added", responseArr) );
    }

    /**
     * Provide a general error message including the part and line number
     * @param header
     * @param lineNumber
     * @param message
     * @return
     */
    public String getGeneralErrorMessage(HeaderType header, int lineNumber, String message) {
        List<String> arguments = new ArrayList<>();
        arguments.add(header != null ? header.name() : "unknown");
        arguments.add(String.valueOf(lineNumber));
        arguments.add(message);
        return BundleUtil.getStringFromBundle("api.admin.datasetfield.load.GeneralErrorMessage", arguments);
    }

    /**
     * Turn ArrayIndexOutOfBoundsException into an informative error message
     * @param header
     * @param lineNumber
     * @param wrongIndex
     * @return
     */
    public String getArrayIndexOutOfBoundMessage(HeaderType header,
                                                 int lineNumber,
                                                 int wrongIndex) {

        List<String> columns = getColumnsByHeader(header);
        
        String column = columns.get(wrongIndex - 1);
        List<String> arguments = new ArrayList<>();
        arguments.add(header.name());
        arguments.add(String.valueOf(lineNumber));
        arguments.add(column);
        arguments.add(String.valueOf(wrongIndex + 1));
        return BundleUtil.getStringFromBundle(
            "api.admin.datasetfield.load.ArrayIndexOutOfBoundMessage",
            arguments
        );
    }

    /**
     * Get the list of columns by the type of header
     * @param header
     * @return
     */
    private List<String> getColumnsByHeader(HeaderType header) {
        List<String> columns = null;
        if (header.equals(HeaderType.METADATABLOCK)) {
            columns = Arrays.asList("name", "dataverseAlias", "displayName");
        } else if (header.equals(HeaderType.DATASETFIELD)) {
            columns = Arrays.asList("name", "title", "description", "watermark",
              "fieldType", "displayOrder", "displayFormat", "advancedSearchField",
              "allowControlledVocabulary", "allowmultiples", "facetable",
              "displayoncreate", "required", "parent", "metadatablock_id");
        } else if (header.equals(HeaderType.CONTROLLEDVOCABULARY)) {
            columns = Arrays.asList("DatasetField", "Value", "identifier", "displayOrder");
        }

        return columns;
    }

    private String parseMetadataBlock(String[] values) {
        //Test to see if it exists by name
        MetadataBlock mdb = metadataBlockService.findByName(values[1]);
        if (mdb == null){
            mdb = new MetadataBlock();
        }
        mdb.setName(values[1]);
        if (!values[2].isEmpty()){
            mdb.setOwner(dataverseService.findByAlias(values[2]));
        }
        mdb.setDisplayName(values[3]);
        if (values.length>4 && !StringUtils.isEmpty(values[4])) {
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
        dsf.setMetadataBlock(dataverseService.findMDBByName(values[15]));
        if(values.length>16 && !StringUtils.isEmpty(values[16])) {
          dsf.setUri(values[16]);
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
        if (values[3] != null && !values[3].trim().isEmpty()){
            cvvi = datasetFieldService.findControlledVocabularyValueByDatasetFieldTypeAndIdentifier(dsv, values[3]);
        }
        
        //if there's a match on identifier use it
        if (cvvi != null){
            cvv = cvvi;
        }

        //if there's no match create a new one
        if (cvv == null) {
            cvv = new ControlledVocabularyValue();
            cvv.setDatasetFieldType(dsv);
        }
        
        // Alternate variants for this controlled vocab. value: 
        
        // Note that these are overwritten every time:
        cvv.getControlledVocabAlternates().clear();
        // - meaning, if an alternate has been removed from the tsv file, 
        // it will be removed from the database! -- L.A. 5.4
        
        for (int i = 5; i < values.length; i++) {
            ControlledVocabAlternate alt = new ControlledVocabAlternate();
            alt.setDatasetFieldType(dsv);
            alt.setControlledVocabularyValue(cvv);
            alt.setStrValue(values[i]);
            cvv.getControlledVocabAlternates().add(alt);
        }
        
        cvv.setStrValue(values[2]);
        cvv.setIdentifier(values[3]);
        cvv.setDisplayOrder(Integer.parseInt(values[4]));
        datasetFieldService.save(cvv);
        return cvv.getStrValue();
    }


    @POST
    @Consumes("application/zip")
    @Path("loadpropertyfiles")
    public Response loadLanguagePropertyFile(File inputFile) {
        try (ZipFile file = new ZipFile(inputFile)) {
            //Get file entries
            Enumeration<? extends ZipEntry> entries = file.entries();

            //We will unzip files in this folder
            String dataverseLangDirectory = getDataverseLangDirectory();

            //Iterate over entries
            while (entries.hasMoreElements())
            {
                ZipEntry entry = entries.nextElement();
                String dataverseLangFileName = dataverseLangDirectory + "/" + entry.getName();
                File entryFile = new File(dataverseLangFileName);
                String canonicalPath = entryFile.getCanonicalPath();
                if (canonicalPath.startsWith(dataverseLangDirectory + "/")) {
                    try (FileOutputStream fileOutput = new FileOutputStream(dataverseLangFileName)) {

                        InputStream is = file.getInputStream(entry);
                        BufferedInputStream bis = new BufferedInputStream(is);

                        while (bis.available() > 0) {
                            fileOutput.write(bis.read());
                        }
                    }
                } else {
                    logger.log(Level.SEVERE, "Zip Slip prevented: uploaded zip file tried to write to {}", canonicalPath);
                    return Response.status(400).entity("The zip file includes an illegal file path").build();
                }
            }
        }
        catch(IOException e) {
            logger.log(Level.SEVERE, "Reading the language property zip file failed", e);
            return Response.status(500).entity("Internal server error. More details available at the server logs.").build();
        }

        return Response.status(200).entity("Uploaded the file successfully ").build();
    }

    public static String getDataverseLangDirectory() {
        String dataverseLangDirectory = System.getProperty("dataverse.lang.directory");
        if (dataverseLangDirectory == null || dataverseLangDirectory.equals("")) {
            dataverseLangDirectory = "/tmp/files";
        }

        if (!Files.exists(Paths.get(dataverseLangDirectory))) {
            try {
                Files.createDirectories(Paths.get(dataverseLangDirectory));
            } catch (IOException ex) {
                logger.severe("Failed to create dataverseLangDirectory: " + dataverseLangDirectory );
                return null;
            }
        }

        return dataverseLangDirectory;
    }

    /**
     * Set setDisplayOnCreate for a DatasetFieldType.
     */
    @POST
    @Path("/setDisplayOnCreate")
    public Response setDisplayOnCreate(@QueryParam("datasetFieldType") String datasetFieldTypeIn, @QueryParam("setDisplayOnCreate") Boolean setDisplayOnCreateIn) {
        DatasetFieldType dft = datasetFieldService.findByName(datasetFieldTypeIn);
        if (dft == null) {
            return error(Status.NOT_FOUND, "Cound not find a DatasetFieldType by looking up " + datasetFieldTypeIn);
        }
        dft.setDisplayOnCreate(setDisplayOnCreateIn);
        DatasetFieldType saved = datasetFieldService.save(dft);
        return ok("DisplayOnCreate for DatasetFieldType " + saved.getName() + " is now " + saved.isDisplayOnCreate());
    }

}
