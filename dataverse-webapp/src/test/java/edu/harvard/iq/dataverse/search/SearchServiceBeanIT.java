package edu.harvard.iq.dataverse.search;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.arquillian.facesmock.FacesContextMocker;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.group.IpAddress;
import edu.harvard.iq.dataverse.persistence.user.GuestUser;
import edu.harvard.iq.dataverse.search.SearchServiceBean.SortOrder;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * @author madryk
 */
@Transactional(TransactionMode.ROLLBACK)
@RunWith(Arquillian.class)
public class SearchServiceBeanIT extends WebappArquillianDeployment {

    @Inject
    private SearchServiceBean searchService;
    
    @Inject
    private DataverseDao dataverseDao;
    
    @Inject
    private AuthenticationServiceBean authenticationServiceBean;
    
    @Inject
    private SolrIndexCleaner solrIndexCleaner;
    
    
    private DataverseRequest adminDataverseRequest;
    private DataverseRequest guestDataverseRequest;
    
    
    @Before
    public void init() throws SolrServerException, IOException {
        adminDataverseRequest = new DataverseRequest(authenticationServiceBean.getAdminUser(), IpAddress.valueOf("127.0.0.1"));
        guestDataverseRequest = new DataverseRequest(GuestUser.get(), IpAddress.valueOf("127.0.0.1"));
        
        FacesContextMocker.mockServletRequest();
        solrIndexCleaner.cleanupSolrIndex();
    }
    
    // -------------------- TESTS --------------------
    
    @Test
    public void search__with_query() throws SearchException {
        // given
        List<Dataverse> dataverses = Collections.singletonList(dataverseDao.findRootDataverse());
        List<String> filters = Lists.newArrayList("dvObjectType:(dataverses OR datasets OR files)");
        
        // when & then
        assertSearchResultIds(searchService.search(adminDataverseRequest, dataverses, "title:only", filters, "dateSort", SortOrder.desc, 0, true, 20, false),
                "dataset_66_draft");
        assertSearchResultIds(searchService.search(adminDataverseRequest, dataverses, "only", filters, "dateSort", SortOrder.desc, 0, true, 20, false),
                "dataset_66_draft");
        assertSearchResultIds(searchService.search(adminDataverseRequest, dataverses, "dateOfDeposit:2019", filters, "dateSort", SortOrder.desc, 0, true, 20, false),
                "dataset_66_draft", "dataset_52_draft");
        assertSearchResultIds(searchService.search(adminDataverseRequest, dataverses, "doi:FK2/MLXK1N", filters, "dateSort", SortOrder.desc, 0, true, 20, false),
                "dataset_52_draft");
        
    }
    
    @Test
    public void search__with_filters() throws SearchException {
        // given
        List<Dataverse> dataverses = Collections.singletonList(dataverseDao.findRootDataverse());
        List<String> filters1 = Lists.newArrayList(
                "publicationStatus: Published",
                "dvObjectType:(dataverses OR datasets OR files)");
        List<String> filters2 = Lists.newArrayList(
                "authorName_ss: Some author name",
                "dvObjectType:(dataverses OR datasets OR files)");
        List<String> filters3 = Lists.newArrayList(
                "fileTypeGroupFacet: ZIP",
                "dvObjectType:(dataverses OR datasets OR files)");
        
        // when & then
        assertSearchResultIds(searchService.search(adminDataverseRequest, dataverses, "*", filters1, "dateSort", SortOrder.desc, 0, true, 20, false),
                "dataset_56", "dataverse_19");
        assertSearchResultIds(searchService.search(adminDataverseRequest, dataverses, "*", filters2, "dateSort", SortOrder.desc, 0, true, 20, false),
                "dataset_66_draft", "dataset_52_draft");
        assertSearchResultIds(searchService.search(adminDataverseRequest, dataverses, "*", filters3, "dateSort", SortOrder.desc, 0, true, 20, false),
                "datafile_55_draft", "datafile_53_draft");
    }
    
    @Test
    public void search__with_dv_object_filter() throws SearchException {
        // given
        List<Dataverse> dataverses = Collections.singletonList(dataverseDao.findRootDataverse());
        List<String> dataverseOnlyFilters = Collections.singletonList("dvObjectType:(dataverses)");
        List<String> filesOnlyFilters = Collections.singletonList("dvObjectType:(files)");
        List<String> datasetsAndFilesFilters = Collections.singletonList("dvObjectType:(datasets OR files)");
        
        // when & then
        assertSearchResultIds(searchService.search(adminDataverseRequest, dataverses, "*", dataverseOnlyFilters, "dateSort", SortOrder.desc, 0, true, 20, false),
                "dataverse_51", "dataverse_19", "dataverse_67");
        assertSearchResultIds(searchService.search(adminDataverseRequest, dataverses, "*", filesOnlyFilters, "dateSort", SortOrder.desc, 0, true, 20, false),
                "datafile_55_draft", "datafile_53_draft");
        assertSearchResultIds(searchService.search(adminDataverseRequest, dataverses, "*", datasetsAndFilesFilters, "dateSort", SortOrder.desc, 0, true, 20, false),
                "dataset_56_draft", "dataset_56",
                "dataset_66_draft", "datafile_55_draft",
                "datafile_53_draft", "dataset_52_draft");
    }
    
    @Test
    public void search__with_sorting() throws SearchException {
        // given
        List<Dataverse> dataverses = Collections.singletonList(dataverseDao.findRootDataverse());
        List<String> filters = Collections.singletonList("dvObjectType:(dataverses OR datasets OR files)");
        
        // when & then
        assertSearchResultIds(searchService.search(adminDataverseRequest, dataverses, "*", filters, "dateSort", SortOrder.desc, 0, true, 20, false),
                "dataset_56_draft", "dataset_56", "dataset_66_draft",
                "datafile_55_draft", "datafile_53_draft", // both have the same create date
                "dataset_52_draft", "dataverse_51", "dataverse_19", "dataverse_67");
        assertSearchResultIds(searchService.search(adminDataverseRequest, dataverses, "*", filters, "dateSort", SortOrder.asc, 0, true, 20, false),
                "dataverse_67", "dataverse_19", "dataverse_51", "dataset_52_draft",
                "datafile_55_draft", "datafile_53_draft", // both have the same create date
                "dataset_66_draft", "dataset_56_draft", "dataset_56");
        assertSearchResultIds(searchService.search(adminDataverseRequest, dataverses, "*", filters, "nameSort", SortOrder.asc, 0, true, 20, false),
                "dataset_66_draft", "dataset_52_draft", "dataverse_19",
                "datafile_55_draft", "datafile_53_draft", "dataverse_51", "dataverse_67",
                "dataset_56_draft", "dataset_56");  // don't have any name (no title)
    }

    @Test
    public void search__with_paging() throws SearchException {
        // given
        List<Dataverse> dataverses = Collections.singletonList(dataverseDao.findRootDataverse());
        List<String> filters = Collections.singletonList("dvObjectType:(dataverses OR datasets OR files)");
        
        // when & then
        assertSearchResultIds(searchService.search(adminDataverseRequest, dataverses, "*", filters, "dateSort", SortOrder.desc, 0, true, 3, false),
                "dataset_56_draft", "dataset_56", "dataset_66_draft");
        assertSearchResultIds(searchService.search(adminDataverseRequest, dataverses, "*", filters, "dateSort", SortOrder.desc, 2, true, 1, false),
                "dataset_66_draft");
        assertSearchResultIds(searchService.search(adminDataverseRequest, dataverses, "*", filters, "dateSort", SortOrder.desc, 6, true, 100, false),
                "dataverse_51", "dataverse_19", "dataverse_67");
    }
    
    @Test
    public void search__by_guest_user() throws SearchException {
        // when
        SolrQueryResponse searchResponse = searchService.search(guestDataverseRequest, Collections.singletonList(dataverseDao.findRootDataverse()),
                "*", Lists.newArrayList("dvObjectType:(dataverses OR datasets OR files)"), "dateSort", SortOrder.desc, 0, true, 20);
        
        // then
        assertSearchResultIds(searchResponse, "dataset_56", "dataverse_19");
    }
    
    @Test
    public void search__query_all() throws SearchException {
        
        // when
        SolrQueryResponse searchResponse = searchService.search(adminDataverseRequest, Collections.singletonList(dataverseDao.findRootDataverse()),
                "*", Lists.newArrayList("dvObjectType:(dataverses OR datasets OR files)"), "dateSort", SortOrder.desc, 0, true, 20);
        
        // then
        assertSearchResultIds(searchResponse, "dataset_56_draft", "dataset_56", "dataset_66_draft",
                "datafile_55_draft", "datafile_53_draft", "dataset_52_draft", "dataverse_51", "dataverse_19", "dataverse_67");
        
        assertEquals(Long.valueOf(9), searchResponse.getNumResultsFound());
        assertEquals(Long.valueOf(0), searchResponse.getResultsStart());
        
        assertThat(searchResponse.getSpellingSuggestionsByToken(), aMapWithSize(0));

        assertThat(searchResponse.getDvObjectCounts().keySet(), containsInAnyOrder("dataverses_count", "datasets_count", "files_count"));
        assertEquals(Long.valueOf(3), searchResponse.getDvObjectCounts().get("dataverses_count"));
        assertEquals(Long.valueOf(4), searchResponse.getDvObjectCounts().get("datasets_count"));
        assertEquals(Long.valueOf(2), searchResponse.getDvObjectCounts().get("files_count"));
        
        assertThat(searchResponse.getPublicationStatusCounts().keySet(), containsInAnyOrder("unpublished_count", "draft_count", "published_count"));
        assertEquals(Long.valueOf(6), searchResponse.getPublicationStatusCounts().get("unpublished_count"));
        assertEquals(Long.valueOf(5), searchResponse.getPublicationStatusCounts().get("draft_count"));
        assertEquals(Long.valueOf(2), searchResponse.getPublicationStatusCounts().get("published_count"));
        
        
        assertThat(searchResponse.getDatasetfieldFriendlyNamesBySolrField(), aMapWithSize(0));
        assertThat(searchResponse.getStaticSolrFieldFriendlyNamesBySolrField(), aMapWithSize(0));
        
        assertThat(searchResponse.getFilterQueriesActual(),
                containsInAnyOrder("dvObjectType:(dataverses OR datasets OR files)"));
        
        
        FacetCategory typeFacet = searchResponse.getTypeFacetCategory();
        
        assertContainsFacetCount(typeFacet, "dataverses", 3);
        assertContainsFacetCount(typeFacet, "datasets", 4);
        assertContainsFacetCount(typeFacet, "files", 2);
        
        List<FacetCategory> facets = searchResponse.getFacetCategoryList();
        assertThat(facets.stream().map(f -> f.getName()).collect(Collectors.toList()),
                containsInAnyOrder("dvCategory", "publicationStatus", "publicationDate",
                        "authorAffiliation_ss", "dateOfDeposit_ss", "subject_ss",
                        "authorName_ss", "fileAccess", "fileTag", "fileTypeGroupFacet"));
        
        FacetCategory dvCategoryFacet = extractFacetWithName(facets, "dvCategory");
        assertContainsFacetCount(dvCategoryFacet, "Journal", 1);
        assertContainsFacetCount(dvCategoryFacet, "Organization or Institution", 2);
        
        FacetCategory publicationStatusFacet = extractFacetWithName(facets, "publicationStatus");
        assertContainsFacetCount(publicationStatusFacet, "Draft", 5);
        assertContainsFacetCount(publicationStatusFacet, "Unpublished", 6);
        assertContainsFacetCount(publicationStatusFacet, "Published", 2);
        
        FacetCategory publicationDateFacet = extractFacetWithName(facets, "publicationDate");
        assertContainsFacetCount(publicationDateFacet, "2019", 1);
        
        FacetCategory authorAffilliationFacet = extractFacetWithName(facets, "authorAffiliation_ss");
        assertContainsFacetCount(authorAffilliationFacet, "author affiliation", 1);
        
        FacetCategory dateOfDepositFacet = extractFacetWithName(facets, "dateOfDeposit_ss");
        assertContainsFacetCount(dateOfDepositFacet, "2019", 2);
        
        FacetCategory subjectFacet = extractFacetWithName(facets, "subject_ss");
        assertContainsFacetCount(subjectFacet, "Arts and Humanities", 1);
        assertContainsFacetCount(subjectFacet, "Astronomy and Astrophysics", 1);
        
        FacetCategory authorNameFacet = extractFacetWithName(facets, "authorName_ss");
        assertContainsFacetCount(authorNameFacet, "Some author name", 2);

        FacetCategory fileAccessFacet = extractFacetWithName(facets, "fileAccess");
        assertContainsFacetCount(fileAccessFacet, "Public", 2);
        
        FacetCategory fileTagFacet = extractFacetWithName(facets, "fileTag");
        assertContainsFacetCount(fileTagFacet, "Code", 2);

        FacetCategory fileTypeGroupFacet = extractFacetWithName(facets, "fileTypeGroupFacet");
        assertContainsFacetCount(fileTypeGroupFacet, "ZIP", 2);
        
    }
    
    // -------------------- PRIVATE --------------------
    
    private void assertContainsFacetCount(FacetCategory actualFacet, String expectedName, long expectedCount) {
        FacetLabel actualFacetLabel = null;
        for (FacetLabel facetLabel: actualFacet.getFacetLabel()) {
            if (StringUtils.equals(facetLabel.getName(), expectedName)) {
                actualFacetLabel = facetLabel;
                break;
            }
        }
        assertNotNull(actualFacetLabel);
        assertEquals(Long.valueOf(expectedCount), actualFacetLabel.getCount());
    }
    
    private FacetCategory extractFacetWithName(List<FacetCategory> facetCategories, String name) {
        return facetCategories.stream()
                .filter(f -> f.getName().equals(name))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }
    
    private void assertSearchResultIds(SolrQueryResponse queryResponse, String ... expectedSolrDocIds) {
        
        assertThat(queryResponse.getSolrSearchResults().stream().map(r -> r.getId()).collect(Collectors.toList()),
                contains(expectedSolrDocIds));
        
    }
}
