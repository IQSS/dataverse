
package edu.harvard.iq.dataverse.util.bagit;

import edu.harvard.iq.dataverse.util.json.JsonLDTerm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.gson.JsonParser;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BagGeneratorInfoFileTest {

    private BagGenerator bagGenerator;
    private JsonObjectBuilder testAggregationBuilder;
    
    @Mock
    private OREMap mockOreMap;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        
        // Create base test aggregation builder with required fields
        testAggregationBuilder = Json.createObjectBuilder();
        testAggregationBuilder.add("@id", "doi:10.5072/FK2/TEST123");
        testAggregationBuilder.add(JsonLDTerm.schemaOrg("name").getLabel(), "Test Dataset");
        testAggregationBuilder.add(JsonLDTerm.schemaOrg("includedInDataCatalog").getLabel(), "Test Catalog");
    }

    /**
     * Helper method to finalize the aggregation and create the BagGenerator
     */
    private void initializeBagGenerator() throws Exception {
        JsonObject testAggregation = testAggregationBuilder.build();
        
        JsonObjectBuilder oremapJsonBuilder = Json.createObjectBuilder();
        oremapJsonBuilder.add(JsonLDTerm.ore("describes").getLabel(), testAggregation);
        JsonObject oremapObject = oremapJsonBuilder.build();
        // Mock the OREMap.getOREMap() method to return the built JSON
        when(mockOreMap.getOREMap()).thenReturn(oremapObject);
        
        // Initialize BagGenerator with test data
        bagGenerator = new BagGenerator(mockOreMap, "");
        setPrivateField(bagGenerator, "aggregation", (com.google.gson.JsonObject) JsonParser
                .parseString(oremapObject.getJsonObject(JsonLDTerm.ore("describes").getLabel()).toString()));
        setPrivateField(bagGenerator, "totalDataSize", 1024000L);
        setPrivateField(bagGenerator, "dataCount", 10L);
    }

    @Test
    public void testGenerateInfoFileWithSingleContact() throws Exception {
        // Arrange
        JsonLDTerm contactTerm = JsonLDTerm.schemaOrg("creator");
        JsonLDTerm contactNameTerm = JsonLDTerm.schemaOrg("name");
        JsonLDTerm contactEmailTerm = JsonLDTerm.schemaOrg("email");
        
        when(mockOreMap.getContactTerm()).thenReturn(contactTerm);
        when(mockOreMap.getContactNameTerm()).thenReturn(contactNameTerm);
        when(mockOreMap.getContactEmailTerm()).thenReturn(contactEmailTerm);

        JsonObjectBuilder contactBuilder = Json.createObjectBuilder();
        contactBuilder.add(contactNameTerm.getLabel(), "John Doe");
        contactBuilder.add(contactEmailTerm.getLabel(), "john.doe@example.com");
        testAggregationBuilder.add(contactTerm.getLabel(), contactBuilder);

        initializeBagGenerator();

        // Act
        String infoFile = invokeGenerateInfoFile();

        // Assert
        assertNotNull(infoFile);
        assertTrue(infoFile.contains("Contact-Name: John Doe"));
        assertTrue(infoFile.contains("Contact-Email: john.doe@example.com"));
    }

    @Test
    public void testGenerateInfoFileWithMultipleContacts() throws Exception {
        // Arrange
        JsonLDTerm contactTerm = JsonLDTerm.schemaOrg("creator");
        JsonLDTerm contactNameTerm = JsonLDTerm.schemaOrg("name");
        JsonLDTerm contactEmailTerm = JsonLDTerm.schemaOrg("email");
        
        when(mockOreMap.getContactTerm()).thenReturn(contactTerm);
        when(mockOreMap.getContactNameTerm()).thenReturn(contactNameTerm);
        when(mockOreMap.getContactEmailTerm()).thenReturn(contactEmailTerm);

        JsonArrayBuilder contactsBuilder = Json.createArrayBuilder();
        
        JsonObjectBuilder contact1 = Json.createObjectBuilder();
        contact1.add(contactNameTerm.getLabel(), "John Doe");
        contact1.add(contactEmailTerm.getLabel(), "john.doe@example.com");
        
        JsonObjectBuilder contact2 = Json.createObjectBuilder();
        contact2.add(contactNameTerm.getLabel(), "Jane Smith");
        contact2.add(contactEmailTerm.getLabel(), "jane.smith@example.com");
        
        JsonObjectBuilder contact3 = Json.createObjectBuilder();
        contact3.add(contactNameTerm.getLabel(), "Bob Johnson");
        contact3.add(contactEmailTerm.getLabel(), "bob.johnson@example.com");
        
        contactsBuilder.add(contact1);
        contactsBuilder.add(contact2);
        contactsBuilder.add(contact3);

        testAggregationBuilder.add(contactTerm.getLabel(), contactsBuilder);

        initializeBagGenerator();

        // Act
        String infoFile = invokeGenerateInfoFile();

        // Assert
        assertNotNull(infoFile);
        assertTrue(infoFile.contains("Contact-Name: John Doe"));
        assertTrue(infoFile.contains("Contact-Email: john.doe@example.com"));
        assertTrue(infoFile.contains("Contact-Name: Jane Smith"));
        assertTrue(infoFile.contains("Contact-Email: jane.smith@example.com"));
        assertTrue(infoFile.contains("Contact-Name: Bob Johnson"));
        assertTrue(infoFile.contains("Contact-Email: bob.johnson@example.com"));
    }

    @Test
    public void testGenerateInfoFileWithSingleDescription() throws Exception {
        // Arrange
        JsonLDTerm descriptionTerm = JsonLDTerm.schemaOrg("description");
        JsonLDTerm descriptionTextTerm = JsonLDTerm.schemaOrg("value");
        
        when(mockOreMap.getDescriptionTerm()).thenReturn(descriptionTerm);
        when(mockOreMap.getDescriptionTextTerm()).thenReturn(descriptionTextTerm);

        JsonObjectBuilder descriptionBuilder = Json.createObjectBuilder();
        descriptionBuilder.add(descriptionTextTerm.getLabel(), "This is a test dataset description.");
        testAggregationBuilder.add(descriptionTerm.getLabel(), descriptionBuilder);

        initializeBagGenerator();

        // Act
        String infoFile = invokeGenerateInfoFile();

        // Assert
        assertNotNull(infoFile);
        assertTrue(infoFile.contains("External-Description: This is a test dataset description."));
    }

     @Test
    public void testGenerateInfoFileWithMultipleDescriptions() throws Exception {
        // Arrange
        JsonLDTerm descriptionTerm = JsonLDTerm.schemaOrg("description");
        JsonLDTerm descriptionTextTerm = JsonLDTerm.schemaOrg("value");
        
        when(mockOreMap.getDescriptionTerm()).thenReturn(descriptionTerm);
        when(mockOreMap.getDescriptionTextTerm()).thenReturn(descriptionTextTerm);

        JsonArrayBuilder descriptionsBuilder = Json.createArrayBuilder();
        
        JsonObjectBuilder desc1 = Json.createObjectBuilder();
        desc1.add(descriptionTextTerm.getLabel(), "First description of the dataset.");
        
        JsonObjectBuilder desc2 = Json.createObjectBuilder();
        desc2.add(descriptionTextTerm.getLabel(), "Second description with additional details.");
        
        JsonObjectBuilder desc3 = Json.createObjectBuilder();
        desc3.add(descriptionTextTerm.getLabel(), "Third description for completeness.");
        
        descriptionsBuilder.add(desc1);
        descriptionsBuilder.add(desc2);
        descriptionsBuilder.add(desc3);

        testAggregationBuilder.add(descriptionTerm.getLabel(), descriptionsBuilder);

        initializeBagGenerator();

        // Act
        String infoFile = invokeGenerateInfoFile();
        // Assert
        assertNotNull(infoFile);
        // Multiple descriptions should be concatenated with commas as per getSingleValue method
        assertTrue(infoFile.contains("External-Description: First description of the dataset.,Second description with\r\n additional details.,Third description for completeness."));
    }

    @Test
    public void testGenerateInfoFileWithRequiredFields() throws Exception {
        // Arrange - minimal setup with required fields already in setUp()
        JsonLDTerm contactTerm = JsonLDTerm.schemaOrg("creator");
        JsonLDTerm contactNameTerm = JsonLDTerm.schemaOrg("name");
        JsonLDTerm descriptionTerm = JsonLDTerm.schemaOrg("description");
        JsonLDTerm descriptionTextTerm = JsonLDTerm.schemaOrg("value");
        
        when(mockOreMap.getContactTerm()).thenReturn(contactTerm);
        when(mockOreMap.getContactNameTerm()).thenReturn(contactNameTerm);
        when(mockOreMap.getContactEmailTerm()).thenReturn(null);
        when(mockOreMap.getDescriptionTerm()).thenReturn(descriptionTerm);
        when(mockOreMap.getDescriptionTextTerm()).thenReturn(descriptionTextTerm);

        JsonObjectBuilder contactBuilder = Json.createObjectBuilder();
        contactBuilder.add(contactNameTerm.getLabel(), "Test Contact");
        testAggregationBuilder.add(contactTerm.getLabel(), contactBuilder);

        JsonObjectBuilder descriptionBuilder = Json.createObjectBuilder();
        descriptionBuilder.add(descriptionTextTerm.getLabel(), "Test description");
        testAggregationBuilder.add(descriptionTerm.getLabel(), descriptionBuilder);

        initializeBagGenerator();

        // Act
        String infoFile = invokeGenerateInfoFile();

        // Assert
        assertNotNull(infoFile);
        assertTrue(infoFile.contains("Contact-Name: Test Contact"));
        assertTrue(infoFile.contains("External-Description: Test description"));
        assertTrue(infoFile.contains("Source-Organization:"));
        assertTrue(infoFile.contains("Organization-Address:"));
        assertTrue(infoFile.contains("Organization-Email:"));
        assertTrue(infoFile.contains("Bagging-Date:"));
        assertTrue(infoFile.contains("External-Identifier: doi:10.5072/FK2/TEST123"));
        assertTrue(infoFile.contains("Bag-Size:"));
        assertTrue(infoFile.contains("Payload-Oxum: 1024000.10"));
        assertTrue(infoFile.contains("Internal-Sender-Identifier: Test Catalog:Test Dataset"));
    }

    @Test
    public void testGenerateInfoFileWithDifferentBagSizes() throws Exception {
        // Arrange
        JsonLDTerm contactTerm = JsonLDTerm.schemaOrg("creator");
        when(mockOreMap.getContactTerm()).thenReturn(contactTerm);
        when(mockOreMap.getContactNameTerm()).thenReturn(null);
        when(mockOreMap.getContactEmailTerm()).thenReturn(null);
        when(mockOreMap.getDescriptionTerm()).thenReturn(null);

        initializeBagGenerator();

        // Test with bytes
        setPrivateField(bagGenerator, "totalDataSize", 512L);
        setPrivateField(bagGenerator, "dataCount", 5L);
        String infoFile1 = invokeGenerateInfoFile();
        assertTrue(infoFile1.contains("Bag-Size: 512 bytes"));
        assertTrue(infoFile1.contains("Payload-Oxum: 512.5"));

        // Test with KB
        setPrivateField(bagGenerator, "totalDataSize", 2048L);
        setPrivateField(bagGenerator, "dataCount", 3L);
        String infoFile2 = invokeGenerateInfoFile();
        assertTrue(infoFile2.contains("Bag-Size: 2.05 KB"));
        assertTrue(infoFile2.contains("Payload-Oxum: 2048.3"));

        // Test with MB
        setPrivateField(bagGenerator, "totalDataSize", 5242880L);
        setPrivateField(bagGenerator, "dataCount", 100L);
        String infoFile3 = invokeGenerateInfoFile();
        assertTrue(infoFile3.contains("Bag-Size: 5.24 MB"));
        assertTrue(infoFile3.contains("Payload-Oxum: 5242880.100"));

        // Test with GB
        setPrivateField(bagGenerator, "totalDataSize", 2147483648L);
        setPrivateField(bagGenerator, "dataCount", 1000L);
        
        String infoFile4 = invokeGenerateInfoFile();
        assertTrue(infoFile4.contains("Bag-Size: 2.15 GB"));
        assertTrue(infoFile4.contains("Payload-Oxum: 2147483648.1000"));
    }

    // Helper methods

    /**
     * Invokes the private generateInfoFile method using reflection
     */
    private String invokeGenerateInfoFile() throws Exception {
        Method method = BagGenerator.class.getDeclaredMethod("generateInfoFile");
        method.setAccessible(true);
        return (String) method.invoke(bagGenerator);
    }

    /**
     * Sets a private field value using reflection
     */
    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = BagGenerator.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}