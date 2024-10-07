package edu.harvard.iq.dataverse.search.query;

import static edu.harvard.iq.dataverse.search.query.SearchObjectType.DATASETS;
import static edu.harvard.iq.dataverse.search.query.SearchObjectType.DATAVERSES;
import static edu.harvard.iq.dataverse.search.query.SearchObjectType.FILES;
import static java.util.Arrays.asList;
import static java.util.EnumSet.complementOf;
import static java.util.EnumSet.copyOf;
import static java.util.EnumSet.noneOf;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

/**
 * Class indicating what dvObjects will be returned from search
 */
public class SearchForTypes {

    private final EnumSet<SearchObjectType> types;

    private SearchForTypes(final EnumSet<SearchObjectType> types) {
        
        this.types = types;
    }

    public Stream<SearchObjectType> types() {
        
        return this.types.stream();
    }

    public boolean containsDataverse() {
        
        return this.types.contains(DATAVERSES);
    }

    public boolean containsDatasets() {
        
        return this.types.contains(DATASETS);
    }

    public boolean containsFiles() {
        
        return this.types.contains(FILES);
    }

    public boolean contains(SearchObjectType type) {
        
        return this.types.contains(type);
    }

    public boolean containsOnly(final SearchObjectType type) {
        
        return this.types.size() == 1 && this.types.contains(type);
    }

    /**
     * Returns new {@link SearchForTypes} object with either:
     * <p>
     * additional type if original {@link SearchForTypes} does not contain it.
     * <p>
     * removed type if original {@link SearchForTypes} does contain it.
     * <p>
     * Method do not modify original {@link SearchForTypes}
     */
    public SearchForTypes toggleType(final SearchObjectType type) {
        
        final SearchForTypes result = new SearchForTypes(this.types.clone());   

        if (result.contains(type)) {
            result.types.remove(type);
        } else {
            result.types.add(type);
        }
        
        return result;
    }

    public SearchForTypes takeInverse() {
        
        return new SearchForTypes(complementOf(this.types));
    }

    /**
     * Returns {@link SearchForTypes} with assigned dvObject types according to the
     * given types
     */
    public static SearchForTypes byTypes(final List<SearchObjectType> types) {
        
        return new SearchForTypes(copyOf(types));
    }

    /**
     * Returns {@link SearchForTypes} with assigned dvObject types according to the
     * given types
     */
    public static SearchForTypes byTypes(final SearchObjectType... types) {
        
        return byTypes(asList(types));
    }

    /**
     * Returns {@link SearchForTypes} with assigned all possible dvObject types
     */
    public static SearchForTypes all() {
        
        return byTypes(SearchObjectType.values());
    }
    
    public static SearchForTypes empty() {
        
        return new SearchForTypes(noneOf(SearchObjectType.class));
    }
}
