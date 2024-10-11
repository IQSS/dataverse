package edu.harvard.iq.dataverse.test.arquillian;

import edu.harvard.iq.dataverse.test.arquillian.ParametrizedGlassfishConfCreator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

public class ArquillianGlassfishConfigurationParserTest {

    private static ParametrizedGlassfishConfCreator glassfishConfCreator = new ParametrizedGlassfishConfCreator();

    @Test
    public void shouldSuccessfullyCreateTemporaryFile() {
        //when
        glassfishConfCreator.createTempGlassfishResources();

        //then
        Assertions.assertTrue(Files.exists(Paths.get(ParametrizedGlassfishConfCreator.NEW_RESOURCE_PATH)));
    }

    @AfterAll
    public static void removeTempGlassfishResource() {
        glassfishConfCreator.cleanTempGlassfishResource();
    }
}

