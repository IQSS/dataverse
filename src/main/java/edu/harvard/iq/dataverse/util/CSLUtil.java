package edu.harvard.iq.dataverse.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.apache.commons.lang3.LocaleUtils;

import de.undercouch.citeproc.CSL;
import de.undercouch.citeproc.helper.CSLUtils;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import jakarta.faces.model.SelectItem;
import jakarta.faces.model.SelectItemGroup;

public class CSLUtil {
    private static final Logger logger = Logger.getLogger(CSLUtil.class.getName());

    static ArrayList<String> supportedStyles;
    static Map<String, List<SelectItem>> groupedStylesCache = new ConcurrentHashMap<>();


    public static String getDefaultStyle() {
        return getCommonStyles()[0];
    }

    public static List<SelectItem> getSupportedStyles(String localeCode) {
        Locale locale = LocaleUtils.toLocale(localeCode);
        if (locale == null) {
            locale = Locale.getDefault();
        }
        String languageKey = locale.getLanguage();

        if (groupedStylesCache.containsKey(languageKey)) {
            return groupedStylesCache.get(languageKey);
        }

        List<SelectItem> groupedStyles = new ArrayList<>();
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

        String commonTitle = BundleUtil.getStringFromBundle("dataset.cite.cslDialog.commonStyles", locale);
        SelectItemGroup commonStyles = new SelectItemGroup(commonTitle);
        ArrayList<SelectItem> commonArray = new ArrayList<>();
        String[] styleStrings = getCommonStyles();
        Arrays.stream(styleStrings).forEach(style -> {
            logger.fine("Found style: " + style);
            commonArray.add(new SelectItem(style, style));
            supportedStyles.remove(style);
        });
        commonStyles.setSelectItems(commonArray);

        String otherTitle = BundleUtil.getStringFromBundle("dataset.cite.cslDialog.otherStyles", locale);
        SelectItemGroup otherStyles = new SelectItemGroup(otherTitle);

        ArrayList<SelectItem> otherArray = new ArrayList<>(supportedStyles.size());
        supportedStyles.forEach(style -> {
            otherArray.add(new SelectItem(style, style));
        });
        otherStyles.setSelectItems(otherArray);

        groupedStyles.add(commonStyles);
        groupedStyles.add(otherStyles);

        groupedStylesCache.put(languageKey, groupedStyles);
        return groupedStyles;
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
        result = result.replace("\"", "\\\"").replace("\r", "").replace("\n", "");
        return result;
    }

    private static String[] commonStyles = null;

    private static String[] getCommonStyles() {
        if (commonStyles == null) {
            commonStyles = JvmSettings.CSL_COMMON_STYLES.lookupOptional().orElse("chicago-author-date, ieee")
                    .split("\\s*,\\s*");
        }
        return commonStyles;
    }

}
