package edu.harvard.iq.dataverse;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
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

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.HttpException;
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
    
    //The hash of the existing CVocConf setting. Used to determine when the setting has changed and it needs to be re-parsed to recreate the cvocMaps
    String oldHash = null;

    public List<DatasetFieldType> findAllAdvancedSearchFieldTypes() {
        return em.createQuery("select object(o) from DatasetFieldType as o where o.advancedSearchFieldType = true and o.title != '' order by o.id", DatasetFieldType.class).getResultList();
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
            if (lenient) {
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

            } else {
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
            return new HashMap<>();
        }
        String newHash = DigestUtils.md5Hex(cvocSetting);
        if (newHash.equals(oldHash)) {
            return byTermUriField ? cvocMapByTermUri : cvocMap;
        } 
        oldHash=newHash;
        cvocMap=new HashMap<>();
        cvocMapByTermUri=new HashMap<>();
        
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
                            }
                        } else {
                            DatasetFieldType childdft = findByNameOpt(jo.getString("term-uri-field"));
                            logger.fine("Found term child field: " + childdft.getName()+ ": " + childdft.getId());
                            cvocMapByTermUri.put(childdft.getId(), jo);
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
                    if (jo.containsKey("child-fields")) {
                        JsonArray childFields = jo.getJsonArray("child-fields");
                        for (JsonString elm : childFields.getValuesAs(JsonString.class)) {
                            dft = findByNameOpt(elm.getString());
                            logger.info("Found: " + dft.getName());
                            if (dft == null) {
                                logger.warning("Ignoring External Vocabulary setting for non-existent child field: "
                                  + elm.getString());
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

    /**
     * Adds information about the external vocabulary term being used in this DatasetField to the ExternalVocabularyValue table if it doesn't already exist.
     * @param df - the primitive/parent compound field containing a newly saved value
     */
    public void registerExternalVocabValues(DatasetField df) {
        DatasetFieldType dft =df.getDatasetFieldType(); 
        logger.fine("Registering for field: " + dft.getName());
        JsonObject cvocEntry = getCVocConf(false).get(dft.getId());
        if(dft.isPrimitive()) {
            for(DatasetFieldValue dfv: df.getDatasetFieldValues()) {
                registerExternalTerm(cvocEntry, dfv.getValue());
            }
            } else {
                if (df.getDatasetFieldType().isCompound()) {
                    DatasetFieldType termdft = findByNameOpt(cvocEntry.getString("term-uri-field"));
                    for (DatasetFieldCompoundValue cv : df.getDatasetFieldCompoundValues()) {
                        for (DatasetField cdf : cv.getChildDatasetFields()) {
                            logger.fine("Found term uri field type id: " + cdf.getDatasetFieldType().getId());
                            if(cdf.getDatasetFieldType().equals(termdft)) {
                                registerExternalTerm(cvocEntry, cdf.getValue());
                            }
                        }
                    }
                }
            }
    }
    
    /**
     * Retrieves indexable strings from a cached externalvocabularyvalue entry.
     * 
     * This method assumes externalvocabularyvalue entries have been filtered and
     * the externalvocabularyvalue entry contain a single JsonObject whose values
     * are either Strings or an array of objects with "lang" and "value" keys. The
     * string, or the "value"s for each language are added to the set.
     * 
     * Any parsing error results in no entries (there can be unfiltered entries with
     * unknown structure - getting some strings from such an entry could give fairly
     * random info that would be bad to addd for searches, etc.)
     * 
     * @param termUri
     * @return - a set of indexable strings
     */
    public Set<String> getStringsFor(String termUri) {
        Set<String> strings = new HashSet<String>();
        JsonObject jo = getExternalVocabularyValue(termUri);

        if (jo != null) {
            try {
                for (String key : jo.keySet()) {
                    JsonValue jv = jo.get(key);
                    if (jv.getValueType().equals(JsonValue.ValueType.STRING)) {
                        logger.fine("adding " + jo.getString(key) + " for " + termUri);
                        strings.add(jo.getString(key));
                    } else {
                        if (jv.getValueType().equals(JsonValue.ValueType.ARRAY)) {
                            JsonArray jarr = jv.asJsonArray();
                            for (int i = 0; i < jarr.size(); i++) {
                                logger.fine("adding " + jarr.getJsonObject(i).getString("value") + " for " + termUri);
                                strings.add(jarr.getJsonObject(i).getString("value"));
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
     * Perform a query to retrieve a cached valie from the externalvocabularvalue table
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
            logger.warning("No external vocab value for uri: " + termUri);
        }
        return null;
    }

    /**
     * Perform a call to the external service to retrieve information about the term URI
     * @param cvocEntry - the configuration for the DatasetFieldType associated with this term 
     * @param term - the term uri as a string
     */
    public void registerExternalTerm(JsonObject cvocEntry, String term) {
        String retrievalUri = cvocEntry.getString("retrieval-uri");
        String prefix = cvocEntry.getString("prefix", null);
        if(term.isBlank()) {
            logger.fine("Ingoring blank term");
            return;
        }
        logger.fine("Registering term: " + term);
        try {
            URI uri = new URI(term);
            ExternalVocabularyValue evv = null;
            try {
                evv = em.createQuery("select object(o) from ExternalVocabularyValue as o where o.uri=:uri",
                        ExternalVocabularyValue.class).setParameter("uri", term).getSingleResult();
            } catch (NoResultException nre) {
                evv = new ExternalVocabularyValue(term, null);
            }
            if (evv.getValue() == null) {
                String adjustedTerm = (prefix==null)? term: term.replace(prefix, "");
                retrievalUri = retrievalUri.replace("{0}", adjustedTerm);
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
                    httpGet.addHeader("Accept", "application/json+ld, application/json");

                    HttpResponse response = httpClient.execute(httpGet);
                    String data = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == 200) {
                        logger.fine("Returned data: " + data);
                        try (JsonReader jsonReader = Json.createReader(new StringReader(data))) {
                            String dataObj =filterResponse(cvocEntry, jsonReader.readObject(), term).toString(); 
                            evv.setValue(dataObj);
                            evv.setLastUpdateDate(Timestamp.from(Instant.now()));
                            logger.fine("JsonObject: " + dataObj);
                            em.merge(evv);
                            em.flush();
                            logger.fine("Wrote value for term: " + term);
                        } catch (JsonException je) {
                            logger.severe("Error retrieving: " + retrievalUri + " : " + je.getMessage());
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
                            JsonValue curPath = readObject;
                            for (int j = 0; j < pathParts.length - 1; j++) {
                                if (pathParts[j].contains("=")) {
                                    JsonArray arr = ((JsonArray) curPath);
                                    for (int k = 0; k < arr.size(); k++) {
                                        String[] keyVal = pathParts[j].split("=");
                                        logger.fine("Looking for object where " + keyVal[0] + " is " + keyVal[1]);
                                        JsonObject jo = arr.getJsonObject(k);
                                        String val = jo.getString(keyVal[0]);
                                        String expected = keyVal[1];
                                        if (expected.equals("@id")) {
                                            expected = termUri;
                                        }
                                        if (val.equals(expected)) {
                                            logger.fine("Found: " + jo.toString());
                                            curPath = jo;
                                            break;
                                        }
                                    }
                                } else {
                                    curPath = ((JsonObject) curPath).get(pathParts[j]);
                                    logger.fine("Found next Path object " + curPath.toString());
                                }
                            }
                            JsonValue jv = ((JsonObject) curPath).get(pathParts[pathParts.length - 1]);
                            if (jv.getValueType().equals(JsonValue.ValueType.STRING)) {
                                vals.add(i, ((JsonString) jv).getString());
                            } else if (jv.getValueType().equals(JsonValue.ValueType.ARRAY)) {
                                vals.add(i, jv);
                            } else if (jv.getValueType().equals(JsonValue.ValueType.OBJECT)) {
                                vals.add(i, jv);
                            }
                            logger.fine("Added param value: " + i + ": " + vals.get(i));
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
                        if (pattern.equals("{0}")) {
                            if (vals.get(0) instanceof JsonArray) {
                                job.add(filterKey, (JsonArray) vals.get(0));
                            } else {
                                job.add(filterKey, (String) vals.get(0));
                            }
                        } else {
                            String result = MessageFormat.format(pattern, vals.toArray());
                            logger.fine("Result: " + result);
                            job.add(filterKey, result);
                            logger.fine("Added : " + filterKey + ": " + result);
                        }
                    } else {
                        logger.fine("Added hardcoded pattern: " + filterKey + ": " + pattern);
                        job.add(filterKey, pattern);
                    }
                } catch (Exception e) {
                    logger.warning("External Vocabulary: " + termUri + " - Failed to find value for " + filterKey + ": "
                            + e.getMessage());
                }
            }
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
        for(JsonObject jo: cvocConf.values()) {
            scripts.add(jo.getString("js-url"));
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
}
