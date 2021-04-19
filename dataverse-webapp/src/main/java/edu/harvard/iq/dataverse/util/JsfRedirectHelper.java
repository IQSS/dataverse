package edu.harvard.iq.dataverse.util;

public class JsfRedirectHelper {

    public static String redirectToDataverse(String dataverseAlias) {
        return "/dataverse.xhtml?alias=" + dataverseAlias + "&faces-redirect=true";
    }
}
