package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.dataset.DatasetType;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.JsonValue.ValueType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;

import jakarta.persistence.criteria.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

/**
 *
 * @author xyang
 */
@Stateless
@Named
public class DatasetFieldServiceBean implements java.io.Serializable {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    private static final Logger logger = Logger.getLogger(DatasetFieldServiceBean.class.getCanonicalName());

    @EJB
    SettingsServiceBean settingsService;

    private static final String NAME_QUERY = "SELECT dsfType from DatasetFieldType dsfType where dsfType.name= :fieldName";

    /*
     * External vocabulary support: These fields cache information from the CVocConf
     * setting which controls how Dataverse connects specific metadata block fields
     * to third-party Javascripts and external vocabulary services to allow users to
     * input values from a vocabulary(ies) those services manage.
     */

    //Configuration json keyed by the id of the 'parent' DatasetFieldType
    Map <Long, JsonObject> cvocMap = null;

    //Configuration json keyed by the id of the child DatasetFieldType specified as the 'term-uri-field'
    //Note that for primitive fields, the prent and term-uri-field are the same and these maps have the same entry
    Map <Long, JsonObject> cvocMapByTermUri = null;

    //Flat list of cvoc term-uri and managed fields by Id
    Set<Long> cvocFieldSet = null;

    //The hash of the existing CVocConf setting. Used to determine when the setting has changed and it needs to be re-parsed to recreate the cvocMaps
    String oldHash = null;

    public List<DatasetFieldType> findAllAdvancedSearchFieldTypes() {
        return em.createQuery("select object(o) from DatasetFieldType as o where o.advancedSearchFieldType = true and o.title != '' order by o.displayOrder,o.id", DatasetFieldType.class).getResultList();
    }

    public List<DatasetFieldType> findAllFacetableFieldTypes() {
         return em.createNamedQuery("DatasetFieldType.findAllFacetable", DatasetFieldType.class)
                .getResultList();
    }

    public List<DatasetFieldType> findFacetableFieldTypesByMetadataBlock(Long metadataBlockId) {
        return em.createNamedQuery("DatasetFieldType.findFacetableByMetadaBlock", DatasetFieldType.class)
                .setParameter("metadataBlockId", metadataBlockId)
                .getResultList();
    }

    public List<DatasetFieldType> findAllRequiredFields() {
        return em.createQuery("select object(o) from DatasetFieldType as o where o.required = true order by o.id", DatasetFieldType.class).getResultList();
    }

    public List<DatasetFieldType> findAllOrderedById() {
        return em.createQuery("select object(o) from DatasetFieldType as o order by o.id", DatasetFieldType.class).getResultList();
    }

    public List<DatasetFieldType> findAllOrderedByName() {
        return em.createQuery("select object(o) from DatasetFieldType as o order by o.name", DatasetFieldType.class).getResultList();
    }

    public DatasetFieldType find(Object pk) {
        return em.find(DatasetFieldType.class, pk);
    }

    public DatasetFieldType findByName(String name) {
        try {
            return  (DatasetFieldType) em.createQuery(NAME_QUERY).setParameter("fieldName", name).getSingleResult();
        } catch (NoResultException e) {
            return null;
        }

    }

    /**
     * Gets the dataset field type, or returns {@code null}. Does not throw
     * exceptions.
     *
     * @param name the name do the field type
     * @return the field type, or {@code null}
     * @see #findByName(java.lang.String)
     */
    public DatasetFieldType findByNameOpt(String name) {
        try {
            return em.createNamedQuery("DatasetFieldType.findByName", DatasetFieldType.class)
                    .setParameter("name", name)
                    .getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }

    /*
     * Similar method for looking up foreign metadata field mappings, for metadata
     * imports. for these the uniquness of names isn't guaranteed (i.e., there
     * can be a field "author" in many different formats that we want to support),
     * so these have to be looked up by both the field name and the name of the
     * foreign format.
     */
    public ForeignMetadataFieldMapping findFieldMapping(String formatName, String pathName) {
        try {
            return em.createNamedQuery("ForeignMetadataFieldMapping.findByPath", ForeignMetadataFieldMapping.class)
                    .setParameter("formatName", formatName)
                    .setParameter("xPath", pathName)
                    .getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
        // TODO: cache looked up results.
    }

    public ControlledVocabularyValue findControlledVocabularyValue(Object pk) {
        return em.find(ControlledVocabularyValue.class, pk);
    }

    /**
     * @param dsft The DatasetFieldType in which to look up a
     * ControlledVocabularyValue.
     * @param strValue String value that may exist in a controlled vocabulary of
     * the provided DatasetFieldType.
     * @param lenient should we accept alternate spellings for value from mapping table
     *
     * @return The ControlledVocabularyValue found or null.
     */
    public ControlledVocabularyValue findControlledVocabularyValueByDatasetFieldTypeAndStrValue(DatasetFieldType dsft, String strValue, boolean lenient) {
        TypedQuery<ControlledVocabularyValue> typedQuery = em.createQuery("SELECT OBJECT(o) FROM ControlledVocabularyValue AS o WHERE o.strValue = :strvalue AND o.datasetFieldType = :dsft", ControlledVocabularyValue.class);
        typedQuery.setParameter("strvalue", strValue);
        typedQuery.setParameter("dsft", dsft);
        try {
            ControlledVocabularyValue cvv = typedQuery.getSingleResult();
            return cvv;
        } catch (NoResultException | NonUniqueResultException ex) {
            // if the value isn't found, check in the list of alternate values for this datasetFieldType
            TypedQuery<ControlledVocabAlternate> alternateQuery = em.createQuery("SELECT OBJECT(o) FROM ControlledVocabAlternate as o WHERE o.strValue = :strvalue AND o.datasetFieldType = :dsft", ControlledVocabAlternate.class);
            alternateQuery.setParameter("strvalue", strValue);
            alternateQuery.setParameter("dsft", dsft);
            try {
                ControlledVocabAlternate alternateValue = alternateQuery.getSingleResult();
                return alternateValue.getControlledVocabularyValue();
            } catch (NoResultException | NonUniqueResultException ex2) {
                return null;
            }
        }
    }

    public ControlledVocabAlternate findControlledVocabAlternateByControlledVocabularyValueAndStrValue(ControlledVocabularyValue cvv, String strValue){
        TypedQuery<ControlledVocabAlternate> typedQuery = em.createQuery("SELECT OBJECT(o) FROM ControlledVocabAlternate AS o WHERE o.strValue = :strvalue AND o.controlledVocabularyValue = :cvv", ControlledVocabAlternate.class);
        typedQuery.setParameter("strvalue", strValue);
        typedQuery.setParameter("cvv", cvv);
        try {
            ControlledVocabAlternate alt = typedQuery.getSingleResult();
            return alt;
        } catch (NoResultException e) {
            return null;
        } catch (NonUniqueResultException ex){
           List results = typedQuery.getResultList();
           return (ControlledVocabAlternate) results.get(0);
        }
    }

    /**
     * @param dsft The DatasetFieldType in which to look up a
     * ControlledVocabularyValue.
     * @param identifier String Identifier that may exist in a controlled vocabulary of
     * the provided DatasetFieldType.
     *
     * @return The ControlledVocabularyValue found or null.
     */
    public ControlledVocabularyValue findControlledVocabularyValueByDatasetFieldTypeAndIdentifier (DatasetFieldType dsft, String identifier)  {
        TypedQuery<ControlledVocabularyValue> typedQuery = em.createQuery("SELECT OBJECT(o) FROM ControlledVocabularyValue AS o WHERE o.identifier = :identifier AND o.datasetFieldType = :dsft", ControlledVocabularyValue.class);
        typedQuery.setParameter("identifier", identifier);
        typedQuery.setParameter("dsft", dsft);
        try {
            ControlledVocabularyValue cvv = typedQuery.getSingleResult();
            return cvv;
        } catch (NoResultException | NonUniqueResultException ex) {
                return null;
        }
    }

    // return singleton NA Controled Vocabulary Value
    public ControlledVocabularyValue findNAControlledVocabularyValue() {
        TypedQuery<ControlledVocabularyValue> typedQuery = em.createQuery("SELECT OBJECT(o) FROM ControlledVocabularyValue AS o WHERE o.datasetFieldType is null AND o.strValue = :strvalue", ControlledVocabularyValue.class);
        typedQuery.setParameter("strvalue", DatasetField.NA_VALUE);
        return typedQuery.getSingleResult();
    }

    public DatasetFieldType save(DatasetFieldType dsfType) {
        return em.merge(dsfType);
    }

    public MetadataBlock save(MetadataBlock mdb) {
        return em.merge(mdb);
    }

    public ControlledVocabularyValue save(ControlledVocabularyValue cvv) {
        return em.merge(cvv);
    }

    public ControlledVocabAlternate save(ControlledVocabAlternate alt) {
        return em.merge(alt);
    }


    /**
     * This method returns a Map relating DatasetFieldTypes with any external
     * vocabulary configuration information associated with them via the CVocConf
     * setting. THe mapping is keyed by the DatasetFieldType id for primitive fields
     * and, for a compound field, by the id of either the 'parent' DatasetFieldType
     * id or of the child field specified as the 'term-uri-field' (the field where
     * the URI of the term is stored (and not one of the child fields where the term
     * name, vocabulary URI, vocabulary Name or other managed information may go.)
     *
     * The map only contains values for DatasetFieldTypes that are configured to use external vocabulary services.
     *
     * @param byTermUriField - false: the id of the parent DatasetFieldType is the key, true: the 'term-uri-field' DatasetFieldType id is used as the key
     * @return - a map of JsonObjects containing configuration information keyed by the DatasetFieldType id (Long)
     */
    public Map<Long, JsonObject> getCVocConf(boolean byTermUriField){

        //ToDo - change to an API call to be able to provide feedback if the json is invalid?
        String cvocSetting = settingsService.getValueForKey(SettingsServiceBean.Key.CVocConf);
        if (cvocSetting == null || cvocSetting.isEmpty()) {
            oldHash=null;
            //Release old maps
            cvocMap=null;
            cvocMapByTermUri=null;
            cvocFieldSet = null;
            return new HashMap<>();
        }
        String newHash = DigestUtils.md5Hex(cvocSetting);
        if (newHash.equals(oldHash)) {
            return byTermUriField ? cvocMapByTermUri : cvocMap;
        }
        oldHash=newHash;
        cvocMap=new HashMap<>();
        cvocMapByTermUri=new HashMap<>();
        cvocFieldSet = new HashSet<>();

        try (JsonReader jsonReader = Json.createReader(new StringReader(settingsService.getValueForKey(SettingsServiceBean.Key.CVocConf)))) {
            JsonArray cvocConfJsonArray = jsonReader.readArray();
            for (JsonObject jo : cvocConfJsonArray.getValuesAs(JsonObject.class)) {
                DatasetFieldType dft = findByNameOpt(jo.getString("field-name"));
                if (dft == null) {
                    logger.warning("Ignoring External Vocabulary setting for non-existent field: "
                      + jo.getString("field-name"));
                } else {
                    cvocMap.put(dft.getId(), jo);
                    if (jo.containsKey("term-uri-field")) {
                        String termUriField = jo.getString("term-uri-field");
                        if (!dft.isHasChildren()) {
                            if (termUriField.equals(dft.getName())) {
                                logger.fine("Found primitive field for term uri : " + dft.getName() + ": " + dft.getId());
                                cvocMapByTermUri.put(dft.getId(), jo);
                                cvocFieldSet.add(dft.getId());
                            }
                        } else {
                            DatasetFieldType childdft = findByNameOpt(jo.getString("term-uri-field"));
                            logger.fine("Found term child field: " + childdft.getName()+ ": " + childdft.getId());
                            cvocMapByTermUri.put(childdft.getId(), jo);
                            cvocFieldSet.add(childdft.getId());
                            if (childdft.getParentDatasetFieldType() != dft) {
                                logger.warning("Term URI field (" + childdft.getDisplayName() + ") not a child of parent: "
                                  + dft.getDisplayName());
                            }
                        }
                        if (dft == null) {
                            logger.warning("Ignoring External Vocabulary setting for non-existent child field: "
                              + jo.getString("term-uri-field"));
                        }
                    }
                    if (jo.containsKey("managed-fields")) {
                        JsonObject managedFields = jo.getJsonObject("managed-fields");
                        for (String s : managedFields.keySet()) {
                            dft = findByNameOpt(managedFields.getString(s));
                            if (dft == null) {
                                logger.warning("Ignoring External Vocabulary setting for non-existent child field: "
                                        + managedFields.getString(s));
                            } else {
                                logger.fine("Found: " + dft.getName());
                                cvocFieldSet.add(dft.getId());
                            }
                        }
                    }
                }
            }
            } catch(JsonException e) {
                logger.warning("Ignoring External Vocabulary setting due to parsing error: " + e.getLocalizedMessage());
            }
        return byTermUriField ? cvocMapByTermUri : cvocMap;
    }

    public Set<Long> getCvocFieldSet() {
        return cvocFieldSet;
    }

    /**
     * Adds information about the external vocabulary term being used in this DatasetField to the ExternalVocabularyValue table if it doesn't already exist.
     * @param df - the primitive/parent compound field containing a newly saved value
     */
    public void registerExternalVocabValues(DatasetField df) {
        DatasetFieldType dft = df.getDatasetFieldType();
        logger.fine("Registering for field: " + dft.getName());
        JsonObject cvocEntry = getCVocConf(true).get(dft.getId());
        if (dft.isPrimitive()) {
            List<DatasetField> siblingsDatasetFields = new ArrayList<>();
            if(dft.getParentDatasetFieldType()!=null) {
                siblingsDatasetFields = df.getParentDatasetFieldCompoundValue().getChildDatasetFields();
            }
            for (DatasetFieldValue dfv : df.getDatasetFieldValues()) {
                registerExternalTerm(cvocEntry, dfv.getValue(), siblingsDatasetFields);
            }
        } else {
            if (df.getDatasetFieldType().isCompound()) {
                DatasetFieldType termdft = findByNameOpt(cvocEntry.getString("term-uri-field"));
                for (DatasetFieldCompoundValue cv : df.getDatasetFieldCompoundValues()) {
                    for (DatasetField cdf : cv.getChildDatasetFields()) {
                        logger.fine("Found term uri field type id: " + cdf.getDatasetFieldType().getId());
                        if (cdf.getDatasetFieldType().equals(termdft)) {
                            registerExternalTerm(cvocEntry, cdf.getValue(), cv.getChildDatasetFields());
                        }
                    }
                }
            }
        }
    }

    /**
     * Retrieves indexable strings from a cached externalvocabularyvalue entry filtered through retrieval-filtering configuration.
     * <p>
     * This method assumes externalvocabularyvalue entries have been filtered and that they contain a single JsonObject.
     * Cases Handled : A String, an Array of Strings, an Array of Objects with "value" or "content" keys, an Object with one or more entries that have String values or Array values with a set of String values.
     * The string(s), or the "value/content"s for each language are added to the set.
     * Retrieved string values are indexed in the term-uri-field (parameter defined in CVOC configuration) by default, or in the field specified by an optional "indexIn" parameter in the retrieval-filtering defined in the CVOC configuration.
     * <p>
     * Any parsing error results in no entries (there can be unfiltered entries with
     * unknown structure - getting some strings from such an entry could give fairly
     * random info that would be bad to addd for searches, etc.)
     *
     * @param termUri unique identifier to search in database
     * @param cvocEntry related cvoc configuration
     * @param indexingField name of solr field that will be filled with getStringsFor while indexing
     * @return - a set of indexable strings
     */
    public Set<String> getIndexableStringsByTermUri(String termUri, JsonObject cvocEntry, String indexingField) {
        Set<String> strings = new HashSet<>();
        JsonObject jo = getExternalVocabularyValue(termUri);
        JsonObject filtering = cvocEntry.getJsonObject("retrieval-filtering");
        String termUriField = cvocEntry.getJsonString("term-uri-field").getString();

        if (jo != null) {
            try {
                for (String key : jo.keySet()) {
                    String indexIn = filtering.getJsonObject(key).getString("indexIn", null);
                    // Either we are in mapping mode so indexingField (solr field) equals indexIn (cvoc config)
                    // Or we are in default mode indexingField is termUriField, indexIn is not defined then only termName and personName keys are used
                    if (indexingField.equals(indexIn) ||
                            (indexIn == null && termUriField.equals(indexingField) && (key.equals("termName")) || key.equals("personName"))) {
                        JsonValue jv = jo.get(key);
                        if (jv.getValueType().equals(JsonValue.ValueType.STRING)) {
                            logger.fine("adding " + jo.getString(key) + " for " + termUri);
                            strings.add(jo.getString(key));
                        } else if (jv.getValueType().equals(JsonValue.ValueType.ARRAY)) {
                            JsonArray jarr = jv.asJsonArray();
                            for (int i = 0; i < jarr.size(); i++) {
                                if (jarr.get(i).getValueType().equals(JsonValue.ValueType.STRING)) {
                                    strings.add(jarr.getString(i));
                                } else if (jarr.get(i).getValueType().equals(ValueType.OBJECT)) { // This condition handles SKOSMOS format like [{"lang": "en","value": "non-apis bee"},{"lang": "fr","value": "abeille non apis"}]
                                    JsonObject entry = jarr.getJsonObject(i);
                                    if (entry.containsKey("value")) {
                                        logger.fine("adding " + entry.getString("value") + " for " + termUri);
                                        strings.add(entry.getString("value"));
                                    } else if (entry.containsKey("content")) {
                                        logger.fine("adding " + entry.getString("content") + " for " + termUri);
                                        strings.add(entry.getString("content"));

                                    }
                                }
                            }
                        } else if (jv.getValueType().equals(JsonValue.ValueType.OBJECT)) {
                            JsonObject joo = jv.asJsonObject();
                            for (Map.Entry<String, JsonValue> entry : joo.entrySet()) {
                                if (entry.getValue().getValueType().equals(JsonValue.ValueType.STRING)) { // This condition handles format like { "fr": "association de quartier", "en": "neighborhood associations"}
                                    logger.fine("adding " + joo.getString(entry.getKey()) + " for " + termUri);
                                    strings.add(joo.getString(entry.getKey()));
                                } else if (entry.getValue().getValueType().equals(ValueType.ARRAY)) { // This condition handles format like {"en": ["neighbourhood societies"]}
                                    JsonArray jarr = entry.getValue().asJsonArray();
                                    for (int i = 0; i < jarr.size(); i++) {
                                        if (jarr.get(i).getValueType().equals(JsonValue.ValueType.STRING)) {
                                            logger.fine("adding " + jarr.getString(i) + " for " + termUri);
                                            strings.add(jarr.getString(i));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.warning(
                        "Problem interpreting external vocab value for uri: " + termUri + " : " + e.getMessage());
                return new HashSet<String>();
            }
        }
        logger.fine("Returning " + String.join(",", strings) + " for " + termUri);
        return strings;
    }

    /**
     * Perform a query to retrieve a cached value from the externalvocabularvalue table
     * @param termUri
     * @return - the entry's value as a JsonObject
     */
    public JsonObject getExternalVocabularyValue(String termUri) {
        try {
            ExternalVocabularyValue evv = em
                    .createQuery("select object(o) from ExternalVocabularyValue as o where o.uri=:uri",
                            ExternalVocabularyValue.class)
                    .setParameter("uri", termUri).getSingleResult();
            String valString = evv.getValue();
            try (JsonReader jr = Json.createReader(new StringReader(valString))) {
                return jr.readObject();
            } catch (Exception e) {
                logger.warning("Problem parsing external vocab value for uri: " + termUri + " : " + e.getMessage());
            }
        } catch (NoResultException nre) {
            //Could just be a plain text value
            logger.fine("No external vocab value for uri: " + termUri);
        }
        return null;
    }

    /**
     * Perform a call to the external service to retrieve information about the term URI
     *
     * @param cvocEntry             - the configuration for the DatasetFieldType associated with this term
     * @param term                  - the term uri as a string
     * @param relatedDatasetFields  - siblings or childs of the term
     */
    public void registerExternalTerm(JsonObject cvocEntry, String term, List<DatasetField> relatedDatasetFields) {
        String retrievalUri = cvocEntry.getString("retrieval-uri");
        String termUriFieldName = cvocEntry.getString("term-uri-field");
        String prefix = cvocEntry.getString("prefix", null);
        if(StringUtils.isBlank(term)) {
            logger.fine("Ignoring blank term");
            return;
        }

        boolean isExternal = false;
        JsonObject vocabs = cvocEntry.getJsonObject("vocabs");
        for (String key: vocabs.keySet()) {
            JsonObject vocab = vocabs.getJsonObject(key);
            if (vocab.containsKey("uriSpace")) {
                if (term.startsWith(vocab.getString("uriSpace"))) {
                    isExternal = true;
                    break;
                }
            }
        }
        if (!isExternal) {
            logger.fine("Ignoring free text entry: " + term);
            return;
        }
        logger.fine("Registering term: " + term);
        try {
            //Assure the term is in URI form - should be if the uriSpace entry was correct
            new URI(term);
            ExternalVocabularyValue evv = null;
            try {
                evv = em.createQuery("select object(o) from ExternalVocabularyValue as o where o.uri=:uri",
                        ExternalVocabularyValue.class).setParameter("uri", term).getSingleResult();
            } catch (NoResultException nre) {
                evv = new ExternalVocabularyValue(term, null);
            }
            if (evv.getValue() == null) {
                String adjustedTerm = (prefix==null)? term: term.replace(prefix, "");

                try {
                    retrievalUri = tryToReplaceRetrievalUriParam(retrievalUri, "0", adjustedTerm);
                    retrievalUri = tryToReplaceRetrievalUriParam(retrievalUri, termUriFieldName, adjustedTerm);
                    for (DatasetField f : relatedDatasetFields) {
                        retrievalUri = tryToReplaceRetrievalUriParam(retrievalUri, f.getDatasetFieldType().getName(), f.getValue());
                    }
                } catch (InvalidParameterException e) {
                    logger.warning("InvalidParameterException in tryReplaceRetrievalUriParam : " + e.getMessage());
                    return;
                }
                if (retrievalUri.contains("{")) {
                    logger.severe("Retrieval URI still contains unreplaced parameter :" + retrievalUri);
                    return;
                }

                logger.fine("Didn't find " + term + ", calling " + retrievalUri);
                try (CloseableHttpClient httpClient = HttpClients.custom()
                        .addInterceptorLast(new HttpResponseInterceptor() {
                            @Override
                            public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
                                int statusCode = response.getStatusLine().getStatusCode();
                                if (statusCode == 504) {
                                    //Throwing an exception triggers the retry handler
                                    throw new IOException("Retry due to 504 response");
                                }
                            }
                        })
                        //The retry handler will also do retries for network errors/other things that cause an IOException
                        .setRetryHandler(new DefaultHttpRequestRetryHandler(3, false))
                        .build()) {
                    HttpGet httpGet = new HttpGet(retrievalUri);
                    //application/json+ld is for backward compatibility
                    httpGet.addHeader("Accept", "application/ld+json, application/json+ld, application/json");
                    //Adding others custom HTTP request headers if exists
                    final JsonObject headers = cvocEntry.getJsonObject("headers");
                    if (headers != null) {
                        final Set<String> headerKeys = headers.keySet();
                        for (final String hKey: headerKeys) {
                            httpGet.addHeader(hKey, headers.getString(hKey));
                        }
                    }
                    HttpResponse response = httpClient.execute(httpGet);
                    String data = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == 200) {
                        logger.fine("Returned data: " + data);
                        try (JsonReader jsonReader = Json.createReader(new StringReader(data))) {
                            String dataObj = filterResponse(cvocEntry, jsonReader.readObject(), term).toString();
                            evv.setValue(dataObj);
                            evv.setLastUpdateDate(Timestamp.from(Instant.now()));
                            logger.fine("JsonObject: " + dataObj);
                            em.merge(evv);
                            em.flush();
                            logger.fine("Wrote value for term: " + term);
                        } catch (JsonException je) {
                            logger.severe("Error retrieving: " + retrievalUri + " : " + je.getMessage());
                        } catch (PersistenceException e) {
                            logger.fine("Problem persisting: " + retrievalUri + " : " + e.getMessage());
                        }
                    } else {
                        logger.severe("Received response code : " + statusCode + " when retrieving " + retrievalUri
                                + " : " + data);
                    }
                } catch (IOException ioe) {
                    logger.severe("IOException when retrieving url: " + retrievalUri + " : " + ioe.getMessage());
                }
            }
        } catch (URISyntaxException e) {
            logger.fine("Term is not a URI: " + term);
        }
    }

    private String tryToReplaceRetrievalUriParam(String retrievalUri, String paramName, String value) throws InvalidParameterException {

        if(StringUtils.isBlank(paramName)) {
            throw new InvalidParameterException("Empty or null paramName is not allowed while replacing retrieval uri parameter");
        }

        if(retrievalUri.contains(paramName)) {
            logger.fine("Parameter {" + paramName + "} found in retrievalUri");

            if(StringUtils.isBlank(value)) {
                throw new InvalidParameterException("Empty or null value is not allowed while replacing retrieval uri parameter");
            }

            if(retrievalUri.contains("encodeUrl:" + paramName)) {
                retrievalUri = retrievalUri.replace("{encodeUrl:"+paramName+"}", URLEncoder.encode(value, StandardCharsets.UTF_8));
            } else {
                retrievalUri = retrievalUri.replace("{"+paramName+"}", value);
            }
        } else {
            logger.fine("Parameter {" + paramName + "} not found in retrievalUri");
        }

        return retrievalUri;
    }

    /**
     * Parse the raw value returned by an external service for a give term uri and
     * filter it according to the 'retrieval-filtering' configuration for this
     * DatasetFieldType, creating a Json value with the specified structure
     *
     * @param cvocEntry - the config for this DatasetFieldType
     * @param readObject - the raw response from the service
     * @param termUri - the term uri
     * @return - a JsonObject with the structure defined by the filtering configuration
     */
    private JsonObject filterResponse(JsonObject cvocEntry, JsonObject readObject, String termUri) {

        JsonObjectBuilder job = Json.createObjectBuilder();
        JsonObject filtering = cvocEntry.getJsonObject("retrieval-filtering");
        logger.fine("RF: " + filtering.toString());
        JsonObject managedFields = cvocEntry.getJsonObject("managed-fields");
        logger.fine("MF: " + managedFields.toString());
        int nrOfNotFound = 0;
        for (String filterKey : filtering.keySet()) {
            if (!filterKey.equals("@context")) {
                try {
                    JsonObject filter = filtering.getJsonObject(filterKey);
                    logger.fine("F: " + filter.toString());
                    JsonArray params = filter.getJsonArray("params");
                    if (params == null) {
                        params = Json.createArrayBuilder().build();
                    }
                    logger.fine("Params: " + params.toString());
                    List<Object> vals = new ArrayList<Object>();
                    for (int i = 0; i < params.size(); i++) {
                        String param = params.getString(i);
                        if (param.startsWith("/")) {
                            // Remove leading /
                            param = param.substring(1);
                            String[] pathParts = param.split("/");
                            logger.fine("PP: " + String.join(", ", pathParts));
                            var foundPart = processPathSegment(0, pathParts, readObject, termUri);
                            if (foundPart == null) {
                                nrOfNotFound ++ ;
                                logger.warning("External Vocabulary: no value found for %s - %s".formatted(filterKey, param));
                            } else {
                                vals.add(i, foundPart);
                                logger.fine("Added param value: " + i + ": " + vals.get(i));
                            }
                        } else {
                            logger.fine("Param is: " + param);
                            // param is not a path - either a reference to the term URI
                            if (param.equals("@id")) {
                                logger.fine("Adding id param: " + termUri);
                                vals.add(i, termUri);
                            } else {
                                // or a hardcoded value
                                logger.fine("Adding hardcoded param: " + param);
                                vals.add(i, param);
                            }
                        }
                    }
                    // Shortcut: nominally using a pattern of {0} and a param that is @id or
                    // hardcoded value allows the same options as letting the pattern itself be @id
                    // or a hardcoded value
                    String pattern = filter.getString("pattern");
                    logger.fine("Pattern: " + pattern);
                    if (pattern.equals("@id")) {
                        logger.fine("Added #id pattern: " + filterKey + ": " + termUri);
                        job.add(filterKey, termUri);
                    } else if (pattern.contains("{")) {
                        if (!vals.isEmpty()) {
                            if (pattern.equals("{0}")) {
                                if (vals.get(0) instanceof JsonArray) {
                                    job.add(filterKey, (JsonArray) vals.get(0));
                                }
                                else if (vals.get(0) instanceof JsonObject) {
                                    job.add(filterKey, (JsonObject) vals.get(0));
                                }
                                else {
                                    job.add(filterKey, (String) vals.get(0));
                                }
                            }
                            else {
                                String result = MessageFormat.format(pattern, vals.toArray());
                                logger.fine("Result: " + result);
                                job.add(filterKey, result);
                                logger.fine("Added : " + filterKey + ": " + result);
                            }
                        } else if (nrOfNotFound == 0) {
                            logger.warning("External Vocabulary: " + termUri + " - No value found for " + filterKey);
                        }
                    } else {
                        logger.fine("Added hardcoded pattern: " + filterKey + ": " + pattern);
                        job.add(filterKey, pattern);
                    }
                } catch (Exception e) {
                    logger.warning("External Vocabulary: " + termUri + " - Failed to find value for " + filterKey + ": "
                            + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        if(nrOfNotFound>0) {
            logger.warning("External Vocabulary: " + termUri + " - Failed to find value(s) reported above in " +readObject);
        }
        JsonObject filteredResponse = job.build();
        if(filteredResponse.isEmpty()) {
            logger.severe("Unable to filter response for term: " + termUri + ",  received: " + readObject.toString());
            //Better to store nothing in this case so unknown values don't propagate to exported metadata (we'll just send the termUri itself in those cases).
            return null;
        } else {
            return filteredResponse;
        }
    }

    Object processPathSegment(int index, String[] pathParts, JsonValue curPath, String termUri) {
        if (index < pathParts.length - 1) {
            if (pathParts[index].contains("=")) {
                JsonArray arr = ((JsonArray) curPath);
                String[] keyVal = pathParts[index].split("=");
                logger.fine("Looking for object where " + keyVal[0] + " is " + keyVal[1]);
                String expected = keyVal[1];

                if (!expected.equals("*")) {
                    if (expected.equals("@id")) {
                        expected = termUri;
                    }
                    for (int k = 0; k < arr.size(); k++) {
                        JsonObject jo = arr.getJsonObject(k);
                        if (!jo.isEmpty()) {
                            JsonValue val = jo.get(keyVal[0]);
                            if (val != null && val.getValueType() == ValueType.STRING && val.toString().equals(expected)) {
                                logger.fine("Found: " + jo.toString());
                                curPath = jo;
                                return processPathSegment(index + 1, pathParts, curPath, termUri);
                            }
                        }
                    }
                    return null;
                } else {
                    JsonArrayBuilder parts = Json.createArrayBuilder();
                    for (JsonValue subPath : arr) {
                        if (subPath instanceof JsonObject) {
                            JsonValue nextValue = ((JsonObject) subPath).get(keyVal[0]);
                            Object obj = processPathSegment(index + 1, pathParts, nextValue, termUri);
                            if (obj instanceof String) {
                                parts.add((String) obj);
                            } else {
                                parts.add((JsonValue) obj);
                            }
                        }
                    }
                    return parts.build();
                }

            } else {
                curPath = ((JsonObject) curPath).get(pathParts[index]);
                logger.fine("Found next Path object " + curPath.toString());
                return processPathSegment(index + 1, pathParts, curPath, termUri);
            }
        } else {
            logger.fine("Last segment: " + curPath.toString());
            logger.fine("Looking for : " + pathParts[index]);
            JsonValue jv = ((JsonObject) curPath).get(pathParts[index]);
            if (jv != null) {
                ValueType type = jv.getValueType();
                if (type.equals(ValueType.STRING)) {
                    return ((JsonString) jv).getString();
                }
                else if (jv.getValueType().equals(ValueType.ARRAY)) {
                    return jv;
                }
                else if (jv.getValueType().equals(ValueType.OBJECT)) {
                    return jv;
                }
            }
        }

        return null;

    }

    /**
     * Supports validation of externally controlled values. If the value is a URI it
     * must be in the namespace (start with) one of the uriSpace values of an
     * allowed vocabulary. If free text entries are allowed for this field (per the
     * configuration), non-uri entries are also assumed valid.
     *
     * @param dft
     * @param value
     * @return - true: valid
     */
    public boolean isValidCVocValue(DatasetFieldType dft, String value) {
        JsonObject jo = getCVocConf(true).get(dft.getId());
        JsonObject vocabs = jo.getJsonObject("vocabs");
        boolean valid = false;
        boolean couldBeFreeText = true;
        boolean freeTextAllowed = jo.getBoolean("allow-free-text", false);
        for (String vocabName : vocabs.keySet()) {
            JsonObject vocab = vocabs.getJsonObject(vocabName);
            String baseUri = vocab.getString("uriSpace");
            if (value.startsWith(baseUri)) {
                valid = true;
                break;
            } else {
                String protocol = baseUri.substring(baseUri.indexOf("://") + 3);
                if (value.startsWith(protocol)) {
                    couldBeFreeText = false;
                    // No break because we need to check for conflicts with all vocabs
                }
            }
        }
        if (!valid) {
            if (freeTextAllowed && couldBeFreeText) {
                valid = true;
            }
        }
        return valid;
    }

    public List<String> getVocabScripts( Map<Long, JsonObject> cvocConf) {
        //ToDo - only return scripts that are needed (those fields are set on display pages, those blocks/fields are allowed in the Dataverse collection for create/edit)?
        Set<String> scripts = new HashSet<String>();
        for (JsonObject jo : cvocConf.values()) {
            // Allow either a single script (a string) or an array of scripts (used, for
            // example, to allow use of the common cvocutils.js script along with a main
            // script for the field.)
            JsonValue scriptValue = jo.get("js-url");
            ValueType scriptType = scriptValue.getValueType();
            if (scriptType.equals(ValueType.STRING)) {
                scripts.add(((JsonString) scriptValue).getString());
            } else if (scriptType.equals(ValueType.ARRAY)) {
                JsonArray scriptArray = ((JsonArray) scriptValue);
                for (int i = 0; i < scriptArray.size(); i++) {
                    scripts.add(scriptArray.getString(i));
                }
            }
        }
        String customScript = settingsService.getValueForKey(SettingsServiceBean.Key.ControlledVocabularyCustomJavaScript);
        if (customScript != null && !customScript.isEmpty()) {
            scripts.add(customScript);
        }
        return Arrays.asList(scripts.toArray(new String[0]));
    }

    public String getFieldLanguage(String languages, String localeCode) {
        // If the fields list of supported languages contains the current locale (e.g.
        // the lang of the UI, or the current metadata input/display lang (tbd)), use
        // that. Otherwise, return the first in the list
        String[] langStrings = languages.split("\\s*,\\s*");
        if (langStrings.length > 0) {
            if (Arrays.asList(langStrings).contains(localeCode)) {
                return localeCode;
            } else {
                return langStrings[0];
            }
        }
        return null;
    }

    public List<DatasetFieldType> findAllDisplayedOnCreateInMetadataBlock(MetadataBlock metadataBlock) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<DatasetFieldType> criteriaQuery = criteriaBuilder.createQuery(DatasetFieldType.class);

        Root<MetadataBlock> metadataBlockRoot = criteriaQuery.from(MetadataBlock.class);
        Root<DatasetFieldType> datasetFieldTypeRoot = criteriaQuery.from(DatasetFieldType.class);

        Predicate fieldRequiredInTheInstallation = buildFieldRequiredInTheInstallationPredicate(criteriaBuilder, datasetFieldTypeRoot);

        criteriaQuery.where(
                criteriaBuilder.and(
                        criteriaBuilder.equal(metadataBlockRoot.get("id"), metadataBlock.getId()),
                        datasetFieldTypeRoot.in(metadataBlockRoot.get("datasetFieldTypes")),
                        criteriaBuilder.or(
                                criteriaBuilder.isTrue(datasetFieldTypeRoot.get("displayOnCreate")),
                                fieldRequiredInTheInstallation
                        )
                )
        );

        criteriaQuery.select(datasetFieldTypeRoot).distinct(true);

        TypedQuery<DatasetFieldType> typedQuery = em.createQuery(criteriaQuery);
        return typedQuery.getResultList();
    }

    public List<DatasetFieldType> findAllInMetadataBlockAndDataverse(MetadataBlock metadataBlock, Dataverse dataverse, boolean onlyDisplayedOnCreate, DatasetType datasetType) {
        if (!dataverse.isMetadataBlockRoot() && dataverse.getOwner() != null) {
            return findAllInMetadataBlockAndDataverse(metadataBlock, dataverse.getOwner(), onlyDisplayedOnCreate, datasetType);
        }

        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<DatasetFieldType> criteriaQuery = criteriaBuilder.createQuery(DatasetFieldType.class);

        Root<MetadataBlock> metadataBlockRoot = criteriaQuery.from(MetadataBlock.class);
        Root<DatasetFieldType> datasetFieldTypeRoot = criteriaQuery.from(DatasetFieldType.class);

        // Build the main predicate to include fields that belong to the specified dataverse and metadataBlock and match the onlyDisplayedOnCreate value.
        Predicate fieldPresentInDataverse = buildFieldPresentInDataversePredicate(dataverse, onlyDisplayedOnCreate, criteriaQuery, criteriaBuilder, datasetFieldTypeRoot, metadataBlockRoot);

        // Build an additional predicate to include fields from the datasetType, if the datasetType is specified and contains the given metadataBlock.
        Predicate fieldPresentInDatasetType = buildFieldPresentInDatasetTypePredicate(datasetType, criteriaQuery, criteriaBuilder, datasetFieldTypeRoot, metadataBlockRoot, onlyDisplayedOnCreate);

        // Build the final WHERE clause by combining all the predicates.
        criteriaQuery.where(
                criteriaBuilder.equal(metadataBlockRoot.get("id"), metadataBlock.getId()), // Match the MetadataBlock ID.
                datasetFieldTypeRoot.in(metadataBlockRoot.get("datasetFieldTypes")), // Ensure the DatasetFieldType is part of the MetadataBlock.
                criteriaBuilder.or(
                        fieldPresentInDataverse,
                        fieldPresentInDatasetType
                )
        );

        criteriaQuery.select(datasetFieldTypeRoot);

        return em.createQuery(criteriaQuery).getResultList();
    }

    public boolean isFieldRequiredInDataverse(DatasetFieldType datasetFieldType, Dataverse dataverse) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);

        Root<Dataverse> dataverseRoot = criteriaQuery.from(Dataverse.class);
        Root<DatasetFieldType> datasetFieldTypeRoot = criteriaQuery.from(DatasetFieldType.class);

        // Join Dataverse with DataverseFieldTypeInputLevel on the "dataverseFieldTypeInputLevels" attribute, using a LEFT JOIN.
        Join<Dataverse, DataverseFieldTypeInputLevel> datasetFieldTypeInputLevelJoin = dataverseRoot.join("dataverseFieldTypeInputLevels", JoinType.LEFT);

        // Define a predicate to include DatasetFieldTypes that are marked as required in the input level.
        Predicate requiredAsInputLevelPredicate = criteriaBuilder.and(
                criteriaBuilder.equal(datasetFieldTypeRoot, datasetFieldTypeInputLevelJoin.get("datasetFieldType")),
                criteriaBuilder.isTrue(datasetFieldTypeInputLevelJoin.get("required"))
        );

        // Define a predicate to include the required fields in the installation.
        Predicate requiredInTheInstallationPredicate = buildFieldRequiredInTheInstallationPredicate(criteriaBuilder, datasetFieldTypeRoot);

        // Build the final WHERE clause by combining all the predicates.
        criteriaQuery.where(
                criteriaBuilder.equal(dataverseRoot.get("id"), dataverse.getId()),
                criteriaBuilder.equal(datasetFieldTypeRoot.get("id"), datasetFieldType.getId()),
                criteriaBuilder.or(
                        requiredAsInputLevelPredicate,
                        requiredInTheInstallationPredicate
                )
        );

        criteriaQuery.select(criteriaBuilder.count(datasetFieldTypeRoot));

        Long count = em.createQuery(criteriaQuery).getSingleResult();

        return count != null && count > 0;
    }

    private Predicate buildFieldPresentInDataversePredicate(Dataverse dataverse, boolean onlyDisplayedOnCreate, CriteriaQuery<DatasetFieldType> criteriaQuery, CriteriaBuilder criteriaBuilder, Root<DatasetFieldType> datasetFieldTypeRoot, Root<MetadataBlock> metadataBlockRoot) {
        Root<Dataverse> dataverseRoot = criteriaQuery.from(Dataverse.class);

        // Join Dataverse with DataverseFieldTypeInputLevel on the "dataverseFieldTypeInputLevels" attribute, using a LEFT JOIN.
        Join<Dataverse, DataverseFieldTypeInputLevel> datasetFieldTypeInputLevelJoin = dataverseRoot.join("dataverseFieldTypeInputLevels", JoinType.LEFT);

        // Define a predicate to include DatasetFieldTypes that are marked as included in the input level.
        Predicate includedAsInputLevelPredicate = criteriaBuilder.and(
                criteriaBuilder.equal(datasetFieldTypeRoot, datasetFieldTypeInputLevelJoin.get("datasetFieldType")),
                criteriaBuilder.isTrue(datasetFieldTypeInputLevelJoin.get("include"))
        );

        // Define a predicate to include DatasetFieldTypes that are marked as required in the input level.
        Predicate requiredAsInputLevelPredicate = criteriaBuilder.and(
                criteriaBuilder.equal(datasetFieldTypeRoot, datasetFieldTypeInputLevelJoin.get("datasetFieldType")),
                criteriaBuilder.isTrue(datasetFieldTypeInputLevelJoin.get("required"))
        );

        // Predicate for displayOnCreate in input level
        Predicate displayOnCreateInputLevelPredicate = criteriaBuilder.and(
            criteriaBuilder.equal(datasetFieldTypeRoot, datasetFieldTypeInputLevelJoin.get("datasetFieldType")),
            criteriaBuilder.equal(datasetFieldTypeInputLevelJoin.get("displayOnCreate"), Boolean.TRUE)
        );

        // Create a subquery to check for the absence of a specific DataverseFieldTypeInputLevel.
        Subquery<Long> subquery = criteriaQuery.subquery(Long.class);
        Root<DataverseFieldTypeInputLevel> subqueryRoot = subquery.from(DataverseFieldTypeInputLevel.class);
        subquery.select(criteriaBuilder.literal(1L))
                .where(
                        criteriaBuilder.equal(subqueryRoot.get("dataverse"), dataverseRoot),
                        criteriaBuilder.equal(subqueryRoot.get("datasetFieldType"), datasetFieldTypeRoot),
                        criteriaBuilder.isNotNull(subqueryRoot.get("displayOnCreate"))
                );

        // Define a predicate to exclude DatasetFieldTypes that have no associated input level (i.e., the subquery does not return a result).
        Predicate hasNoInputLevelPredicate = criteriaBuilder.not(criteriaBuilder.exists(subquery));

        // Define a predicate to include the required fields in the installation.
        Predicate fieldRequiredInTheInstallation = buildFieldRequiredInTheInstallationPredicate(criteriaBuilder, datasetFieldTypeRoot);

        // Define a predicate for displaying DatasetFieldTypes on create.
        // If onlyDisplayedOnCreate is true, include fields that:
        // - Are either marked as displayed on create OR marked as required, OR
        // - Are required according to the input level.
        // Otherwise, use an always-true predicate (conjunction).
        Predicate displayedOnCreatePredicate = onlyDisplayedOnCreate
                ? criteriaBuilder.or(
                // 1. Field marked as displayOnCreate in input level
                displayOnCreateInputLevelPredicate,
                
                // 2. Field without input level that is marked as displayOnCreate or required
                criteriaBuilder.and(
                    hasNoInputLevelPredicate,
                    criteriaBuilder.or(
                        criteriaBuilder.isTrue(datasetFieldTypeRoot.get("displayOnCreate")),
                        fieldRequiredInTheInstallation
                    )
                ),
                
                // 3. Field required by input level
                requiredAsInputLevelPredicate
        )
                : criteriaBuilder.conjunction();

        // Combine all the predicates.
        return criteriaBuilder.and(
                criteriaBuilder.equal(dataverseRoot.get("id"), dataverse.getId()), // Match the Dataverse ID.
                metadataBlockRoot.in(dataverseRoot.get("metadataBlocks")), // Ensure the MetadataBlock is part of the Dataverse.
                criteriaBuilder.or(includedAsInputLevelPredicate, hasNoInputLevelPredicate), // Include DatasetFieldTypes based on the input level predicates.
                displayedOnCreatePredicate // Apply the display-on-create filter if necessary.
        );
    }

    private Predicate buildFieldPresentInDatasetTypePredicate(DatasetType datasetType,
                                                              CriteriaQuery<DatasetFieldType> criteriaQuery,
                                                              CriteriaBuilder criteriaBuilder,
                                                              Root<DatasetFieldType> datasetFieldTypeRoot,
                                                              Root<MetadataBlock> metadataBlockRoot,
                                                              boolean onlyDisplayedOnCreate) {
        Predicate datasetTypePredicate = criteriaBuilder.isFalse(criteriaBuilder.literal(true)); // Initialize datasetTypePredicate to always false by default
        if (datasetType != null) {
            // Create a subquery to check for the presence of the specified metadataBlock within the datasetType
            Subquery<Long> datasetTypeSubquery = criteriaQuery.subquery(Long.class);
            Root<DatasetType> datasetTypeRoot = criteriaQuery.from(DatasetType.class);

            // Define a predicate for displaying DatasetFieldTypes on create.
            // If onlyDisplayedOnCreate is true, include fields that are either marked as displayed on create OR marked as required.
            // Otherwise, use an always-true predicate (conjunction).
            Predicate displayedOnCreatePredicate = onlyDisplayedOnCreate ?
                    criteriaBuilder.or(
                            criteriaBuilder.isTrue(datasetFieldTypeRoot.get("displayOnCreate")),
                            buildFieldRequiredInTheInstallationPredicate(criteriaBuilder, datasetFieldTypeRoot)
                    )
                    : criteriaBuilder.conjunction();

            datasetTypeSubquery.select(criteriaBuilder.literal(1L))
                    .where(
                            criteriaBuilder.equal(datasetTypeRoot.get("id"), datasetType.getId()), // Match the DatasetType ID.
                            metadataBlockRoot.in(datasetTypeRoot.get("metadataBlocks")), // Ensure the metadataBlock is included in the datasetType's list of metadata blocks.
                            displayedOnCreatePredicate
                    );

            // Now set the datasetTypePredicate to true if the subquery finds a matching metadataBlock
            datasetTypePredicate = criteriaBuilder.exists(datasetTypeSubquery);
        }
        return datasetTypePredicate;
    }

    private Predicate buildFieldRequiredInTheInstallationPredicate(CriteriaBuilder criteriaBuilder, Root<DatasetFieldType> datasetFieldTypeRoot) {
        // Predicate to check if the current DatasetFieldType is required.
        Predicate isRequired = criteriaBuilder.isTrue(datasetFieldTypeRoot.get("required"));

        // Subquery to check if the parentDatasetFieldType is required or null.
        // We need this check to avoid including conditionally required fields.
        Subquery<Boolean> subquery = criteriaBuilder.createQuery(Boolean.class).subquery(Boolean.class);
        Root<DatasetFieldType> parentRoot = subquery.from(DatasetFieldType.class);

        subquery.select(criteriaBuilder.literal(true))
                .where(
                        criteriaBuilder.equal(parentRoot, datasetFieldTypeRoot.get("parentDatasetFieldType")),
                        criteriaBuilder.or(
                                criteriaBuilder.isNull(parentRoot.get("required")),
                                criteriaBuilder.isTrue(parentRoot.get("required"))
                        )
                );

        // Predicate to check that either the parentDatasetFieldType meets the condition or doesn't exist (is null).
        Predicate parentCondition = criteriaBuilder.or(
                criteriaBuilder.exists(subquery),
                criteriaBuilder.isNull(datasetFieldTypeRoot.get("parentDatasetFieldType"))
        );

        return criteriaBuilder.and(isRequired, parentCondition);
    }
}
