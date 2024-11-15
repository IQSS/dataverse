package edu.harvard.iq.dataverse.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import de.undercouch.citeproc.CSL;
import de.undercouch.citeproc.helper.CSLUtils;
import jakarta.ejb.Singleton;

public class CSLUtil {
    private static final Logger logger = Logger.getLogger(CSLUtil.class.getName());

    static ArrayList<String> supportedStyles;

    public static List<String> getSupportedStyles() {
        if (supportedStyles != null) {
            return supportedStyles;
        }
        supportedStyles = new ArrayList<>();
        try {
            Set<String> styleSet = CSL.getSupportedStyles();
            // Remove styles starting with "dependent/"
            styleSet.removeIf(style -> style.startsWith("dependent/"));
            supportedStyles = new ArrayList<>(styleSet);
        } catch (IOException e) {
            logger.warning("Unable to retrieve supported CSL styles: " + e.getMessage());
            e.printStackTrace();
        }
        supportedStyles.sort(Comparator.naturalOrder());
        return supportedStyles;
    }

    /**
     * Adapted from private retrieveStyle method in de.undercouch.citeproc.CSL
     * Retrieves a CSL style from the classpath. For example, if the given name is
     * <code>ieee</code> this method will load the file <code>/ieee.csl</code>
     * 
     * @param styleName the style's name
     * @return the serialized XML representation of the style
     * @throws IOException if the style could not be loaded
     */
    public static String getCitationFormat(String styleName) throws IOException {
        URL url;

        // normalize file name
        if (!styleName.endsWith(".csl")) {
            styleName = styleName + ".csl";
        }
        if (!styleName.startsWith("/")) {
            styleName = "/" + styleName;
        }

        // try to find style in classpath
        url = CSL.class.getResource(styleName);
        if (url == null) {
            throw new FileNotFoundException("Could not find style in " + "classpath: " + styleName);
        }

        // load style
        String result = CSLUtils.readURLToString(url, "UTF-8");
        result=result.replace("\"", "\\\"").replace("\r","").replace("\n","");
        return result;
    }

}
