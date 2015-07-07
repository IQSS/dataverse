package edu.harvard.iq.dataverse.mydata;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.harvard.iq.dataverse.DatasetPage;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.DvObject;
import static edu.harvard.iq.dataverse.DvObject.DATAFILE_DTYPE_STRING;
import static edu.harvard.iq.dataverse.DvObject.DATASET_DTYPE_STRING;
import static edu.harvard.iq.dataverse.DvObject.DATAVERSE_DTYPE_STRING;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.SearchServiceBean;
import edu.harvard.iq.dataverse.SolrQueryResponse;
import edu.harvard.iq.dataverse.SolrSearchResult;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.DataverseRolePermissionHelper;
import edu.harvard.iq.dataverse.authorization.MyDataQueryHelperServiceBean;
import edu.harvard.iq.dataverse.search.SearchException;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.search.SortBy;
import java.io.IOException;
import static java.lang.Math.max;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.lang.StringUtils;
import org.primefaces.json.JSONException;
import org.primefaces.json.JSONObject;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author rmp553
 */
@ViewScoped
@Named("RolePermissionHelperPage")
public class RolePermissionHelperPage implements java.io.Serializable {
    
    private static final Logger logger = Logger.getLogger(DatasetPage.class.getCanonicalName());

    @Inject DataverseSession session;    

    @EJB
    DataverseRoleServiceBean dataverseRoleService;
    @EJB
    RoleAssigneeServiceBean roleAssigneeService;
    @EJB
    DvObjectServiceBean dvObjectServiceBean;
    @EJB
    SearchServiceBean searchService;
    
    private String testName = "blah";
    private DataverseRolePermissionHelper rolePermissionHelper;// = new DataverseRolePermissionHelper();
    private MyDataFinder myDataFinder;
    private Pager pager;
    private MyDataFilterParams filterParams;
    private SolrQueryResponse solrQueryResponse;
    
    private void msg(String s){
        //System.out.println(s);
    }
    
    private void msgt(String s){
        msg("-------------------------------");
        msg(s);
        msg("-------------------------------");
    }
    
    public String init() {
       // msgt("_YE_OLDE_QUERY_COUNTER_");  // for debug purposes

        List<DataverseRole> roleList = dataverseRoleService.findAll();
        rolePermissionHelper = new DataverseRolePermissionHelper(roleList);

        String userIdentifier = "dataverseAdmin";
        
        List<String> dtypes = MyDataFilterParams.defaultDvObjectTypes;
        //List<String> dtypes = Arrays.asList(DvObject.DATAFILE_DTYPE_STRING, DvObject.DATASET_DTYPE_STRING);
        //DvObject.DATAFILE_DTYPE_STRING, DvObject.DATASET_DTYPE_STRING, DvObject.DATAVERSE_DTYPE_STRING
        
        //List<String> dtypes = new ArrayList<>();
        this.filterParams = new MyDataFilterParams(userIdentifier, dtypes, null, null, null);
        
        this.myDataFinder = new MyDataFinder(rolePermissionHelper,
                                        roleAssigneeService,
                                        dvObjectServiceBean);
        //myDataFinder.runFindDataSteps(userIdentifier);
        this.myDataFinder.runFindDataSteps(filterParams);
        
        if (!this.myDataFinder.hasError()){

            int paginationStart = 1;
            boolean dataRelatedToMe = true;
            int numResultsPerPage = 10;
            msgt("getSolrFilterQueries: " + this.myDataFinder.getSolrFilterQueries().toString());
            try {
                solrQueryResponse = searchService.search(
                        null, // no user
                        null, // subtree, default it to Dataverse for now
                        "*", //this.filterParams.getSearchTerm(),
                        this.myDataFinder.getSolrFilterQueries(),//filterQueries,
                        SearchFields.NAME_SORT, SortBy.ASCENDING,
                        //SearchFields.RELEASE_OR_CREATE_DATE, SortBy.DESCENDING,
                        paginationStart,
                        dataRelatedToMe,
                        10
                );
                msgt("getResultsStart: " + this.solrQueryResponse.getResultsStart());
                msgt("getNumResultsFound: " + this.solrQueryResponse.getNumResultsFound());
                msgt("getSolrSearchResults: " + this.solrQueryResponse.getSolrSearchResults().toString());
                
                //User user,
                //Dataverse dataverse,
                //String query, 
                //List<String> filterQueries, String sortField, String sortOrder, int paginationStart, boolean onlyDatatRelatedToMe, int numResultsPerPage) throws SearchException {
                
            } catch (SearchException ex) {
                solrQueryResponse = null;
                Logger.getLogger(RolePermissionHelperPage.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
        
        this.pager = new Pager(111, 10, 3);
        return null;
    }
    
    
    
    public DataverseRolePermissionHelper getRolePermissionHelper(){
        return this.rolePermissionHelper;
    }
    
}
