package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.api.SearchFields;
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
    private List<MetadataBlock> metadataBlocks;
    private Map<Long,List<DatasetFieldType>> metadataFieldMap = new HashMap();
    private List<DatasetFieldType> metadataFieldList;    
    private String dvFieldName;
    private String dvFieldDescription;
    private String dvFieldAffiliation;
    private String fileFieldName;
    private String fileFieldDescription;
    private String fileFieldFiletype;

    public void init() {
        /**
         * @todo: support advanced search at any depth in the dataverse
         * hierarchy https://redmine.hmdc.harvard.edu/issues/3894
         */
        this.dataverse = dataverseServiceBean.findRootDataverse();
        this.metadataBlocks = dataverseServiceBean.findAllMetadataBlocks();
        this.metadataFieldList = datasetFieldService.findAllAdvancedSearchFieldTypes();

        for (MetadataBlock mdb : metadataBlocks) {
           
            List dsfTypes = new ArrayList();
            for (DatasetFieldType dsfType : metadataFieldList) {
                if (dsfType.getMetadataBlock().getId().equals(mdb.getId())) {
                    dsfTypes.add(dsfType);
                }
            }
            metadataFieldMap.put(mdb.getId(), dsfTypes);
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
        StringBuilder queryBuilder = new StringBuilder();
        
        String delimiter = "[\"]+";
        for (DatasetFieldType dsfType : metadataFieldList) {
            List<String> queryStrings = new ArrayList();            
            if (dsfType.getSearchValue() != null && !dsfType.getSearchValue().equals("")) {
                String myString = dsfType.getSearchValue();
                if (myString.contains("\"")) {
                    String [] tempString = dsfType.getSearchValue().split(delimiter);
                    for (int i = 1; i < tempString.length; i++) {
                        if (!tempString[i].equals(" ") && !tempString[i].isEmpty()) {
                            queryStrings.add(dsfType.getSolrField().getNameSearchable() + ":" + "\"" + tempString[i].trim() + "\"");
                        }
                    }
                } else {
                    StringTokenizer st = new StringTokenizer(dsfType.getSearchValue());
                    while (st.hasMoreElements()) {
                        queryStrings.add(dsfType.getSolrField().getNameSearchable() + ":" + st.nextElement());
                    }
                } 
            } else if (dsfType.getListValues() != null && !dsfType.getListValues().isEmpty()){
                for (String value : dsfType.getListValues()) {
                    queryStrings.add(dsfType.getSolrField().getNameSearchable() + ":" + "\"" + value + "\"");
                }
            }

            if (queryStrings.size() > 0 && queryBuilder.length() > 0 ) {
                queryBuilder.append(" AND ");
            }            
            
            if (queryStrings.size() > 1) {
                queryBuilder.append("(");
            }
            
            for (int i = 0; i < queryStrings.size(); i++) {
                if ( i > 0 ) {
                    queryBuilder.append(" ");
                }                 
                queryBuilder.append(queryStrings.get(i));
            }
            
            if (queryStrings.size() > 1) {
                queryBuilder.append(")");
            }            

            /**
             * @todo: What people really want (we think) is fancy combination
             * searches with users typing a little under Dataverses, a little
             * under Datasets, and a little under Files and logic would exist
             * here to construct and OR (or AND?) query. For now, we reset the
             * whole query every time we pass through the if's below.
             *
             * see also https://redmine.hmdc.harvard.edu/issues/3745
             */
            if (!dvFieldName.isEmpty()) {
                queryBuilder = new StringBuilder();
                queryBuilder.append(SearchFields.DATAVERSE_NAME + ":" + dvFieldName);
            }

            if (!dvFieldAffiliation.isEmpty()) {
                queryBuilder = new StringBuilder();
                queryBuilder.append(SearchFields.DATAVERSE_AFFILIATION + ":" + dvFieldAffiliation);
            }

            if (!dvFieldDescription.isEmpty()) {
                queryBuilder = new StringBuilder();
                queryBuilder.append(SearchFields.DATAVERSE_DESCRIPTION + ":" + dvFieldDescription);
            }

            if (!fileFieldName.isEmpty()) {
                queryBuilder = new StringBuilder();
                queryBuilder.append(SearchFields.FILE_NAME + ":" + fileFieldName);
            }

            if (!fileFieldDescription.isEmpty()) {
                queryBuilder = new StringBuilder();
                queryBuilder.append(SearchFields.FILE_DESCRIPTION + ":" + fileFieldDescription);
            }

            if (!fileFieldFiletype.isEmpty()) {
                queryBuilder = new StringBuilder();
                queryBuilder.append(SearchFields.FILE_TYPE_SEARCHABLE + ":" + fileFieldFiletype);
            }

        }

        return "/dataverse.xhtml?q=" + queryBuilder.toString().trim() + "faces-redirect=true";
    }
    

    public Dataverse getDataverse() {
        return dataverse;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    public List<MetadataBlock> getMetadataBlocks() {
        return metadataBlocks;
    }

    public void setMetadataBlocks(List<MetadataBlock> metadataBlocks) {
        this.metadataBlocks = metadataBlocks;
    }

    public Map<Long, List<DatasetFieldType>> getMetadataFieldMap() {
        return metadataFieldMap;
    }

    public void setMetadataFieldMap(Map<Long, List<DatasetFieldType>> metadataFieldMap) {
        this.metadataFieldMap = metadataFieldMap;
    }

    public String getDvFieldName() {
        return dvFieldName;
    }

    public void setDvFieldName(String dvFieldName) {
        this.dvFieldName = dvFieldName;
    }

    public String getDvFieldDescription() {
        return dvFieldDescription;
    }

    public void setDvFieldDescription(String dvFieldDescription) {
        this.dvFieldDescription = dvFieldDescription;
    }

    public String getDvFieldAffiliation() {
        return dvFieldAffiliation;
    }

    public void setDvFieldAffiliation(String dvFieldAffiliation) {
        this.dvFieldAffiliation = dvFieldAffiliation;
    }

    public String getFileFieldName() {
        return fileFieldName;
    }

    public void setFileFieldName(String fileFieldName) {
        this.fileFieldName = fileFieldName;
    }

    public String getFileFieldDescription() {
        return fileFieldDescription;
    }

    public void setFileFieldDescription(String fileFieldDescription) {
        this.fileFieldDescription = fileFieldDescription;
    }

    public String getFileFieldFiletype() {
        return fileFieldFiletype;
    }

    public void setFileFieldFiletype(String fileFieldFiletype) {
        this.fileFieldFiletype = fileFieldFiletype;
    }

}
