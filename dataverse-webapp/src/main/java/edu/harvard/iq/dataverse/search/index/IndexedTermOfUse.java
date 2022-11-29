package edu.harvard.iq.dataverse.search.index;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum IndexedTermOfUse {
    MULTIPLE("multiple", "facets.search.license.multiple"),
    RESTRICTED("restricted", "facets.search.license.restricted"),
    ALL_RIGHTS_RESERVED("allRightsReserved", "facets.search.license.allRightsReserved");
    
    private static final Map<String, String> labels = Stream.of(IndexedTermOfUse.values()).collect(Collectors.toMap(IndexedTermOfUse::getName, IndexedTermOfUse::getLabel));
    
    String name;
    String label;
    
    IndexedTermOfUse(String name, String label) {
        this.name = name;
        this.label = label;
    }
    
    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }
    
    public static String getLabelFromName(String givenName) {
        return labels.get(givenName);
    }
};
