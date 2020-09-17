package edu.harvard.iq.dataverse.util.json;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.ws.rs.BadRequestException;
import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.api.JsonLdError;
import com.apicatalog.jsonld.document.JsonDocument;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldValue;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.MetadataBlockServiceBean;
import edu.harvard.iq.dataverse.TermsOfUseAndAccess;
import edu.harvard.iq.dataverse.DatasetVersion.VersionState;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.bagit.OREMap;

public class JSONLDUtil {

	private static final Logger logger = Logger.getLogger(JSONLDUtil.class.getCanonicalName());

	public static Map<String, String> populateContext(JsonValue json) {
		Map<String, String> context = new TreeMap<String, String>();
		if (json instanceof JsonArray) {
			logger.warning("Array @context not yet supported");
		} else {
			for (String key : ((JsonObject) json).keySet()) {
				context.putIfAbsent(key, ((JsonObject) json).getString(key));
			}
		}
		return context;
	}

	public static JsonObject getContext(Map<String, String> contextMap) {
		JsonObjectBuilder contextBuilder = Json.createObjectBuilder();
		for (Entry<String, String> e : contextMap.entrySet()) {
			contextBuilder.add(e.getKey(), e.getValue());
		}
		return contextBuilder.build();
	}

	public static Dataset updateDatasetFromJsonLD(Dataset ds, String jsonLDBody,
			MetadataBlockServiceBean metadataBlockSvc, DatasetFieldServiceBean datasetFieldSvc) {

		DatasetVersion dsv = new DatasetVersion();
		
		JsonObject jsonld = recontextualizeJsonLD(jsonLDBody, metadataBlockSvc);
		Optional<GlobalId> maybePid = GlobalId.parse(jsonld.getString("@id"));
        if (maybePid.isPresent()) {
            ds.setGlobalId(maybePid.get());
        } else {
            // unparsable PID passed. Terminate.
            throw new BadRequestException ("Cannot parse the @id '" + jsonld.getString("@id") + "'. Make sure it is in valid form - see Dataverse Native API documentation.");
        }
        
		dsv = updateDatasetVersionFromJsonLD(dsv, jsonld, metadataBlockSvc, datasetFieldSvc);
		dsv.setDataset(ds);

		List<DatasetVersion> versions = new ArrayList<>(1);
		versions.add(dsv);

		ds.setVersions(versions);
		try {
			logger.fine("Output dsv: " + new OREMap(dsv, false).getOREMap().toString());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ds;
	}

	public static DatasetVersion updateDatasetVersionFromJsonLD(DatasetVersion dsv, String jsonLDBody,
			MetadataBlockServiceBean metadataBlockSvc, DatasetFieldServiceBean datasetFieldSvc) {
		JsonObject jsonld = recontextualizeJsonLD(jsonLDBody, metadataBlockSvc);
		return updateDatasetVersionFromJsonLD(dsv, jsonld, metadataBlockSvc, datasetFieldSvc);
	}

	public static DatasetVersion updateDatasetVersionFromJsonLD(DatasetVersion dsv, JsonObject jsonld,
			MetadataBlockServiceBean metadataBlockSvc, DatasetFieldServiceBean datasetFieldSvc) {

		Map<String, DatasetFieldType> dsftMap = new TreeMap<String, DatasetFieldType>();

		List<MetadataBlock> mdbList = metadataBlockSvc.listMetadataBlocks();
		for (MetadataBlock mdb : mdbList) {
			if (mdb.getNamespaceUri() != null) {
				for (DatasetFieldType dsft : mdb.getDatasetFieldTypes()) {
					dsftMap.put(mdb.getName() + ":" + dsft.getName(), dsft);
				}
			}
		}
		// get existing ones?
		List<DatasetField> dsfl = dsv.getDatasetFields();
		Map<DatasetFieldType, DatasetField> fieldByTypeMap = new HashMap<DatasetFieldType, DatasetField>();
		for (DatasetField dsf : dsfl) {
			fieldByTypeMap.put(dsf.getDatasetFieldType(), dsf);
		}
		TermsOfUseAndAccess terms = new TermsOfUseAndAccess();
		for (String key : jsonld.keySet()) {
			if (!key.equals("@context")) {
				if (dsftMap.containsKey(key)) {
					DatasetFieldType dsft = dsftMap.get(key);

					DatasetField dsf = null;
					if (fieldByTypeMap.containsKey(dsft)) {
						dsf = fieldByTypeMap.get(dsft);
					} else {
						dsf = new DatasetField();
						dsfl.add(dsf);
					}
					dsf.setDatasetFieldType(dsft);
					// Todo - normalize object vs. array
					JsonValue val = jsonld.get(key);
					JsonArray valArray = null;
					if (val instanceof JsonArray) {
						if (!dsft.isAllowMultiples()) {
							throw new BadRequestException("Array for single value notsupported: " + dsft.getName());
						} else {
							valArray = (JsonArray) val;
						}
					} else {
						valArray = Json.createArrayBuilder().add(val).build();
					}

					if (dsft.isCompound()) {
						/*
						 * List<DatasetFieldCompoundValue> vals = parseCompoundValue(type, json,
						 * testType); for (DatasetFieldCompoundValue dsfcv : vals) {
						 * dsfcv.setParentDatasetField(ret); } dsf.setDatasetFieldCompoundValues(vals);
						 */
					} else if (dsft.isControlledVocabulary()) {

						List<ControlledVocabularyValue> vals = new LinkedList<>();
						for (JsonString strVal : valArray.getValuesAs(JsonString.class)) {
							String strValue = strVal.getString();
							ControlledVocabularyValue cvv = datasetFieldSvc
									.findControlledVocabularyValueByDatasetFieldTypeAndStrValue(dsft, strValue, true);
							if (cvv == null) {
								throw new BadRequestException("Unknown value for Controlled Vocab Field: "
										+ dsft.getName() + " : " + strValue);
							}
							// Only add value to the list if it is not a duplicate
							if (strValue.equals("Other")) {
								System.out.println("vals = " + vals + ", contains: " + vals.contains(cvv));
							}
							if (!vals.contains(cvv)) {
								vals.add(cvv);
								cvv.setDatasetFieldType(dsft);
							}
						}
						dsf.setControlledVocabularyValues(vals);

					} else {
						List<DatasetFieldValue> vals = new LinkedList<>();

						for (JsonString strVal : valArray.getValuesAs(JsonString.class)) {
							String strValue = strVal.getString();

							DatasetFieldValue datasetFieldValue = new DatasetFieldValue();
							if (valArray.size() > 1) {
								datasetFieldValue.setDisplayOrder(vals.size() - 1);
							}
							datasetFieldValue.setValue(strValue.trim());
							vals.add(datasetFieldValue);
						}
						dsf.setDatasetFieldValues(vals);

					}

					// assemble new terms, add to existing
					// multivalue?
					// compound?
					// merge with existing dv metadata
					// dsfl.add(dsf);
				} else {
					// Internal/non-metadatablock terms
					// Add metadata related to the Dataset/DatasetVersion

					// ("@id", id) - check is equal to existing globalID?
					// Add to 'md on original' ?
					// (JsonLDTerm.schemaOrg("version").getLabel(),
					// version.getFriendlyVersionNumber())
					// Citation metadata?
					// (JsonLDTerm.schemaOrg("datePublished").getLabel(),
					// dataset.getPublicationDateFormattedYYYYMMDD())
					// (JsonLDTerm.schemaOrg("name").getLabel())
					// (JsonLDTerm.schemaOrg("dateModified").getLabel())

					// Todo - handle non-CC0 licenses, without terms as an alternate field.
					if(key.equals(JsonLDTerm.schemaOrg("datePublished").getLabel())) {
						dsv.setVersionState(VersionState.RELEASED);
                    } else if(key.equals(JsonLDTerm.schemaOrg("version").getLabel())) {
                    	String friendlyVersion = jsonld.getString(JsonLDTerm.schemaOrg("version").getLabel());
                    	int index = friendlyVersion.indexOf(".");
                    	if(index>0) {
                          dsv.setVersionNumber(Long.parseLong(friendlyVersion.substring(0, index)));
                          dsv.setMinorVersionNumber(Long.parseLong(friendlyVersion.substring(index+1)));
                    	}
                    } else if (key.equals(JsonLDTerm.schemaOrg("license").getLabel())) {
						if (jsonld.getString(JsonLDTerm.schemaOrg("license").getLabel())
								.equals("https://creativecommons.org/publicdomain/zero/1.0/")) {
							terms.setLicense(TermsOfUseAndAccess.defaultLicense);
						} else {
							terms.setLicense(TermsOfUseAndAccess.License.NONE);
						}
					} else if(key.equals(JsonLDTerm.termsOfUse.getLabel())) {
							terms.setTermsOfUse(jsonld.getString(JsonLDTerm.termsOfUse.getLabel()));
					} else if (key.equals(JsonLDTerm.confidentialityDeclaration.getLabel())) {
						terms.setConfidentialityDeclaration(
								jsonld.getString(JsonLDTerm.confidentialityDeclaration.getLabel()));
					} else if (key.equals(JsonLDTerm.specialPermissions.getLabel())) {
						terms.setConfidentialityDeclaration(jsonld.getString(JsonLDTerm.specialPermissions.getLabel()));
					} else if (key.equals(JsonLDTerm.restrictions.getLabel())) {
						terms.setConfidentialityDeclaration(jsonld.getString(JsonLDTerm.restrictions.getLabel()));
					} else if (key.equals(JsonLDTerm.citationRequirements.getLabel())) {
						terms.setConfidentialityDeclaration(
								jsonld.getString(JsonLDTerm.citationRequirements.getLabel()));
					} else if (key.equals(JsonLDTerm.depositorRequirements.getLabel())) {
						terms.setConfidentialityDeclaration(
								jsonld.getString(JsonLDTerm.depositorRequirements.getLabel()));
					} else if (key.equals(JsonLDTerm.conditions.getLabel())) {
						terms.setConfidentialityDeclaration(jsonld.getString(JsonLDTerm.conditions.getLabel()));
					} else if (key.equals(JsonLDTerm.disclaimer.getLabel())) {
						terms.setConfidentialityDeclaration(jsonld.getString(JsonLDTerm.disclaimer.getLabel()));
					} else if (key.equals(JsonLDTerm.fileTermsOfAccess.getLabel())) {
						JsonObject fAccessObject = jsonld.getJsonObject(JsonLDTerm.fileTermsOfAccess.getLabel());
						if (fAccessObject.containsKey(JsonLDTerm.termsOfAccess.getLabel())) {
							terms.setTermsOfAccess(fAccessObject.getString(JsonLDTerm.termsOfAccess.getLabel()));
						}
						if (fAccessObject.containsKey(JsonLDTerm.fileRequestAccess.getLabel())) {
							terms.setFileAccessRequest(
									fAccessObject.getBoolean(JsonLDTerm.fileRequestAccess.getLabel()));
						}
						if (fAccessObject.containsKey(JsonLDTerm.dataAccessPlace.getLabel())) {
							terms.setDataAccessPlace(fAccessObject.getString(JsonLDTerm.dataAccessPlace.getLabel()));
						}
						if (fAccessObject.containsKey(JsonLDTerm.originalArchive.getLabel())) {
							terms.setOriginalArchive(fAccessObject.getString(JsonLDTerm.originalArchive.getLabel()));
						}
						if (fAccessObject.containsKey(JsonLDTerm.availabilityStatus.getLabel())) {
							terms.setAvailabilityStatus(
									fAccessObject.getString(JsonLDTerm.availabilityStatus.getLabel()));
						}
						if (fAccessObject.containsKey(JsonLDTerm.contactForAccess.getLabel())) {
							terms.setContactForAccess(fAccessObject.getString(JsonLDTerm.contactForAccess.getLabel()));
						}
						if (fAccessObject.containsKey(JsonLDTerm.sizeOfCollection.getLabel())) {
							terms.setSizeOfCollection(fAccessObject.getString(JsonLDTerm.sizeOfCollection.getLabel()));
						}
						if (fAccessObject.containsKey(JsonLDTerm.studyCompletion.getLabel())) {
							terms.setStudyCompletion(fAccessObject.getString(JsonLDTerm.studyCompletion.getLabel()));
						}
					}
					dsv.setTermsOfUseAndAccess(terms);
					// move to new dataverse?
					// aggBuilder.add(JsonLDTerm.schemaOrg("includedInDataCatalog").getLabel(),
					// dataset.getDataverseContext().getDisplayName());

				}

			}
		}

		dsv.setDatasetFields(dsfl);

		return dsv;
	}

	private static JsonObject recontextualizeJsonLD(String jsonLDString, MetadataBlockServiceBean metadataBlockSvc) {
		logger.fine(jsonLDString.replaceAll("\r?\n", ""));
		try (StringReader rdr = new StringReader(jsonLDString.replaceAll("\r?\n", ""))) {

			Map<String, String> localContext = new TreeMap<String, String>();
			// Add namespaces corresponding to core terms
			localContext.put(JsonLDNamespace.dcterms.getPrefix(), JsonLDNamespace.dcterms.getUrl());
			localContext.put(JsonLDNamespace.dvcore.getPrefix(), JsonLDNamespace.dvcore.getUrl());
			localContext.put(JsonLDNamespace.schema.getPrefix(), JsonLDNamespace.schema.getUrl());

			Map<String, MetadataBlock> mdbMap = new TreeMap<String, MetadataBlock>();
			Map<String, DatasetFieldType> dsftMap = new TreeMap<String, DatasetFieldType>();

			List<MetadataBlock> mdbList = metadataBlockSvc.listMetadataBlocks();
			for (MetadataBlock mdb : mdbList) {
				if (mdb.getNamespaceUri() != null) {
					localContext.putIfAbsent(mdb.getName(), mdb.getNamespaceUri());
					mdbMap.put(mdb.getName(), mdb);
					for (DatasetFieldType dsft : mdb.getDatasetFieldTypes()) {
						dsftMap.put(mdb.getName() + ":" + dsft.getName(), dsft);
					}
				} else {
					for (DatasetFieldType dftp : mdb.getDatasetFieldTypes()) {
						if (dftp.getUri() != null) {
							localContext.putIfAbsent(dftp.getName(), dftp.getUri());

						}
					}
				}
			}

			// Use JsonLd to expand/compact to localContext
			JsonObject jsonld = Json.createReader(rdr).readObject();
			JsonDocument doc = JsonDocument.of(jsonld);
			JsonArray array = null;
			try {
				array = JsonLd.expand(doc).get();

				jsonld = JsonLd.compact(JsonDocument.of(array), JsonDocument.of(JSONLDUtil.getContext(localContext)))
						.get();
				logger.fine("Compacted: " + jsonld);
				return jsonld;
			} catch (JsonLdError e) {
				System.out.println(e.getMessage());
				return null;
			}
		}
	}
}
