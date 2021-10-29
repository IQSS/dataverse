package edu.harvard.iq.dataverse.validation.datasetfield;

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
        logger.info(String.format("Registered DatasetField validator [%s]", validator.getName()));
    }

    public Set<String> getRegisteredValidatorNames() {
        return Collections.unmodifiableSet(registry.keySet());
    }

    public FieldValidator get(String name) {
        return registry.get(name);
    }
}
