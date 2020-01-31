package edu.harvard.iq.dataverse.search.query;

import com.google.common.base.Preconditions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class indicating what dvObjects will be returned from search
 * 
 * @author madryk
 */
public class SearchForTypes {

    private Set<SearchObjectType> types = new HashSet<>();
    
    // -------------------- CONSTRUCTORS --------------------
    
    private SearchForTypes(Set<SearchObjectType> types) {
        Preconditions.checkArgument(types.size() > 0, "At least one dvObject type is required");
        this.types = types;
    }

    // -------------------- GETTERS --------------------
    
    public Set<SearchObjectType> getTypes() {
        return types;
    }

    // -------------------- LOGIC --------------------
    
    public boolean contains(SearchObjectType type) {
        return types.contains(type);
    }
    
    public boolean containsOnly(SearchObjectType type) {
        return types.size() == 1 && types.contains(type);
    }
    
    /**
     * Returns new {@link SearchForTypes} object with
     * either:
     * <p>
     * additional type if original {@link SearchForTypes}
     * does not contain it.
     * <p>
     * removed type if original {@link SearchForTypes} does
     * contain it.
     * <p>
     * Method do not modify original {@link SearchForTypes}
     */
    public SearchForTypes toggleType(SearchObjectType type) {
        Set<SearchObjectType> newTypes = new HashSet<>(types);
        
        if (newTypes.contains(type)) {
            newTypes.remove(type);
        } else {
            newTypes.add(type);
        }
        return new SearchForTypes(newTypes);
    }
    
    /**
     * Returns {@link SearchForTypes} with assigned dvObject types according
     * to the given types
     */
    public static SearchForTypes byTypes(List<SearchObjectType> types) {
        return new SearchForTypes(new HashSet<>(types));
    }
    
    /**
     * Returns {@link SearchForTypes} with assigned dvObject types according
     * to the given types
     */
    public static SearchForTypes byTypes(SearchObjectType ... types) {
        return byTypes(Arrays.asList(types));
    }
    
    /**
     * Returns {@link SearchForTypes} with assigned all possible dvObject types
     */
    public static SearchForTypes all() {
        return byTypes(SearchObjectType.values());
    }
}
