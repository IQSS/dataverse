package edu.harvard.iq.dataverse.arquillian.arquillianglassfishconfig;

import edu.harvard.iq.dataverse.arquillian.ParametrizedGlassfishConfCreator;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

public class ArquillianGlassfishConfigurationParserTest {

    private static ParametrizedGlassfishConfCreator glassfishConfCreator = new ParametrizedGlassfishConfCreator();

    @Test
    public void shouldSuccessfullyCreateTemporaryFile() {
        //when
        glassfishConfCreator.createTempGlassfishResources();

        //then
        Assert.assertTrue(Files.exists(Paths.get(ParametrizedGlassfishConfCreator.NEW_RESOURCE_PATH)));
    }

    @AfterClass
    public static void removeTempGlassfishResource() {
        glassfishConfCreator.cleanTempGlassfishResource();
    }
}

