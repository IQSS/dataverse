package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.Suggestion;
import edu.harvard.iq.dataverse.search.ror.RorDto;
import edu.harvard.iq.dataverse.search.ror.RorSolrDataFinder;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.Stateless;
import javax.inject.Inject;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

@Stateless
public class RorSuggestionHandler implements SuggestionHandler {

    @Inject
    private RorSolrDataFinder rorSolrDataFinder;
    
    // -------------------- LOGIC --------------------

    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns class name.
     */
    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation doesn't support filters. Always
     * returns empty map.
     */
    @Override
    public List<String> getAllowedFilters() {
        return Collections.emptyList();
    }

    @Override
    public List<Suggestion> generateSuggestions(Map<String, String> filteredBy, String query) {
        
        return rorSolrDataFinder.findRorData(query, 5).stream()
            .map(this::convertSolrRorToSuggestion)
            .collect(toList());
    }

    // -------------------- PRIVATE --------------------
    
    private Suggestion convertSolrRorToSuggestion(RorDto solrRor) {
        return new Suggestion(solrRor.getRorUrl(), generateDisplayName(solrRor));
    }
    
    private String generateDisplayName(RorDto solrRor) {
        StringBuilder rorDisplay = new StringBuilder(solrRor.getName());
        
        if (StringUtils.isNotEmpty(solrRor.getCountryName())) {
            rorDisplay.append(" (" + solrRor.getCountryName() + ")");
        }
        rorDisplay.append('.');
        
        
        String otherNamesString = generateOtherNames(solrRor);
        if (StringUtils.isNotEmpty(otherNamesString)) {
            rorDisplay
                .append(' ')
                .append(BundleUtil.getStringFromBundle("dataset.metadata.inputRenderer.suggestion.ror.otherNames"))
                .append(": ")
                .append(otherNamesString);
        }
        
        return rorDisplay.toString();
    }
    
    private String generateOtherNames(RorDto solrRor) {
        List<String> otherNames = Lists.newArrayList();
        otherNames.addAll(solrRor.getNameAliases());
        otherNames.addAll(solrRor.getAcronyms());
        otherNames.addAll(solrRor.getLabels());
        return otherNames.stream().collect(joining("; "));
    }
}
