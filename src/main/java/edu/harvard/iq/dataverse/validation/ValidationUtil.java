package edu.harvard.iq.dataverse.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;

public final class ValidationUtil {
    
    private ValidationUtil() {
        // Private constructor to prevent instantiation of utility class
    }
    
    /**
     * Constructs a string representation of the property path for a given {@link ConstraintViolation}.
     * The path includes each node of the violation's property path, appending indices or keys
     * for iterable nodes as necessary.
     *
     * @param violation The {@link ConstraintViolation} instance containing the property path
     *                  information to be converted into a string.
     * @return A string representation of the property path, including indices or keys of iterable nodes.
     */
    public static String propertyPath(ConstraintViolation<?> violation) {
        StringBuilder path = new StringBuilder();
        
        for (Path.Node node : violation.getPropertyPath()) {
            if (node.isInIterable()) {
                if (node.getIndex() != null) {
                    path.append('[').append(node.getIndex()).append(']');
                } else if (node.getKey() != null) {
                    path.append("['").append(node.getKey()).append("']");
                } else {
                    path.append("[]");
                }
            }
            
            String name = node.getName();
            
            // Container-element nodes can be unnamed or use implementation-specific names.
            if (name == null ||
                node.getKind() == ElementKind.BEAN ||
                node.getKind() == ElementKind.METHOD ||
                (node.getKind() == ElementKind.PARAMETER && "arg0".equals(node.getName()))
            ) {
                continue;
            }
            
            if (!path.isEmpty() && (
                path.charAt(path.length() - 1) != ']' || node.getKind() != ElementKind.CONTAINER_ELEMENT
            )) {
                path.append('.');
            }
            
            path.append(name);
        }
        
        return path.toString();
    }
    
}
