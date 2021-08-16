package edu.harvard.iq.dataverse.importer.metadata;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

/**
 * Container for name and value fields, but with the ability to contain also child elements, so
 * it can be used to build tree-like structures.
 */
public class ResultField {

    private final String name;
    private final String value;
    private final List<ResultField> children;

    // -------------------- CONSTRUCTORS --------------------

    private ResultField(String name, String value) {
        this(name, value, emptyList());
    }

    private ResultField(String name, ResultField... children) {
        this(name, StringUtils.EMPTY, ofNullable(children).map(Arrays::asList).orElseGet(Collections::emptyList));
    }

    private ResultField(String name, String value, List<ResultField> children) {
        this.name = name;
        this.value = value;
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

    public static ResultField of(String name, List<ResultField> children) {
        return new ResultField(name, StringUtils.EMPTY, children);
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

    public Stream<ResultField> stream() {
        if (StringUtils.EMPTY.equals(value)) {
            return children.stream();
        } else {
            return Stream.of(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResultField that = (ResultField) o;

        if (!Objects.equals(name, that.name)) return false;
        if (!Objects.equals(value, that.value)) return false;
        return Objects.equals(children, that.children);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (children != null ? children.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ResultField{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", children=" + children +
                '}';
    }
}
