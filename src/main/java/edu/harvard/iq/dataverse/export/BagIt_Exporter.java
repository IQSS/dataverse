package edu.harvard.iq.dataverse.export;

import com.google.auto.service.AutoService;
import com.google.gson.JsonParser;

import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.export.ExportException;
import edu.harvard.iq.dataverse.util.BagGenerator;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldType;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@AutoService(Exporter.class)
public class BagIt_Exporter implements Exporter {

	@EJB
	SettingsServiceBean settingsService;

	private static final Logger logger = Logger.getLogger(BagIt_Exporter.class.getCanonicalName());

	public static final String NAME = "BagIt";

	@Override
	public void exportDataset(DatasetVersion version, javax.json.JsonObject json, OutputStream outputStream)
			throws ExportException {
		try {

			Dataset dataset = version.getDataset();
			InputStream mapInputStream = ExportService.getInstance(settingsService).getExport(dataset,
					OAI_OREExporter.NAME);
			JsonParser jsonParser = new JsonParser();
			JsonObject oremap = (JsonObject) jsonParser.parse(new InputStreamReader(mapInputStream, "UTF-8"));

			BagGenerator bagger = new BagGenerator(oremap);
			bagger.setIgnoreHashes(true); //Temporarily force sha256 computation
			bagger.generateBag(outputStream);
		} catch (Exception e) {
			logger.severe(e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	public String getProviderName() {
		return NAME;
	}

	@Override
	public String getDisplayName() {
		return ResourceBundle.getBundle("Bundle").getString("dataset.exportBtn.itemLabel.bagit") != null
				? ResourceBundle.getBundle("Bundle").getString("dataset.exportBtn.itemLabel.bagit")
				: "BagIt";
	}

	@Override
	public Boolean isXMLFormat() {
		return false;
	}

	@Override
	public Boolean isHarvestable() {
		// Defer harvesting because the current effort was estimated as a "2":
		// https://github.com/IQSS/dataverse/issues/3700
		return false;
	}

	@Override
	public Boolean isAvailableToUsers() {
		return true;
	}

	@Override
	public String getXMLNameSpace() throws ExportException {
		throw new ExportException(BagIt_Exporter.class.getSimpleName() + ": not an XML format.");
	}

	@Override
	public String getXMLSchemaLocation() throws ExportException {
		throw new ExportException(BagIt_Exporter.class.getSimpleName() + ": not an XML format.");
	}

	@Override
	public String getXMLSchemaVersion() throws ExportException {
		throw new ExportException(SchemaDotOrgExporter.class.getSimpleName() + ": not an XML format.");
	}

	@Override
	public void setParam(String name, Object value) {
		// this exporter doesn't need/doesn't currently take any parameters
	}

	private void addIfNotNull(JsonObjectBuilder builder, String key, String value) {
		if (value != null) {
			builder.add(key, value);
		}
	}

	private void addIfNotNull(JsonObjectBuilder builder, String key, JsonValue value) {
		if (value != null) {
			builder.add(key, value);
		}
	}

	private void addIfNotNull(JsonObjectBuilder builder, String key, Boolean value) {
		if (value != null) {
			builder.add(key, value);
		}
	}

	private void addIfNotNull(JsonObjectBuilder builder, String key, Long value) {
		if (value != null) {
			builder.add(key, value);
		}
	}

}
