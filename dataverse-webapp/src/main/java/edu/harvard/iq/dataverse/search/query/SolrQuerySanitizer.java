package edu.harvard.iq.dataverse.search.query;

import com.google.common.collect.Sets;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.search.SolrField;
import edu.harvard.iq.dataverse.search.SolrFieldFactory;
import io.vavr.control.Option;
import org.apache.commons.lang.StringUtils;

import javax.ejb.Stateless;
import javax.inject.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Stateless
public class SolrQuerySanitizer {

    private final static Set<Character> SOLR_SPECIAL_CHARACTERS = Sets.newHashSet(
            '\\', ':', '+', '-', '!', '(', ')', 
            '^' , '[', ']', '"', '{', '}', '~',
            '*' , '?', '|', '&', ';', '/');
    
    
    @Inject
    private DatasetFieldServiceBean datasetFieldService;
    @Inject
    private SolrFieldFactory solrFieldFactory;
    
    // -------------------- LOGIC --------------------
    
    public String sanitizeQuery(String query) {
        if (query == null) {
            return StringUtils.EMPTY;
        }
        
        query = replaceQueryFieldNames(query, buildFieldNamesMapping());
        query = escapeGlobalIdValues(query);
        
        return query;
    }
    
    // -------------------- PRIVATE --------------------
    
    private Map<String, String> buildFieldNamesMapping() {
        Map<String, String> fieldNamesMapping = new HashMap<>();
        List<DatasetFieldType> datasetFields = datasetFieldService.findAllOrderedById();
        for (DatasetFieldType datasetFieldType : datasetFields) {

            SolrField dsfSolrField = solrFieldFactory.getSolrField(datasetFieldType.getName(),
                                                                   datasetFieldType.getFieldType(),
                                                                   datasetFieldType.isThisOrParentAllowsMultipleValues(),
                                                                   datasetFieldType.isFacetable());
            fieldNamesMapping.put(datasetFieldType.getName(), dsfSolrField.getNameSearchable());
        }
        return fieldNamesMapping;
    }
    
    private void transferString(StringBuilder source, StringBuilder target) {
        target.append(source);
        source.setLength(0);
    }
    
    /**
     * Replaces field names in the given query according to provided mapping.
     * <p>
     * This process is needed because we want user to be able to use
     * simple {@link DatasetFieldType} names instead of internal
     * solr field name equivalents (for example "title" instead of "dsf_txt_title")
     * <p>
     * Method is aware of position where field name can be placed in
     * the query (that is before ":" sign)
     * <p>
     * <code>
     * replaceQueryFieldNames("field:search", {"field": "internalSolrField"}) = "internalSolrField:search"
     * </code>
     */
    private String replaceQueryFieldNames(String query, Map<String, String> fieldNamesMapping) {
        StringBuilder transformedQuery = new StringBuilder();
        StringBuilder currentSegmentWithoutSpecialCharacters = new StringBuilder();

        boolean insideQuotation = false;
        boolean currentCharIsEscaped = false;
        
        for (int i=0; i<query.length(); ++i) {
            char currentChar = query.charAt(i);

            if (currentChar == '\\') {
                transformedQuery.append(currentChar);
                currentCharIsEscaped = !currentCharIsEscaped;
                continue;
            }
            
            if (currentChar == '"' && !currentCharIsEscaped) {
                insideQuotation = !insideQuotation;

                if(insideQuotation) {
                    transferString(currentSegmentWithoutSpecialCharacters, transformedQuery);
                }
            }

            if (insideQuotation) {
                transformedQuery.append(currentChar);
                continue;
            }

            if (currentChar == ':') {
                String fieldNameFromView = currentSegmentWithoutSpecialCharacters.toString();
                Option<String> solrFieldName = Option.none();
                
                if (fieldNamesMapping.containsKey(fieldNameFromView)) {
                    solrFieldName = Option.of(fieldNamesMapping.get(fieldNameFromView));
                }
                
                transformedQuery.append(solrFieldName.getOrElse(fieldNameFromView));
                currentSegmentWithoutSpecialCharacters.setLength(0);
                transformedQuery.append(currentChar);
                continue;
            }

            if (SOLR_SPECIAL_CHARACTERS.contains(currentChar) || Character.isWhitespace(currentChar)) {
                transferString(currentSegmentWithoutSpecialCharacters, transformedQuery);
                transformedQuery.append(currentChar);
                continue;
            }

            currentSegmentWithoutSpecialCharacters.append(currentChar);

        }
        transformedQuery.append(currentSegmentWithoutSpecialCharacters);
        return transformedQuery.toString();
    }
    
    
    /**
     * @param query The query string that might be mutated before feeding it
     *              into Solr.
     * @return The query string that may have been mutated or null if null was
     * passed in.
     */
    private String escapeGlobalIdValues(String query) {
        
        String[] colonParts = query.split(":");
        if (colonParts.length > 0) {
            String first = colonParts[0];
            if (first.startsWith("doi")) {
                query = query.replaceAll("doi:", "doi\\\\:");
            } else if (first.startsWith("hdl")) {
                query = query.replaceAll("hdl:", "hdl\\\\:");
            } else if (first.startsWith("datasetPersistentIdentifier")) {
                query = query.replaceAll("datasetPersistentIdentifier:doi:", "datasetPersistentIdentifier:doi\\\\:");
                query = query.replaceAll("datasetPersistentIdentifier:hdl:", "datasetPersistentIdentifier:hdl\\\\:");
            }
        }
        return query;
    }
}
