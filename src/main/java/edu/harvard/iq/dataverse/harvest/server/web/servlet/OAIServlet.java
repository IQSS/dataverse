/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.harvest.server.web.servlet;

import com.lyncode.xml.exceptions.XmlWriteException;
import com.lyncode.xoai.dataprovider.DataProvider;
import com.lyncode.xoai.dataprovider.builder.OAIRequestParametersBuilder;
import com.lyncode.xoai.dataprovider.exceptions.BadArgumentException;
import com.lyncode.xoai.dataprovider.exceptions.DuplicateDefinitionException;
import com.lyncode.xoai.dataprovider.exceptions.IllegalVerbException;
import com.lyncode.xoai.dataprovider.exceptions.OAIException;
import com.lyncode.xoai.dataprovider.exceptions.UnknownParameterException;
import com.lyncode.xoai.dataprovider.repository.Repository;
import com.lyncode.xoai.dataprovider.repository.RepositoryConfiguration;
import com.lyncode.xoai.dataprovider.model.Context;
import com.lyncode.xoai.dataprovider.model.MetadataFormat;
import com.lyncode.xoai.services.impl.SimpleResumptionTokenFormat;
import static com.lyncode.xoai.dataprovider.model.MetadataFormat.identity;
import com.lyncode.xoai.dataprovider.parameters.OAICompiledRequest;
import static com.lyncode.xoai.dataprovider.parameters.OAIRequest.Parameter.MetadataPrefix;
import com.lyncode.xoai.dataprovider.repository.ItemRepository;
import com.lyncode.xoai.dataprovider.repository.SetRepository;
import com.lyncode.xoai.exceptions.InvalidResumptionTokenException;
import com.lyncode.xoai.model.oaipmh.DeletedRecord;
import com.lyncode.xoai.model.oaipmh.GetRecord;
import com.lyncode.xoai.model.oaipmh.Granularity;
import com.lyncode.xoai.model.oaipmh.OAIPMH;

import com.lyncode.xoai.xml.XmlWriter;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.export.ExportException;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.harvest.server.OAIRecordServiceBean;
import edu.harvard.iq.dataverse.harvest.server.OAISetServiceBean;
import edu.harvard.iq.dataverse.harvest.server.web.XOAIItemRepository;
import edu.harvard.iq.dataverse.harvest.server.web.XOAISetRepository;
import edu.harvard.iq.dataverse.harvest.server.web.xMetadata;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.File;
import java.io.FileInputStream;

import java.io.IOException;
import java.io.InputStream;
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
    private static boolean debug = false;
    private static final String DATAVERSE_EXTENDED_METADATA_FORMAT = "dataverse_json";
    private static final String DATAVERSE_EXTENDED_METADATA_INFO = "Custom Dataverse metadata in JSON format (Dataverse4 to Dataverse4 harvesting only)";
    private static final String DATAVERSE_EXTENDED_METADATA_SCHEMA = "JSON schema pending";
    private static final String DATAVERSE_EXTENDED_METADATA_API = "/api/datasets/export";
     
    
    private Context xoaiContext;
    private SetRepository setRepository;
    private ItemRepository itemRepository;
    private RepositoryConfiguration repositoryConfiguration;
    private Repository xoaiRepository;
    private DataProvider dataProvider; 

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        
        xoaiContext =  createContext();
        
        if (isDataverseOaiExtensionsSupported()) {
            xoaiContext = addDataverseJsonMetadataFormat(xoaiContext);
        }
        
        setRepository = new XOAISetRepository(setService);
        itemRepository = new XOAIItemRepository(recordService);

        repositoryConfiguration = createRepositoryConfiguration(); 
                        
        xoaiRepository = new Repository()
            .withSetRepository(setRepository)
            .withItemRepository(itemRepository)
            .withResumptionTokenFormatter(new SimpleResumptionTokenFormat())
            .withConfiguration(repositoryConfiguration);
        
        dataProvider = new DataProvider(getXoaiContext(), getXoaiRepository());
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
            logger.fine("executing dataProvider.handle():");
            
            OAIPMH handle = dataProvider.handle(parametersBuilder);
            logger.fine("executed dataProvider.handle().");
            response.setContentType("text/xml;charset=UTF-8");
           
            if (isGetRecord(request)) {
                String formatName = parametersBuilder.build().get(MetadataPrefix);
                writeGetRecord(response, handle, formatName);
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

    private void writeGetRecord(HttpServletResponse response, OAIPMH handle, String formatName) throws IOException, XmlWriteException, XMLStreamException {
        // TODO: 
        // produce clean failure records when proper record cannot be 
        // produced for some reason. 
        
        String responseBody = XmlWriter.toString(handle);

        responseBody = responseBody.replaceFirst("<metadata/></record></GetRecord></OAI-PMH>", "<metadata");
        OutputStream outputStream = response.getOutputStream();

        GetRecord getRecord = (GetRecord) handle.getVerb();
        String identifier = getRecord.getRecord().getHeader().getIdentifier();
        logger.fine("identifier from GetRecord: " + identifier);
        
        Dataset dataset = datasetService.findByGlobalId(identifier);
        if (dataset == null) {
            // TODO: Failure record:
        }
        
        if (!isExtendedDataverseMetadataMode(formatName)) {
            InputStream inputStream = null; 
            try {
                inputStream = ExportService.getInstance().getExport(dataset, formatName);
            } catch (ExportException ex) {
                inputStream = null; 
            }

            if (inputStream == null) {
                // TODO: Failure record:
            }
        
            responseBody = responseBody.concat(">");
            outputStream.write(responseBody.getBytes());
            outputStream.flush();
            
            writeMetadataStream(inputStream, outputStream);
        } else {
            // Custom Dataverse metadata extension: 
            // (check again if the client has explicitly requested/advertised support
            // of the extensions?)
            
            responseBody = responseBody.concat(customMetadataExtensionAttribute(identifier)+">");
            outputStream.write(responseBody.getBytes());
            outputStream.flush();
        }

        

        String responseFooter = "</metadata></record></GetRecord></OAI-PMH>";
        outputStream.write(responseFooter.getBytes());
        outputStream.flush();
        outputStream.close();
        
        
    }
    
    private String customMetadataExtensionAttribute(String identifier) {
        String ret = " directApiCall=\"" 
                + systemConfig.getDataverseSiteUrl() 
                + DATAVERSE_EXTENDED_METADATA_API 
                + "?exporter=" 
                + DATAVERSE_EXTENDED_METADATA_FORMAT 
                + "&amp;persistentId="
                + identifier
                + "\"";
        
        return ret;
    }
    
    private void writeMetadataStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        int bufsize;
        byte[] buffer = new byte[4 * 8192];

        while ((bufsize = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bufsize);
            outputStream.flush();
        }

        inputStream.close();
    }
    
    private boolean isGetRecord(HttpServletRequest request) {
        return "GetRecord".equals(request.getParameter("verb"));
        
    }


    private boolean isExtendedDataverseMetadataMode(String formatName) {
        return DATAVERSE_EXTENDED_METADATA_FORMAT.equals(formatName);
    }
    /**                                                                                                                                      
     * Get a response Writer depending on acceptable encodings                                                                               
     * @param request the servlet's request information                                                                                      
     * @param response the servlet's response information                                                                                    
     * @exception IOException an I/O error occurred                                                                                          
     */
    public static Writer getWriter(HttpServletRequest request, HttpServletResponse response)
    throws IOException {
        Writer out;
        String encodings = request.getHeader("Accept-Encoding");
        if (debug) {
            System.out.println("encodings=" + encodings);
        }
        if (encodings != null && encodings.indexOf("gzip") != -1) {
            response.setHeader("Content-Encoding", "gzip");
            out = new OutputStreamWriter(new GZIPOutputStream(response.getOutputStream()),
            "UTF-8");
                                                                                 
        } else if (encodings != null && encodings.indexOf("deflate") != -1) {
                                                                                            
            response.setHeader("Content-Encoding", "deflate");
            out = new OutputStreamWriter(new DeflaterOutputStream(response.getOutputStream()),
            "UTF-8");
        } else {
            out = response.getWriter();
        }
        return out;
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     *//*
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }*/

    protected Context getXoaiContext () {
        return xoaiContext;
    }
    
    protected Repository getXoaiRepository() {
        return xoaiRepository;
    }
    
    protected OAIRequestParametersBuilder newXoaiRequest() {
        return new OAIRequestParametersBuilder();
    }
    
    protected OAICompiledRequest compileXoaiRequest (OAIRequestParametersBuilder builder) throws BadArgumentException, InvalidResumptionTokenException, UnknownParameterException, IllegalVerbException, DuplicateDefinitionException {
        return OAICompiledRequest.compile(builder);
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
