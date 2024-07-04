package edu.harvard.iq.dataverse.persistence.dataset.formatter;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import io.vavr.control.Option;

/**
 * Decorates the pre-formatted field value with a link provided for the given dataset field.
 */
public class LinkFormattedValueDecorator implements DatasetFieldFormattedValueDecorator {

    private final UrlProvider urlProvider;

    // -------------------- CONSTRUCTORS --------------------

    public LinkFormattedValueDecorator(UrlProvider urlProvider) {
        this.urlProvider = urlProvider;
    }

    // -------------------- LOGIC --------------------

    /**
     * Returns the pre-formatted value decorated as a link, or none if no link/url could be retrieved.
     */
    @Override
    public Option<String> decorate(DatasetField field, String formattedValue) {
        return urlProvider.getUrl(field)
                .map(href -> "<a href=\"" + href + "\" target=\"_blank\">" + formattedValue + "</a>");
    }

    public interface UrlProvider {
        /**
         * Return the url for the given field, if applicable. Otherwise, should return none.
         */
        Option<String> getUrl(DatasetField field);
    }
}
