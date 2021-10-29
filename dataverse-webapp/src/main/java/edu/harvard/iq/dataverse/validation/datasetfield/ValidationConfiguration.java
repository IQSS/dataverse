package edu.harvard.iq.dataverse.validation.datasetfield;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ValidationConfiguration {
    private List<ValidationDescriptor> validations = new ArrayList<>();

    // -------------------- GETTERS --------------------

    public List<ValidationDescriptor> getValidations() {
        return validations;
    }

    // -------------------- LOGIC --------------------

    public boolean shouldValidate() {
        return !getValidations().isEmpty();
    }

    // -------------------- SETTERS --------------------

    public void setValidations(List<ValidationDescriptor> validations) {
        this.validations = validations;
    }

    // -------------------- INNER CLASSES --------------------

    public static class ValidationDescriptor {

        private String name;
        private List<String> parameters = new ArrayList<>();

        // -------------------- GETTERS --------------------

        public String getName() {
            return name;
        }

        public List<String> getParameters() {
            return parameters;
        }

        // -------------------- LOGIC --------------------

        public Map<String, String> getParametersAsMap() {
            return parameters.stream()
                    .filter(Objects::nonNull)
                    .map(p -> p.split(":", 2))
                    .filter(p -> p.length == 2)
                    .collect(Collectors.toMap(p -> p[0], p -> p[1], (prev, next) -> next));
        }

        // -------------------- SETTERS --------------------

        public void setName(String name) {
            this.name = name;
        }
    }
}
