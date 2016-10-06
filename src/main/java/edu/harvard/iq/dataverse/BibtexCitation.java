/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author gdurand
 */
public class BibtexCitation {

    private List<String> authors = new ArrayList();
    private String title;
    private String year;
    private GlobalId persistentId;
    private String publisher;

    public BibtexCitation(DatasetVersion dsv) {        
        //authors
        dsv.getDatasetAuthors().stream().forEach((author) -> {
            authors.add(author.getName().getDisplayValue());
        });        
        
        // year
        year = dsv.getVersionYear();

        // title
        title = dsv.getTitle();

        // The Global Identifier:
        persistentId = new GlobalId(dsv.getDataset());

        // publisher
        publisher = dsv.getRootDataverseNameforCitation();
    }

    public List<String> getAuthors() {
        return authors;
    }

    public String getTitle() {
        return title;
    }

    public String getYear() {
        return year;
    }

    public GlobalId getPersistentId() {
        return persistentId;
    }

    public String getPublisher() {
        return publisher;
    }
    

    @Override
    public String toString() {
        StringBuilder citation = new StringBuilder("@data{");
        citation.append(persistentId.getIdentifier() + "_" + year + "," + "\r\n");
        citation.append("author = {").append(String.join(" and ", authors)).append("},\r\n");
        citation.append("publisher = {").append(publisher).append("},\r\n");
        citation.append("title = {").append(title).append("},\r\n");        
        citation.append("year = {").append(year).append("},\r\n");        
        citation.append("doi = {").append(persistentId.getAuthority()).append("/").append(persistentId.getIdentifier()).append("},\r\n");
        citation.append("url = {").append(persistentId.toURL().toString()).append("}\r\n");
        citation.append("}");

        return citation.toString();
    }
   
}

