package edu.harvard.iq.dataverse.persistence.dataset.formatter;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import io.vavr.control.Option;

/**
 * Decorating pre-formatted field values for display.
 */
public interface DatasetFieldFormattedValueDecorator {

    /**
     * Decorate the preformatted value of the given field.
     *
     * @return Decorated formattedValue or none, if not applicable
     */
    Option<String> decorate(DatasetField field, String formattedValue);
}
