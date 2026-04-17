package edu.harvard.iq.dataverse.util.testing.recipes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Recipe describing how many variables should be created for a tabular file
 * or data table, and whether generated file-variable pairs should receive
 * {@code VariableMetadata}.
 */
public interface VariableSetRecipe {
    
    /**
     * Returns the number of variables to create for the given context.
     *
     * @param context contextual information about the file/table being populated
     * @return variable count to create
     */
    int variableCount(VariableSetBuildContext context);
    
    /**
     * Returns the recipe describing whether metadata should be created for a
     * generated {@code (FileMetadata, DataVariable)} pair.
     *
     * @return variable metadata recipe
     */
    VariableMetadataRecipe variableMetadataRecipe();
    
    /**
     * Creates a uniform variable set recipe with no metadata generation.
     *
     * @param variableCount uniform variable count
     * @return uniform variable set recipe
     */
    static VariableSetRecipe uniform(int variableCount) {
        return new UniformVariableSetRecipe(variableCount, VariableMetadataRecipe.noop());
    }
    
    /**
     * Creates a uniform variable set recipe with the supplied metadata recipe.
     *
     * @param variableCount uniform variable count
     * @param variableMetadataRecipe metadata recipe for generated pairs
     * @return uniform variable set recipe
     */
    static VariableSetRecipe uniform(int variableCount, VariableMetadataRecipe variableMetadataRecipe) {
        return new UniformVariableSetRecipe(variableCount, variableMetadataRecipe);
    }
    
    /**
     * Creates a predicate-driven variable set recipe with no metadata generation.
     *
     * @return predicate-driven variable set recipe
     */
    static PredicateVariableSetRecipe byPredicate() {
        return new PredicateVariableSetRecipe(VariableMetadataRecipe.noop());
    }
    
    /**
     * Creates a predicate-driven variable set recipe with the supplied metadata recipe.
     *
     * @param variableMetadataRecipe metadata recipe for generated pairs
     * @return predicate-driven variable set recipe
     */
    static PredicateVariableSetRecipe byPredicate(VariableMetadataRecipe variableMetadataRecipe) {
        return new PredicateVariableSetRecipe(variableMetadataRecipe);
    }
    
    /**
     * Uniform variable set recipe.
     *
     * @param variableCount uniform variable count
     * @param variableMetadataRecipe metadata recipe for generated pairs
     */
    record UniformVariableSetRecipe(
        int variableCount,
        VariableMetadataRecipe variableMetadataRecipe
    ) implements VariableSetRecipe {
        
        @Override
        public int variableCount(VariableSetBuildContext context) {
            return variableCount;
        }
    }
    
    /**
     * Predicate-driven variable set recipe.
     */
    final class PredicateVariableSetRecipe implements VariableSetRecipe {
        
        private final List<Rule> rules = new ArrayList<>();
        private final VariableMetadataRecipe variableMetadataRecipe;
        private int defaultCount = 0;
        
        public PredicateVariableSetRecipe(VariableMetadataRecipe variableMetadataRecipe) {
            this.variableMetadataRecipe = variableMetadataRecipe;
        }
        
        /**
         * Adds a variable-count rule.
         *
         * @param predicate rule predicate
         * @param variableCount variable count to use when matched
         * @return this recipe
         */
        public PredicateVariableSetRecipe when(
            Predicate<VariableSetBuildContext> predicate,
            int variableCount
        ) {
            rules.add(new Rule(predicate, variableCount));
            return this;
        }
        
        /**
         * Sets the default variable count.
         *
         * @param variableCount default variable count
         * @return this recipe
         */
        public PredicateVariableSetRecipe otherwise(int variableCount) {
            this.defaultCount = variableCount;
            return this;
        }
        
        @Override
        public int variableCount(VariableSetBuildContext context) {
            for (Rule rule : rules) {
                if (rule.predicate().test(context)) {
                    return rule.variableCount();
                }
            }
            return defaultCount;
        }
        
        @Override
        public VariableMetadataRecipe variableMetadataRecipe() {
            return variableMetadataRecipe;
        }
        
        /**
         * Internal immutable predicate rule.
         *
         * @param predicate match condition
         * @param variableCount variable count to use when matched
         */
        private record Rule(
            Predicate<VariableSetBuildContext> predicate,
            int variableCount
        ) {
        }
    }
}