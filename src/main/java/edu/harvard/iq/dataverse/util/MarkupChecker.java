package edu.harvard.iq.dataverse.util;

import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.jsoup.parser.Parser;

/**
 * Provides utility methods for sanitizing and processing HTML content.
 * <p>
 * This class serves as a wrapper for the {@code Jsoup.clean} method and offers
 * multiple configurations for cleaning HTML input. It also provides a method
 * for escaping HTML entities and stripping all HTML tags.
 * </p>
 *
 * @author rmp553
 */
public class MarkupChecker {

    /**
     * Sanitizes the provided HTML content using a customizable configuration.
     * <p>
     * This method uses the {@code Jsoup.clean} method with a configurable {@code Safelist}.
     * For more details, see the
     * <a href="http://jsoup.org/cookbook/cleaning-html/safelist-sanitizer">Jsoup SafeList Sanitizer</a>.
     * </p>
     * <p>
     * It supports preserving class attributes and optionally adding "noopener noreferrer nofollow"
     * attributes to anchor tags to enhance security and usability.
     * </p>
     *
     * @param unsafe                    the HTML content to be sanitized; may contain unsafe or untrusted elements.
     * @param keepClasses               whether to preserve class attributes in the sanitized HTML.
     * @param includeNoopenerNoreferrer whether to add "noopener noreferrer nofollow" to <a> tags.
     * @return a sanitized HTML string, free from potentially harmful content.
     */
    private static String sanitizeHTML(String unsafe, boolean keepClasses, boolean includeNoopenerNoreferrer) {
        if (unsafe == null) {
            return null;
        }

        // Create a base Safelist configuration
        Safelist sl = Safelist.basicWithImages()
                .addTags("h1", "h2", "h3", "kbd", "hr", "s", "del", "map", "area")
                .addAttributes("img", "usemap")
                .addAttributes("map", "name")
                .addAttributes("area", "shape", "coords", "href", "title", "alt")
                .addEnforcedAttribute("a", "target", "_blank");

        // Add class attributes if requested
        if (keepClasses) {
            sl.addAttributes(":all", "class");
        }

        // Add "noopener noreferrer nofollow" to <a> tags if requested
        if (includeNoopenerNoreferrer) {
            sl.addEnforcedAttribute("a", "rel", "noopener noreferrer nofollow");
        }

        return Jsoup.clean(unsafe, sl);
    }

    /**
     * Sanitizes the provided HTML content using a basic configuration.
     *
     * @param unsafe the HTML content to be sanitized; may contain unsafe or untrusted elements.
     * @return a sanitized HTML string, free from potentially harmful content.
     */
    public static String sanitizeBasicHTML(String unsafe) {
        return sanitizeHTML(unsafe, false, false);
    }

    /**
     * Sanitizes the provided HTML content using an advanced configuration.
     * <p>
     * This configuration preserves class attributes and adds "noopener noreferrer nofollow"
     * attributes to <a> tags to enhance security and usability.
     * </p>
     *
     * @param unsafe the HTML content to be sanitized; may contain unsafe or untrusted elements.
     * @return a sanitized HTML string, free from potentially harmful content.
     */
    public static String sanitizeAdvancedHTML(String unsafe) {
        return sanitizeHTML(unsafe, true, true);
    }

    /**
     * Removes all HTML tags from the provided content, leaving only plain text.
     *
     * @param unsafe the HTML content to process; may contain HTML tags.
     * @return the plain text content with all HTML tags removed, or {@code null} if the input is {@code null}.
     */
    public static String stripAllTags(String unsafe) {
        if (unsafe == null) {
            return null;
        }

        return Parser.unescapeEntities(Jsoup.clean(unsafe, Safelist.none()), true);
    }

    /**
     * Escapes special characters in the provided string into their corresponding HTML entities.
     *
     * @param unsafe the string to escape; may contain special characters.
     * @return a string with HTML entities escaped.
     */
    public static String escapeHtml(String unsafe) {
        return StringEscapeUtils.escapeHtml4(unsafe);
    }
}
