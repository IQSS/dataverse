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
        JsonObject result = JsonPrinter.json(block, false, null, null, Locale.forLanguageTag("pt-BR")).build();

        assertEquals("Bloco de teste", result.getString("displayName"));
        JsonObject titleJson = result.getJsonObject("fields").getJsonObject("title");
        assertEquals("Título", titleJson.getString("title"));
        assertEquals("Título", titleJson.getString("displayName"));
        assertEquals("Descrição do título", titleJson.getString("description"));
        assertEquals("Digite o título", titleJson.getString("watermark"));
        JsonObject child = result.getJsonObject("fields").getJsonObject("author")
                .getJsonObject("childFields").getJsonObject("authorName");
        assertEquals("Nome", child.getString("title"));
        assertEquals("Autor Nome", child.getString("displayName"));
        assertEquals("Descrição do nome", child.getString("description"));
        assertEquals("Digite o nome", child.getString("watermark"));
    }

    @Test
    @SystemProperty(key = "dataverse.lang.directory", value = "")
    void translatesBriefBlockList() {
        JsonArray result = JsonPrinter.json(List.of(block), false, false, null, null,
                Locale.forLanguageTag("pt-BR")).build();

        assertEquals("Bloco de teste", result.getJsonObject(0).getString("displayName"));
        assertEquals("apiLocaleTest", result.getJsonObject(0).getString("name"));
        assertFalse(result.getJsonObject(0).containsKey("fields"));
    }

    @Test
    @SystemProperty(key = "dataverse.lang.directory", value = "")
    void preservesFieldDefinitionsAndControlledVocabularyValues() {
        JsonObject original = JsonPrinter.json(title).build();
        JsonObject translated = JsonPrinter.json(title, null, Locale.forLanguageTag("pt-BR")).build();

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
    void fallsBackToDatabaseWhenTranslationKeyIsUnavailable() {
        DatasetFieldType customField = field("customField", "Custom title");
        JsonObject result = JsonPrinter.json(customField, null, Locale.forLanguageTag("pt-BR")).build();

        assertEquals("Custom title", result.getString("title"));
        assertEquals("Custom title", result.getString("displayName"));
        assertEquals("Database description", result.getString("description"));
        assertEquals("Database watermark", result.getString("watermark"));
    }

    @Test
    @SystemProperty(key = "dataverse.lang.directory", value = "")
    void fallsBackToDatabaseForCustomBlockWithoutBundle() {
        block.setName("apiLocaleTestWithoutBundle");
        JsonObject result = JsonPrinter.json(block, false, null, null, Locale.forLanguageTag("pt-BR")).build();

        assertEquals("Database block name", result.getString("displayName"));
        assertEquals("Database title", result.getJsonObject("fields").getJsonObject("title").getString("title"));
    }

    @Test
    @SystemProperty(key = "dataverse.lang.directory", value = "")
    void keepsExplicitLocalesIndependent() {
        Locale defaultLocale = Locale.getDefault();
        JsonObject portuguese = JsonPrinter.json(title, null, Locale.forLanguageTag("pt-BR")).build();
        JsonObject english = JsonPrinter.json(title, null, Locale.ENGLISH).build();
        JsonObject portugueseAgain = JsonPrinter.json(title, null, Locale.forLanguageTag("pt-BR")).build();

        assertEquals("Título", portuguese.getString("title"));
        assertEquals("Title", english.getString("title"));
        assertEquals(portuguese, portugueseAgain);
        assertEquals(defaultLocale, Locale.getDefault());
    }

    @Test
    @SystemProperty(key = "dataverse.lang.directory", value = "")
    void usesInstalledLanguageBundle(@TempDir Path languageDirectory) throws IOException {
        Files.writeString(languageDirectory.resolve("apiLocaleTest_pt_BR.properties"),
                "datasetfieldtype.title.title=Título instalado\n");
        System.setProperty("dataverse.lang.directory", languageDirectory.toString());

        JsonObject result = JsonPrinter.json(title, null, Locale.forLanguageTag("pt-BR")).build();

        assertEquals("Título instalado", result.getString("title"));
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
