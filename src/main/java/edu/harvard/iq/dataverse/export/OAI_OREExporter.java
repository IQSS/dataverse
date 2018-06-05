package edu.harvard.iq.dataverse.export;

import com.google.auto.service.AutoService;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.TermsOfUseAndAccess;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.export.ExportException;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.json.JsonLDTerm;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldType;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

@AutoService(Exporter.class)
public class OAI_OREExporter implements Exporter {

	private static final Logger logger = Logger.getLogger(OAI_OREExporter.class.getCanonicalName());

	public static final String NAME = "OAI_ORE";

	private Map<String, String> localContext = new TreeMap<String, String>();

	@Override
	public void exportDataset(DatasetVersion version, JsonObject json, OutputStream outputStream)
			throws ExportException {
		try {

			Dataset dataset = version.getDataset();
			String id = dataset.getGlobalId();
			// json.getString("protocol") + ":" + json.getString("authority") + "/" +
			// json.getString("identifier");
			JsonArrayBuilder fileArray = Json.createArrayBuilder();

			JsonObjectBuilder aggBuilder = Json.createObjectBuilder();
			List<DatasetField> fields = version.getDatasetFields();
			for (DatasetField field : fields) {
				if (!field.isEmpty()) {
					DatasetFieldType dfType = field.getDatasetFieldType();
					// Add context entry
					JsonLDTerm fieldName = new JsonLDTerm(dfType.getTitle(), SystemConfig.getDataverseSiteUrlStatic()
							+ "/schema/" + dfType.getMetadataBlock().getName() + "#" + dfType.getName());
					addToContextMap(fieldName);

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
									JsonLDTerm subFieldName = new JsonLDTerm(dsft.getTitle(),
											SystemConfig.getDataverseSiteUrlStatic() + "/schema/"
													+ dfType.getMetadataBlock().getName() + "/" + dfType.getName() + "#"
													+ dsft.getName());
									addToContextMap(subFieldName);
									JsonArrayBuilder childVals = Json.createArrayBuilder();
									// TBD: single value not sent as array?
									for (String val : dsf.getValues()) {
										childVals.add(val);
									}
									child.add(subFieldName.getLabel(), childVals);
								}
							}
							vals.add(child);
						}
					}
					// Add value, suppress array when only one value
					JsonArray valArray = vals.build();
					aggBuilder.add(fieldName.getLabel(), (valArray.size() != 1) ? valArray : valArray.get(0));
				}
			}

			aggBuilder.add("@id", id)
					.add("@type",
							Json.createArrayBuilder().add(JsonLDTerm.ore("Aggregation").getLabel())
									.add(JsonLDTerm.schemaOrg("Dataset").getLabel()))
					.add(JsonLDTerm.schemaOrg("version").getLabel(), version.getFriendlyVersionNumber())
					.add(JsonLDTerm.schemaOrg("datePublished").getLabel(),
							dataset.getPublicationDateFormattedYYYYMMDD())
					.add(JsonLDTerm.schemaOrg("name").getLabel(), version.getTitle())
					.add(JsonLDTerm.schemaOrg("dateModified").getLabel(), version.getLastUpdateTime().toString());

			TermsOfUseAndAccess terms = version.getTermsOfUseAndAccess();
			if (terms.getLicense() == TermsOfUseAndAccess.License.CC0) {
				aggBuilder.add(JsonLDTerm.schemaOrg("license").getLabel(),
						"https://creativecommons.org/publicdomain/zero/1.0/");
			} else {
				addIfNotNull(aggBuilder, JsonLDTerm.DVCore("termsOfUse"), terms.getTermsOfUse());
			}
			addIfNotNull(aggBuilder, JsonLDTerm.DVCore("confidentialityDeclaration"),
					terms.getConfidentialityDeclaration());
			addIfNotNull(aggBuilder, JsonLDTerm.DVCore("specialPermissions"), terms.getSpecialPermissions());
			addIfNotNull(aggBuilder, JsonLDTerm.DVCore("restrictions"), terms.getRestrictions());
			addIfNotNull(aggBuilder, JsonLDTerm.DVCore("citationRequirements"), terms.getCitationRequirements());
			addIfNotNull(aggBuilder, JsonLDTerm.DVCore("depositorRequirements"), terms.getDepositorRequirements());
			addIfNotNull(aggBuilder, JsonLDTerm.DVCore("conditions"), terms.getConditions());
			addIfNotNull(aggBuilder, JsonLDTerm.DVCore("disclaimer"), terms.getDisclaimer());

			JsonObjectBuilder fAccess = Json.createObjectBuilder();
			addIfNotNull(fAccess, JsonLDTerm.DVCore("termsOfAccess"), terms.getTermsOfAccess());
			addIfNotNull(fAccess, JsonLDTerm.DVCore("fileRequestAccess"), terms.isFileAccessRequest());
			addIfNotNull(fAccess, JsonLDTerm.DVCore("dataAccessPlace"), terms.getDataAccessPlace());
			addIfNotNull(fAccess, JsonLDTerm.DVCore("originalArchive"), terms.getOriginalArchive());
			addIfNotNull(fAccess, JsonLDTerm.DVCore("availabilityStatus"), terms.getAvailabilityStatus());
			addIfNotNull(fAccess, JsonLDTerm.DVCore("contactForAccess"), terms.getContactForAccess());
			addIfNotNull(fAccess, JsonLDTerm.DVCore("sizeOfCollection"), terms.getSizeOfCollection());
			addIfNotNull(fAccess, JsonLDTerm.DVCore("studyCompletion"), terms.getStudyCompletion());
			JsonObject fAccessObject = fAccess.build();
			if (!fAccessObject.isEmpty()) {
				aggBuilder.add(JsonLDTerm.DVCore("fileTermsOfAccess").getLabel(), fAccessObject);
			}

			aggBuilder.add(JsonLDTerm.schemaOrg("includedInDataCatalog").getLabel(),
					dataset.getDataverseContext().getDisplayName());

			JsonArrayBuilder aggResArrayBuilder = Json.createArrayBuilder();

			for (FileMetadata fmd : version.getFileMetadatas()) {
				DataFile df = fmd.getDataFile();
				JsonObjectBuilder aggRes = Json.createObjectBuilder();

				if (fmd.getDescription() != null) {
					aggRes.add(JsonLDTerm.schemaOrg("description").getLabel(), fmd.getDescription());
				} else {
					addIfNotNull(aggRes, JsonLDTerm.schemaOrg("description"), df.getDescription());
				}
				addIfNotNull(aggRes, JsonLDTerm.schemaOrg("name"), fmd.getLabel()); // "label" is the filename
				addIfNotNull(aggRes, JsonLDTerm.DVCore("restricted"), fmd.isRestricted());
				addIfNotNull(aggRes, JsonLDTerm.DVCore("directoryLabel"), fmd.getDirectoryLabel());
				addIfNotNull(aggRes, JsonLDTerm.schemaOrg("version"), fmd.getVersion());
				addIfNotNull(aggRes, JsonLDTerm.DVCore("datasetVersionId"), fmd.getDatasetVersion().getId());
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
				addIfNotNull(aggRes, JsonLDTerm.DVCore("categories"), catArray);
				// Will be file DOI eventually
				String fileId = SystemConfig.getDataverseSiteUrlStatic() + "/api/access/datafile/" + df.getId();
				aggRes.add("@id", fileId);
				fileArray.add(fileId);

				aggRes.add("@type", JsonLDTerm.ore("AggregatedResource").getLabel());
				addIfNotNull(aggRes, JsonLDTerm.schemaOrg("fileFormat"), df.getContentType());
				addIfNotNull(aggRes, JsonLDTerm.DVCore("filesize"), df.getFilesize());
				addIfNotNull(aggRes, JsonLDTerm.DVCore("storageIdentifier"), df.getStorageIdentifier());
				addIfNotNull(aggRes, JsonLDTerm.DVCore("originalFileFormat"), df.getOriginalFileFormat());
				addIfNotNull(aggRes, JsonLDTerm.DVCore("originalFormatLabel"), df.getOriginalFormatLabel());
				addIfNotNull(aggRes, JsonLDTerm.DVCore("UNF"), df.getUnf());
				// ---------------------------------------------
				// For file replace: rootDataFileId, previousDataFileId
				// ---------------------------------------------
				addIfNotNull(aggRes, JsonLDTerm.DVCore("rootDataFileId"), df.getRootDataFileId());
				addIfNotNull(aggRes, JsonLDTerm.DVCore("previousDataFileId"), df.getPreviousDataFileId());
				JsonObject checksum = null;
				if (df.getChecksumType() != null && df.getChecksumValue() != null) {
					checksum = Json.createObjectBuilder()
							.add("@type", JsonLDTerm.DVCore(df.getChecksumType().toString()).getLabel())
							.add("@value", df.getChecksumValue()).build();
					aggRes.add(JsonLDTerm.DVCore("checksum").getLabel(), checksum);
				}
				JsonArray tabTags = null;
				JsonArrayBuilder jab = JsonPrinter.getTabularFileTags(df);
				if (jab != null) {
					tabTags = jab.build();
				}
				addIfNotNull(aggRes, JsonLDTerm.DVCore("tabularTags"), tabTags);

				aggResArrayBuilder.add(aggRes.build());
			}
			// For namespaces using a prefix (like these two) we need to have at least one
			// term from that namespace added to the context
			addToContextMap(JsonLDTerm.ore("describes"));
			addToContextMap(JsonLDTerm.dcTerms("modified"));
			JsonObjectBuilder contextBuilder = Json.createObjectBuilder();
			for (Entry<String, String> e : localContext.entrySet()) {
				contextBuilder.add(e.getKey(), e.getValue());
			}
			JsonObject oremap = Json.createObjectBuilder()
					.add(JsonLDTerm.dcTerms("modified").getLabel(), LocalDate.now().toString())
					.add(JsonLDTerm.dcTerms("creator").getLabel(),
							ResourceBundle.getBundle("Bundle").getString("institution.name"))
					.add("@type", JsonLDTerm.ore("ResourceMap").getLabel())
					.add("@id",
							SystemConfig.getDataverseSiteUrlStatic() + "/api/datasets/export?exporter="
									+ getProviderName() + "&persistentId=" + id)

					.add(JsonLDTerm.ore("describes").getLabel(),
							aggBuilder.add("aggregates", aggResArrayBuilder.build()).add("hasPart", fileArray.build())
									.build())

					.add("@context", contextBuilder.build()).build();
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

	private void addIfNotNull(JsonObjectBuilder builder, JsonLDTerm key, String value) {
		if (value != null) {
			builder.add(key.getLabel(), value);
			addToContextMap(key);
		}
	}

	private void addIfNotNull(JsonObjectBuilder builder, JsonLDTerm key, JsonValue value) {
		if (value != null) {
			builder.add(key.getLabel(), value);
			addToContextMap(key);
		}
	}

	private void addIfNotNull(JsonObjectBuilder builder, JsonLDTerm key, Boolean value) {
		if (value != null) {
			builder.add(key.getLabel(), value);
			addToContextMap(key);
		}
	}

	private void addIfNotNull(JsonObjectBuilder builder, JsonLDTerm key, Long value) {
		if (value != null) {
			builder.add(key.getLabel(), value);
			addToContextMap(key);
		}
	}

	private void addToContextMap(JsonLDTerm key) {
		localContext.putIfAbsent(key.getContextLabel(), key.getUrl());
	}

}
