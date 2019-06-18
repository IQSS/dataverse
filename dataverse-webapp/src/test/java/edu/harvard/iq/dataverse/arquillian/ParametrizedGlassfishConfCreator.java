package edu.harvard.iq.dataverse.arquillian;

import io.vavr.control.Try;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

/**
 * Class responsible for parsing glassfish configuration.
 */
public class ParametrizedGlassfishConfCreator {

    public static String NEW_RESOURCE_PATH = System.getProperty("java.io.tmpdir") + "/dataverse/glassfish-resources.xml";

    // -------------------- LOGIC --------------------

    /**
     * Reads glassfish-resources template, reads values from properties file
     * and creates new version of glassfish-resources with new values in temporary location.
     */
    public void createTempGlassfishResources() {
        try {
            Path dataversePropertiesDirectory = Paths.get(System.getProperty("user.home") + "/.dataverse");
            Path propertiesPath = Paths.get(dataversePropertiesDirectory.toString() + "/glassfish.properties");

            if (!isPropertiesFileExists(propertiesPath)) {
                propertiesPath = Paths.get(ParametrizedGlassfishConfCreator.class.getResource("/glassfish.properties").getPath());
            }

            Properties properties = new Properties();
            properties.load(new FileInputStream(propertiesPath.toString()));

            Document document = replaceGlassfishXmlValues(properties);

            Files.createDirectories(Paths.get(System.getProperty("java.io.tmpdir") + "/dataverse"));
            createGlassfishResources(document, NEW_RESOURCE_PATH);
        } catch (IOException ex) {
            throw new IllegalStateException("There was a problem with parsing xml file", ex);
        }
    }

    public void cleanTempGlassfishResource() {
        Try.of(() -> Files.deleteIfExists(Paths.get(ParametrizedGlassfishConfCreator.NEW_RESOURCE_PATH)))
                .getOrElseThrow(throwable -> new RuntimeException("Unable to delete temporary glassfish resource", throwable));
    }

    // -------------------- PRIVATE --------------------

    private void createGlassfishResources(Document document, String savePath) throws IOException {

        XMLWriter xmlWriter = null;
        try {
            xmlWriter = new XMLWriter(new FileWriter(savePath));
            xmlWriter.write(document);
        } finally {
            if (xmlWriter != null) {
                xmlWriter.close();
            }
        }
    }

    private Document replaceGlassfishXmlValues(Properties properties) {
        SAXReader reader = new SAXReader();

        Document document = Try.of(() -> reader
                .read(new FileInputStream("src/test/resources-glassfish-embedded/glassfish-resources.xml")))
                .getOrElseThrow(throwable -> new RuntimeException("Unable to read glassfish-resources.xml", throwable));

        List<Node> list = document.selectNodes("/resources/jdbc-connection-pool/child::*");

        list.forEach(node -> {
                         Element element = (Element) node;
                         String propertyName = element.attribute(0).getValue();

                         switch (propertyName) {
                             case "password":
                                 element.attribute(1).setValue(properties.getProperty("db.password"));
                                 break;
                             case "PortNumber":
                                 element.attribute(1).setValue(properties.getProperty("db.portnumber"));
                                 break;
                             case "User":
                                 element.attribute(1).setValue(properties.getProperty("db.user"));
                                 break;
                             case "databaseName":
                                 element.attribute(1).setValue(properties.getProperty("db.databasename"));
                                 break;
                             case "ServerName":
                                 element.attribute(1).setValue(properties.getProperty("db.host"));
                                 break;
                         }
                     }
        );
        return document;
    }

    private boolean isPropertiesFileExists(Path propertiesPath) {
        return propertiesPath.toFile().exists();
    }
}
