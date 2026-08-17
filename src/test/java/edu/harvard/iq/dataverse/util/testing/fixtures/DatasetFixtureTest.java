package edu.harvard.iq.dataverse.util.testing.fixtures;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.branding.BrandingUtilTest;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;
import edu.harvard.iq.dataverse.util.testing.recipes.DatasetRecipe;
import edu.harvard.iq.dataverse.util.testing.recipes.DatasetTypeRecipe;
import edu.harvard.iq.dataverse.util.testing.recipes.FileRecipe;
import edu.harvard.iq.dataverse.util.testing.recipes.VariableSetRecipe;
import edu.harvard.iq.dataverse.util.testing.recipes.VersionRecipe;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DatasetFixtureTest {
    
    @BeforeAll
    static void setUp() {
        BrandingUtilTest.setupMocks();
        // Let MPCONFIG init and cache the lookup classes
        JvmSettings.PREFIX.lookupOptional();
    }
    
    @AfterAll
    static void tearDown() {
        BrandingUtilTest.tearDownMocks();
    }
    
    @Test
    void smoketest() {
        
        var recipe = DatasetRecipe.of(
            DatasetTypeRecipe.dataset(),
            VersionRecipe.of(
                FileRecipe.tabular(10, VariableSetRecipe.uniform(10)),
                //FileRecipe.tabular(50, VariableSetRecipe.byPredicate()),
                //FileRecipe.tabular(50, VariableSetRecipe.byRandom(10, 1000, 12345)),
                FileRecipe.regular(1)
            )
        );
        
        Instant start = Instant.now();
        
        var fixture = DatasetFixtureBuilder.builder()
            .recipe(recipe)
            .populator(FixturePopulator.minimal())
            .build();
        
        Instant finish = Instant.now();
        System.out.println("build: " + Duration.between(start, finish).toMillis() + " msec");
        
        start = Instant.now();
        
        JsonArrayBuilder jab = Json.createArrayBuilder();
        for (FileMetadata fileMetadata : fixture.fileMetadatas()) {
            DataFile dataFile = fileMetadata.getDataFile();
            jab.add(JsonPrinter.json(dataFile, fileMetadata, true));
        }
        var result = jab.build();
        
        finish = Instant.now();
        System.out.println("convert: " + Duration.between(start, finish).toMillis() + " msec");
        
        assertNotNull(result);
        
        start = Instant.now();
        result.toString();
        finish = Instant.now();
        System.out.println("print: " + Duration.between(start, finish).toMillis() + " msec");
    }
    
}
