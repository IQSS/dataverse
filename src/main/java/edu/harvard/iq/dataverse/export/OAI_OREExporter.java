package edu.harvard.iq.dataverse.export;

import com.google.auto.service.AutoService;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.export.ExportException;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldType;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;

@AutoService(Exporter.class)
public class OAI_OREExporter implements Exporter {

	private static final Logger logger = Logger.getLogger(OAI_OREExporter.class.getCanonicalName());

	public static final String NAME = "OAI_ORE";

	@Override
	public void exportDataset(DatasetVersion version, JsonObject json, OutputStream outputStream)
			throws ExportException {
		try {

			Dataset dataset = version.getDataset();
			String id = dataset.getGlobalId();
			// json.getString("protocol") + ":" + json.getString("authority") + "/" +
			// json.getString("identifier");
			JsonArrayBuilder fileArray = Json.createArrayBuilder();

			JsonArrayBuilder contextArray = Json.createArrayBuilder()
					.add("http://www.openarchives.org/ore/0.9/context.json")
					// .add("https://w3id.org/ore/context") - JSON-LD Playground doesn't like this
					// due to redirects
					.add("http://schema.org");
			// Note schema.org is open and accepts any terms into its vocab whether or not
			// they are defined - need to make sure that keys such as "restricted" that are
			// defined by Dataverse have a context entry (could also use a prefix to make
			// this clearer)
			JsonObjectBuilder localContextObject = Json.createObjectBuilder();

			JsonObjectBuilder aggBuilder = Json.createObjectBuilder();
			List<DatasetField> fields = version.getDatasetFields();
			for (DatasetField field : fields) {
				if (!field.isEmpty()) {
					DatasetFieldType dfType = field.getDatasetFieldType();
					// Add context entry
					localContextObject.add(dfType.getTitle(), SystemConfig.getDataverseSiteUrlStatic() + "/schema/"
							+ dfType.getMetadataBlock().getName() + "#" + dfType.getName());

					JsonArrayBuilder vals = Json.createArrayBuilder();
					if (!dfType.isCompound()) {
						for (String val : field.getValues()) {
							vals.add(val);
						}
					} else {
						// Needs to be recursive (as in JsonPrinter?)
						for (DatasetFieldCompoundValue dscv : field.getDatasetFieldCompoundValues()) {
							// compound values are of different types
							JsonObjectBuilder child = Json.createObjectBuilder();

							for (DatasetField dsf : dscv.getChildDatasetFields()) {
								DatasetFieldType dsft = dsf.getDatasetFieldType();
								// which may have multiple values
								if (!dsf.isEmpty()) {
									// Add context entry - also needs to recurse
									localContextObject.add(dsft.getTitle(),
											SystemConfig.getDataverseSiteUrlStatic() + "/schema/"
													+ dfType.getMetadataBlock().getName() + "/" + dfType.getName() + "#"
													+ dsft.getName());

									JsonArrayBuilder childVals = Json.createArrayBuilder();
									for (String val : dsf.getValues()) {
										childVals.add(val);
									}
									child.add(dsft.getTitle(), childVals);
								}
							}
							vals.add(child);
						}
					}
					// Add value, suppress array when only one value
					JsonArray valArray = vals.build();
					aggBuilder.add(dfType.getTitle(), (valArray.size() != 1) ? valArray : valArray.get(0));
				}
			}

			aggBuilder.add("@id", id).add("@type", Json.createArrayBuilder().add("Aggregation").add("Dataset"))
					.add("version", version.getFriendlyVersionNumber())
					.add("datePublished", dataset.getPublicationDateFormattedYYYYMMDD()).add("name", version.getTitle())
					.add("dateModified", version.getLastUpdateTime().toString());
			
			aggBuilder.add("includedInDataCatalog", dataset.getDataverseContext().getDisplayName());

			JsonArrayBuilder aggResArrayBuilder = Json.createArrayBuilder();
			for (FileMetadata fmd : version.getFileMetadatas()) {
				DataFile df = fmd.getDataFile();
				JsonObjectBuilder aggRes = Json.createObjectBuilder();
				addIfNotNull(aggRes, "description", fmd.getDescription());
				if (fmd.getDescription() == null)
					addIfNotNull(aggRes, "description", df.getDescription());
				addIfNotNull(aggRes, "name", fmd.getLabel()); // "label" is the filename
				localContextObject.add("restricted",
						SystemConfig.getDataverseSiteUrlStatic() + "/schema/core#restricted");
				addIfNotNull(aggRes, "restricted", fmd.isRestricted());
				localContextObject.add("directoryLabel",
						SystemConfig.getDataverseSiteUrlStatic() + "/schema/core#directoryLabel");
				addIfNotNull(aggRes, "directoryLabel", fmd.getDirectoryLabel());
				addIfNotNull(aggRes, "version", fmd.getVersion());
				localContextObject.add("datasetVersionId",
						SystemConfig.getDataverseSiteUrlStatic() + "/schema/core#datasetVersionId");
				addIfNotNull(aggRes, "datasetVersionId", fmd.getDatasetVersion().getId());
				JsonArray catArray = null;
				if (fmd != null) {
					List<String> categories = fmd.getCategoriesByName();
					if (categories.size() > 0) {
						JsonArrayBuilder jab = Json.createArrayBuilder();
						for (String s : categories) {
							jab.add(s);
						}
						catArray = jab.build();
					}
				}
				localContextObject.add("categories",
						SystemConfig.getDataverseSiteUrlStatic() + "/schema/core#categories");
				addIfNotNull(aggRes, "categories", catArray);
				// Will be file DOI eventually
				String fileId = SystemConfig.getDataverseSiteUrlStatic() + "/api/access/datafile/" + df.getId();
				aggRes.add("@id", fileId);
				fileArray.add(fileId);

				aggRes.add("@type", "AggregatedResource");
				addIfNotNull(aggRes, "fileFormat", df.getContentType());
				localContextObject.add("filesize", SystemConfig.getDataverseSiteUrlStatic() + "/schema/core#filesize");
				addIfNotNull(aggRes, "filesize", df.getFilesize());
				// .add("released", df.isReleased())
				// .add("restricted", df.isRestricted())
				localContextObject.add("storageIdentifier",
						SystemConfig.getDataverseSiteUrlStatic() + "/schema/core#storageidentifier");
				addIfNotNull(aggRes, "storageIdentifier", df.getStorageIdentifier());
				localContextObject.add("originalFileFormat",
						SystemConfig.getDataverseSiteUrlStatic() + "/schema/core#originalfileformat");
				addIfNotNull(aggRes, "originalFileFormat", df.getOriginalFileFormat());
				localContextObject.add("originalFormatLabel",
						SystemConfig.getDataverseSiteUrlStatic() + "/schema/core#originalformatlabel");
				addIfNotNull(aggRes, "originalFormatLabel", df.getOriginalFormatLabel());
				localContextObject.add("UNF", SystemConfig.getDataverseSiteUrlStatic() + "/schema/core#unf");
				addIfNotNull(aggRes, "UNF", df.getUnf());
				// ---------------------------------------------
				// For file replace: rootDataFileId, previousDataFileId
				// ---------------------------------------------
				localContextObject.add("rootDataFileId",
						SystemConfig.getDataverseSiteUrlStatic() + "/schema/core#rootdatafileid");
				addIfNotNull(aggRes, "rootDataFileId", df.getRootDataFileId());
				localContextObject.add("previousDataFileId",
						SystemConfig.getDataverseSiteUrlStatic() + "/schema/core#previousdatafileid");
				addIfNotNull(aggRes, "previousDataFileId", df.getPreviousDataFileId());
				localContextObject.add("MD5", SystemConfig.getDataverseSiteUrlStatic() + "/schema/core#md5");
				JsonObject checksum = null;
				if (df.getChecksumType() != null && df.getChecksumValue() != null) {
					checksum = Json.createObjectBuilder().add("@type", df.getChecksumType().toString())
							.add("@value", df.getChecksumValue()).build();
					localContextObject.add("checksum",
							SystemConfig.getDataverseSiteUrlStatic() + "/schema/core#checksum");
					aggRes.add("checksum", checksum);
				}
				JsonArray tabTags = null;
				JsonArrayBuilder jab = JsonPrinter.getTabularFileTags(df);
				if (jab != null) {
					tabTags = jab.build();
				}
				addIfNotNull(aggRes, "tabularTags", tabTags);

				aggResArrayBuilder.add(aggRes.build());
			}

			JsonObject oremap = Json.createObjectBuilder().add("dateCreated", LocalDate.now().toString())
					.add("creator", ResourceBundle.getBundle("Bundle").getString("institution.name"))
					.add("@type", "ResourceMap")
					.add("@id",
							SystemConfig.getDataverseSiteUrlStatic() + "/api/datasets/export?exporter="
									+ getProviderName() + "&persistentId=" + id)

					.add("describes",
							aggBuilder.add("aggregates", aggResArrayBuilder.build()).add("hasPart", fileArray.build())
									.build())

					.add("@context", contextArray.add(localContextObject.build()).build()).build();
			logger.info(oremap.toString());

			try {
				outputStream.write(oremap.toString().getBytes("UTF8"));
				outputStream.flush();
			} catch (IOException ex) {
				logger.info("IOException calling outputStream.write: " + ex);
			}
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
		return ResourceBundle.getBundle("Bundle").getString("dataset.exportBtn.itemLabel.oai_ore") != null
				? ResourceBundle.getBundle("Bundle").getString("dataset.exportBtn.itemLabel.oai_ore")
				: "OAI_ORE";
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
		throw new ExportException(OAI_OREExporter.class.getSimpleName() + ": not an XML format.");
	}

	@Override
	public String getXMLSchemaLocation() throws ExportException {
		throw new ExportException(OAI_OREExporter.class.getSimpleName() + ": not an XML format.");
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
