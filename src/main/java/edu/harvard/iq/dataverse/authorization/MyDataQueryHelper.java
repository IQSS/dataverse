/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.ejb.EJB;

/**
 *
 * @author skraffmiller
 */

public class MyDataQueryHelper {

    private AuthenticatedUser user;
    private ArrayList<DataverseRole> roles;
    private ArrayList<String> dvObjectTypes;
    private Boolean publishedOnly = false;
    private String searchTerm = "*";
    private ArrayList<Long> dataverseIds;
    private ArrayList<Long> primaryDatasetIds;
    private ArrayList<Long> primaryFileIds;
    private ArrayList<Long> secondaryDatasetIds;
    private ArrayList<Long> secondaryFileIds;

    private MyDataQueryHelperServiceBean myDataQueryHelperService;



    public MyDataQueryHelper(AuthenticatedUser user, ArrayList<DataverseRole> roles, ArrayList<String> dvObjectTypes, Boolean publishedOnly, String searchTerm, MyDataQueryHelperServiceBean injectedBean) {
        
        this.user = user;
        this.roles = roles;
        this.dvObjectTypes = dvObjectTypes;

        if (publishedOnly != null) {
            this.publishedOnly = publishedOnly;
        }
        
        if (searchTerm != null && !searchTerm.isEmpty()) {
            this.searchTerm = searchTerm;
        }
        myDataQueryHelperService = injectedBean;
        initializeLists();
    }

    private void initializeLists() {
        initializeDataverseIds();
        initializePrimaryDatasetIds();
        initializePrimaryFileIds();
        //initializeSecondaryDatasetIds();
        //initializeSecondaryFileIds();
    }

    private ArrayList<Long> initializeDirectList(String dtype) {
        List<Integer> dataverseIdsAdd = myDataQueryHelperService.getDirectQuery(dtype, this.roles, this.user ).getResultList();
        ArrayList<Long> dvObjectsUserHasPermissionOn = new ArrayList<>();
        for (int dvIdAsInt : dataverseIdsAdd) {
            dvObjectsUserHasPermissionOn.add(Long.valueOf(dvIdAsInt));
        }
        return dvObjectsUserHasPermissionOn;
    }

    private ArrayList<Long> initializeSecondaryList(String indirectParentIds) {
        List<Integer> dataverseIdsAdd = myDataQueryHelperService.getIndirectQuery(indirectParentIds).getResultList();
        ArrayList<Long> dvObjectsUserHasPermissionOn = new ArrayList<>();
        for (int dvIdAsInt : dataverseIdsAdd) {
            dvObjectsUserHasPermissionOn.add(Long.valueOf(dvIdAsInt));
        }
        return dvObjectsUserHasPermissionOn;
    }

    private String convertLongArrayToString(ArrayList<Long> arrayIn) {
        String retVal = "";
        boolean first = true;
        for (Long val : arrayIn) {
            if (!first) {
                retVal += ", " + val.toString();
            } else {
                retVal = val.toString();
                first = false;
            }
        }
        return retVal;
    }

    private void initializeDataverseIds() {
        setDataverseIds(initializeDirectList("Dataverse"));
    }

    private void initializePrimaryDatasetIds() {
        setPrimaryDatasetIds(initializeDirectList("Dataset"));
    }

    private void initializePrimaryFileIds() {
        if (getTypesClause(this.getDvObjectTypes()).contains("DataFile")) {
            setPrimaryFileIds(initializeDirectList("DataFile"));
        } else {
            setPrimaryFileIds(new ArrayList());
        }
    }

    private void initializeSecondaryDatasetIds() {
        setSecondaryDatasetIds(initializeSecondaryList(convertLongArrayToString(getDataverseIds())));
    }

    private void initializeSecondaryFileIds() {
        if (getTypesClause(this.getDvObjectTypes()).contains("DataFile")) {
            setSecondaryFileIds(initializeSecondaryList(convertLongArrayToString(getPrimaryDatasetIds()) + convertLongArrayToString(getSecondaryDatasetIds())));
        } else {
            setSecondaryFileIds(new ArrayList());
        }
    }

    public String getSolrQueryString() {
        /*(entityId:(20 11 592 7 17 24 14 15 21 18 25 19 22 23 12 2 8 3 16 4 9 5 13 6 10))*/
        System.out.print("in get solr query string");
        String retPrimaryString = "entityId:(";
        boolean firstPrimary = true;
        if (getTypesClause(this.getDvObjectTypes()).contains("Dataverse") && getDataverseIds() != null) {

            for (Long id : getDataverseIds()) {

                if (firstPrimary) {
                    retPrimaryString += " " + id;
                    firstPrimary = false;
                } else {
                    retPrimaryString += " OR " + id;
                }

            }
        }
        if (getTypesClause(this.getDvObjectTypes()).contains("Dataset") && getPrimaryDatasetIds() != null) {
            for (Long id : getPrimaryDatasetIds()) {
                if (firstPrimary) {
                    retPrimaryString += " " + id;
                    firstPrimary = false;
                } else {
                    retPrimaryString += " OR " + id;
                }
            }
        }
        if (getTypesClause(this.getDvObjectTypes()).contains("DataFile") && getPrimaryFileIds() != null) {
            for (Long id : getPrimaryFileIds()) {
                if (firstPrimary) {
                    retPrimaryString += " " + id;
                    firstPrimary = false;
                } else {
                    retPrimaryString += " OR " + id;
                }
            }
        }
        retPrimaryString += ")";
        
        boolean firstParent = true;
        String retParentString = "parentId:(";
        if (getTypesClause(this.getDvObjectTypes()).contains("Dataset") && getDataverseIds() != null) {

            for (Long id : getDataverseIds()) {

                if (firstParent) {
                    retParentString += " " + id;
                    firstParent = false;
                } else {
                    retParentString += " OR " + id;
                }
            }
        }
        if (getTypesClause(this.getDvObjectTypes()).contains("DataFile")) {
            if (getPrimaryDatasetIds() != null) {
                for (Long id : getPrimaryDatasetIds()) {
                    
                    if (firstParent) {
                        retParentString += " " + id;
                        firstParent = false;
                    } else {
                        retParentString += " OR " + id;
                    }
                }
            }
            if (getSecondaryDatasetIds() != null) {
                for (Long id : getSecondaryDatasetIds()) {
                    if (firstParent) {
                        retParentString += " " + id;
                        firstParent = false;
                    } else {
                        retParentString += " OR " + id;
                    }
                }
            }

        }
        retParentString += ")";
        
        System.out.print("retPrimaryString " + retPrimaryString);
         System.out.print("retParentString " + retParentString);
       // return "" + retPrimaryString + " OR " + retParentString + "";
         return retPrimaryString;
    }



    private String getTypesClause(List<String> types) {
        boolean firstType = true;
        String typeString = " dtype in ('Dataverse', 'Dataset')";
        if (types != null && !types.isEmpty()) {
            typeString = " dtype in (";
            for (String type : types) {
                if (!firstType) {
                    typeString += ",";
                }
                typeString += "'" + type + "'";
            }
            typeString += ") and ";
        }
        return typeString;
    }
    
    private Boolean doesRoleApply(DataverseRole role, String dvObjectType) {
        Boolean retVal = false;
        if (dvObjectType != null) {

            switch (dvObjectType) {
                case "Dataset":
                    for (Permission permission : role.permissions()) {
                        if (permission.appliesTo(Dataset.class) || permission.appliesTo(DataFile.class)) {
                            retVal = true;
                            break;
                        }
                    }
                    break;
                case "DataFile":
                    if (role.permissions().contains(Permission.DownloadFile)){
                        retVal = true;
                    }
                    break;
            }

        }
        return retVal;
    }

    public AuthenticatedUser getUser() {
        return user;
    }

    public void setUser(AuthenticatedUser user) {
        this.user = user;
    }

    public ArrayList<DataverseRole> getRoles() {
        return roles;
    }

    public void setRoles(ArrayList<DataverseRole> roles) {
        this.roles = roles;
    }

    public ArrayList<String> getDvObjectTypes() {
        return dvObjectTypes;
    }

    public void setDvObjectTypes(ArrayList<String> dvObjectTypes) {
        this.dvObjectTypes = dvObjectTypes;
    }

    public boolean isPublishedOnly() {
        return publishedOnly;
    }

    public void setPublishedOnly(boolean publishedOnly) {
        this.publishedOnly = publishedOnly;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public ArrayList<Long> getDataverseIds() {
        return dataverseIds;
    }

    public void setDataverseIds(ArrayList<Long> dataverseIds) {
        this.dataverseIds = dataverseIds;
    }

    public ArrayList<Long> getPrimaryDatasetIds() {
        return primaryDatasetIds;
    }

    public void setPrimaryDatasetIds(ArrayList<Long> primaryDatasetIds) {
        this.primaryDatasetIds = primaryDatasetIds;
    }

    public ArrayList<Long> getPrimaryFileIds() {
        return primaryFileIds;
    }

    public void setPrimaryFileIds(ArrayList<Long> primaryFileIds) {
        this.primaryFileIds = primaryFileIds;
    }

    public ArrayList<Long> getSecondaryDatasetIds() {
        return secondaryDatasetIds;
    }

    public void setSecondaryDatasetIds(ArrayList<Long> secondaryDatasetIds) {
        this.secondaryDatasetIds = secondaryDatasetIds;
    }

    public ArrayList<Long> getSecondaryFileIds() {
        return secondaryFileIds;
    }

    public void setSecondaryFileIds(ArrayList<Long> secondaryFileIds) {
        this.secondaryFileIds = secondaryFileIds;
    }
    
    public Boolean getPublishedOnly() {
        return publishedOnly;
    }

    public void setPublishedOnly(Boolean publishedOnly) {
        this.publishedOnly = publishedOnly;
    }

}
