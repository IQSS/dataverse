/*
   Copyright (C) 2005-2012, by the President and Fellows of Harvard College.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   Dataverse Network - A web application to share, preserve and analyze research data.
   Developed at the Institute for Quantitative Social Science, Harvard University.
   Version 3.0.
*/
package edu.harvard.iq.dataverse.harvest.client;

import java.io.IOException;
import java.io.FileNotFoundException;

import java.io.InputStream;
import java.io.StringReader;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.File;

import java.io.FileOutputStream;

import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipInputStream;


import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.xml.sax.SAXException;

//import org.xml.sax.InputSource;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLInputFactory;

/* 
 * This is an optimized implementation of OAIPMH GetRecord method.
 * Some code is borrowed from the OCLC implementation.
 * It handles the retrieval of the record in a drastically different manner: 
 * It parses and validates the top, "administrative" portion of the record using 
 * an event-driven parser. Once it reaches the "payload", the actual metadata 
 * record enclosed in <metadata>...</metadata> tags, it just reads it line by 
 * line without parsing and saves it in a temp file. (The record will be parsed 
 * and validated in the next step, when we attempt to import it). 
 * On a very large record, for example, a DDI of a Dataset with a large number
 * of associated data variables, even event-driven XML parsing can end up 
 * being rather expensive. 
 * This optimized version was originally written for DVN 3.*. 
 * Added in Dataverse 4: custom protocol extension for sending the metadata
 * record as a pre-declared numbe of bytes. 
 * @author Leonid Andreev
 * 
*/

public class FastGetRecord {
   
    private static final String DATAVERSE_EXTENDED_METADATA = "dataverse_json";
    private static final String XML_METADATA_TAG = "metadata";
    private static final String XML_METADATA_TAG_OPEN = "<"+XML_METADATA_TAG+">";
    private static final String XML_METADATA_TAG_CLOSE = "</"+XML_METADATA_TAG+">";
    private static final String XML_OAI_PMH_CLOSING_TAGS = "</record></GetRecord></OAI-PMH>";
    private static final String XML_XMLNS_XSI_ATTRIBUTE_TAG = "xmlns:xsi=";
    private static final String XML_XMLNS_XSI_ATTRIBUTE = " "+XML_XMLNS_XSI_ATTRIBUTE_TAG+"\"http://www.w3.org/2001/XMLSchema-instance\">";
    private static final String XML_COMMENT_START = "<!--";
    private static final String XML_COMMENT_END = "-->";
    
    /**
     * Client-side GetRecord verb constructor
     *
     * @param baseURL the baseURL of the server to be queried
     * @exception MalformedURLException the baseURL is bad
     * @exception SAXException the xml response is bad
     * @exception IOException an I/O error occurred
     */

    public FastGetRecord(String baseURL, String identifier, String metadataPrefix)
    throws IOException, ParserConfigurationException, SAXException,
    TransformerException {
        harvestRecord (baseURL, identifier, metadataPrefix);

    }
    
    private String errorMessage = null;
    private File savedMetadataFile = null; 
    private XMLInputFactory xmlInputFactory = null; 
    private boolean recordDeleted = false;

    // TODO: logging

    public String getErrorMessage () {
        return errorMessage;
    }

    public File getMetadataFile () {
        return savedMetadataFile;
    }

    public boolean isDeleted () {
        return this.recordDeleted;
    }


    public void harvestRecord(String baseURL, String identifier, String metadataPrefix) throws IOException,
        ParserConfigurationException, SAXException, TransformerException {

        xmlInputFactory = javax.xml.stream.XMLInputFactory.newInstance();

        String requestURL = getRequestURL(baseURL, identifier, metadataPrefix);

        InputStream in = null;
        URL url = new URL(requestURL);
        HttpURLConnection con = null;
        int responseCode = 0;

        con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("User-Agent", "DataverseHarvester/3.0");
        con.setRequestProperty("Accept-Encoding",
                                   "compress, gzip, identify");
        try {
            responseCode = con.getResponseCode();
            //logger.debug("responseCode=" + responseCode);
        } catch (FileNotFoundException e) {
            //logger.info(requestURL, e);
            responseCode = HttpURLConnection.HTTP_UNAVAILABLE;
        }

        // TODO: -- L.A.
        //
        // support for cookies;
        // support for limited retry attempts -- ?
        // implement reading of the stream as filterinputstream -- ?
        // -- that could make it a little faster still. -- L.A. 



        if (responseCode == 200) {

            String contentEncoding = con.getHeaderField("Content-Encoding");
            //logger.debug("contentEncoding=" + contentEncoding);

            // support for the standard compress/gzip/deflate compression
            // schemes:

            if ("compress".equals(contentEncoding)) {
                ZipInputStream zis = new ZipInputStream(con.getInputStream());
                zis.getNextEntry();
                in = zis;
            } else if ("gzip".equals(contentEncoding)) {
                in = new GZIPInputStream(con.getInputStream());
            } else if ("deflate".equals(contentEncoding)) {
                in = new InflaterInputStream(con.getInputStream());
            } else {
                in = con.getInputStream();
            }

            // We are going to read the OAI header and SAX-parse it for the
            // error messages and other protocol information;
            // The metadata section we're going to simply save in a temporary
            // file, unparsed.

            BufferedReader rd = new BufferedReader(new InputStreamReader(in));

            String line = null;
            String oaiResponseHeader = "";
            boolean metadataFlag = false;
            boolean metadataWritten = false;
            boolean schemaChecked = false;
            FileOutputStream tempFileStream = null;
            PrintWriter metadataOut = null;

            savedMetadataFile = File.createTempFile("meta", ".tmp");
            
            

            int mopen = 0;
            int mclose = 0;
 
            while ( ( line = rd.readLine () ) != null) {
                if (!metadataFlag) {
                    if (line.matches(".*"+XML_METADATA_TAG_OPEN+".*")) {
                        String lineCopy = line;

                        int i = line.indexOf(XML_METADATA_TAG_OPEN);
                        if (line.length() > i + XML_METADATA_TAG_OPEN.length()) { 
                            line = line.substring(i + XML_METADATA_TAG_OPEN.length());
                            // TODO: check if there's anything useful (non-white space, etc.)
                            // in the remaining part of the line?
                            if ((i = line.indexOf('<')) > -1) {
                                if (i > 0) {
                                    line = line.substring(i);
                                }
                            } else {
                                line = null; 
                            }
                        
                        } else {
                            line = null;
                        }

                        oaiResponseHeader = oaiResponseHeader.concat(lineCopy.replaceAll(XML_METADATA_TAG_OPEN+".*", XML_METADATA_TAG_OPEN+XML_METADATA_TAG_CLOSE+XML_OAI_PMH_CLOSING_TAGS));
                        tempFileStream = new FileOutputStream(savedMetadataFile);
                        metadataOut = new PrintWriter (tempFileStream, true);

                        //metadataOut.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); /* ? */

                        metadataFlag = true;
                    } else if (line.matches(".*<"+XML_METADATA_TAG+" [^>]*>.*")) {
                        if (metadataPrefix.equals(DATAVERSE_EXTENDED_METADATA)) {
                            oaiResponseHeader = oaiResponseHeader.concat(line);
                            metadataWritten = true;
                            metadataFlag = true;
                        }
                    }
                }
                
                //System.out.println(line);

                if (line != null) {
                if (metadataFlag) {
                    if (!metadataWritten) {
                        // Inside an OAI-PMH GetRecord response, the metadata
                        // record returned is enclosed in <metadata> ... </metadata>
                        // tags, after the OAI service sections that provide the
                        // date, identifier and other protocol-level information.
                        // However, it is possible for the metadata record itself
                        // to have <metadata> tags of its own. So we have no
                        // choice but to count the opening and closing tags in
                        // order to recognize the one terminating the metadata
                        // section.
                        // This code isn't pretty, but on seriously large records
                        // the savings from not fully parsing the XML are
                        // significant.
                        //  -- L.A. 

                        if (line.matches("<"+XML_METADATA_TAG)) {
                           int i = 0;
                           while ((i = line.indexOf("<"+XML_METADATA_TAG, i)) > -1) {
                               if (!line.substring(i).matches("^<"+XML_METADATA_TAG+"[^>]*/")) {
                                   // don't count if it's a closed, empty tag:
                                   // <metadata />
                                   mopen++;
                               }
                               i+=XML_METADATA_TAG_OPEN.length();
                           }
                        }
                        if (line.matches(".*"+XML_METADATA_TAG_CLOSE+".*")) {
                            int i = 0;
                            while ((i = line.indexOf(XML_METADATA_TAG_CLOSE, i)) > -1) {
                                i+=XML_METADATA_TAG_CLOSE.length();
                                mclose++;
                            }

                            if ( mclose > mopen ) {
                                line = line.substring(0, line.lastIndexOf(XML_METADATA_TAG_CLOSE));
                                metadataWritten = true;
                            }
                        }

                        if (!schemaChecked) {
                            // if the top-level XML element lacks the schema definition,
                            // insert the generic xmlns and xmlns:xsi attributes; these
                            // may be needed by the transform stylesheets.
                            // this mimicks the behaviour of the OCLC GetRecord
                            // client implementation.
                            //      -L.A.

                            int offset = 0;

                            // However, there may be one or more XML comments before
                            // the first "real" XML element (of the form
                            // <!-- ... -->). So we need to skip these!

                            int j = 0; 
                            while ( ((j = line.indexOf('<', offset)) > -1)
                                && line.length() >= j+XML_COMMENT_START.length()
                                && XML_COMMENT_START.equals(line.substring(j,j+XML_COMMENT_START.length()))) {

                                offset = j; 
                                
                                //OK, this is a comment allright.

                                // is it terminated on the same line?
                                // if not, let's read the stream until
                                // we find the closing '-->':

                                while (line != null
                                        &&
                                        ((offset = line.indexOf(XML_COMMENT_END,offset)) < 0)) {
                                    line = line.replaceAll("[\n\r]", " ");
                                    offset = line.length();
                                    line = line.concat(rd.readLine());
                                }

                                offset += XML_COMMENT_END.length();
                            }

                            // if we have skipped some comments, is there another
                            // XML element left in the buffered line?
                            int firstElementStart = -1;

                            if ((firstElementStart = line.indexOf('<', offset)) > -1 ) {
                                // OK, looks like there is. 
                                // is it terminated? 
                                // if not, let's read the stream until
                                // we find the closing '>':

                                int firstElementEnd = -1;
                                offset = firstElementStart;

                                while (line != null
                                        &&
                                        ((firstElementEnd = line.indexOf('>',offset)) < 0)) {

                                    line = line.replaceAll("[\n\r]", "");
                                    offset = line.length();
                                    line = line.concat(rd.readLine());
                                }

                                if (firstElementEnd < 0) {
                                    // this should not happen!
                                    // we've reached the end of the XML stream
                                    // without encountering a single valid XML tag -- ??

                                    this.errorMessage = "Malformed GetRecord response; reached the end of the stream but couldn't find a single valid XML element in the metadata section.";
                                } else {

                                    // OK, we now have a line that contains a complete,
                                    // terminated (possibly multi-line) first XML element
                                    // that starts at [offset].

                                    int i = firstElementStart;

                                    if (!line.substring(i).matches("^<[^>]*"+XML_XMLNS_XSI_ATTRIBUTE_TAG+".*")) {
                                        String head = line.substring(0, i);
                                        String tail = line.substring(i);
                                        //tail = tail.replaceFirst(">", " xmlns=\"http://www.openarchives.org/OAI/2.0/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");
                                        tail = tail.replaceFirst(">", XML_XMLNS_XSI_ATTRIBUTE);
                                        line = head + tail;
                                    }

                                    schemaChecked = true;
                                } 
                            } else {
                                // there was no "real" XML elements, only comments.
                                // We'll perform this schema check in the next 
                                // iteration. 
                            }
                        }

                        metadataOut.println(line);
                    }
                } else {
                    oaiResponseHeader = oaiResponseHeader.concat(line);
                }
                }
            }

            // parse the OAI Record header:

            XMLStreamReader xmlr = null;

            try {
                StringReader reader = new StringReader(oaiResponseHeader);
                xmlr = xmlInputFactory.createXMLStreamReader(reader);
                processOAIheader(xmlr, metadataPrefix.equals(DATAVERSE_EXTENDED_METADATA));

            } catch (XMLStreamException ex) {
                //Logger.getLogger("global").log(Level.SEVERE, null, ex);
                if (this.errorMessage == null) {
                    this.errorMessage = "Malformed GetRecord response: " + oaiResponseHeader;
                }

                // delete the temp metadata file; we won't need it:
                if (savedMetadataFile != null) {
                    //savedMetadataFile.delete();
                }

            }

            try {
                if (xmlr != null) {
                    xmlr.close();
                }
            } catch (Exception ed) {
                // seems OK to ignore;
            }


            if (rd != null) {
                rd.close();
            }

            if (metadataOut != null) {
                metadataOut.close();
            }

            if (!(metadataWritten) && !(this.isDeleted())) {
                if (oaiResponseHeader.length() > 64) {
                    oaiResponseHeader = oaiResponseHeader.substring(0, 32) + "...";
                }
                this.errorMessage = "Failed to parse GetRecord response; "+oaiResponseHeader;
                //savedMetadataFile.delete();
            }

            if (this.isDeleted()) {
                //savedMetadataFile.delete();
            }


        } else {
            this.errorMessage = "GetRecord request failed. HTTP error code "+responseCode;
        }
   }

    /**
     * Construct the query portion of the http request
     * (borrowed from OCLC implementation)
     *
     * @return a String containing the query portion of the http request
     */
    private static String getRequestURL(String baseURL,
            String identifier,
            String metadataPrefix) {

        StringBuffer requestURL =  new StringBuffer(baseURL);
        requestURL.append("?verb=GetRecord");
        requestURL.append("&identifier=").append(identifier);
        requestURL.append("&metadataPrefix=").append(metadataPrefix);

        return requestURL.toString();
    }

    private void processOAIheader (XMLStreamReader xmlr, boolean extensionMode) throws XMLStreamException, IOException {

        // is this really a GetRecord response?
        xmlr.nextTag();
        xmlr.require(XMLStreamConstants.START_ELEMENT, null, "OAI-PMH");
        processOAIPMH(xmlr, extensionMode);

    }

    private void processOAIPMH (XMLStreamReader xmlr, boolean extensionMode) throws XMLStreamException, IOException {

        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                // TODO:
                // process all the fields currently skipped -- ? -- L.A.
                if (xmlr.getLocalName().equals("responseDate")) {}
                else if (xmlr.getLocalName().equals("request")) {}
                else if (xmlr.getLocalName().equals("error")) {
                    String errorCode = xmlr.getAttributeValue(null, "code");
                    String errorMessageText = getElementText(xmlr);

                    if (errorCode != null) {
                        this.errorMessage = "GetRecord error code: "+errorCode+"; ";
                    }

                    if (errorCode != null) {
                        this.errorMessage = this.errorMessage + "GetRecord error message: "+errorMessageText+"; ";
                    }
                    throw new XMLStreamException(this.errorMessage);

                }
                else if (xmlr.getLocalName().equals("GetRecord")) {
                    processGetRecordSection(xmlr, extensionMode);
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("OAI-PMH")) return;
            }
        }
    }

    private void processGetRecordSection (XMLStreamReader xmlr, boolean extensionMode) throws XMLStreamException, IOException {
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                 if (xmlr.getLocalName().equals("record")) {
                     processRecord(xmlr, extensionMode);
                 }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("GetRecord")) return;
            }
        }

    }

    private void processRecord (XMLStreamReader xmlr, boolean extensionMode) throws XMLStreamException, IOException {
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                 if (xmlr.getLocalName().equals("header")) {
                     if ("deleted".equals( xmlr.getAttributeValue(null, "status"))) {
                        this.recordDeleted = true;
                     }
                     processHeader(xmlr);
                 } else if (xmlr.getLocalName().equals("metadata")) {
                     if (extensionMode) {
                        String extendedMetadataApiUrl = xmlr.getAttributeValue(null, "directApiCall");
                        processMetadataExtended(extendedMetadataApiUrl);
                     }
                 }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("record")) return;
            }
        }
    }

    private void processHeader (XMLStreamReader xmlr) throws XMLStreamException {
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                 if (xmlr.getLocalName().equals("identifier")) {/*do nothing*/}
                 else if (xmlr.getLocalName().equals("datestamp")) {/*do nothing -- ?*/}
                 else if (xmlr.getLocalName().equals("setSpec")) {/*do nothing*/}


            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals("header")) return;
            }
        }
    }
    
    private void processMetadataExtended (String extendedApiUrl) throws IOException {
        InputStream in = null;
        int responseCode = 0;
        HttpURLConnection con = null;


        
        try {
            URL url = new URL(extendedApiUrl.replaceAll("&amp;", "&")); // is this necessary?

            con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("User-Agent", "DataverseHarvester/3.0");
            responseCode = con.getResponseCode();
        } catch (MalformedURLException mue) {
            throw new IOException ("Bad API URL: "+extendedApiUrl);
        } catch (FileNotFoundException e) {
            responseCode = HttpURLConnection.HTTP_UNAVAILABLE;
        }

        


        if (responseCode == 200) {
            in = con.getInputStream();
            // TODO: 
            /* we should probably still support gzip/compress encoding here - ?
            String contentEncoding = con.getHeaderField("Content-Encoding");

            // support for the standard compress/gzip/deflate compression
            // schemes:

            if ("compress".equals(contentEncoding)) {
                ZipInputStream zis = new ZipInputStream(con.getInputStream());
                zis.getNextEntry();
                in = zis;
            } else if ("gzip".equals(contentEncoding)) {
                in = new GZIPInputStream(con.getInputStream());
            } else if ("deflate".equals(contentEncoding)) {
                in = new InflaterInputStream(con.getInputStream());
            } ...
            */
            FileOutputStream tempOut = new FileOutputStream(savedMetadataFile);
            
            int bufsize;
            byte[] buffer = new byte[4 * 8192];

            while ((bufsize = in.read(buffer)) != -1) {
                tempOut.write(buffer, 0, bufsize);
                tempOut.flush();
            }

            in.close();
            tempOut.close();
            return;
        }

        throw new IOException("Failed to download extended metadata.");
  
    }

    
    // (from Gustavo's ddiServiceBean -- L.A.)
    //
    /* We had to add this method because the ref getElementText has a bug where it
     * would append a null before the text, if there was an escaped apostrophe; it appears
     * that the code finds an null ENTITY_REFERENCE in this case which seems like a bug;
     * the workaround for the moment is to comment or handling ENTITY_REFERENCE in this case
     */
    private String getElementText(XMLStreamReader xmlr) throws XMLStreamException {
        if(xmlr.getEventType() != XMLStreamConstants.START_ELEMENT) {
            throw new XMLStreamException("parser must be on START_ELEMENT to read next text", xmlr.getLocation());
        }
        int eventType = xmlr.next();
        StringBuffer content = new StringBuffer();
        while(eventType != XMLStreamConstants.END_ELEMENT ) {
            if(eventType == XMLStreamConstants.CHARACTERS
            || eventType == XMLStreamConstants.CDATA
            || eventType == XMLStreamConstants.SPACE
            /* || eventType == XMLStreamConstants.ENTITY_REFERENCE*/) {
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
