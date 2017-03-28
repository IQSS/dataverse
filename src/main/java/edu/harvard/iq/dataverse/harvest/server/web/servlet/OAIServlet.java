/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.harvest.server.web.servlet;

import com.lyncode.xml.exceptions.XmlWriteException;
import com.lyncode.xoai.dataprovider.builder.OAIRequestParametersBuilder;
import com.lyncode.xoai.dataprovider.exceptions.OAIException;
import com.lyncode.xoai.dataprovider.repository.Repository;
import com.lyncode.xoai.dataprovider.repository.RepositoryConfiguration;
import com.lyncode.xoai.dataprovider.model.Context;
import com.lyncode.xoai.dataprovider.model.MetadataFormat;
import com.lyncode.xoai.services.impl.SimpleResumptionTokenFormat;
import com.lyncode.xoai.dataprovider.repository.ItemRepository;
import com.lyncode.xoai.dataprovider.repository.SetRepository;
import com.lyncode.xoai.model.oaipmh.DeletedRecord;
import com.lyncode.xoai.model.oaipmh.Granularity;
import com.lyncode.xoai.model.oaipmh.OAIPMH;
import static com.lyncode.xoai.model.oaipmh.OAIPMH.NAMESPACE_URI;
import static com.lyncode.xoai.model.oaipmh.OAIPMH.SCHEMA_LOCATION;
import com.lyncode.xoai.model.oaipmh.Verb;
import com.lyncode.xoai.xml.XSISchema;

import com.lyncode.xoai.xml.XmlWriter;
import static com.lyncode.xoai.xml.XmlWriter.defaultContext;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.export.ExportException;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.harvest.server.OAIRecordServiceBean;
import edu.harvard.iq.dataverse.harvest.server.OAISetServiceBean;
import edu.harvard.iq.dataverse.harvest.server.xoai.XdataProvider;
import edu.harvard.iq.dataverse.harvest.server.xoai.XgetRecord;
import edu.harvard.iq.dataverse.harvest.server.xoai.XitemRepository;
import edu.harvard.iq.dataverse.harvest.server.xoai.XsetRepository;
import edu.harvard.iq.dataverse.harvest.server.xoai.XlistRecords;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.ByteArrayOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;
import javax.ejb.EJB;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLStreamException;
import org.codehaus.plexus.util.StringUtils;

/**
 *
 * @author Leonid Andreev
 * Dedicated servlet for handling OAI-PMH requests.
 * Uses lyncode XOAI data provider implementation for serving content. 
 * The servlet itself is somewhat influenced by the older OCLC OAIcat implementation.
 */
public class OAIServlet extends HttpServlet {
    @EJB 
    OAISetServiceBean setService;
    @EJB
    OAIRecordServiceBean recordService;
    @EJB
    SettingsServiceBean settingsService;
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DatasetServiceBean datasetService;
    
    @EJB
    SystemConfig systemConfig;
    
    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.harvest.server.web.servlet.OAIServlet");
    protected HashMap attributesMap = new HashMap();
    private static final String OAI_PMH = "OAI-PMH";
    private static final String RESPONSEDATE_FIELD = "responseDate";
    private static final String REQUEST_FIELD = "request";
    private static final String DATAVERSE_EXTENDED_METADATA_FORMAT = "dataverse_json";
    private static final String DATAVERSE_EXTENDED_METADATA_INFO = "Custom Dataverse metadata in JSON format (Dataverse4 to Dataverse4 harvesting only)";
    private static final String DATAVERSE_EXTENDED_METADATA_SCHEMA = "JSON schema pending";
     
    
    private Context xoaiContext;
    private SetRepository setRepository;
    private ItemRepository itemRepository;
    private RepositoryConfiguration repositoryConfiguration;
    private Repository xoaiRepository;
    private XdataProvider dataProvider; 

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        
        xoaiContext =  createContext();
        
        if (isDataverseOaiExtensionsSupported()) {
            xoaiContext = addDataverseJsonMetadataFormat(xoaiContext);
        }
        
        setRepository = new XsetRepository(setService);
        itemRepository = new XitemRepository(recordService, datasetService);

        repositoryConfiguration = createRepositoryConfiguration(); 
                        
        xoaiRepository = new Repository()
            .withSetRepository(setRepository)
            .withItemRepository(itemRepository)
            .withResumptionTokenFormatter(new SimpleResumptionTokenFormat())
            .withConfiguration(repositoryConfiguration);
        
        dataProvider = new XdataProvider(getXoaiContext(), getXoaiRepository());
    }
    
    private Context createContext() {
        
        Context context = new Context();
        addSupportedMetadataFormats(context);
        return context;
    }
    
    private void addSupportedMetadataFormats(Context context) {
        for (String[] provider : ExportService.getInstance().getExportersLabels()) {
            String formatName = provider[1];
            Exporter exporter;
            try {
                exporter = ExportService.getInstance().getExporter(formatName);
            } catch (ExportException ex) {
                exporter = null;
            }

            if (exporter != null && exporter.isXMLFormat() && exporter.isHarvestable()) {
                MetadataFormat metadataFormat;

                try {

                    metadataFormat = MetadataFormat.metadataFormat(formatName);
                    metadataFormat.withNamespace(exporter.getXMLNameSpace());
                    metadataFormat.withSchemaLocation(exporter.getXMLSchemaLocation());
                } catch (ExportException ex) {
                    metadataFormat = null;
                }
                if (metadataFormat != null) {
                    context.withMetadataFormat(metadataFormat);
                }
            }
        }
        //return context;
    }
    
    private Context addDataverseJsonMetadataFormat(Context context) {
        MetadataFormat metadataFormat = MetadataFormat.metadataFormat(DATAVERSE_EXTENDED_METADATA_FORMAT);
        metadataFormat.withNamespace(DATAVERSE_EXTENDED_METADATA_INFO);
        metadataFormat.withSchemaLocation(DATAVERSE_EXTENDED_METADATA_SCHEMA);
        context.withMetadataFormat(metadataFormat);
        return context;
    }
    
    private boolean isDataverseOaiExtensionsSupported() {
        return true;
    }
    
    private RepositoryConfiguration createRepositoryConfiguration() {
        // TODO: 
        // some of the settings below - such as the max list numbers - 
        // need to be configurable!
        
        String dataverseName = dataverseService.findRootDataverse().getName();
        String repositoryName = StringUtils.isEmpty(dataverseName) || "Root".equals(dataverseName) ? "Test Dataverse OAI Archive" : dataverseName + " Dataverse OAI Archive";
        

        RepositoryConfiguration repositoryConfiguration = new RepositoryConfiguration()
                .withRepositoryName(repositoryName)
                .withBaseUrl(systemConfig.getDataverseSiteUrl()+"/oai")
                .withCompression("gzip")        // ?
                .withCompression("deflate")     // ?
                .withAdminEmail(settingsService.getValueForKey(SettingsServiceBean.Key.SystemEmail))
                .withDeleteMethod(DeletedRecord.TRANSIENT)
                .withGranularity(Granularity.Second)
                .withMaxListIdentifiers(100)
                .withMaxListRecords(100)
                .withMaxListSets(100)
                .withEarliestDate(new Date());
        
        return repositoryConfiguration; 
    }
    
    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
    
    
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
     
    
    private void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            if (!isHarvestingServerEnabled()) {
                response.sendError(
                        HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                        "Sorry. OAI Service is disabled on this Dataverse node.");
                return;
            }
            
            OAIRequestParametersBuilder parametersBuilder = newXoaiRequest();
            
            for (Object p : request.getParameterMap().keySet()) {
                String parameterName = (String)p; 
                String parameterValue = request.getParameter(parameterName);
                parametersBuilder = parametersBuilder.with(parameterName, parameterValue);

            }
            
            OAIPMH handle = dataProvider.handle(parametersBuilder);
            response.setContentType("text/xml;charset=UTF-8");
            
            if (isGetRecord(request) && !handle.hasErrors()) {
                writeGetRecord(response, handle);
            } else if (isListRecords(request) && !handle.hasErrors()) {
                writeListRecords(response, handle);
            } else {
                XmlWriter xmlWriter = new XmlWriter(response.getOutputStream());
                xmlWriter.write(handle);
                xmlWriter.flush();
                xmlWriter.close();
            }
                       
        } catch (IOException ex) {
            logger.warning("IO exception in Get; "+ex.getMessage());
            throw new ServletException ("IO Exception in Get");
        } catch (OAIException oex) {
            logger.warning("OAI exception in Get; "+oex.getMessage());
            throw new ServletException ("OAI Exception in Get");
        } catch (XMLStreamException xse) {
            logger.warning("XML Stream exception in Get; "+xse.getMessage());
            throw new ServletException ("XML Stream Exception in Get");
        } catch (XmlWriteException xwe) {
            logger.warning("XML Write exception in Get; "+xwe.getMessage());
            throw new ServletException ("XML Write Exception in Get");  
        } catch (Exception e) {
            logger.warning("Unknown exception in Get; "+e.getMessage());
            throw new ServletException ("Unknown servlet exception in Get.");
        }
        
    }
    
    // Custom methods for the potentially expensive GetRecord and ListRecords requests:
    
    private void writeListRecords(HttpServletResponse response, OAIPMH handle) throws IOException {
        OutputStream outputStream = response.getOutputStream();

        outputStream.write(oaiPmhResponseToString(handle).getBytes());

        Verb verb = handle.getVerb();

        if (verb == null) {
            throw new IOException("An error or a valid response must be set");
        }

        if (!verb.getType().equals(Verb.Type.ListRecords)) {
            throw new IOException("writeListRecords() called on a non-ListRecords verb");
        }

        outputStream.write(("<" + verb.getType().displayName() + ">").getBytes());

        outputStream.flush();

        ((XlistRecords) verb).writeToStream(outputStream);

        outputStream.write(("</" + verb.getType().displayName() + ">").getBytes());
        outputStream.write(("</" + OAI_PMH + ">\n").getBytes());

        outputStream.flush();
        outputStream.close();

    }
    
    private void writeGetRecord(HttpServletResponse response, OAIPMH handle) throws IOException, XmlWriteException, XMLStreamException {
        OutputStream outputStream = response.getOutputStream();

        outputStream.write(oaiPmhResponseToString(handle).getBytes());

        Verb verb = handle.getVerb();

        if (verb == null) {
            throw new IOException("An error or a valid response must be set");
        }

        if (!verb.getType().equals(Verb.Type.GetRecord)) {
            throw new IOException("writeListRecords() called on a non-GetRecord verb");
        }

        outputStream.write(("<" + verb.getType().displayName() + ">").getBytes());

        outputStream.flush();

        ((XgetRecord) verb).writeToStream(outputStream);

        outputStream.write(("</" + verb.getType().displayName() + ">").getBytes());
        outputStream.write(("</" + OAI_PMH + ">\n").getBytes());

        outputStream.flush();
        outputStream.close();

     }
    
    // This function produces the string representation of the top level,
    // "service" record of an OAIPMH response (i.e., the header that precedes
    // the actual "payload" record, such as <GetRecord>, <ListIdentifiers>, 
    // <ListRecords>, etc.
    
    private String oaiPmhResponseToString(OAIPMH handle) {
        try {
            ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
            XmlWriter writer = new XmlWriter(byteOutputStream, defaultContext());

            writer.writeStartElement(OAI_PMH);
            writer.writeDefaultNamespace(NAMESPACE_URI); 
            writer.writeNamespace(XSISchema.PREFIX, XSISchema.NAMESPACE_URI);
            writer.writeAttribute(XSISchema.PREFIX, XSISchema.NAMESPACE_URI, "schemaLocation",
                    NAMESPACE_URI + " " + SCHEMA_LOCATION);

            writer.writeElement(RESPONSEDATE_FIELD, handle.getResponseDate(), Granularity.Second);
            writer.writeElement(REQUEST_FIELD, handle.getRequest());
            writer.writeEndElement();
            writer.flush();
            writer.close();

            String ret = byteOutputStream.toString().replaceFirst("</"+OAI_PMH+">", "");

            return ret;
        } catch (Exception ex) {
            logger.warning("caught exception trying to convert an OAIPMH response header to string: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        }
    }
    
    private boolean isGetRecord(HttpServletRequest request) {
        return "GetRecord".equals(request.getParameter("verb"));
        
    }
    
    private boolean isListRecords(HttpServletRequest request) {
        return "ListRecords".equals(request.getParameter("verb"));
    }

    protected Context getXoaiContext () {
        return xoaiContext;
    }
    
    protected Repository getXoaiRepository() {
        return xoaiRepository;
    }
    
    protected OAIRequestParametersBuilder newXoaiRequest() {
        return new OAIRequestParametersBuilder();
    }
    
    
    public boolean isHarvestingServerEnabled() {
        return systemConfig.isOAIServerEnabled();
    }
    
    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Dataverse OAI Servlet";
    }

}
