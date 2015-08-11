package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.users.User;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;

@Named
@Stateless
public class SearchFilesServiceBean {

    private static final Logger logger = Logger.getLogger(SearchFilesServiceBean.class.getCanonicalName());

    @EJB
    SearchServiceBean searchService;

    public List<String> getFileCards(Dataset dataset, User user) {
        Dataverse dataverse = null;
        String query = "*";
        List<String> filterQueries = new ArrayList<>();
        filterQueries.add(SearchFields.TYPE + ":" + SearchConstants.FILES);
        filterQueries.add(SearchFields.PARENT_ID + ":" + dataset.getId());
        SortBy sortBy = getSortBy();
        String sortField = sortBy.getField();
        String sortOrder = sortBy.getOrder();
        int paginationStart = 0;
        boolean onlyDataRelatedToMe = false;
        int numResultsPerPage = 25;
        SolrQueryResponse solrQueryResponse = null;
        try {
            solrQueryResponse = searchService.search(user, dataverse, query, filterQueries, sortField, sortOrder, paginationStart, onlyDataRelatedToMe, numResultsPerPage);
        } catch (SearchException ex) {
            logger.info(SearchException.class + " searching for files: " + ex);
        } catch (Exception ex) {
            logger.info(Exception.class + " searching for files: " + ex);
        }
        List<String> toReturn = new ArrayList<>();
        for (SolrSearchResult result : solrQueryResponse.getSolrSearchResults()) {
            toReturn.add(result.getNameSort());
        }

        return toReturn;
    }

    private SortBy getSortBy() {
        try {
            return SearchUtil.getSortBy(null, null);
        } catch (Exception ex) {
            return null;
        }
    }

}
