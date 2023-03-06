package edu.harvard.iq.dataverse.validation.field.validators;

import edu.harvard.iq.dataverse.validation.field.FieldValidator;
import edu.harvard.iq.dataverse.validation.field.FieldValidatorRegistry;

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
