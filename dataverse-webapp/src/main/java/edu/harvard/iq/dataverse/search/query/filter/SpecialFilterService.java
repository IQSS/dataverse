package edu.harvard.iq.dataverse.search.query.filter;

import org.apache.commons.lang3.StringUtils;
import org.omnifaces.cdi.Eager;

import javax.enterprise.context.ApplicationScoped;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Eager
@ApplicationScoped
public class SpecialFilterService {
    private static final Pattern GENERAL_PATTERN = Pattern.compile("\\[[A-Z]+\\[.*]]");
    private static final Pattern EXTRACTOR = Pattern.compile("\\[([A-Z]+)\\[(.*)]]");

    private ConcurrentMap<String, SpecialFilterCreator> creatorRegistry = new ConcurrentHashMap<>();

    // -------------------- LOGIC --------------------

    public void register(SpecialFilterCreator specialFilterCreator) {
        creatorRegistry.putIfAbsent(specialFilterCreator.getKey(), specialFilterCreator);
    }

    public boolean isSpecialFilter(String query) {
        return StringUtils.isNotBlank(query)
                && query.endsWith("]]") // should discard the majority of non-special filters
                && GENERAL_PATTERN.matcher(query).matches();
    }

    public SpecialFilter createFromQuery(String query) {
        Matcher extractor = EXTRACTOR.matcher(query);
        if(!extractor.matches() || extractor.groupCount() != 2) {
            return SpecialFilter.EMPTY;
        }
        String filter = extractor.group(1);
        String[] params = extractor.group(2).split("\\|");
        if (creatorRegistry.containsKey(filter)) {
            return creatorRegistry.get(filter).create(query, params);
        } else {
            throw new IllegalArgumentException("Unrecognized filter: " + filter);
        }
    }
}
