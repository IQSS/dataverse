/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

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
public class DataCitation {

    private String authors;
    private String title;
    private String year;
    private GlobalId persistentId;
    private String version;
    private String UNF;
    private String distributors;

    private List<DatasetField> optionalValues = new ArrayList<>();
    private int optionalURLcount = 0; 

    public DataCitation(DatasetVersion dsv) {
        // authors (or producer)
        authors = dsv.getAuthorsStr(false);
        if (StringUtils.isEmpty(authors)) {
            authors = dsv.getDatasetProducersString();
        }

        // year
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
        if (!dsv.getDataset().isHarvested()) {
            Date citationDate = dsv.getCitationDate();
            if (citationDate == null) {
                if (dsv.getDataset().getPublicationDate() != null) {
                    citationDate = dsv.getDataset().getPublicationDate();
                } else { // for drafts
                    citationDate = new Date();
                }
            }

            year = new SimpleDateFormat("yyyy").format(citationDate);

        } else {
            try {
                year = sdf.format(sdf.parse(dsv.getDistributionDate()));
            } catch (ParseException ex) {
                // ignore
            }
        }

        // title
        title = dsv.getTitle();

        // The Global Identifier: 
        // It is always part of the citation for the local datasets; 
        // And for *some* harvested datasets. 
        if (!dsv.getDataset().isHarvested()
                || HarvestingDataverseConfig.HARVEST_STYLE_VDC.equals(dsv.getDataset().getOwner().getHarvestingDataverseConfig().getHarvestStyle())
                || HarvestingDataverseConfig.HARVEST_STYLE_ICPSR.equals(dsv.getDataset().getOwner().getHarvestingDataverseConfig().getHarvestStyle())
                || HarvestingDataverseConfig.HARVEST_STYLE_DATAVERSE.equals(dsv.getDataset().getOwner().getHarvestingDataverseConfig().getHarvestStyle())) {
            if (!StringUtils.isEmpty(dsv.getDataset().getIdentifier())) {
                persistentId = new GlobalId(dsv.getDataset().getGlobalId());
            }
        }

        // distributors
        if (!dsv.getDataset().isHarvested()) {
            distributors = dsv.getRootDataverseNameforCitation();
        } else {
            distributors = dsv.getDistributorName();
            if (!StringUtils.isEmpty(distributors)) {
                distributors += " [distributor]";
            }
        }

        // version
        if (!dsv.getDataset().isHarvested()) {
            if (dsv.isDraft()) {
                version = "DRAFT VERSION";
            } else if (dsv.getVersionNumber() != null) {
                version = "V" + dsv.getVersionNumber();
                if (dsv.isDeaccessioned()) {
                    version += ", DEACCESSIONED VERSION";
                }
            }
        }

        // UNF
        UNF = dsv.getUNF();

        // optional values
        for (DatasetFieldType dsfType : dsv.getDataset().getOwner().getCitationDatasetFieldTypes()) {
            DatasetField dsf = dsv.getDatasetField(dsfType);
            if (dsf != null) {
                optionalValues.add(dsf);
                
                if (dsf.getDatasetFieldType().getFieldType().equals(DatasetFieldType.FieldType.URL)) {
                    optionalURLcount++;
                }                 
            }
        }        
    }
    

    public String getAuthors() {
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

    public String getVersion() {
        return version;
    }

    public String getUNF() {
        return UNF;
    }

    public String getDistributors() {
        return distributors;
    }

    @Override
    public String toString() {
        return toString(false);
    }

    public String toString(boolean html) {
        // first add comma separated parts        
        List<String> citationList = new ArrayList<>();
        citationList.add(formatString(authors, html));
        citationList.add(year);
        citationList.add(formatString(title, html, "\""));
        citationList.add(formatURL(persistentId.toString(), persistentId.toURL().toString(), html));
        citationList.add(formatString(distributors, html));
        citationList.add(version);

        StringBuilder citation = new StringBuilder(
                citationList.stream()
                .filter(value -> !StringUtils.isEmpty(value))
                .collect(Collectors.joining(", ")));

        // append UNF
        if (!StringUtils.isEmpty(UNF)) {
            citation.append(" [").append(UNF).append("]");
        }

        for (DatasetField dsf : optionalValues) {
            String displayName = dsf.getDatasetFieldType().getDisplayName();
            String displayValue;
            
            if (dsf.getDatasetFieldType().getFieldType().equals(DatasetFieldType.FieldType.URL)) {
                displayValue = formatURL(dsf.getDisplayValue(), dsf.getDisplayValue(), html);
                if (optionalURLcount == 1) {
                    displayName = "URL";
                }
            } else {
                displayValue = formatString(dsf.getDisplayValue(), html);
            }

            citation.append(" [")
                    .append(displayName).append(": ")
                    .append(displayValue)
                    .append("]");
        }

        return citation.toString();
    }

    // helper methods   
    private String formatString(String value, boolean escapeHtml) {
        return formatString(value, escapeHtml, "");
    }

    private String formatString(String value, boolean escapeHtml, String wrapper) {
        if (!StringUtils.isEmpty(value)) {
            return new StringBuilder(wrapper)
                    .append(escapeHtml ? StringEscapeUtils.escapeHtml(value) : value)
                    .append(wrapper).toString();
        }
        return null;
    }

    private String formatURL(String text, String url, boolean html) {
        if (text == null) {
            return null;
        }

        if (html && url != null) {
            return "<a href=\"" + url + "\" target=\"_blank\">" + StringEscapeUtils.escapeHtml(text) + "</a>";
        } else {
            return text;
        }

    }
}
