package edu.harvard.iq.dataverse.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import de.undercouch.citeproc.CSL;
import jakarta.ejb.Singleton;

@Singleton
public class CSLUtil {
    private static final Logger logger = Logger.getLogger(CSLUtil.class.getName());

    ArrayList<String> supportedStyles;
    
        public List<String> getSupportedStyles() {
            if (supportedStyles!= null) {
                return supportedStyles;
            }
            supportedStyles = new ArrayList<>();
            try {
                supportedStyles = new ArrayList<>(CSL.getSupportedStyles());
            } catch (IOException e) {
                logger.warning("Unable to retrieve supported CSL styles: " + e.getMessage());
                e.printStackTrace();
            }
            supportedStyles.sort(Comparator.naturalOrder());
            return supportedStyles;
        }
        
        /**
         * Adapted from private retrieveStyle method in de.undercouch.citeproc.CSL
         * Retrieves a CSL style from the classpath. For example, if the given name
         * is <code>ieee</code> this method will load the file <code>/ieee.csl</code>
         * @param styleName the style's name
         * @return the serialized XML representation of the style
         * @throws IOException if the style could not be loaded
         */
        public String getCitationFormat(String citationKey) {
            /**
             * Retrieves a CSL style from the classpath. For example, if the given name
             * is <code>ieee</code> this method will load the file <code>/ieee.csl</code>
             * @param styleName the style's name
             * @return the serialized XML representation of the style
             * @throws IOException if the style could not be loaded
             */
            private static String retrieveStyle(String styleName) throws IOException {
                URL url;
                if (styleName.startsWith("http://") || styleName.startsWith("https://")) {
                    try {
                        // try to load matching style from classpath
                        return retrieveStyle(styleName.substring(styleName.lastIndexOf('/') + 1));
                    } catch (FileNotFoundException e) {
                        // there is no matching style in classpath
                        url = new URL(styleName);
                    }
                } else {
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
                        throw new FileNotFoundException("Could not find style in "
                                + "classpath: " + styleName);
                    }
                }

                // load style
                String result = CSLUtils.readURLToString(url, "UTF-8");

                // handle dependent styles
                if (isDependent(result)) {
                    String independentParentLink;
                    try {
                        independentParentLink = getIndependentParentLink(result);
                    } catch (ParserConfigurationException | IOException | SAXException e) {
                        throw new IOException("Could not load independent parent style", e);
                    }
                    if (independentParentLink == null) {
                        throw new IOException("Dependent style does not have an "
                                + "independent parent");
                    }
                    return retrieveStyle(independentParentLink);
                }

                return result;
            }
        }
            
        }
        
}
