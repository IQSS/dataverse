package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.dataset.DatasetTypeServiceBean;
import edu.harvard.iq.dataverse.engine.command.impl.ListMetadataBlocksCommand;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.testing.SystemProperty;
import jakarta.json.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetadataLocaleApiTest {

    @Mock
    private EjbDataverseEngine engineSvc;
    @Mock
    private DataverseServiceBean dataverseSvc;
    @Mock
    private DatasetTypeServiceBean datasetTypeSvc;
    @Mock
    private HttpServletRequest httpRequest;
    @Mock
    private ContainerRequestContext requestContext;
    @InjectMocks
    private Dataverses target;

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"pt-BR", "pt_BR", "en", "zz-ZZ"})
    @SystemProperty(key = "dataverse.lang.directory", value = "")
    void acceptsOptionalLocaleAndTranslatesBlockNames(String locale) throws Exception {
        Dataverse dataverse = new Dataverse();
        dataverse.setId(1L);
        dataverse.setAlias("root");
        when(dataverseSvc.findByAlias("root")).thenReturn(dataverse);
        when(requestContext.getProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_USER)).thenReturn(new AuthenticatedUser());
        MetadataBlock block = new MetadataBlock();
        block.setId(1L);
        block.setName("apiLocaleTest");
        block.setDisplayName("Database block name");
        block.setDatasetFieldTypes(List.of());
        when(engineSvc.submit(any(ListMetadataBlocksCommand.class))).thenReturn(List.of(block));

        try (Response response = target.listMetadataBlocks(requestContext, "root", false, false, null, locale)) {
            assertEquals(200, response.getStatus());
            JsonObject result = ((JsonObject) response.getEntity()).getJsonArray("data").getJsonObject(0);
            String expected = locale == null ? "Database block name"
                    : locale.startsWith("pt") ? "Bloco de teste" : "Test block";
            assertEquals(expected, result.getString("displayName"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "pt--BR", "en_US.UTF-8", "-"})
    @SystemProperty(key = "dataverse.lang.directory", value = "")
    void rejectsMalformedLocaleWithBundleMessage(String locale) {
        try (Response response = target.listMetadataBlocks(requestContext, "root", false, false, null, locale)) {
            assertEquals(400, response.getStatus());
            assertEquals(BundleUtil.getStringFromBundle("dataverses.api.metadatablocks.locale.invalid"),
                    ((JsonObject) response.getEntity()).getString("message"));
        }
        verifyNoInteractions(engineSvc);
    }

    @ParameterizedTest
    @ValueSource(strings = {"und", "x-private"})
    @SystemProperty(key = "dataverse.lang.directory", value = "")
    void rejectsLocaleWithoutLanguageWithBundleMessage(String locale) {
        try (Response response = target.listMetadataBlocks(requestContext, "root", false, false, null, locale)) {
            assertEquals(400, response.getStatus());
            assertEquals(BundleUtil.getStringFromBundle("dataverses.api.metadatablocks.locale.languageRequired"),
                    ((JsonObject) response.getEntity()).getString("message"));
        }
        verifyNoInteractions(engineSvc);
    }

    @ParameterizedTest
    @CsvSource({"pt--BR, Idioma inválido.", "und, Informe um idioma."})
    @SystemProperty(key = "dataverse.lang.directory", value = "")
    void usesInstalledBundleForLocaleErrors(String locale, String expectedMessage,
            @TempDir Path languageDirectory) throws IOException {
        Files.writeString(languageDirectory.resolve("Bundle.properties"),
                "dataverses.api.metadatablocks.locale.invalid=Idioma inválido.\n"
                + "dataverses.api.metadatablocks.locale.languageRequired=Informe um idioma.\n");
        System.setProperty("dataverse.lang.directory", languageDirectory.toString());

        try (Response response = target.listMetadataBlocks(requestContext, "root", false, false, null, locale)) {
            assertEquals(400, response.getStatus());
            assertEquals(expectedMessage, ((JsonObject) response.getEntity()).getString("message"));
        }
    }
}
