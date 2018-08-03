/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.datavariable;

import edu.harvard.iq.dataverse.DataTable;
import edu.harvard.iq.dataverse.util.StringUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 *
 * @author Leonid Andreev
 */
public class DataTableImportDDI {

    public static final String VAR_INTERVAL_DISCRETE = "discrete";
    public static final String VAR_INTERVAL_CONTIN = "contin";
    public static final String VAR_INTERVAL_NOMINAL = "nominal";
    public static final String VAR_INTERVAL_DICHOTOMOUS = "dichotomous";
    
    public static final String VAR_TYPE_NUMERIC = "numeric";
    public static final String VAR_TYPE_CHARACTER = "character";


    public static final String VAR_WEIGHTED = "wgtd";

    public static final String LEVEL_VARIABLE = "variable";
    public static final String LEVEL_CATEGORY = "category";
    public static final String CAT_STAT_TYPE_FREQUENCY = "freq";


    
    public static final String NOTE_TYPE_UNF = "VDC:UNF";

    // Method processDataDscr takes XMLStreamReader xmlr that has just 
    // encountered the DDI tag <dataDscr>, processes all the variables and 
    // returns a Map of DataTables mapped by the strings found in the 
    // "location" attributes of the variables. The DataTables from the 
    // Map will need to be linked to the corresponding DataFiles by these 
    // file ids. The DataVariable objects found in this dataDscr section 
    // have already been linked to the corresponding DataTables in the Map. 
    // -- L.A. 4.0 beta 9
    
    private Map<String, DataTable> processDataDscr(XMLStreamReader xmlr) throws XMLStreamException {
        Map<String, DataTable> dataTablesMap = new HashMap<>();
        Map<String, Integer> varsPerFileMap = new HashMap<>();

        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("var")) {
                    processVar(xmlr, dataTablesMap, varsPerFileMap);
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("dataDscr")) {

                    for (String fileId : dataTablesMap.keySet()) {
                        Integer numberOfVariables = varsPerFileMap.get(fileId);
                        if (numberOfVariables != null && numberOfVariables > 0) {
                            // OK, this looks like we have found variables for this
                            // data file entry.
                        } else {
                            // TODO:
                            //  otherwise, the studyfile needs to be converted
                            //  from TabularFile to OtherFile; i.e., it should
                            //  be treated as non-subsettable, if there are
                            //  no variables in the <dataDscr> section of the
                            //  DDI referencing the file.
                            //  This actually happens in real life. For example,
                            //  Roper puts some of their files into the <fileDscr>
                            //  section, even though there's no <dataDscr>
                            //  provided for them.
                            //      -- L.A. 
                            // TODO: confirm that this works under 4.0 as is
                            // -- L.A. 4.0 beta 9
                        }
                    }

                    return dataTablesMap;
                }
            }
        }
        return null;
    }

    private void processVar(XMLStreamReader xmlr, Map<String, DataTable> dataTablesMap, Map<String, Integer> varsPerFileMap) throws XMLStreamException {
        DataVariable dv = new DataVariable(0,null);
        dv.setName( xmlr.getAttributeValue(null, "name") );

        try {
            dv.setNumberOfDecimalPoints( new Long( xmlr.getAttributeValue(null, "dcml") ) );
        } catch (NumberFormatException nfe) {}

        // interval type (DB value may be different than DDI value)
        String _interval = xmlr.getAttributeValue(null, "intrvl");
        
        if (VAR_INTERVAL_CONTIN.equals(_interval)) {
            dv.setIntervalContinuous();
        } else if (VAR_INTERVAL_NOMINAL.equals(_interval)) {
            dv.setIntervalNominal();
        } else if (VAR_INTERVAL_DICHOTOMOUS.equals(_interval)) {
            dv.setIntervalDichotomous();
        } else {
            // default is discrete
            dv.setIntervalDiscrete();
        }

        dv.setWeighted( VAR_WEIGHTED.equals( xmlr.getAttributeValue(null, "wgt") ) );
        // default is not-wgtd, so null sets weighted to false


        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("location")) {
                    processLocation(xmlr, dv, dataTablesMap, varsPerFileMap);
                }
                else if (xmlr.getLocalName().equals("labl")) {
                    String _labl = processLabl( xmlr, LEVEL_VARIABLE );
                    if (_labl != null && !_labl.isEmpty()) {
                        dv.setLabel( _labl );
                    }
                } else if (xmlr.getLocalName().equals("universe")) {
                    dv.setUniverse( parseText(xmlr) );
                } else if (xmlr.getLocalName().equals("invalrng")) {
                    processInvalrng( xmlr, dv );
                } else if (xmlr.getLocalName().equals("varFormat")) {
                    processVarFormat( xmlr, dv );
                } else if (xmlr.getLocalName().equals("sumStat")) {
                    processSumStat( xmlr, dv );
                } else if (xmlr.getLocalName().equals("catgry")) {
                    processCatgry( xmlr, dv );
                } else if (xmlr.getLocalName().equals("notes")) {
                    String _note = parseNoteByType( xmlr, NOTE_TYPE_UNF );
                    if (_note != null && !_note.isEmpty()) {
                        dv.setUnf( parseUNF( _note ) );
                    }
                }

            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("var")) return;
            }
        }
    }


    private void processLocation(XMLStreamReader xmlr, DataVariable dv, Map<String, DataTable> dataTablesMap, Map<String, Integer> varsPerFileMap) throws XMLStreamException {

        // fileStartPos, FileEndPos, and RecSegNo
        // if these fields don't convert to Long, just leave blank
        try {
            dv.setFileStartPosition( new Long( xmlr.getAttributeValue(null, "StartPos") ) );
        } catch (NumberFormatException ex) {}
        try {
            dv.setFileEndPosition( new Long( xmlr.getAttributeValue(null, "EndPos") ) );
        } catch (NumberFormatException ex) {}
        try {
            dv.setRecordSegmentNumber( new Long( xmlr.getAttributeValue(null, "RecSegNo") ) );
        } catch (NumberFormatException ex) {}


        if (dv.getDataTable() == null) {
            String fileId = xmlr.getAttributeValue(null, "fileid");

            if (fileId != null && !fileId.isEmpty()) {

                DataTable datatable = null;

                if (dataTablesMap.get(fileId) != null) {
                    datatable = dataTablesMap.get(fileId);
                } else {
                    datatable = new DataTable();
                    dataTablesMap.put(fileId, datatable);
                    varsPerFileMap.put(fileId, 0);
                }

                dv.setDataTable(datatable);
                if (datatable.getDataVariables() == null) {
                    datatable.setDataVariables(new ArrayList<>());
                }
                datatable.getDataVariables().add(dv);

                int filePosition = varsPerFileMap.get(fileId);
                dv.setFileOrder(filePosition++);
                varsPerFileMap.put(fileId, filePosition);
            }
        } else {
            throw new XMLStreamException("Empty or NULL location attribute in a variable section.");
        }
    }


    private void processInvalrng(XMLStreamReader xmlr, DataVariable dv) throws XMLStreamException {
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("item")) {
                    VariableRange range = new VariableRange();
                    dv.getInvalidRanges().add(range);
                    range.setDataVariable(dv);

                    range.setBeginValue( xmlr.getAttributeValue(null, "VALUE") );
                    range.setBeginValueTypePoint();
                } else if (xmlr.getLocalName().equals("range")) {
                    VariableRange range = new VariableRange();
                    dv.getInvalidRanges().add(range);
                    range.setDataVariable(dv);

                    String min = xmlr.getAttributeValue(null, "min");
                    String minExclsuive = xmlr.getAttributeValue(null, "minExclusive");
                    String max = xmlr.getAttributeValue(null, "max");
                    String maxExclusive = xmlr.getAttributeValue(null, "maxExclusive");

                    if ( !StringUtil.isEmpty(min) ) {
                        range.setBeginValue( min );
                        range.setBeginValueTypeMin( );
                    } else if ( !StringUtil.isEmpty(minExclsuive) ) {
                        range.setBeginValue( minExclsuive );
                        range.setBeginValueTypeMinExcl();
                    }

                    if ( !StringUtil.isEmpty(max) ) {
                        range.setEndValue( max );
                        range.setEndValueTypeMax();
                    } else if ( !StringUtil.isEmpty(maxExclusive) ) {
                        range.setEndValue( maxExclusive );
                        range.setEndValueTypeMaxExcl();
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("invalrng")) return;
            }
        }
    }

    private void processVarFormat(XMLStreamReader xmlr, DataVariable dv) throws XMLStreamException {
        String type = xmlr.getAttributeValue(null, "type");
        type = (type == null ? VAR_TYPE_NUMERIC : type); 

        if (VAR_TYPE_CHARACTER.equals(type)) {
            dv.setTypeCharacter();
        } else {
            dv.setTypeNumeric(); // default is numeric
        }
        
        dv.setFormat( xmlr.getAttributeValue(null, "formatname") );
        
        String varFormatCategoryAtt = xmlr.getAttributeValue(null, "category");
        String varFormatText = parseText(xmlr); 

        
        
        /* 
         * A somewhat hackish way of recognizing "boolean" variables; 
         * This is not a universally accepted convention - we (the DVN team)
         * simply decided to handle it this way. Booleans are treated simply 
         * as categorical variables with integers 0 and 1 for the values, and 
         * "FALSE" and "TRUE" for the labels. On top of that, we make a note
         * of the variable's "booleanness", in the DDI, like this:
         *      <varFormat ...>Boolean</varFormat>
         * and in the database, by setting the value of dv.formatCategory to 
         * "Boolean". 
         * This information isn't used much in the application (as of May, 2013), 
         * except in the subsetting: when the column is subset and re-imported
         * into an R data frame, we'll convert it into a logical vector.
         * TODO: 
         * Add this to the export end! --L.A. 
         */
        
        if ("Boolean".equalsIgnoreCase(varFormatText)) {
            dv.setFormatCategory( "Boolean" );
        } else {
            dv.setFormatCategory( varFormatCategoryAtt );
        }
    }

    private void processSumStat(XMLStreamReader xmlr, DataVariable dv) throws XMLStreamException {
        SummaryStatistic ss = new SummaryStatistic();
        ss.setTypeByLabel(xmlr.getAttributeValue(null, "type"));
        ss.setValue( parseText(xmlr)) ;
        ss.setDataVariable(dv);
        dv.getSummaryStatistics().add(ss);
    }

    private void processCatgry(XMLStreamReader xmlr, DataVariable dv) throws XMLStreamException {
        VariableCategory cat = new VariableCategory();
        cat.setMissing( "Y".equals( xmlr.getAttributeValue(null, "missing") ) ); // default is N, so null sets missing to false
        cat.setDataVariable(dv);
                
        if (dv.getCategories() == null || dv.getCategories().isEmpty()) {
            // if this is the first category we encounter, we'll assume that this
            // categorical data/"factor" variable is ordered. 
            // But we'll switch it back to unordered later, if we encounter
            // *any* categories with no order attribute defined. 
            
            dv.setOrderedCategorical(true);
        } 
        
        
        // Process extra level order values, if available; 
        // Currently (as of 3.6) only available in R Data ingests.
        // TODO: 
        // revisit this (for 4.0) - we've discussed encoding this order
        // simply by the order in which the categories appear in the 
        // DDI. (-- L.A. 4.0 beta 9)
        
        String order = null; 
        order = xmlr.getAttributeValue(null, "order");
        Integer orderValue = null; 
        if (order != null) {
            try {
                orderValue = new Integer (order);
            } catch (NumberFormatException ex) {
                orderValue = null; 
            }
        }

        if (orderValue != null && orderValue >= 0) {
            cat.setOrder(orderValue);
        } else if (!cat.isMissing()) {
            // Everey category of an ordered categorical ("factor") variable
            // must have the order rank defined. Which means that if we 
            // encounter a single NON-MISSING category with no ordered attribute, it
            // will be processed as un-ordered. 

            dv.setOrderedCategorical(false);
        }


        dv.getCategories().add(cat);

        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (xmlr.getLocalName().equals("labl")) {
                    String _labl = processLabl( xmlr, LEVEL_CATEGORY );
                    if (_labl != null && !_labl.isEmpty()) {
                        cat.setLabel( _labl );
                    }
                } else if (xmlr.getLocalName().equals("catValu")) {
                    cat.setValue( parseText(xmlr, false) );
                }
                else if (xmlr.getLocalName().equals("catStat")) {
                    String type = xmlr.getAttributeValue(null, "type");
                    if (type == null || CAT_STAT_TYPE_FREQUENCY.equalsIgnoreCase( type ) ) {
                        String _freq = parseText(xmlr);
                        if (_freq != null && !_freq.isEmpty()) {
                            cat.setFrequency( new Double( _freq ) );
                        }
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("catgry")) return;
            }
        }
    }

    private String processLabl(XMLStreamReader xmlr, String level) throws XMLStreamException {
        if (level.equalsIgnoreCase( xmlr.getAttributeValue(null, "level") ) ) {
            return parseText(xmlr);
        } else {
            return null;
        }
    }

        
    private String parseNoteByType (XMLStreamReader xmlr, String type) throws XMLStreamException {
        if (type.equalsIgnoreCase( xmlr.getAttributeValue(null, "type") ) ) {
            return parseText(xmlr);
        } else {
            return null;
        }
    }

    private String parseUNF(String unfString) {
        if (unfString.contains("UNF:")) {
            return unfString.substring( unfString.indexOf("UNF:") );
        } else {
            return null;
        }
    }

     private String parseText(XMLStreamReader xmlr) throws XMLStreamException {
        return parseText(xmlr,true);
     }

     private String parseText(XMLStreamReader xmlr, boolean scrubText) throws XMLStreamException {
        String tempString = getElementText(xmlr);
        if (scrubText) {
            tempString = tempString.trim().replace('\n',' ');
        }
        return tempString;
     }
    
     /* We had to add this method because the ref getElementText has a bug where it
     * would append a null before the text, if there was an escaped apostrophe; it appears
     * that the code finds an null ENTITY_REFERENCE in this case which seems like a bug;
     * the workaround for the moment is to comment or handling ENTITY_REFERENCE in this case
     */
     /* 
      * TODO: do we still need this method? ( -- L.A. 4.0 beta 9)
      */
    private String getElementText(XMLStreamReader xmlr) throws XMLStreamException {
        if(xmlr.getEventType() != XMLStreamConstants.START_ELEMENT) {
            throw new XMLStreamException("parser must be on START_ELEMENT to read next text", xmlr.getLocation());
        }
        int eventType = xmlr.next();
        StringBuilder content = new StringBuilder();
        while(eventType != XMLStreamConstants.END_ELEMENT ) {
            if(eventType == XMLStreamConstants.CHARACTERS
            || eventType == XMLStreamConstants.CDATA
            || eventType == XMLStreamConstants.SPACE) {
                content.append(xmlr.getText());
            } else if(eventType == XMLStreamConstants.PROCESSING_INSTRUCTION
                || eventType == XMLStreamConstants.COMMENT
                || eventType == XMLStreamConstants.ENTITY_REFERENCE) {
                // skipping
            } else if(eventType == XMLStreamConstants.END_DOCUMENT) {
                throw new XMLStreamException("unexpected end of document when reading element text content");
            } else if(eventType == XMLStreamConstants.START_ELEMENT) {
                throw new XMLStreamException("element text content may not contain START_ELEMENT", xmlr.getLocation());
            } else {
                throw new XMLStreamException("Unexpected event type "+eventType, xmlr.getLocation());
            }
            eventType = xmlr.next();
        }
        return content.toString();
    }
}
