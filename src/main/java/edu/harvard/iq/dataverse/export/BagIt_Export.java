package edu.harvard.iq.dataverse.export;

import com.google.gson.JsonParser;

import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BagGenerator;
import edu.harvard.iq.dataverse.Dataset;

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

        Dataset dataset = version.getDataset();
        InputStream mapInputStream = ExportService.getInstance(settingsService).getExport(dataset,
                OAI_OREExporter.NAME);
        JsonParser jsonParser = new JsonParser();
        JsonObject oremap = (JsonObject) jsonParser.parse(new InputStreamReader(mapInputStream, "UTF-8"));

        BagGenerator bagger = new BagGenerator(oremap);
        bagger.setAuthenticationKey(apiToken.getTokenString());
        bagger.setIgnoreHashes(false); // true would force sha256 computation
        bagger.generateBag(outputStream);
    }
}
