package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.IndexServiceBean;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("index")
public class Index {

    @EJB
    IndexServiceBean indexService;

    @GET
    public String index() {
        try {
            indexService.index();
            return "called index service bean\n";
        } catch (EJBException ex) {
            Throwable cause = ex;
            StringBuilder sb = new StringBuilder();
            while (cause.getCause() != null) {
                cause = cause.getCause();
                sb.append(cause.getClass().getCanonicalName() + " ");
                if (cause instanceof ConstraintViolationException) {
                    ConstraintViolationException constraintViolationException = (ConstraintViolationException) cause;
                    for (ConstraintViolation<?> violation : constraintViolationException.getConstraintViolations()) {
                        sb.append(violation.toString() + " ");
                        sb.append("(invalid value: <<<" + violation.getInvalidValue() + ">>>)");
                    }
                }
            }
            return Util.message2ApiError(sb.toString());
        }
    }
}
