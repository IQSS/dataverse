package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Map;

@Stateless
public class GrantSuggestionDao {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    // -------------------- LOGIC --------------------

    /**
     * Function used to find suggestions according to filters.
     * @param filteredBy filters that are going to be concatenated with AND.
     * @param suggestionSourceFieldName target field for query
     * @param suggestionSourceFieldValue value that is going to be filtered with LIKE %value% and UPPER(value)
     * @param queryLimit limits the amount of values returned
     * @return list of suggestions.
     */
    public List<String> fetchSuggestions(Map<String, String> filteredBy,
                                         String suggestionSourceFieldName,
                                         String suggestionSourceFieldValue,
                                         int queryLimit) {

        if (filteredBy.isEmpty()){
            return fetchSuggestions(suggestionSourceFieldName, suggestionSourceFieldValue, queryLimit);
        }

        String filters = generateFilters(filteredBy);

        TypedQuery<String> query = em.createQuery("SELECT DISTINCT grant." + suggestionSourceFieldName + " FROM GrantSuggestion grant WHERE "
                                                          + filters + " AND UPPER(grant." + suggestionSourceFieldName + ")" +
                                                          " LIKE UPPER(:" + suggestionSourceFieldName + ")",
                                                  String.class)
                .setParameter(suggestionSourceFieldName, "%" + suggestionSourceFieldValue + "%")
                .setMaxResults(queryLimit);

        setQueryParams(filteredBy, query);

        return query.getResultList();
    }

    // -------------------- PRIVATE --------------------

    /**
     * Function used to find suggestions according to filters.
     * @param suggestionSourceFieldName target field for query
     * @param suggestionSourceFieldValue value that is going to be filtered with LIKE %value% and UPPER(value)
     * @param queryLimit limits the amount of values returned
     * @return list of suggestions.
     */
    private List<String> fetchSuggestions(String suggestionSourceFieldName,
                                          String suggestionSourceFieldValue,
                                          int queryLimit) {

        List<String> result = em.createQuery("SELECT DISTINCT grant." + suggestionSourceFieldName + " FROM GrantSuggestion grant " +
                                                     " WHERE UPPER(grant." + suggestionSourceFieldName + ") LIKE UPPER(:" + suggestionSourceFieldName + ")",
                                             String.class)
                .setParameter(suggestionSourceFieldName, "%" + suggestionSourceFieldValue + "%")
                .setMaxResults(queryLimit)
                .getResultList();

        return result;
    }

    private String generateFilters(Map<String, String> filteredBy) {
        StringBuilder filterBuilder = new StringBuilder();

        filteredBy.forEach((key, value) -> {
            filterBuilder.append("grant.");
            filterBuilder.append(key);
            filterBuilder.append(" = ");
            filterBuilder.append(":").append(key);
            filterBuilder.append(" AND ");
        });

        filterBuilder.delete(filterBuilder.lastIndexOf(" AND "), filterBuilder.length());

        return filterBuilder.toString();
    }

    private TypedQuery<String> setQueryParams(Map<String, String> filterValues, TypedQuery<String> queryWithFilters) {
        for (Map.Entry<String, String> entry : filterValues.entrySet()) {
            queryWithFilters.setParameter(entry.getKey(), entry.getValue());
        }

        return queryWithFilters;
    }
}
