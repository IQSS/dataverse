package edu.harvard.iq.dataverse.util.bagit;

import com.google.gson.JsonParser;

import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.export.OAI_OREExporter;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.Dataset;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import javax.ejb.EJB;
import com.google.gson.JsonObject;

public class BagIt_Export {

    @EJB
    SettingsServiceBean settingsService;

    public static final String NAME = "BagIt";

    public static void exportDatasetVersionAsBag(DatasetVersion version, ApiToken apiToken,
            SettingsServiceBean settingsService, OutputStream outputStream) throws Exception {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExportService.getInstance(settingsService).exportFormatToStream(version, OAI_OREExporter.NAME, out);
        JsonParser jsonParser = new JsonParser();
        JsonObject oremap = (JsonObject) jsonParser.parse(out.toString("UTF-8"));

        BagGenerator bagger = new BagGenerator(oremap);
        bagger.setAuthenticationKey(apiToken.getTokenString());
        bagger.setIgnoreHashes(false); // true would force sha256 computation
        bagger.generateBag(outputStream);
    }
}
