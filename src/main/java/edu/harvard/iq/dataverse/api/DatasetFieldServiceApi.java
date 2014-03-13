package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import java.io.BufferedReader;
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

@Path("datasetfield")
public class DatasetFieldServiceApi {

    @EJB
    DatasetFieldServiceBean datasetFieldService;

    @EJB
    DataverseServiceBean dataverseService;

    @GET
    public String getAll() {
        try {
            List<String> listOfIsHasParentsTrue = new ArrayList<>();
            List<String> listOfIsHasParentsFalse = new ArrayList<>();
            List<String> listOfIsAllowsMultiplesTrue = new ArrayList<>();
            List<String> listOfIsAllowsMultiplesFalse = new ArrayList<>();
            for (DatasetField dsf : datasetFieldService.findAllAll()) {
                if (dsf.isHasParent()) {
                    listOfIsHasParentsTrue.add(dsf.getName());
                    listOfIsAllowsMultiplesTrue.add(dsf.getName());
                } else {
                    listOfIsHasParentsFalse.add(dsf.getName());
                    listOfIsAllowsMultiplesFalse.add(dsf.getName());
                }
            }
            return "have parents: " + listOfIsHasParentsTrue + "\n\n" + "no parent: " + listOfIsHasParentsFalse + "\n\n"
                    + "allows multiples: " + listOfIsAllowsMultiplesTrue + "\n\n" + "doesn't allow multiples: " + listOfIsAllowsMultiplesFalse + "\n";
        } catch (EJBException ex) {
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

    @GET
    @Path("info/{name}")
    public String getByName(@PathParam("name") String name) {
        try {
            DatasetField dsf = datasetFieldService.findByName(name);
            Long id = dsf.getId();
            String title = dsf.getTitle();
            String fieldType = dsf.getFieldType();
            String solrField = dsf.getSolrField();
            boolean hasParent = dsf.isHasParent();
            boolean allowsMultiples = dsf.isAllowMultiples();
            String parentAllowsMultiplesDisplay = "N/A (no parent)";
            Boolean parentAllowsMultiplesBoolean = false;
            if (hasParent) {
                DatasetField parent = dsf.getParentDatasetField();
                parentAllowsMultiplesBoolean = parent.isAllowMultiples();
                parentAllowsMultiplesDisplay = parentAllowsMultiplesBoolean.toString();
            }
            return dsf.getName() + ":\n"
                    + "- id: " + id + "\n"
                    + "- title: " + title + "\n"
                    + "- fieldType: " + fieldType + "\n"
                    + "- allowsMultiples: " + allowsMultiples + "\n"
                    + "- hasParent: " + hasParent + "\n"
                    + "- parentAllowsMultiples: " + parentAllowsMultiplesDisplay + "\n"
                    + "- solrField: " + solrField + "\n"
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

    @GET
    @Path("load/{fileName}")
    public String loadDatasetFields(@PathParam("fileName") String fileName) {
        BufferedReader br = null;
        String line = "";
        String splitBy = "\t";
        int lineNumber = 0;

        try {

            br = new BufferedReader(new FileReader("/" + fileName.replace(".", "/")));
            while ((line = br.readLine()) != null) {
                // if lineNumber == 0, skip header
                if (lineNumber++ != 0) {

                    // use comma as separator
                    String[] dsfString = line.split(splitBy);

                    DatasetField dsf = new DatasetField();
                    dsf.setName(dsfString[0]);
                    dsf.setTitle(dsfString[1]);
                    dsf.setDescription(dsfString[2]);
                    dsf.setFieldType(dsfString[3]);
                    dsf.setDisplayOrder(new Integer(dsfString[4]).intValue());
                    dsf.setAdvancedSearchField(new Boolean(dsfString[5]).booleanValue());
                    dsf.setAllowControlledVocabulary(new Boolean(dsfString[6]).booleanValue());
                    dsf.setAllowMultiples(new Boolean(dsfString[7]).booleanValue());
                    dsf.setFacetable(new Boolean(dsfString[8]).booleanValue());
                    dsf.setShowAboveFold(new Boolean(dsfString[9]).booleanValue());
                    dsf.setRequired(new Boolean(dsfString[10]).booleanValue());
                    dsf.setMetadataBlock( dataverseService.findMDBByName(dsfString[11]) );
                    if (dsfString.length == 13) {
                        dsf.setParentDatasetField( datasetFieldService.findByName(dsfString[12]));
                    }
                    
                    datasetFieldService.save(dsf);
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
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

        return "DatasetFields loaded: " + (lineNumber - 1);
    }

    @POST
    @Consumes("text/tab-separated-values")
    @Path("showtsv")
    public String showDatasetFieldsfromTsv(String tsvAsString) {
        return tsvAsString;
    }

}
