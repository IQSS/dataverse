package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Path("datasetfield")
public class DatasetFieldServiceApi {

    @EJB
    DatasetFieldServiceBean datasetFieldService;

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
    @Path("{name}")
    public String getByName(@PathParam("name") String name) {
        try {
            DatasetField dsf = datasetFieldService.findByName(name);
            Long id = dsf.getId();
            boolean hasParent = dsf.isHasParent();
            boolean allowsMultiples = dsf.isAllowMultiples();
            String parentAllowsMultiplesDisplay = "N/A (no parent)";
            Boolean parentAllowsMultiplesBoolean = false;
            if (hasParent) {
                DatasetField parent = dsf.getParentDatasetField();
                parentAllowsMultiplesBoolean = parent.isAllowMultiples();
                parentAllowsMultiplesDisplay = parentAllowsMultiplesBoolean.toString();
            }
            boolean makeSolrFieldMultivalued = false;
            if (allowsMultiples || parentAllowsMultiplesBoolean) {
                makeSolrFieldMultivalued = true;
            }
            return dsf.getTitle() + ":\n"
                    + "- id: " + id + "\n"
                    + "- allowsMultiples: " + allowsMultiples + "\n"
                    + "- hasParent: " + hasParent + "\n"
                    + "- parentAllowsMultiples: " + parentAllowsMultiplesDisplay + "\n"
                    + "- makeSolrFieldMultivalued: " + makeSolrFieldMultivalued + "\n"
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

}
