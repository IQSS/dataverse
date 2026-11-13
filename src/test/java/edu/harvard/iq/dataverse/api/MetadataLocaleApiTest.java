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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
    @CsvSource(value = {
        "NULL|Database block name",
        "''|Database block name",
        "fr-CA|Bloc de test",
        "fr|Bloc de test français",
        "en|Test block",
        "de|Test block",
        "zz-ZZ|Test block",
        "frCA|Test block",
        "fr-CAN|Test block",
        "und|Test block",
        "*|Test block",
        "fr-CA,fr;q=0.8,en;q=0.6|Bloc de test",
        "en;q=0.5,fr-CA;q=0.9|Bloc de test",
        "de,fr-CA;q=0.8|Test block"
    }, delimiter = '|', nullValues = "NULL")
    @SystemProperty(key = "dataverse.lang.directory", value = "")
    void acceptsLanguageHeaderAndTranslatesBlockNames(String acceptLanguage, String expectedName) throws Exception {
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

        try (Response response = target.listMetadataBlocks(requestContext, "root", false, false, null, acceptLanguage)) {
            assertEquals(200, response.getStatus());
            JsonObject result = ((JsonObject) response.getEntity()).getJsonArray("data").getJsonObject(0);
            assertEquals(expectedName, result.getString("displayName"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"fr#CA", "fr_CA", "fr--CA", "en_US.UTF-8", "-", "fr;q=invalid", "fr;q=1.5"})
    @SystemProperty(key = "dataverse.lang.directory", value = "")
    void rejectsMalformedLanguageHeaderWithBundleMessage(String acceptLanguage) {
        try (Response response = target.listMetadataBlocks(requestContext, "root", false, false, null, acceptLanguage)) {
            assertEquals(400, response.getStatus());
            assertEquals(BundleUtil.getStringFromBundle("dataverses.api.metadatablocks.acceptLanguage.invalid"),
                    ((JsonObject) response.getEntity()).getString("message"));
        }
        verifyNoInteractions(engineSvc);
    }

    @Test
    @SystemProperty(key = "dataverse.lang.directory", value = "")
    void usesInstalledBundleForMalformedHeaderError(@TempDir Path languageDirectory) throws IOException {
        Files.writeString(languageDirectory.resolve("Bundle.properties"),
                "dataverses.api.metadatablocks.acceptLanguage.invalid=En-tête Accept-Language invalide.\n");
        System.setProperty("dataverse.lang.directory", languageDirectory.toString());

        try (Response response = target.listMetadataBlocks(requestContext, "root", false, false, null, "fr#CA")) {
            assertEquals(400, response.getStatus());
            assertEquals("En-tête Accept-Language invalide.", ((JsonObject) response.getEntity()).getString("message"));
        }
    }
}
