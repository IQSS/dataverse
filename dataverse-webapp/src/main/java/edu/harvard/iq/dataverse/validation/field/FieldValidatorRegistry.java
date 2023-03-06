package edu.harvard.iq.dataverse.validation.field;

import org.omnifaces.cdi.Eager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Eager
@ApplicationScoped
public class FieldValidatorRegistry {
    private static final Logger logger = LoggerFactory.getLogger(FieldValidatorRegistry.class);

    private ConcurrentMap<String, FieldValidator> registry = new ConcurrentHashMap<>();

    // -------------------- LOGIC --------------------

    public void register(FieldValidator validator) {
        if (validator == null) {
            logger.warn("Tried to register a null validator. The validator was not registered.");
            return;
        }
        registry.putIfAbsent(validator.getName(), validator);
        logger.info(String.format("Registered validator [%s]", validator.getName()));
    }

    public Set<String> getRegisteredValidatorNames() {
        return Collections.unmodifiableSet(registry.keySet());
    }

    public FieldValidator getOrThrow(String name) {
        FieldValidator validator = registry.get(name);
        if (validator == null) {
            throw new RuntimeException(String.format("Cannot find validator [%s]. Registered validators: [%s]",
                    name, String.join(", ", getRegisteredValidatorNames())));
        }
        return validator;
    }
}
