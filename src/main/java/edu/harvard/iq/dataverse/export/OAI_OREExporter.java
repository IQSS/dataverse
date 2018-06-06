package edu.harvard.iq.dataverse.export;

import com.google.auto.service.AutoService;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.TermsOfUseAndAccess;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.export.ExportException;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.json.JsonLDNamespace;
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
			//Add namespaces we'll use to Context
			localContext.putIfAbsent(JsonLDNamespace.ore.getPrefix(), JsonLDNamespace.ore.getUrl());
			localContext.putIfAbsent(JsonLDNamespace.dcterms.getPrefix(), JsonLDNamespace.dcterms.getUrl());
			localContext.putIfAbsent(JsonLDNamespace.dvcore.getPrefix(), JsonLDNamespace.dvcore.getUrl());
			localContext.putIfAbsent(JsonLDNamespace.schema.getPrefix(), JsonLDNamespace.schema.getUrl());

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
					JsonLDNamespace blockNamespace = new JsonLDNamespace(dfType.getMetadataBlock().getName(), SystemConfig.getDataverseSiteUrlStatic()
							+ "/schema/" + dfType.getMetadataBlock().getName() + "#");
					// Add context entry for metadata block
					localContext.putIfAbsent(blockNamespace.getPrefix(), blockNamespace.getUrl());
					
					JsonLDTerm fieldName = new JsonLDTerm(blockNamespace, dfType.getTitle());

					JsonArrayBuilder vals = Json.createArrayBuilder();
					if (!dfType.isCompound()) {
						for (String val : field.getValues()) {
							vals.add(val);
						}
					} else {
						// Needs to be recursive (as in JsonPrinter?)

						JsonLDNamespace fieldNamespace = new JsonLDNamespace(dfType.getName(), SystemConfig.getDataverseSiteUrlStatic()
								+ "/schema/" + dfType.getMetadataBlock().getName() + "/" + dfType.getName() + "#");
						// Add context entry for metadata block
						localContext.putIfAbsent(fieldNamespace.getPrefix(), fieldNamespace.getUrl());

						for (DatasetFieldCompoundValue dscv : field.getDatasetFieldCompoundValues()) {
							// compound values are of different types
							JsonObjectBuilder child = Json.createObjectBuilder();

							for (DatasetField dsf : dscv.getChildDatasetFields()) {
								DatasetFieldType dsft = dsf.getDatasetFieldType();
								// which may have multiple values
								if (!dsf.isEmpty()) {
									// Add context entry - also needs to recurse
									JsonLDTerm subFieldName = new JsonLDTerm(fieldNamespace, dsft.getTitle());

									List<String> values = dsf.getValues();
									if (values.size() > 1) {
										JsonArrayBuilder childVals = Json.createArrayBuilder();

										for (String val : dsf.getValues()) {
											childVals.add(val);
										}
										child.add(subFieldName.getLabel(), childVals);
									} else {
										child.add(subFieldName.getLabel(), values.get(0));
									}
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
				addIfNotNull(aggBuilder, JsonLDTerm.termsOfUse, terms.getTermsOfUse());
			}
			addIfNotNull(aggBuilder, JsonLDTerm.confidentialityDeclaration,
					terms.getConfidentialityDeclaration());
			addIfNotNull(aggBuilder, JsonLDTerm.specialPermissions, terms.getSpecialPermissions());
			addIfNotNull(aggBuilder, JsonLDTerm.restrictions, terms.getRestrictions());
			addIfNotNull(aggBuilder, JsonLDTerm.citationRequirements, terms.getCitationRequirements());
			addIfNotNull(aggBuilder, JsonLDTerm.depositorRequirements, terms.getDepositorRequirements());
			addIfNotNull(aggBuilder, JsonLDTerm.conditions, terms.getConditions());
			addIfNotNull(aggBuilder, JsonLDTerm.disclaimer, terms.getDisclaimer());

			JsonObjectBuilder fAccess = Json.createObjectBuilder();
			addIfNotNull(fAccess, JsonLDTerm.termsOfAccess, terms.getTermsOfAccess());
			addIfNotNull(fAccess, JsonLDTerm.fileRequestAccess, terms.isFileAccessRequest());
			addIfNotNull(fAccess, JsonLDTerm.dataAccessPlace, terms.getDataAccessPlace());
			addIfNotNull(fAccess, JsonLDTerm.originalArchive, terms.getOriginalArchive());
			addIfNotNull(fAccess, JsonLDTerm.availabilityStatus, terms.getAvailabilityStatus());
			addIfNotNull(fAccess, JsonLDTerm.contactForAccess, terms.getContactForAccess());
			addIfNotNull(fAccess, JsonLDTerm.sizeOfCollection, terms.getSizeOfCollection());
			addIfNotNull(fAccess, JsonLDTerm.studyCompletion, terms.getStudyCompletion());
			JsonObject fAccessObject = fAccess.build();
			if (!fAccessObject.isEmpty()) {
				aggBuilder.add(JsonLDTerm.fileTermsOfAccess.getLabel(), fAccessObject);
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
				addIfNotNull(aggRes, JsonLDTerm.restricted, fmd.isRestricted());
				addIfNotNull(aggRes, JsonLDTerm.directoryLabel, fmd.getDirectoryLabel());
				addIfNotNull(aggRes, JsonLDTerm.schemaOrg("version"), fmd.getVersion());
				addIfNotNull(aggRes, JsonLDTerm.datasetVersionId, fmd.getDatasetVersion().getId());
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
				addIfNotNull(aggRes, JsonLDTerm.categories, catArray);
				// Will be file DOI eventually
				String fileId = SystemConfig.getDataverseSiteUrlStatic() + "/api/access/datafile/" + df.getId();
				aggRes.add("@id", fileId);
				fileArray.add(fileId);

				aggRes.add("@type", JsonLDTerm.ore("AggregatedResource").getLabel());
				addIfNotNull(aggRes, JsonLDTerm.schemaOrg("fileFormat"), df.getContentType());
				addIfNotNull(aggRes, JsonLDTerm.filesize, df.getFilesize());
				addIfNotNull(aggRes, JsonLDTerm.storageIdentifier, df.getStorageIdentifier());
				addIfNotNull(aggRes, JsonLDTerm.originalFileFormat, df.getOriginalFileFormat());
				addIfNotNull(aggRes, JsonLDTerm.originalFormatLabel, df.getOriginalFormatLabel());
				addIfNotNull(aggRes, JsonLDTerm.UNF, df.getUnf());
				// ---------------------------------------------
				// For file replace: rootDataFileId, previousDataFileId
				// ---------------------------------------------
				addIfNotNull(aggRes, JsonLDTerm.rootDataFileId, df.getRootDataFileId());
				addIfNotNull(aggRes, JsonLDTerm.previousDataFileId, df.getPreviousDataFileId());
				JsonObject checksum = null;
				if (df.getChecksumType() != null && df.getChecksumValue() != null) {
					checksum = Json.createObjectBuilder()
							.add("@type", df.getChecksumType().toString())
							.add("@value", df.getChecksumValue()).build();
					aggRes.add(JsonLDTerm.checksum.getLabel(), checksum);
				}
				JsonArray tabTags = null;
				JsonArrayBuilder jab = JsonPrinter.getTabularFileTags(df);
				if (jab != null) {
					tabTags = jab.build();
				}
				addIfNotNull(aggRes, JsonLDTerm.tabularTags, tabTags);

				aggResArrayBuilder.add(aggRes.build());
			}
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
							aggBuilder.add(JsonLDTerm.ore("aggregates").getLabel(), aggResArrayBuilder.build()).add(JsonLDTerm.dcTerms("hasPart").getLabel(), fileArray.build())
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
		if(!key.inNamespace()) {
			localContext.putIfAbsent(key.getLabel(), key.getUrl());
		}
	}

}
