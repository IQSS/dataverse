package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.api.SearchFields;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
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
    private String title;
    private String author;
    private List<DatasetField> metadataFieldList;


    public void init() {
        /**
         * @todo: support advanced search at any depth in the dataverse
         * hierarchy
         */
        this.dataverse = dataverseServiceBean.findRootDataverse();
        this.metadataFieldList = datasetFieldService.findAllAdvancedSearchFields();          
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
            if (!"".equals(datasetField.getSearchValue())) {
                String myString = datasetField.getSearchValue();
                if (myString.contains("\"")) {
                    String [] tempString = datasetField.getSearchValue().split(delimiter);
                    for (int i = 1; i < tempString.length; i++) {
                        if (!tempString[i].equals(" ") && !tempString[i].isEmpty()) {
                            queryStrings.add(datasetField.getSolrField() + ":" + "\"" + tempString[i].trim() + "\"");
                        }
                    }
                } else {
                    StringTokenizer st = new StringTokenizer(datasetField.getSearchValue());
                    while (st.hasMoreElements()) {
                        queryStrings.add(datasetField.getSolrField() + ":" + st.nextElement());
                    }
                } 
            }
        }
        query = new String();
        for (String string : queryStrings) {
            query += string + " ";
        }
        return "/dataverse.xhtml?q=" + query + "faces-redirect=true";
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public List getMetadataFieldList() {
        List metadataBlock = new ArrayList();
        for (int i = 1; i < 6; i++) {
            List metadata = new ArrayList();
            for (DatasetField datasetField : metadataFieldList) {
                if (datasetField.getMetadataBlock().getId() == i) {
                    metadata.add(datasetField);
                }
            }
            metadataBlock.add(metadata);
        }
        return metadataBlock;
    }
}
