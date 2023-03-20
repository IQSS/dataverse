package edu.harvard.iq.dataverse.search.advanced.field;

import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.search.advanced.query.QueryPart;
import edu.harvard.iq.dataverse.search.advanced.query.QueryPartType;
import edu.harvard.iq.dataverse.search.query.SearchObjectType;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class LicenseCheckboxSearchField extends CheckboxSearchField {

    private Map<Long, String> licenseNames = new HashMap<>();

    // -------------------- CONSTRUCTORS --------------------

    public LicenseCheckboxSearchField(String name, String displayName, String description, Map<Long, String> licenseNames) {
        super(name, displayName, description);
        this.licenseNames.putAll(licenseNames);
    }

    // -------------------- LOGIC --------------------

    @Override
    public QueryPart getQueryPart() {
        String licensesQuery = getCheckedFieldValues().stream()
                .map(v -> v.split(":"))
                .filter(v -> v.length > 1)
                .map(v -> StringUtils.isNotBlank(v[1]) ? licenseNames.get(Long.parseLong(v[1])) : StringUtils.EMPTY)
                .filter(StringUtils::isNotBlank)
                .map(v -> String.format("%s:\"%s\"", getName(), v))
                .collect(Collectors.joining(" OR "));
        return StringUtils.isNotBlank(licensesQuery)
                ? new QueryPart(QueryPartType.QUERY,
                    String.format("%s AND %s:\"%s\"",
                            licensesQuery.contains(" OR ") ? String.format("(%s)", licensesQuery) : licensesQuery,
                            SearchFields.TYPE, SearchObjectType.FILES.getSolrValue()))
                : QueryPart.EMPTY;
    }
}
