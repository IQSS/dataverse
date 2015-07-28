/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.util.ArrayList;

/**
 *
 * @author skraffmiller
 */
public class MyDataQueryHelper {

    private AuthenticatedUser user;
    private ArrayList<String> dvObjectTypes;
    private ArrayList<Long> directIds;
    private ArrayList<Long> parentIds;
    private final MyDataQueryHelperServiceBean myDataQueryHelperService;

    public MyDataQueryHelper(AuthenticatedUser user, MyDataQueryHelperServiceBean injectedBean) {
        this.user = user;
        myDataQueryHelperService = injectedBean;
        initializeLists();
    }

    private void initializeLists() {
        initializeParentIds();
        initializeDirectIds();
    }

    private void initializeParentIds() {
        parentIds = new ArrayList();
        parentIds.addAll(myDataQueryHelperService.getParentIds("Dataverse", "DataFile",  this.user));
        parentIds.addAll(myDataQueryHelperService.getParentIds("Dataset", "DataFile",  this.user));
        parentIds.addAll(myDataQueryHelperService.getParentIds("Dataverse", "Dataset",  this.user));
    }

    private void initializeDirectIds() {
        directIds = new ArrayList();
        directIds.addAll(myDataQueryHelperService.getDirectQuery(this.user).getResultList());
    }

    public String getSolrQueryString() {
        /*(entityId:(20 11 592 7 17 24 14 15 21 18 25 19 22 23 12 2 8 3 16 4 9 5 13 6 10))*/

        String retPrimaryString = "entityId:(";
        if (this.directIds != null && !this.directIds.isEmpty()) {
            for (Object id : this.directIds) {
                Integer r1 = (Integer) id;
                retPrimaryString += " " + r1;
            }
        }
        retPrimaryString += ")";

        String retParentString;

        if (!getParentIds().isEmpty()) {
            retParentString = "parentId:(";

            for (Long id : getParentIds()) {
                retParentString += " " + id;
            }
            retParentString += ")";
            return "" + retPrimaryString + " OR " + retParentString + "";
        }
        return "" + retPrimaryString;
    }

    public AuthenticatedUser getUser() {
        return user;
    }

    public void setUser(AuthenticatedUser user) {
        this.user = user;
    }

    public ArrayList<String> getDvObjectTypes() {
        return dvObjectTypes;
    }

    public void setDvObjectTypes(ArrayList<String> dvObjectTypes) {
        this.dvObjectTypes = dvObjectTypes;
    }

    public ArrayList<Long> getParentIds() {
        return parentIds;
    }

    public void setParentIds(ArrayList<Long> parentIds) {
        this.parentIds = parentIds;
    }

}
