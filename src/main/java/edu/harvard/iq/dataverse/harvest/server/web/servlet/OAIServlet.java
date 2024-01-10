/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.harvest.server.web.servlet;

import io.gdcc.xoai.dataprovider.DataProvider;
import io.gdcc.xoai.dataprovider.repository.Repository;
import io.gdcc.xoai.dataprovider.repository.RepositoryConfiguration;
import io.gdcc.xoai.dataprovider.model.Context;
import io.gdcc.xoai.dataprovider.model.MetadataFormat;
import io.gdcc.xoai.dataprovider.request.RequestBuilder;
import io.gdcc.xoai.dataprovider.request.RequestBuilder.RawRequest;
import io.gdcc.xoai.dataprovider.repository.ItemRepository;
import io.gdcc.xoai.dataprovider.repository.SetRepository;
import io.gdcc.xoai.model.oaipmh.DeletedRecord;
import io.gdcc.xoai.model.oaipmh.OAIPMH;

import io.gdcc.xoai.xml.XmlWriter;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.export.ExportService;
import io.gdcc.spi.export.ExportException;
import io.gdcc.spi.export.Exporter;
import io.gdcc.spi.export.XMLExporter;
import edu.harvard.iq.dataverse.harvest.server.OAIRecordServiceBean;
import edu.harvard.iq.dataverse.harvest.server.OAISetServiceBean;
import edu.harvard.iq.dataverse.harvest.server.xoai.DataverseXoaiItemRepository;
import edu.harvard.iq.dataverse.harvest.server.xoai.DataverseXoaiSetRepository;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.MailUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import io.gdcc.xoai.exceptions.OAIException;
import io.gdcc.xoai.model.oaipmh.Granularity;
import io.gdcc.xoai.services.impl.SimpleResumptionTokenFormat;
import org.apache.commons.lang3.StringUtils;


import java.io.IOException;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.mail.internet.InternetAddress;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLStreamException;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 *
 * @author Leonid Andreev
 * Dedicated servlet for handling OAI-PMH requests.
 * Uses lyncode/Dspace/gdcc XOAI data provider implementation for serving content. 
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

    @Inject
    @ConfigProperty(name = "dataverse.oai.server.maxidentifiers", defaultValue="100")
    private Integer maxListIdentifiers;
    
    @Inject
    @ConfigProperty(name = "dataverse.oai.server.maxsets", defaultValue="100")
    private Integer maxListSets;
    
    @Inject
    @ConfigProperty(name = "dataverse.oai.server.maxrecords", defaultValue="10")
    private Integer maxListRecords;
    
    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.harvest.server.web.servlet.OAIServlet");
    // If we are going to stick with this solution - of providing a minimalist 
    // xml record containing a link to the proprietary json metadata API for 
    // "dataverse json harvesting", we'll probably want to create minimalistic,  
    // but valid schemas for this format as well. 
    // UPDATE: we are keeping this hack on the server side purely for backward 
    // compatibility with older (pre v6) Dataverses who may still be using the 
    // format. Once v6 has been around for a while, we will get rid of it completely. 
    // Starting this version, harvesting clients will not be making GetRecord 
    // calls at all when using harvesting dataverse_json; instead they will only 
    // be calling ListIdentifiers, and then making direct calls to the export 
    // API of the remote Dataverse, to obtain the records in native json. This 
    // is how we should have implemented this in the first place, really. 
    /*
    SEK
     per #3621 we are adding urls to the namespace and schema
     These will not resolve presently. the change is so that the
     xml produced by  https://demo.dataverse.org/oai?verb=ListMetadataFormats will validate
    */
    private static final String DATAVERSE_EXTENDED_METADATA_FORMAT = "dataverse_json";
    private static final String DATAVERSE_EXTENDED_METADATA_NAMESPACE = "https://dataverse.org/schema/core";
    private static final String DATAVERSE_EXTENDED_METADATA_SCHEMA = "https://dataverse.org/schema/core.xsd";     
    
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
        
        setRepository = new DataverseXoaiSetRepository(setService);
        itemRepository = new DataverseXoaiItemRepository(recordService, datasetService, SystemConfig.getDataverseSiteUrlStatic());

        repositoryConfiguration = createRepositoryConfiguration(); 
                                
        xoaiRepository = new Repository(repositoryConfiguration)
            .withSetRepository(setRepository)
            .withItemRepository(itemRepository);
        
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

            if (exporter != null && (exporter instanceof XMLExporter) && exporter.isHarvestable()) {
                MetadataFormat metadataFormat;

                metadataFormat = MetadataFormat.metadataFormat(formatName);
                metadataFormat.withNamespace(((XMLExporter) exporter).getXMLNameSpace());
                metadataFormat.withSchemaLocation(((XMLExporter) exporter).getXMLSchemaLocation());

                if (metadataFormat != null) {
                    context.withMetadataFormat(metadataFormat);
                }
            }
        }
    }
    
    private Context addDataverseJsonMetadataFormat(Context context) {
        MetadataFormat metadataFormat = MetadataFormat.metadataFormat(DATAVERSE_EXTENDED_METADATA_FORMAT);
        metadataFormat.withNamespace(DATAVERSE_EXTENDED_METADATA_NAMESPACE);
        metadataFormat.withSchemaLocation(DATAVERSE_EXTENDED_METADATA_SCHEMA);
        context.withMetadataFormat(metadataFormat);
        return context;
    }
    
    private boolean isDataverseOaiExtensionsSupported() {
        return true;
    }
    
    private RepositoryConfiguration createRepositoryConfiguration() {
        Config config = ConfigProvider.getConfig();
        String repositoryName = config.getOptionalValue("dataverse.oai.server.repositoryname", String.class).orElse("");
        
        if (repositoryName == null || repositoryName.isEmpty()) {
            String dataverseName = dataverseService.getRootDataverseName();
            repositoryName = StringUtils.isEmpty(dataverseName) || "Root".equals(dataverseName) ? "Test Dataverse OAI Archive" : dataverseName + " Dataverse OAI Archive";
        }
        // The admin email address associated with this installation: 
        // (Note: if the setting does not exist, we are going to assume that they
        // have a reason not to want to configure their email address, if it is
        // a developer's instance, for example; or a reason not to want to 
        // advertise it to the world.) 
        InternetAddress systemEmailAddress = MailUtil.parseSystemAddress(settingsService.getValueForKey(SettingsServiceBean.Key.SystemEmail));
        String systemEmailLabel = systemEmailAddress != null ? systemEmailAddress.getAddress() : "donotreply@localhost";
        
        RepositoryConfiguration configuration = new RepositoryConfiguration.RepositoryConfigurationBuilder()
                .withAdminEmail(systemEmailLabel)
                .withCompression("gzip")
                .withCompression("deflate")
                .withGranularity(Granularity.Lenient)
                .withResumptionTokenFormat(new SimpleResumptionTokenFormat().withGranularity(Granularity.Second))
                .withRepositoryName(repositoryName)
                .withBaseUrl(systemConfig.getDataverseSiteUrl()+"/oai")
                .withEarliestDate(recordService.getEarliestDate())
                .withMaxListIdentifiers(maxListIdentifiers)
                .withMaxListSets(maxListSets)
                .withMaxListRecords(maxListRecords)
                .withDeleteMethod(DeletedRecord.TRANSIENT)
                .withEnableMetadataAttributes(true)
                .withRequireFromAfterEarliest(false)
                .build();        
        
        return configuration; 
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
          
    
    private void processRequest(HttpServletRequest httpServletRequest, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            if (!isHarvestingServerEnabled()) {
                response.sendError(
                        HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                        "Sorry. OAI Service is disabled on this Dataverse node.");
                return;
            }
                        
            RawRequest rawRequest = RequestBuilder.buildRawRequest(httpServletRequest.getParameterMap());
            
            OAIPMH handle = dataProvider.handle(rawRequest);
            response.setContentType("text/xml;charset=UTF-8");

            try (XmlWriter xmlWriter = new XmlWriter(response.getOutputStream(), repositoryConfiguration);) {
                xmlWriter.write(handle);
            }
                       
        } catch (XMLStreamException | OAIException e) {
            throw new ServletException (e);
        }
        
    }
    
    protected Context getXoaiContext () {
        return xoaiContext;
    }
    
    protected Repository getXoaiRepository() {
        return xoaiRepository;
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
