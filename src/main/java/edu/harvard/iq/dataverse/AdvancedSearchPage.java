package edu.harvard.iq.dataverse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

@ViewScoped
@Named("AdvancedSearchPage")
public class AdvancedSearchPage {

    private static final Logger logger = Logger.getLogger(AdvancedSearchPage.class.getCanonicalName());

    @EJB
    DataverseServiceBean dataverseServiceBean;

    @EJB
    DatasetFieldServiceBean datasetFieldService;

    private Dataverse dataverse;
    private String query;
    private List<MetadataBlock> metadataBlocks;
    private Map<Long,List<DatasetField>> metadataFieldMap = new HashMap();
    private List<DatasetField> metadataFieldList;    


    public void init() {
        /**
         * @todo: support advanced search at any depth in the dataverse
         * hierarchy
         */
        this.dataverse = dataverseServiceBean.findRootDataverse();
        this.metadataBlocks = dataverseServiceBean.findAllMetadataBlocks();
        this.metadataFieldList = datasetFieldService.findAllAdvancedSearchFields();

        for (MetadataBlock mdb : metadataBlocks) {
           
            List datasetFields = new ArrayList();
            for (DatasetField datasetField : metadataFieldList) {
                if (datasetField.getMetadataBlock().getId().equals(mdb.getId())) {
                    datasetFields.add(datasetField);
                }
            }
            metadataFieldMap.put(mdb.getId(), datasetFields);
        }       
        
    }

    public String find() throws IOException {
        /*
         logger.info("clicked find. author: " + author + ". title: " + title);
         List<String> queryStrings = new ArrayList();
         if (title != null && !title.isEmpty()) {
         queryStrings.add(SearchFields.TITLE + ":" + title);
         }

         if (author != null && !author.isEmpty()) {
         queryStrings.add(SearchFields.AUTHOR_STRING + ":" + author);
         }
         query = new String();
         for (String string : queryStrings) {
         query += string + " ";
         }
         logger.info("query: " + query); */
        List<String> queryStrings = new ArrayList();
        String delimiter = "[\"]+";
        for (DatasetField datasetField : metadataFieldList) {
            if (datasetField.getSearchValue() != null && !datasetField.getSearchValue().equals("")) {
                String myString = datasetField.getSearchValue();
                if (myString.contains("\"")) {
                    String [] tempString = datasetField.getSearchValue().split(delimiter);
                    for (int i = 1; i < tempString.length; i++) {
                        if (!tempString[i].equals(" ") && !tempString[i].isEmpty()) {
                            queryStrings.add(datasetField.getSolrField().getNameSearchable() + ":" + "\"" + tempString[i].trim() + "\"");
                        }
                    }
                } else {
                    StringTokenizer st = new StringTokenizer(datasetField.getSearchValue());
                    while (st.hasMoreElements()) {
                        queryStrings.add(datasetField.getSolrField().getNameSearchable() + ":" + st.nextElement());
                    }
                } 
            } else if (datasetField.getListValues() != null && !datasetField.getListValues().isEmpty()){
                for (String value : datasetField.getListValues()) {
                    queryStrings.add(datasetField.getSolrField().getNameSearchable() + ":" + "\"" + value + "\"");
                }
            }
        }

        for (String string : queryStrings) {
            query += " " + string;
        }
        return "/dataverse.xhtml?q=" + query.trim() + "faces-redirect=true";
    }

    public Dataverse getDataverse() {
        return dataverse;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;

    }


    public List<MetadataBlock> getMetadataBlocks() {
        return metadataBlocks;
    }

    public void setMetadataBlocks(List<MetadataBlock> metadataBlocks) {
        this.metadataBlocks = metadataBlocks;
    }

    public Map<Long, List<DatasetField>> getMetadataFieldMap() {
        return metadataFieldMap;
    }

    public void setMetadataFieldMap(Map<Long, List<DatasetField>> metadataFieldMap) {
        this.metadataFieldMap = metadataFieldMap;
    }
}
