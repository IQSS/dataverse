package edu.harvard.iq.dataverse.util.json;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.util.testing.SystemProperty;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class MetadataLocaleJsonPrinterTest {

    private MetadataBlock block;
    private DatasetFieldType title;
    private DatasetFieldType author;
    private DatasetFieldType authorName;

    @BeforeEach
    void setUp() {
        block = new MetadataBlock();
        block.setId(1L);
        block.setName("apiLocaleTest");
        block.setDisplayName("Database block name");
        title = field("title", "Database title");
        title.setRequired(true);
        title.setAllowControlledVocabulary(true);
        title.setControlledVocabularyValues(List.of(new ControlledVocabularyValue(1L, "original-value", title)));
        author = field("author", "Author");
        authorName = field("authorName", "Name");
        authorName.setParentDatasetFieldType(author);
        author.setChildDatasetFieldTypes(List.of(authorName));
        block.setDatasetFieldTypes(List.of(title, author, authorName));
    }

    @Test
    @SystemProperty(key = "dataverse.lang.directory", value = "")
    void translatesBlockFieldsAndCompoundChildren() {
        JsonObject result = JsonPrinter.json(block, false, null, null, Locale.forLanguageTag("fr-CA")).build();

        assertEquals("Bloc de test", result.getString("displayName"));
        JsonObject titleJson = result.getJsonObject("fields").getJsonObject("title");
        assertEquals("Titre", titleJson.getString("title"));
        assertEquals("Titre", titleJson.getString("displayName"));
        assertEquals("Description française du titre", titleJson.getString("description"));
        assertEquals("Saisissez le titre", titleJson.getString("watermark"));
        JsonObject child = result.getJsonObject("fields").getJsonObject("author")
                .getJsonObject("childFields").getJsonObject("authorName");
        assertEquals("Nom", child.getString("title"));
        assertEquals("Auteur Nom", child.getString("displayName"));
        assertEquals("Description du nom", child.getString("description"));
        assertEquals("Saisissez le nom", child.getString("watermark"));
    }

    @Test
    @SystemProperty(key = "dataverse.lang.directory", value = "")
    void translatesBriefBlockList() {
        JsonArray result = JsonPrinter.json(List.of(block), false, false, null, null,
                Locale.forLanguageTag("fr-CA")).build();

        assertEquals("Bloc de test", result.getJsonObject(0).getString("displayName"));
        assertEquals("apiLocaleTest", result.getJsonObject(0).getString("name"));
        assertFalse(result.getJsonObject(0).containsKey("fields"));
    }

    @Test
    @SystemProperty(key = "dataverse.lang.directory", value = "")
    void preservesFieldDefinitionsAndControlledVocabularyValues() {
        JsonObject original = JsonPrinter.json(title).build();
        JsonObject translated = JsonPrinter.json(title, null, Locale.forLanguageTag("fr-CA")).build();

        for (String key : original.keySet()) {
            if (!List.of("displayName", "title", "description", "watermark").contains(key)) {
                assertEquals(original.get(key), translated.get(key), key);
            }
        }
        assertEquals("original-value", translated.getJsonArray("controlledVocabularyValues").getString(0));
    }

    @Test
    @SystemProperty(key = "dataverse.lang.directory", value = "")
    void preservesExistingResponseWithoutLocale() {
        JsonObject original = JsonPrinter.json(block).build();
        JsonObject withoutLocale = JsonPrinter.json(block, false, null, null, null).build();

        assertEquals(original, withoutLocale);
        assertEquals("Database block name", withoutLocale.getString("displayName"));
        JsonObject titleJson = withoutLocale.getJsonObject("fields").getJsonObject("title");
        assertEquals("Database title", titleJson.getString("title"));
        assertEquals("Database description", titleJson.getString("description"));
        assertEquals("Database watermark", titleJson.getString("watermark"));
    }

    @Test
    @SystemProperty(key = "dataverse.lang.directory", value = "")
    void fallsBackToDefaultBundleWhenLanguageIsUnavailable() {
        JsonObject result = JsonPrinter.json(title, null, Locale.forLanguageTag("zz-ZZ")).build();

        assertEquals("Title", result.getString("title"));
        assertEquals("Title description", result.getString("description"));
    }

    @Test
    @SystemProperty(key = "dataverse.lang.directory", value = "")
    void fallsBackToLanguageAndBaseBundlesWhenRegionalBundleIsUnavailable() {
        JsonObject result = JsonPrinter.json(block, false, null, null, Locale.forLanguageTag("fr-FR")).build();

        assertEquals("Bloc de test français", result.getString("displayName"));
        JsonObject titleJson = result.getJsonObject("fields").getJsonObject("title");
        assertEquals("Description française du titre", titleJson.getString("description"));
        assertEquals("Title", titleJson.getString("title"));
    }

    @Test
    @SystemProperty(key = "dataverse.lang.directory", value = "")
    void fallsBackToDatabaseWhenTranslationKeyIsUnavailable() {
        DatasetFieldType customField = field("customField", "Custom title");
        JsonObject result = JsonPrinter.json(customField, null, Locale.forLanguageTag("fr-CA")).build();

        assertEquals("Custom title", result.getString("title"));
        assertEquals("Custom title", result.getString("displayName"));
        assertEquals("Database description", result.getString("description"));
        assertEquals("Database watermark", result.getString("watermark"));
    }

    @Test
    @SystemProperty(key = "dataverse.lang.directory", value = "")
    void fallsBackToDatabaseForCustomBlockWithoutBundle() {
        block.setName("apiLocaleTestWithoutBundle");
        JsonObject result = JsonPrinter.json(block, false, null, null, Locale.forLanguageTag("fr-CA")).build();

        assertEquals("Database block name", result.getString("displayName"));
        assertEquals("Database title", result.getJsonObject("fields").getJsonObject("title").getString("title"));
    }

    @Test
    @SystemProperty(key = "dataverse.lang.directory", value = "")
    void keepsExplicitLocalesIndependent() {
        Locale defaultLocale = Locale.getDefault();
        JsonObject french = JsonPrinter.json(title, null, Locale.forLanguageTag("fr-CA")).build();
        JsonObject english = JsonPrinter.json(title, null, Locale.ENGLISH).build();
        JsonObject frenchAgain = JsonPrinter.json(title, null, Locale.forLanguageTag("fr-CA")).build();

        assertEquals("Titre", french.getString("title"));
        assertEquals("Title", english.getString("title"));
        assertEquals(french, frenchAgain);
        assertEquals(defaultLocale, Locale.getDefault());
    }

    @Test
    @SystemProperty(key = "dataverse.lang.directory", value = "")
    void usesInstalledLanguageBundle(@TempDir Path languageDirectory) throws IOException {
        Files.writeString(languageDirectory.resolve("apiLocaleTest_fr_CA.properties"),
                "datasetfieldtype.title.title=Titre installé\n");
        System.setProperty("dataverse.lang.directory", languageDirectory.toString());

        JsonObject result = JsonPrinter.json(title, null, Locale.forLanguageTag("fr-CA")).build();

        assertEquals("Titre installé", result.getString("title"));
        assertEquals("Database description", result.getString("description"));
    }

    private DatasetFieldType field(String name, String fieldTitle) {
        DatasetFieldType field = new DatasetFieldType(name, DatasetFieldType.FieldType.TEXT, false);
        field.setId((long) name.hashCode());
        field.setDisplayOrder(name.hashCode());
        field.setTitle(fieldTitle);
        field.setMetadataBlock(block);
        field.setDescription("Database description");
        field.setWatermark("Database watermark");
        field.setDisplayFormat(":#VALUE");
        field.setDisplayOnCreate(true);
        return field;
    }
}
