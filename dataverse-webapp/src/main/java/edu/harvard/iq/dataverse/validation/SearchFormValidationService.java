package edu.harvard.iq.dataverse.validation;

import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.search.advanced.SearchField;
import edu.harvard.iq.dataverse.validation.field.SearchFormValidationDispatcherFactory;
import edu.harvard.iq.dataverse.validation.field.ValidationResult;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;

@Stateless
public class SearchFormValidationService {

    private SearchFormValidationDispatcherFactory dispatcherFactory;

    // -------------------- CONSTRUCTORS --------------------

    public SearchFormValidationService() { }

    @Inject
    public SearchFormValidationService(SearchFormValidationDispatcherFactory dispatcherFactory) {
        this.dispatcherFactory = dispatcherFactory;
    }

    // -------------------- LOGIC --------------------

    public List<ValidationResult> validateSearchForm(Map<String, SearchField> searchFields,
                                                     Map<String, SearchField> nonSearchFields) {
        searchFields.forEach((k, v) -> v.setValidationMessage(null));
        List<ValidationResult> validationResults = dispatcherFactory.create(searchFields, nonSearchFields)
                .executeValidations();
        validationResults.forEach(r -> {
            ValidatableField field = r.getField();
            field.setValidationMessage(r.getMessage());
        });
        return validationResults;
    }
}
