package edu.harvard.iq.dataverse.workflow.step;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

/**
 * Convenience wrapper for accessing workflow step parameters of different types.
 *
 * @author kaczynskid
 */
public class WorkflowStepParams {

    private final Map<String, String> parameters;

    // -------------------- CONSTRUCTORS --------------------

    public WorkflowStepParams() {
        this(emptyMap());
    }

    public WorkflowStepParams(String paramName, String paramValue) {
        this(singletonMap(paramName, paramValue));
    }

    public WorkflowStepParams(Map<String, String> parameters) {
        this.parameters = new HashMap<>(parameters);
    }

    public  WorkflowStepParams with(String paramName, String paramValue) {
        Map<String, String> params = new HashMap<>(parameters);
        params.put(paramName, paramValue);
        return new WorkflowStepParams(params);
    }

    // -------------------- LOGIC --------------------

    public Map<String, String> asMap() {
        return new HashMap<>(parameters);
    }

    public boolean containsKey(String paramName) {
        return parameters.containsKey(paramName);
    }

    public String get(String paramName) {
        return parameters.get(paramName);
    }

    public String getOrDefault(String paramName, String defaultValue) {
        return parameters.getOrDefault(paramName, defaultValue);
    }

    public String getRequired(String paramName) {
        return ofNullable(get(paramName))
                .orElseThrow(() -> new IllegalArgumentException("Command parameter is required"));
    }

    public boolean getBoolean(String paramName) {
        return getBooleanOrDefault(paramName, false);
    }

    public boolean getBooleanOrDefault(String paramName, boolean defaultValue) {
        return ofNullable(get(paramName))
                .map(Boolean::parseBoolean)
                .orElse(defaultValue);
    }

    public List<String> getList(String paramName, String delimiterRegex) {
        return getListOrDefault(paramName, delimiterRegex, emptyList());
    }

    public List<String> getListOrDefault(String paramName, String delimiterRegex, List<String> defaultValue) {
        return ofNullable(get(paramName))
                .map(args -> args.split(delimiterRegex))
                .map(Stream::of).orElseGet(defaultValue::stream)
                .map(StringUtils::trimToEmpty)
                .filter(StringUtils::isNotEmpty)
                .collect(toList());
    }
}
