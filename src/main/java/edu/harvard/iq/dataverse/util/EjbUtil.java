package edu.harvard.iq.dataverse.util;

import jakarta.ejb.EJBException;

public class EjbUtil {

    /**
     * @param ex An EJBException.
     * @return The message from the root cause of the EJBException as a String.
     */
    public static String ejbExceptionToString(EJBException ex) {
        Throwable cause = ex;
        // An EJBException always has a cause. It won't be null.
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getLocalizedMessage();
    }
}
