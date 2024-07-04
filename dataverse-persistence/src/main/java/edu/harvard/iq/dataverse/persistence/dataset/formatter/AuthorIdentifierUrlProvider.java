package edu.harvard.iq.dataverse.persistence.dataset.formatter;

import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetAuthor;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import io.vavr.control.Option;

/**
 * UrlProvider for {@link DatasetFieldConstant#authorIdValue} dataset fields.
 */
public class AuthorIdentifierUrlProvider implements LinkFormattedValueDecorator.UrlProvider {

    // -------------------- LOGIC --------------------

    /**
     * Retrieves the URL for an {@link DatasetFieldConstant#authorIdValue} dataset field.
     * Assumptions:
     * - passed in field is of type {@link DatasetFieldConstant#authorIdValue}
     * - There's a sibling dataset field of type {@link DatasetFieldConstant#authorIdType}
     * If conditions are not met, none is returned.
     */
    @Override
    public Option<String> getUrl(DatasetField field) {
        if (!DatasetFieldConstant.authorIdValue.equals(field.getTypeName())) {
            return Option.none();
        }

        return getAuthorIdType(field)
                .flatMap(idType -> DatasetAuthor.getIdentifierAsUrl(idType, field.getValue()));
    }

    // -------------------- PRIVATE --------------------

    private Option<String> getAuthorIdType(DatasetField field) {
        return field.getParent()
                .flatMap(a -> Option.ofOptional(a.getChildren().stream()
                        .filter(c -> c.getDatasetFieldType().getName().equals(DatasetFieldConstant.authorIdType))
                        .findFirst()))
                .map(DatasetField::getValue);
    }
}


