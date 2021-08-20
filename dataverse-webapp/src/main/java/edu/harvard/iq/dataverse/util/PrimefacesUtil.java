package edu.harvard.iq.dataverse.util;

import org.primefaces.PrimeFaces;

/**
 * Commonly used PrimeFaces specific operations
 *
 * @author madryk
 */
public class PrimefacesUtil {

    public final static String GLOBAL_FILTER_KEY = "globalFilter";

    public static void showDialog(String dialogWidgetVar) {
        PrimeFaces.current().executeScript("PF('" + dialogWidgetVar + "').show()");
    }

    public static void showDialogAndResize(String dialogWidgetVar) {
        PrimeFaces.current().executeScript("PF('" + dialogWidgetVar + "').show();handleResizeDialog('" + dialogWidgetVar + "');");
    }
}
