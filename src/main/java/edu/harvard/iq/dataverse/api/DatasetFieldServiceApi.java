package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.MetadataBlockServiceBean;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import org.apache.commons.lang.StringUtils;

@Path("datasetfield")
public class DatasetFieldServiceApi {

    @EJB
    DatasetFieldServiceBean datasetFieldService;

    @EJB
    DataverseServiceBean dataverseService;

    @EJB
    MetadataBlockServiceBean metadataBlockService;

    @GET
    public String getAll() {
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
            return "have parents: " + listOfIsHasParentsTrue + "\n\n" + "no parent: " + listOfIsHasParentsFalse + "\n\n"
                    + "allows multiples: " + listOfIsAllowsMultiplesTrue + "\n\n" + "doesn't allow multiples: " + listOfIsAllowsMultiplesFalse + "\n\n" + "required fields: " + requiredFields + "\n";
        } catch (EJBException ex) {
            Throwable cause = ex;
            StringBuilder sb = new StringBuilder();
            sb.append(ex).append(" ");
            while (cause.getCause() != null) {
                cause = cause.getCause();
                sb.append(cause.getClass().getCanonicalName()).append(" ");
                sb.append(cause.getMessage()).append(" ");
                if (cause instanceof ConstraintViolationException) {
                    ConstraintViolationException constraintViolationException = (ConstraintViolationException) cause;
                    for (ConstraintViolation<?> violation : constraintViolationException.getConstraintViolations()) {
                        sb.append("(invalid value: <<<")
                                .append(violation.getInvalidValue())
                                .append(">>> for ")
                                .append(violation.getPropertyPath())
                                .append(" at ")
                                .append(violation.getLeafBean())
                                .append(" - ")
                                .append(violation.getMessage())
                                .append(")");
                    }
                }
            }
            return Util.message2ApiError(sb.toString());
        }
    }

    @GET
    @Path("{name}")
    public String getByName(@PathParam("name") String name) {
        try {
            DatasetFieldType dsf = datasetFieldService.findByName(name);
            Long id = dsf.getId();
            String title = dsf.getTitle();
            String fieldType = dsf.getFieldType();
            String solrFieldSearchable = dsf.getSolrField().getNameSearchable();
            String solrFieldFacetable = dsf.getSolrField().getNameFacetable();
            String metadataBlock = dsf.getMetadataBlock().getName();
            boolean hasParent = dsf.isHasParent();
            boolean allowsMultiples = dsf.isAllowMultiples();
            boolean isRequired = dsf.isRequired();
            String parentAllowsMultiplesDisplay = "N/A (no parent)";
            Boolean parentAllowsMultiplesBoolean = false;
            if (hasParent) {
                DatasetFieldType parent = dsf.getParentDatasetFieldType();
                parentAllowsMultiplesBoolean = parent.isAllowMultiples();
                parentAllowsMultiplesDisplay = parentAllowsMultiplesBoolean.toString();
            }
            return dsf.getName() + ":\n"
                    + "- id: " + id + "\n"
                    + "- title: " + title + "\n"
                    + "- metadataBlock: " + metadataBlock + "\n"
                    + "- fieldType: " + fieldType + "\n"
                    + "- allowsMultiples: " + allowsMultiples + "\n"
                    + "- hasParent: " + hasParent + "\n"
                    + "- parentAllowsMultiples: " + parentAllowsMultiplesDisplay + "\n"
                    + "- solrFieldSearchable: " + solrFieldSearchable + "\n"
                    + "- solrFieldFacetable: " + solrFieldFacetable + "\n"
                    + "- isRequired: " + isRequired + "\n"
                    + "";
        } catch (EJBException | NullPointerException ex) {
            Throwable cause = ex;
            StringBuilder sb = new StringBuilder();
            sb.append(ex + " ");
            while (cause.getCause() != null) {
                cause = cause.getCause();
                sb.append(cause.getClass().getCanonicalName() + " ");
                sb.append(cause.getMessage() + " ");
                if (cause instanceof ConstraintViolationException) {
                    ConstraintViolationException constraintViolationException = (ConstraintViolationException) cause;
                    for (ConstraintViolation<?> violation : constraintViolationException.getConstraintViolations()) {
                        sb.append("(invalid value: <<<" + violation.getInvalidValue() + ">>> for " + violation.getPropertyPath() + " at " + violation.getLeafBean() + " - " + violation.getMessage() + ")");
                    }
                }
            }
            return Util.message2ApiError(sb.toString());
        }

    }

    private enum HeaderType {

        METADATABLOCK, DATASETFIELD, CONTROLLEDVOCABULARY
    }

    @POST
    @Consumes("text/tab-separated-values")
    @Path("load")
    public String loadDatasetFields(File file) {
        BufferedReader br = null;
        String line = "";
        String splitBy = "\t";
        int lineNumber = 0;
        HeaderType header = null;
        String returnString = "";

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
                            returnString += parseMetadataBlock(values) + " MetadataBlock added.\n";
                            break;
                        case DATASETFIELD:
                            returnString += parseDatasetField(values) + " DatasetField added.\n";
                            break;
                        case CONTROLLEDVOCABULARY:
                            returnString += parseControlledVocabulary(values) + " Controlled Vocabulary added.\n";
                            break;
                        default:
                            throw new IOException("No #header defined in file.");

                    }
                }
            }
        } catch (FileNotFoundException e) {
            returnString = "File not found.";
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return "File parsing completed:\n\n" + returnString;
    }

    private String parseMetadataBlock(String[] values) {
        MetadataBlock mdb = new MetadataBlock();
        mdb.setName(values[1]);
        mdb.setDisplayName(values[2]);

        metadataBlockService.save(mdb);
        return mdb.getName();
    }

    private String parseDatasetField(String[] values) {
        DatasetFieldType dsf = new DatasetFieldType();
        dsf.setName(values[1]);
        dsf.setTitle(values[2]);
        dsf.setDescription(values[3]);
        dsf.setWatermark(values[4]);
        dsf.setFieldType(values[5]);
        dsf.setDisplayOrder( Integer.parseInt(values[6]) );
        dsf.setAdvancedSearchFieldType(Boolean.parseBoolean(values[7]));
        dsf.setAllowControlledVocabulary(Boolean.parseBoolean(values[8]));
        dsf.setAllowMultiples(Boolean.parseBoolean(values[9]));
        dsf.setFacetable(Boolean.parseBoolean(values[10]));
        dsf.setDisplayOnCreate(Boolean.parseBoolean(values[11]));
        dsf.setRequired(Boolean.parseBoolean(values[12]));
        if (!StringUtils.isEmpty(values[13])) {
            dsf.setParentDatasetFieldType(datasetFieldService.findByName(values[13]));
        }
        dsf.setMetadataBlock(dataverseService.findMDBByName(values[14]));

        datasetFieldService.save(dsf);
        return dsf.getName();
    }

    private String parseControlledVocabulary(String[] values) {
        ControlledVocabularyValue cvv = new ControlledVocabularyValue();
        cvv.setDatasetFieldType(datasetFieldService.findByName(values[1]));
        cvv.setStrValue(values[2]);
        cvv.setDisplayOrder(new Integer(values[3]).intValue());

        datasetFieldService.save(cvv);
        return cvv.getStrValue();
    }
}
