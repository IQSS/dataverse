package edu.harvard.iq.dataverse.importer.metadata;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Container for name and value fields, but with the ability to contain also child elements, so
 * it can be used to build tree-like structures.
 */
public class ResultField {
    private String name;
    private String value;
    private List<ResultField> children;

    // -------------------- CONSTRUCTORS --------------------

    private ResultField(String name, String value) {
        this.name = name;
        this.value = value;
        this.children = Collections.emptyList();
    }

    private ResultField(String name, ResultField... children) {
        this.name = name;
        this.value = StringUtils.EMPTY;
        this.children = children == null || children.length == 0
                ? Collections.emptyList()
                : Arrays.stream(children).collect(Collectors.toList());
    }

    private ResultField(String name, String value, List<ResultField> children) {
        this(name, value);
        this.children = children;
    }

    // -------------------- GETTERS --------------------

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public List<ResultField> getChildren() {
        return children;
    }

    // -------------------- LOGIC --------------------

    public static ResultField of(String name, String value) {
        return new ResultField(name, value);
    }

    /**
     * Convenience factory method for vocabulary values
     */
    public static ResultField ofValue(String value) {
        return new ResultField(StringUtils.EMPTY, value);
    }

    public static ResultField of(String name, ResultField... children) {
        return new ResultField(name, children);
    }

    /**
     * Merges this ResultField with another and returns new ResultField object. Every value of the {@code other}
     * parameter that is not null or empty will override value coming from caller's object.
     */
    public ResultField merge(ResultField other) {
        return new ResultField(
                StringUtils.isBlank(other.name) ? this.name : other.name,
                StringUtils.isBlank(other.value) ? this.value : other.value,
                other.children == null || other.children.isEmpty() ? this.children : other.children
         );
    }
}
