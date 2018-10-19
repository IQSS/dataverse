/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.harvest.client.HarvestingClient;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.ejb.EJBException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author gdurand, qqmyers
 */
public class DataCitation {

    private static final Logger logger = Logger.getLogger(DataCitation.class.getCanonicalName());

    private List<String> authors = new ArrayList<String>();
    private List<String> producers = new ArrayList<String>();
    private String title;
    private String fileTitle = null;
    private String year;
    private Date date;
    private GlobalId persistentId;
    private String version;
    private String UNF = null;
    private String publisher;
    private boolean direct;
    private List<String> funders;
    private String seriesTitle;
    private String description;
    private List<String> datesOfCollection;
    private List<String> keywords;
    private List<String> kindsOfData;
    private List<String> languages;
    private List<String> spatialCoverages;

    private List<DatasetField> optionalValues = new ArrayList<>();
    private int optionalURLcount = 0; 

    public DataCitation(DatasetVersion dsv) {
        this(dsv, false);
    }


    public DataCitation(DatasetVersion dsv, boolean direct) {
        this.direct = direct;
        getCommonValuesFrom(dsv);

        // The Global Identifier: 
        // It is always part of the citation for the local datasets; 
        // And for *some* harvested datasets. 
        persistentId = getPIDFrom(dsv, dsv.getDataset());

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
    
    public DataCitation(FileMetadata fm) {
        this(fm, false);
    }

    public DataCitation(FileMetadata fm, boolean direct) {
        this.direct = direct;
        DatasetVersion dsv = fm.getDatasetVersion();

        getCommonValuesFrom(dsv);

        // file Title for direct File citation
        fileTitle = fm.getLabel();
        DataFile df = fm.getDataFile();

        // File description
        description = fm.getDescription();

        // The Global Identifier of the Datafile (if published and isDirect==true) or Dataset as appropriate
        persistentId = getPIDFrom(dsv, df);

        // UNF
        if (df.isTabularData() && df.getUnf() != null && !df.getUnf().isEmpty()) {
            UNF = df.getUnf();
        }
    }

    private void getCommonValuesFrom(DatasetVersion dsv) {

        getAuthorsAndProducersFrom(dsv);
        funders = dsv.getUniqueGrantAgencyValues();
        kindsOfData = dsv.getKindOfData();
        // publication year
        date = getDateFrom(dsv);
        year = new SimpleDateFormat("yyyy").format(date);

        datesOfCollection = dsv.getDatesOfCollection();
        title = dsv.getTitle();
        seriesTitle = dsv.getSeriesTitle();
        keywords = dsv.getKeywords();
        languages = dsv.getLanguages();
        spatialCoverages = dsv.getSpatialCoverages();
        publisher = getPublisherFrom(dsv);
        version = getVersionFrom(dsv);
    }

    public String getAuthorsString() {
        return String.join(";", authors);
    }

    public String getTitle() {
        return title;
    }

    public String getFileTitle() {
        return fileTitle;
    }

    public boolean isDirect() {
        return direct;
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

    public String getPublisher() {
        return publisher;
    }

    @Override
    public String toString() {
        return toString(false);
    }

    public String toString(boolean html) {
        // first add comma separated parts        
        String separator = ", ";
        List<String> citationList = new ArrayList<>();
        citationList.add(formatString(getAuthorsString(), html));
        citationList.add(year);
        if ((fileTitle != null) && isDirect()) {
            citationList.add(formatString(fileTitle, html, "\""));
            citationList.add(formatString(title, html, "<i>", "</i>"));
        } else {
        citationList.add(formatString(title, html, "\""));
        }

        if (persistentId != null) {
        	// always show url format
            citationList.add(formatURL(persistentId.toURL().toString(), persistentId.toURL().toString(), html)); 
        }
        citationList.add(formatString(publisher, html));
        citationList.add(version);

        StringBuilder citation = new StringBuilder(citationList.stream().filter(value -> !StringUtils.isEmpty(value))
                .collect(Collectors.joining(separator)));

        if ((fileTitle != null) && !isDirect()) {
            citation.append("; " + formatString(fileTitle, html, "") + " [fileName]");
        }
        // append UNF
        if (!StringUtils.isEmpty(UNF)) {
            citation.append(separator).append(UNF).append(" [fileUNF]");
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
            citation.append(" [").append(displayName).append(": ").append(displayValue).append("]");
        }
        return citation.toString();
    }

    public String toBibtexString() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            writeAsBibtexCitation(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Use UTF-8?
        return buffer.toString();
    }
    
    public void writeAsBibtexCitation(OutputStream os) throws IOException {
        // Use UTF-8
        Writer out = new BufferedWriter(new OutputStreamWriter(os, "utf-8"));
        if(getFileTitle() !=null && isDirect()) {
            out.write("@incollection{");
        } else {
            out.write("@data{");
        }
        out.write(persistentId.getIdentifier() + "_" + year + "," + "\r\n");
        out.write("author = {");
        out.write(String.join(" and ", authors));
        out.write("},\r\n");
        out.write("publisher = {");
        out.write(publisher);
        out.write("},\r\n");
        if(getFileTitle() !=null && isDirect()) {
        out.write("title = {");
        out.write(fileTitle);
        out.write("},\r\n");
        out.write("booktitle = {");
        out.write(title);
        out.write("},\r\n");
        } else {
            out.write("title = {");
            out.write(title);
            out.write("},\r\n");
            
        }
        out.write("year = {");
        out.write(year);
        out.write("},\r\n");
        out.write("doi = {");
        out.write(persistentId.getAuthority());
        out.write("/");
        out.write(persistentId.getIdentifier());
        out.write("},\r\n");
        out.write("url = {");
        out.write(persistentId.toURL().toString());
        out.write("}\r\n");
        out.flush();
    }

    public String toRISString() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            writeAsRISCitation(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Use UTF-8?
        return buffer.toString();
    }

    public void writeAsRISCitation(OutputStream os) throws IOException {
        // Use UTF-8
        Writer out = new BufferedWriter(new OutputStreamWriter(os, "utf-8"));
        out.write("Provider: " + publisher + "\r\n");
        out.write("Content: text/plain; charset=\"utf-8\"" + "\r\n");
        // Using type "DATA" - see https://github.com/IQSS/dataverse/issues/4816
        
        if ((getFileTitle()!=null)&&isDirect()) {
            out.write("TY  - DATA" + "\r\n");
            out.write("T1  - " + getFileTitle() + "\r\n");
            out.write("T2  - " + getTitle() + "\r\n");
        } else {
            out.write("TY  - DATA" + "\r\n");
            out.write("T1  - " + getTitle() + "\r\n");
        }
        if (seriesTitle != null) {
            out.write("T3  - " + seriesTitle + "\r\n");
        }
        if(description!=null) {
            out.write("AB  - " + flattenHtml(description) + "\r\n");
        }
        for (String author : authors) {
            out.write("AU  - " + author + "\r\n");
        }
        
        if (!producers.isEmpty()) {
            for (String author : producers) {
                out.write("A2  - " + author + "\r\n");
            }
        }
        if (!funders.isEmpty()) {
            for (String author : funders) {
                out.write("A4  - " + author + "\r\n");
            }
        }
        if (!kindsOfData.isEmpty()) {
            for (String kod : kindsOfData) {
                out.write("C3  - " + kod + "\r\n");
            }
        }    
        if (!datesOfCollection.isEmpty()) {
            for (String dateRange : datesOfCollection) {
                out.write("DA  - " + dateRange + "\r\n");
            }
        }

        if (persistentId != null) {
            out.write("DO  - " + persistentId.toString() + "\r\n");
        }
        out.write("ET  - " + version + "\r\n");
        if (!keywords.isEmpty()) {
            for (String keyword : keywords) {
                out.write("KW  - " + keyword + "\r\n");
            }
        }
        if (!languages.isEmpty()) {
            for (String lang : languages) {
                out.write("LA  - " + lang + "\r\n");
            }
        }

        out.write("PY  - " + year + "\r\n");
        
        if (!spatialCoverages.isEmpty()) {
            for (String coverage : spatialCoverages) {
                out.write("RI  - " + coverage + "\r\n");
            }
        }
        
        out.write("SE  - " + date + "\r\n");

        out.write("UR  - " + persistentId.toURL().toString() + "\r\n");
        out.write("PB  - " + publisher + "\r\n");

        // a DataFile citation also includes filename und UNF, if applicable:
        if (getFileTitle() != null) {
            if(!isDirect()) {
                out.write("C1  - " + getFileTitle() + "\r\n");
            }
            if (getUNF() != null) {
                out.write("C2  - " + getUNF() + "\r\n");
            }
        }
        // closing element:
        out.write("ER  - \r\n");
        out.flush();
    }

    private XMLOutputFactory xmlOutputFactory = null;

    public String toEndNoteString() {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        writeAsEndNoteCitation(outStream);
        String xml = outStream.toString();
        return xml; 
    } 
    
    public void writeAsEndNoteCitation(OutputStream os) {

        xmlOutputFactory = javax.xml.stream.XMLOutputFactory.newInstance();
        XMLStreamWriter xmlw = null;
        try {
            xmlw = xmlOutputFactory.createXMLStreamWriter(os);
            xmlw.writeStartDocument();
            createEndNoteXML(xmlw);
            xmlw.writeEndDocument();
        } catch (XMLStreamException ex) {
            Logger.getLogger("global").log(Level.SEVERE, null, ex);
            throw new EJBException("ERROR occurred during creating endnote xml.", ex);
        } finally {
            try {
                if (xmlw != null) {
                    xmlw.close();
                }
            } catch (XMLStreamException ex) {
            }
        }
    }
    
    private void createEndNoteXML(XMLStreamWriter xmlw) throws XMLStreamException {

        xmlw.writeStartElement("xml");
        xmlw.writeStartElement("records");

        xmlw.writeStartElement("record");

        // "Ref-type" indicates which of the (numerous!) available EndNote
        // schemas this record will be interpreted as. 
        // This is relatively important. Certain fields with generic 
        // names like "custom1" and "custom2" become very specific things
        // in specific schemas; for example, custom1 shows as "legal notice"
        // in "Journal Article" (ref-type 84), or as "year published" in 
        // "Government Document". 
        // We don't want the UNF to show as a "legal notice"! 
        // We have found a ref-type that works ok for our purposes - 
        // "Dataset" (type 59). In this one, the fields Custom1
        // and Custom2 are not translated and just show as is. 
        // And "Custom1" still beats "legal notice". 
        // -- L.A. 12.12.2014 beta 10
        // and see https://github.com/IQSS/dataverse/issues/4816
        
        xmlw.writeStartElement("ref-type");
        xmlw.writeAttribute("name", "Dataset");
        xmlw.writeCharacters("59");
        xmlw.writeEndElement(); // ref-type

        xmlw.writeStartElement("contributors");
        if (!authors.isEmpty()) {
        xmlw.writeStartElement("authors");
        for (String author : authors) {
            xmlw.writeStartElement("author");
            xmlw.writeCharacters(author);
            xmlw.writeEndElement(); // author                    
        }
        xmlw.writeEndElement(); // authors 
        }
        if (!producers.isEmpty()) {
            xmlw.writeStartElement("secondary-authors");
            for (String producer : producers) {
                xmlw.writeStartElement("author");
                xmlw.writeCharacters(producer);
                xmlw.writeEndElement(); // author
            }
            xmlw.writeEndElement(); // secondary-authors
        }
        if (!funders.isEmpty()) {
            xmlw.writeStartElement("subsidiary-authors");
            for (String funder : funders) {
                xmlw.writeStartElement("author");
                xmlw.writeCharacters(funder);
                xmlw.writeEndElement(); // author
            }
            xmlw.writeEndElement(); // subsidiary-authors
        }
        xmlw.writeEndElement(); // contributors 

        xmlw.writeStartElement("titles");
        if ((fileTitle != null) && isDirect()) {
            xmlw.writeStartElement("title");
            xmlw.writeCharacters(fileTitle);
            xmlw.writeEndElement(); // title
            xmlw.writeStartElement("secondary-title");
            xmlw.writeCharacters(title);
            xmlw.writeEndElement(); // secondary-title
        } else {
        xmlw.writeStartElement("title");
        xmlw.writeCharacters(title);
        xmlw.writeEndElement(); // title
        }
        
        if (seriesTitle != null) {
            xmlw.writeStartElement("tertiary-title");
            xmlw.writeCharacters(seriesTitle);
            xmlw.writeEndElement(); // tertiary-title
        }
        xmlw.writeEndElement(); // titles

        xmlw.writeStartElement("section");
        String sectionString;
        sectionString = new SimpleDateFormat("yyyy-MM-dd").format(date);

        xmlw.writeCharacters(sectionString);
        xmlw.writeEndElement(); // section

        xmlw.writeStartElement("abstract");
        if(description!=null) {
            xmlw.writeCharacters(flattenHtml(description));
        }
        xmlw.writeEndElement(); // abstract

        xmlw.writeStartElement("dates");
        xmlw.writeStartElement("year");
        xmlw.writeCharacters(year);
        xmlw.writeEndElement(); // year
        if (!datesOfCollection.isEmpty()) {
            xmlw.writeStartElement("pub-dates");
            for (String dateRange : datesOfCollection) {
                xmlw.writeStartElement("date");
                xmlw.writeCharacters(dateRange);
                xmlw.writeEndElement(); // date
            }
            xmlw.writeEndElement(); // pub-dates
        }
        xmlw.writeEndElement(); // dates

        xmlw.writeStartElement("edition");
        xmlw.writeCharacters(version);
        xmlw.writeEndElement(); // edition

        if (!keywords.isEmpty()) {
            xmlw.writeStartElement("keywords");
            for (String keyword : keywords) {
                xmlw.writeStartElement("keyword");
                xmlw.writeCharacters(keyword);
                xmlw.writeEndElement(); // keyword
            }
            xmlw.writeEndElement(); // keywords
        }
        if (!kindsOfData.isEmpty()) {
            for (String kod : kindsOfData) {
                xmlw.writeStartElement("custom3");
                xmlw.writeCharacters(kod);
                xmlw.writeEndElement(); // custom3
            }
        }
        if (!languages.isEmpty()) {
            for (String lang : languages) {
                xmlw.writeStartElement("language");
                xmlw.writeCharacters(lang);
                xmlw.writeEndElement(); // language
            }
        }
        xmlw.writeStartElement("publisher");
        xmlw.writeCharacters(publisher);
        xmlw.writeEndElement(); // publisher

        if (!spatialCoverages.isEmpty()) {
            for (String coverage : spatialCoverages) {
                xmlw.writeStartElement("reviewed-item");
                xmlw.writeCharacters(coverage);
                xmlw.writeEndElement(); // reviewed-item
            }
        }

        xmlw.writeStartElement("urls");
        xmlw.writeStartElement("related-urls");
        xmlw.writeStartElement("url");
        xmlw.writeCharacters(getPersistentId().toURL().toString());
        xmlw.writeEndElement(); // url
        xmlw.writeEndElement(); // related-urls
        xmlw.writeEndElement(); // urls
        
        // a DataFile citation also includes the filename and (for Tabular
        // files) the UNF signature, that we put into the custom1 and custom2 
        // fields respectively:
        
        if (getFileTitle() != null) {
            xmlw.writeStartElement("custom1");
            xmlw.writeCharacters(fileTitle);
            xmlw.writeEndElement(); // custom1
            
                if (getUNF() != null) {
                    xmlw.writeStartElement("custom2");
                    xmlw.writeCharacters(getUNF());
                    xmlw.writeEndElement(); // custom2
            }
        }
        if (persistentId != null) {
            xmlw.writeStartElement("electronic-resource-num");
            String electResourceNum = persistentId.getProtocol() + "/" + persistentId.getAuthority() + "/"
                    + persistentId.getIdentifier();
            xmlw.writeCharacters(electResourceNum);
            xmlw.writeEndElement();
        }
        //<electronic-resource-num>10.3886/ICPSR03259.v1</electronic-resource-num>                  
        xmlw.writeEndElement(); // record

        xmlw.writeEndElement(); // records
        xmlw.writeEndElement(); // xml

    }

	public Map<String, String> getDataCiteMetadata() {
        Map<String, String> metadata = new HashMap<>();
        String authorString = getAuthorsString();

        if (authorString.isEmpty()) {
            authorString = ":unav";
    }
        String producerString = getPublisher();

        if (producerString.isEmpty()) {
            producerString = ":unav";
        }

        metadata.put("datacite.creator", authorString);
        metadata.put("datacite.title", getTitle());
        metadata.put("datacite.publisher", producerString);
        metadata.put("datacite.publicationyear", getYear());
        return metadata;
	}

	
    // helper methods   
    private String formatString(String value, boolean escapeHtml) {
        return formatString(value, escapeHtml, "");
    }

    private String formatString(String value, boolean escapeHtml, String wrapperFront) {
        return formatString(value, escapeHtml, wrapperFront, wrapperFront);
    }

    private String formatString(String value, boolean escapeHtml, String wrapperStart, String wrapperEnd) {
        if (!StringUtils.isEmpty(value)) {
            return new StringBuilder(wrapperStart).append(escapeHtml ? StringEscapeUtils.escapeHtml(value) : value)
                    .append(wrapperEnd).toString();
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

    /** This method flattens html for the textual export formats.
     * It removes <b> and <i> tags, replaces <br>, <p> and headers <hX> with 
     * line breaks, converts lists to form where items start with an indented '*  ',
     * and converts links to simple text showing the label and, if different, 
     * the url in parenthesis after it. Since these operations may create
     * multiple line breaks, a final step limits the changes and compacts multiple 
     * line breaks into one.  
     *
     * @param html input string
     * @return the flattened text output
     */
    private String flattenHtml(String html) {
        html = html.replaceAll("<[pP]>", "\r\n");
        html = html.replaceAll("<\\/[pP]>", "\r\n");
        html = html.replaceAll("<[hH]\\d>", "\r\n");
        html = html.replaceAll("<\\/[hH]\\d>", "\r\n");
        html = html.replaceAll("<[\\/]?[bB]>", "");
        html = html.replaceAll("<[\\/]?[iI]>", "\r\n");
        
        html = html.replaceAll("<[bB][rR][\\/]?>", "\r\n");
        html = html.replaceAll("<[uU][lL]>", "\r\n");
        html = html.replaceAll("<\\/[uU][lL]>", "\r\n");
        html = html.replaceAll("<[lL][iI]>", "\t*  ");
        html = html.replaceAll("<\\/[lL][iI]>", "\r\n");
        Pattern p = Pattern.compile("<a\\W+href=\\\"(.*?)\\\".*?>(.*?)<\\/a>");
        Matcher m = p.matcher(html);
        String url = null;
        String label = null;
        while(m.find()) {
            url = m.group(1); // this variable should contain the link URL
            label = m.group(2); // this variable should contain the label
            //display either the label or label(url)
            if(!url.equals(label)) {
                label = label + "(" + url +")";
            }
            html = html.replaceFirst("<a\\W+href=\\\"(.*?)\\\".*?>(.*?)<\\/a>", label);
        }
        //Note, this does not affect single '\n' chars originally in the text
        html=html.replaceAll("(\\r\\n?)+", "\r\n");
        
        return html;
    }

    private Date getDateFrom(DatasetVersion dsv) {
        Date citationDate = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
        if (!dsv.getDataset().isHarvested()) {
            citationDate = dsv.getCitationDate();
            if (citationDate == null) {
                if (dsv.getDataset().getPublicationDate() != null) {
                    citationDate = dsv.getDataset().getPublicationDate();
                } else { // for drafts
                    citationDate = dsv.getLastUpdateTime();
                }
            }
        } else {
            try {
                citationDate= sdf.parse(dsv.getDistributionDate());
            } catch (ParseException ex) {
                // ignore
            } catch (Exception ex) {
                // ignore
            }
        }
        if (citationDate == null) {
            //As a last resort, pick the current date
            logger.warning("Unable to find citation date for datasetversion: " + dsv.getId());
            citationDate = new Date();
        }
        return citationDate;
    }

    private void getAuthorsAndProducersFrom(DatasetVersion dsv) {

        dsv.getDatasetAuthors().stream().forEach((author) -> {
            if (!author.isEmpty()) {
                String an = author.getName().getDisplayValue().trim();
                authors.add(an);
            }
        });
        producers = dsv.getDatasetProducerNames();
    }

    private String getPublisherFrom(DatasetVersion dsv) {
        if (!dsv.getDataset().isHarvested()) {
            return dsv.getRootDataverseNameforCitation();
        } else {
            return dsv.getDistributorName();
            // remove += [distributor] SEK 8-18-2016
        }
    }

    private String getVersionFrom(DatasetVersion dsv) {
        String version = "";
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
        return version;
    }

    private GlobalId getPIDFrom(DatasetVersion dsv, DvObject dv) {
        if (!dsv.getDataset().isHarvested()
                || HarvestingClient.HARVEST_STYLE_VDC.equals(dsv.getDataset().getHarvestedFrom().getHarvestStyle())
                || HarvestingClient.HARVEST_STYLE_ICPSR.equals(dsv.getDataset().getHarvestedFrom().getHarvestStyle())
                || HarvestingClient.HARVEST_STYLE_DATAVERSE
                        .equals(dsv.getDataset().getHarvestedFrom().getHarvestStyle())) {
                // creating a global id like this:
                // persistentId = new GlobalId(dv.getGlobalId());
                // you end up doing new GlobalId((New GlobalId(dv)).toString())
                // - doing an extra formatting-and-parsing-again
                // This achieves the same thing:
                if(!isDirect()) {
                if (!StringUtils.isEmpty(dsv.getDataset().getIdentifier())) {
                    return new GlobalId(dsv.getDataset());
                }
                } else {
                if (!StringUtils.isEmpty(dv.getIdentifier())) {
                    return new GlobalId(dv);
                }
            }
        }
        return null;
    }
}
