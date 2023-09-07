package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

@Named
@Stateless
public class SearchFilesServiceBean {

    private static final Logger logger = Logger.getLogger(SearchFilesServiceBean.class.getCanonicalName());

    @EJB
    SearchServiceBean searchService;
    
    public FileView getFileView(DatasetVersion datasetVersion, User user, String userSuppliedQuery) {
        Dataverse dataverse = null;
        List<String> filterQueries = new ArrayList<>();
        filterQueries.add(SearchFields.TYPE + ":" + SearchConstants.FILES);
        filterQueries.add(SearchFields.PARENT_ID + ":" + datasetVersion.getDataset().getId());
        /**
         * @todo In order to support searching for files based on dataset
         * version for https://github.com/IQSS/dataverse/issues/2455 we're going
         * to need to make the dataset version id searchable, perhaps as part of
         * https://github.com/IQSS/dataverse/issues/2038
         */
//        filterQueries.add(SearchFields.DATASET_VERSION_ID + ":" + datasetVersion.getId());
        String finalQuery = SearchUtil.determineFinalQuery(userSuppliedQuery);
        SortBy sortBy = getSortBy(finalQuery);
        String sortField = sortBy.getField();
        String sortOrder = sortBy.getOrder();
        int paginationStart = 0;
        boolean onlyDataRelatedToMe = false;
        int numResultsPerPage = 25;
        SolrQueryResponse solrQueryResponse = null;
        List<Dataverse> dataverses = new ArrayList<>();
        dataverses.add(dataverse);
        try {
            HttpServletRequest httpServletRequest = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
            solrQueryResponse = searchService.search(new DataverseRequest(user, httpServletRequest), dataverses, finalQuery, filterQueries, sortField, sortOrder, paginationStart, onlyDataRelatedToMe, numResultsPerPage);
        } catch (SearchException ex) {
            logger.info(SearchException.class + " searching for files: " + ex);
            return null;
        } catch (Exception ex) {
            logger.info(Exception.class + " searching for files: " + ex);
            return null;
        }

        return new FileView(
                solrQueryResponse.getSolrSearchResults(),
                solrQueryResponse.getFacetCategoryList(),
                solrQueryResponse.getFilterQueriesActual(),
                solrQueryResponse.getSolrQuery().getQuery()
        );
    }

    public static SortBy getSortBy(String query) {
        try {
            if (query != null) {
                return SearchUtil.getSortBy("name", SortBy.ASCENDING);
            } else {
                return SearchUtil.getSortBy(null, null);
            }
        } catch (Exception ex) {
            return null;
        }
    }
    
}
