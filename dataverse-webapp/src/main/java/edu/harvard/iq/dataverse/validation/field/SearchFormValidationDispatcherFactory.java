package edu.harvard.iq.dataverse.validation.field;

import edu.harvard.iq.dataverse.search.advanced.field.SearchField;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Map;

@Stateless
public class SearchFormValidationDispatcherFactory {

    private FieldValidatorRegistry registry;

    // -------------------- CONSTRUCOTRS --------------------

    public SearchFormValidationDispatcherFactory() { }

    @Inject
    public SearchFormValidationDispatcherFactory(FieldValidatorRegistry registry) {
        this.registry = registry;
    }

    // -------------------- LOGIC --------------------

    public SearchFormValidationDispatcher create(Map<String, SearchField> searchFields,
                                                 Map<String, SearchField> nonSearchFields) {
        return new SearchFormValidationDispatcher(registry)
                .init(searchFields, nonSearchFields);
    }
}
