/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.mydata;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import edu.harvard.iq.dataverse.search.SearchFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang.StringUtils;

/**
 * Convenience methods for formatting long arrays of ids into solrQuery strings
 * @author rmp553
 */
public class SolrQueryFormatter {
    
    public static int SOLR_ID_GROUP_SIZE = 1000; 
    
    public void setSolrIdGroupSize(int groupSize){
        SOLR_ID_GROUP_SIZE = groupSize;
    }
    
    /**
     *
     * @param sliceOfIds
     * @param paramName
     * @return
     */
    private String formatIdsForSolrClause(List<Long> sliceOfIds, String paramName,  String dvObjectType){ //='entityId'):
        if (paramName == null){
            throw new NullPointerException("paramName cannot be null");
        }
        if (sliceOfIds == null){
            throw new NullPointerException("sliceOfIds cannot be null");
        }
        if (sliceOfIds.isEmpty()){
            throw new IllegalStateException("sliceOfIds must have at least 1 value");
        }
        
        List<String> idList = new ArrayList<>();        
        for (Long id : sliceOfIds) {          
            if (id != null){
                idList.add("" + id);
            }
        }
        String orClause = StringUtils.join(idList, " ");
                            String qPart = "(" + paramName  + ":(" + orClause + "))";
        if (dvObjectType != null){
             qPart = "(" + paramName  + ":(" + orClause + ") AND " + SearchFields.TYPE + ":(" +  dvObjectType + "))";
             //valStr;
        }

        return qPart;
    }
    
    
    /**
     *  SOLR cannot parse over 1024 items in a boolean clause
     *   Group IDs in batches of 1000
     * @param idList
     * @param paramName
     * @return 
     */    
    public String buildIdQuery(Set<Long> idListSet, String paramName, String dvObjectType){
        if (paramName == null){
            throw new NullPointerException("paramName cannot be null");
        }
        if ((idListSet == null)||(idListSet.isEmpty())){
            return null;
        }
        
        List<Long> idList = new ArrayList<>(idListSet);
        int numIds = idList.size();
        
        List<String> queryClauseParts = new ArrayList<>();
        int idCnt = 0;

        int numFullGroups = numIds / this.SOLR_ID_GROUP_SIZE;
        List<Long> sliceOfIds; 
        
        // -------------------------------------------
        // Ids in groups of SOLR_ID_GROUP_SIZE (1,000)
        // -------------------------------------------
        for (int current_group_num=0; current_group_num < numFullGroups; current_group_num++){
            // slice group of ids off
            //
            sliceOfIds = idList.subList(idCnt, this.SOLR_ID_GROUP_SIZE * (current_group_num+1));
            
            // add them to the count
            idCnt += sliceOfIds.size();
            
            // format ids into solr OR clause
            //
            queryClauseParts.add(this.formatIdsForSolrClause(sliceOfIds, paramName, dvObjectType));
        }

        // -------------------------------------------
        // Extra ids not evenly divisible by SOLR_ID_GROUP_SIZE
        // -------------------------------------------
        int extraIdCount = numIds % this.SOLR_ID_GROUP_SIZE;

        if (extraIdCount > 0){
            // slice group of ids off
            //
            sliceOfIds = idList.subList(idCnt, idCnt + extraIdCount);
            
            // format ids into solr OR clause
            //
            queryClauseParts.add(this.formatIdsForSolrClause(sliceOfIds, paramName, dvObjectType));        
        }
        
        return StringUtils.join(queryClauseParts, " OR ");
        
    }
}
