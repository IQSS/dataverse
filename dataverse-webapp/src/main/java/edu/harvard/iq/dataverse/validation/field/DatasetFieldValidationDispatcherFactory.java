package edu.harvard.iq.dataverse.validation.field;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.List;

@Stateless
public class DatasetFieldValidationDispatcherFactory {

    private FieldValidatorRegistry registry;

    // -------------------- CONSTRUCTORS --------------------

    public DatasetFieldValidationDispatcherFactory() { }

    @Inject
    public DatasetFieldValidationDispatcherFactory(FieldValidatorRegistry registry) {
        this.registry = registry;
    }

    // -------------------- LOGIC --------------------

    public DatasetFieldValidationDispatcher create(List<DatasetField> parentAndChildrenFields) {
        return new DatasetFieldValidationDispatcher(registry)
                .init(parentAndChildrenFields);
    }
}
