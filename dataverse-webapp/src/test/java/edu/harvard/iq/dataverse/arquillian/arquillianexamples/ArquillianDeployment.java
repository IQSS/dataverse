package edu.harvard.iq.dataverse.arquillian.arquillianexamples;

import com.github.sleroy.fakesmtp.core.ServerConfiguration;
import com.github.sleroy.junit.mail.server.test.FakeSmtpRule;
import edu.harvard.iq.dataverse.ArquillianIntegrationTests;
import edu.harvard.iq.dataverse.arquillian.ParametrizedGlassfishConfCreator;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.experimental.categories.Category;

import java.util.logging.Logger;


@Category(ArquillianIntegrationTests.class)
public class ArquillianDeployment {

    private static final Logger logger = Logger.getLogger(ArquillianDeployment.class.getName());

    private static ParametrizedGlassfishConfCreator glassfishConfCreator = new ParametrizedGlassfishConfCreator();

    @Rule
    public FakeSmtpRule smtpServer = new FakeSmtpRule(ServerConfiguration.create()
                                                              .port(2525)
                                                              .relayDomains("gmail.com")
                                                              .charset("UTF-8"));
    @Deployment
    public static Archive<?> createDeployment() {

        JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addPackages(true, "edu.harvard.iq.dataverse")
                .addAsResource("test-persistence.xml", "META-INF/persistence.xml")
                .addAsResource("Bundle_en.properties");
        logger.info(javaArchive.toString(true));

        return javaArchive;
    }

    @AfterClass
    public static void removeTempGlassfishResource() {
        glassfishConfCreator.cleanTempGlassfishResource();
    }

}
