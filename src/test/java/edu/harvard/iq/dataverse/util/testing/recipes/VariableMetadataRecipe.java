package edu.harvard.iq.dataverse.util.testing.recipes;

import java.util.function.Predicate;

/**
 * Recipe describing whether a {@link edu.harvard.iq.dataverse.datavariable.VariableMetadata} row should be created for
 * a generated {@code (FileMetadata, DataVariable)} pair.
 *
 * <p>This is modeled as a yes/no decision because the current schema enforces
 * uniqueness for each pair of {@code datavariable_id} and {@code filemetadata_id}.
 * As filemetadata is associated with a single dataset version, this makes variable metadata versioned, too.</p>
 */
public interface VariableMetadataRecipe {
    
    /**
     * Returns whether metadata should be created for the supplied pair context.
     *
     * @param context build context describing the file-variable pair
     * @return {@code true} if metadata should be created, otherwise {@code false}
     */
    boolean createFor(VariableMetadataBuildContext context);
    
    /**
     * Returns a recipe that never creates metadata.
     *
     * @return no-op recipe
     */
    static VariableMetadataRecipe noop() {
        return new Noop();
    }
    
    /**
     * Returns a recipe that always creates metadata.
     *
     * @return always-on recipe
     */
    static VariableMetadataRecipe always() {
        return new Always();
    }
    
    /**
     * Returns a predicate-driven metadata recipe.
     *
     * @param predicate predicate deciding whether metadata should be created
     * @return predicate-based recipe
     */
    static VariableMetadataRecipe byPredicate(Predicate<VariableMetadataBuildContext> predicate) {
        return new PredicateBased(predicate);
    }
    
    /**
     * Recipe that never creates metadata.
     */
    record Noop() implements VariableMetadataRecipe {
        
        @Override
        public boolean createFor(VariableMetadataBuildContext context) {
            return false;
        }
    }
    
    /**
     * Recipe that always creates metadata.
     */
    record Always() implements VariableMetadataRecipe {
        
        @Override
        public boolean createFor(VariableMetadataBuildContext context) {
            return true;
        }
    }
    
    /**
     * Predicate-based metadata recipe.
     *
     * @param predicate predicate deciding whether metadata should be created
     */
    record PredicateBased(
        Predicate<VariableMetadataBuildContext> predicate
    ) implements VariableMetadataRecipe {
        
        @Override
        public boolean createFor(VariableMetadataBuildContext context) {
            return predicate.test(context);
        }
    }
}