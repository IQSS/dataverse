package edu.harvard.iq.dataverse.validation.datasetfield.validators;

import edu.harvard.iq.dataverse.validation.datasetfield.FieldValidator;
import edu.harvard.iq.dataverse.validation.datasetfield.FieldValidatorRegistry;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

public abstract class FieldValidatorBase implements FieldValidator {

    @Inject
    private FieldValidatorRegistry registry;

    @PostConstruct
    public void init() {
        registry.register(this);
    }
}
