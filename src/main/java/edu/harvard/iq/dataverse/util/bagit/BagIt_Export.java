package edu.harvard.iq.dataverse.util.bagit;

import com.google.gson.JsonParser;

import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import com.google.gson.JsonObject;

public class BagIt_Export {

	public static final String NAME = "BagIt";

	public static void exportDatasetVersionAsBag(DatasetVersion version, ApiToken apiToken, OutputStream outputStream)
			throws Exception {

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		new OREMap_Export(version).exportOREMap(out);
		JsonParser jsonParser = new JsonParser();
		JsonObject oremap = (JsonObject) jsonParser.parse(out.toString("UTF-8"));

		BagGenerator bagger = new BagGenerator(oremap);
		bagger.setAuthenticationKey(apiToken.getTokenString());
		bagger.setIgnoreHashes(false); // true would force sha256 computation
		bagger.generateBag(outputStream);
	}
}
