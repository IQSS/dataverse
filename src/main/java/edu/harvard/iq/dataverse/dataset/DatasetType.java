package edu.harvard.iq.dataverse.dataset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.io.Serializable;
import java.util.Arrays;

@Entity
public class DatasetType implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Type baseType;

    /**
     * This default constructor is only here to prevent this error at
     * deployment:
     *
     * Exception Description: The instance creation method
     * [...DatasetType.<Default Constructor>], with no parameters, does not
     * exist, or is not accessible
     *
     * Don't use it.
     */
    @Deprecated
    public DatasetType() {
    }

    public DatasetType(Type baseType) {
        this.baseType = baseType;
    }

    public enum Type {

        DATASET("dataset"),
        SOFTWARE("software"),
        WORKFLOW("workflow");

        private final String text;

        private Type(final String text) {
            this.text = text;
        }

        public static Type fromString(String text) {
            if (text != null) {
                for (Type type : Type.values()) {
                    if (text.equals(type.text)) {
                        return type;
                    }
                }
            }
            throw new IllegalArgumentException("Type must be one of these values: " + Arrays.asList(Type.values()) + ".");
        }

        @Override
        public String toString() {
            return text;
        }
    }

    public Type getBaseType() {
        return baseType;
    }

    public void setBaseType(Type baseType) {
        this.baseType = baseType;
    }

}
