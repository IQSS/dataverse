package io.gdcc.schemas.datacite45;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xmlunit.builder.Input;
import org.xmlunit.validation.Languages;
import org.xmlunit.validation.ValidationProblem;
import org.xmlunit.validation.Validator;

import java.io.StringWriter;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

class SchemaTest {
    
    static JAXBContext context;
    static Marshaller marshaller;
    static Validator validator;
    
    @BeforeAll
    static void setup() throws JAXBException {
        context = JAXBContext.newInstance(Resource.class);
        marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "http://datacite.org/schema/kernel-4 http://schema.datacite.org/meta/kernel-4/metadata.xsd");
        
        validator = Validator.forLanguage(Languages.W3C_XML_SCHEMA_NS_URI);
    }
    
    @Test
    void generateResource() throws JAXBException {
        Resource dataciteResource = new Resource()
            .withResourceType(new Resource.ResourceType().withResourceTypeGeneral(ResourceType.DATASET))
            .withTitles(new Resource.Titles.Title("test", null, null))
            .withCreators(new Resource.Creators.Creator()
                .withCreatorName(
                    new Resource.Creators.Creator.CreatorName()
                        .withValue("Foobar, Test")
                        .withNameType(NameType.PERSONAL))
                .withFamilyName("Foobar")
                .withGivenName("Testy")
                .withAffiliation("Dataverse")
            )
            .withPublisher(new Resource.Publisher().withValue("Dataverse Schema Test"))
            .withPublicationYear("2024")
            .withLanguage("en")
            .withIdentifier(new Resource.Identifier("10.0001/test1234", "doi"));
        
        StringWriter writer = new StringWriter();
        marshaller.marshal(dataciteResource, writer);
        String xml = writer.toString();
        
        Assertions.assertNotNull(xml);
        Assertions.assertFalse(xml.isEmpty());
        
        //System.out.println(xml);
        
        var results = validator.validateInstance(Input.fromString(xml).build());
        Assertions.assertTrue(results.isValid(),
            StreamSupport.stream(results.getProblems().spliterator(), false).map(ValidationProblem::getMessage).collect(Collectors.joining(" :: "))
        );
    }
    
}
