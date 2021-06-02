package edu.harvard.iq.dataverse;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.stream.JsonParsingException;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
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
    
    Map <Long, JsonObject> cvocMap = null;
    String oldHash = null;
    
    public Map<Long, JsonObject> getCVocConf(){
        
        //ToDo - change to an API call to be able to provide feedback if the json is invalid?
        String cvocSetting = settingsService.getValueForKey(SettingsServiceBean.Key.CVocConf);
        if (cvocSetting == null || cvocSetting.isEmpty()) {
            oldHash=null;
            return new HashMap<>();
    } 
        String newHash = DigestUtils.md5Hex(cvocSetting);
        if(newHash.equals(oldHash)) {
            return cvocMap;
        } 
            oldHash=newHash;
        cvocMap=new HashMap<>();
        
        try (JsonReader jsonReader = Json.createReader(new StringReader(settingsService.getValueForKey(SettingsServiceBean.Key.CVocConf)))) {
        JsonArray cvocConfJsonArray = jsonReader.readArray();
            for (JsonObject jo : cvocConfJsonArray.getValuesAs(JsonObject.class)) {
                DatasetFieldType dft = findByNameOpt(jo.getString("field-name"));
                if(dft!=null) {
                    cvocMap.put(dft.getId(), jo);
                   } else {
                       logger.warning("Ignoring External Vocabulary setting for non-existent field: " + jo.getString("field-name"));
                   }
                if(jo.containsKey("term-uri-field")) {
                    String termUriField=jo.getString("term-uri-field");
                    if (!dft.isHasChildren()) {
                        if (termUriField.equals(dft.getName())) {
                            logger.info("Found primitive field for term uri : " + dft.getName());
                        }
                    } else {
                        DatasetFieldType childdft = findByNameOpt(jo.getString("term-uri-field"));
                        logger.info("Found term child field: " + childdft.getName());
                        if (childdft.getParentDatasetFieldType() != dft) {
                            logger.warning("Term URI field (" + childdft.getDisplayName() + ") not a child of parent: "
                                    + dft.getDisplayName());
                        }
                    }
                    if(dft==null) {
                        logger.warning("Ignoring External Vocabulary setting for non-existent child field: " + jo.getString("term-uri-field"));
                    }

                }if(jo.containsKey("child-fields")) {
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
            } catch(JsonException e) {
                logger.warning("Ignoring External Vocabulary setting due to parsing error: " + e.getLocalizedMessage());
            }
        return cvocMap;
    }

    public void registerExternalVocabValues(DatasetField df) {
        DatasetFieldType dft =df.getDatasetFieldType(); 
        logger.info("Registering for field: " + dft.getName());
        JsonObject cvocEntry = getCVocConf().get(dft.getId());
        if(dft.isPrimitive()) {
            for(DatasetFieldValue dfv: df.getDatasetFieldValues()) {
                registerExternalTerm(dfv.getValue(), cvocEntry.getString("retrieval-uri"));
            }
            } else {
                if (df.getDatasetFieldType().isCompound()) {
                    DatasetFieldType termdft = findByNameOpt(cvocEntry.getString("term-uri-field"));
                    for (DatasetFieldCompoundValue cv : df.getDatasetFieldCompoundValues()) {
                        for (DatasetField cdf : cv.getChildDatasetFields()) {
                            logger.info("Found term uri field type id: " + cdf.getDatasetFieldType().getId());
                            if(cdf.getDatasetFieldType().equals(termdft)) {
                                registerExternalTerm(cdf.getValue(), cvocEntry.getString("retrieval-uri"));
                            }
                        }
                    }
                }
            }
    }
    
    @Asynchronous
    private void registerExternalTerm(String term, String retrievalUri) {
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
                retrievalUri = retrievalUri.replace("{0}", term);
                logger.info("Didn't find " + term + ", calling " + retrievalUri);
                try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                    HttpGet httpGet = new HttpGet(retrievalUri);
                    httpGet.addHeader("Accept", "application/json+ld, application/json");

                    HttpResponse response = httpClient.execute(httpGet);
                    String data = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == 200) {
                        logger.fine("Returned data: " + data);
                        try (JsonReader jsonReader = Json.createReader(new StringReader(data))) {
                            String dataObj =jsonReader.readObject().toString(); 
                            evv.setValue(dataObj);
                            logger.fine("JsonObject: " + dataObj);
                            em.merge(evv);
                            em.flush();
                            logger.fine("Wrote value for term: " + term);
                        } catch (JsonException je) {
                            logger.warning("Error retrieving: " + retrievalUri + " : " + je.getMessage());
                        }
                    } else {
                        logger.warning("Received response code : " + statusCode + " when retrieving " + retrievalUri
                                + " : " + data);
                    }
                } catch (IOException ioe) {
                    logger.warning("IOException when retrieving url: " + retrievalUri + " : " + ioe.getMessage());
                }

            }
        } catch (URISyntaxException e) {
            logger.fine("Term is not a URI: " + term);
        }

    }
    
    /*
    public class CVoc {
        String cvocUrl;
        String language;
        String protocol;
        String vocabUri;
        String termParentUri;
        String jsUrl;
        String mapId;
        String mapQuery;
        boolean readonly;
        boolean hideReadonlyUrls;
        int minChars;
        List<String> vocabs;
        List<String> keys;
        public CVoc(String cvocUrl, String language, String protocol, String vocabUri, String termParentUri, boolean readonly, boolean hideReadonlyUrls, int minChars,
                    List<String> vocabs, List<String> keys, String jsUrl, String mapId, String mapQuery){
            this.cvocUrl = cvocUrl;
            this.language = language;
            this.protocol = protocol;
            this.readonly = readonly;
            this.hideReadonlyUrls = hideReadonlyUrls;
            this.minChars = minChars;
            this.vocabs = vocabs;
            this.vocabUri = vocabUri;
            this.termParentUri = termParentUri;
            this.keys = keys;
            this.jsUrl = jsUrl;
            this.mapId = mapId;
            this.mapQuery = mapQuery;
        }

        public String getCVocUrl() {
            return cvocUrl;
        }
        public String getLanguage() {
            return language;
        }
        public String getProtocol() { return protocol; }
        public String getVocabUri() {
            return vocabUri;
        }
        public String getTermParentUri() {
            return termParentUri;
        }
        public boolean isReadonly() {
            return readonly;
        }
        public boolean isHideReadonlyUrls() {
            return hideReadonlyUrls;
        }
        public int getMinChars() { return minChars; }
        public List<String> getVocabs() {
            return vocabs;
        }
        public List<String> getKeys() {
            return keys;
        }

        public String getJsUrl() {
            return jsUrl;
        }

        public String getMapId() {
            return mapId;
        }

        public String getMapQuery() {
            return mapQuery;
        }
    }
*/
}
