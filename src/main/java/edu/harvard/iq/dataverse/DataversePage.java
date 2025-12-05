package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.UserNotification.Type;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataset.DatasetUtil;
import edu.harvard.iq.dataverse.dataverse.DataverseUtil;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CheckRateLimitForCollectionPageCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateSavedSearchCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.LinkDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseCommand;
import edu.harvard.iq.dataverse.pidproviders.PidProvider;
import edu.harvard.iq.dataverse.pidproviders.PidProviderFactoryBean;
import edu.harvard.iq.dataverse.pidproviders.PidUtil;
import edu.harvard.iq.dataverse.search.FacetCategory;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.search.SearchIncludeFragment;
import edu.harvard.iq.dataverse.search.savedsearch.SavedSearch;
import edu.harvard.iq.dataverse.search.savedsearch.SavedSearchFilterQuery;
import edu.harvard.iq.dataverse.search.savedsearch.SavedSearchServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.util.List;

import edu.harvard.iq.dataverse.util.cache.CacheFactoryBean;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIInput;
import org.primefaces.model.DualListModel;
import jakarta.ejb.EJBException;
import jakarta.faces.event.ValueChangeEvent;
import jakarta.faces.model.SelectItem;
import jakarta.json.JsonObject;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;
import org.primefaces.event.TransferEvent;

/**
 *
 * @author gdurand
 */
@ViewScoped
@Named("DataversePage")
public class DataversePage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(DataversePage.class.getCanonicalName());

    public enum EditMode {
        CREATE, INFO, FEATURED
    }
    
    public enum LinkMode {
        SAVEDSEARCH,  LINKDATAVERSE
    }

    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DatasetServiceBean datasetService;
    @Inject
    DataverseSession session;
    @EJB
    EjbDataverseEngine commandEngine;
    @EJB
    DatasetFieldServiceBean datasetFieldService;
    @EJB
    DataverseFacetServiceBean dataverseFacetService;
    @EJB
    UserNotificationServiceBean userNotificationService;
    @EJB
    FeaturedDataverseServiceBean featuredDataverseService;
    @EJB
    DataverseFieldTypeInputLevelServiceBean dataverseFieldTypeInputLevelService;
    @EJB
    PermissionServiceBean permissionService;
    @EJB
    ControlledVocabularyValueServiceBean controlledVocabularyValueServiceBean;
    @EJB
    SavedSearchServiceBean savedSearchService;
    @EJB
    SystemConfig systemConfig;
    @EJB DataverseRoleServiceBean dataverseRoleServiceBean;
    @Inject
    SearchIncludeFragment searchIncludeFragment;
    @Inject
    DataverseRequestServiceBean dvRequestService;
    @Inject
    SettingsWrapper settingsWrapper; 
    @EJB
    DataverseLinkingServiceBean linkingService;
    @Inject PermissionsWrapper permissionsWrapper;
    @Inject 
    NavigationWrapper navigationWrapper;
    @Inject DataverseHeaderFragment dataverseHeaderFragment;
    @EJB
    PidProviderFactoryBean pidProviderFactoryBean;
    @EJB
    CacheFactoryBean cacheFactory;

    private Dataverse dataverse = new Dataverse();  

    /**
     * View parameters
     */
    private Long id = null;
    private String alias = null;
    private Long ownerId = null;    
    private EditMode editMode;
    private LinkMode linkMode;

    private DualListModel<DatasetFieldType> facets = new DualListModel<>(new ArrayList<>(), new ArrayList<>());
    private DualListModel<Dataverse> featuredDataverses = new DualListModel<>(new ArrayList<>(), new ArrayList<>());
    private List<Dataverse> dataversesForLinking;
    private Long linkingDataverseId;
    private List<SelectItem> linkingDVSelectItems;
    private Dataverse linkingDataverse;
    private List<ControlledVocabularyValue> selectedSubjects;

    public List<ControlledVocabularyValue> getSelectedSubjects() {
        return selectedSubjects;
    }

    public void setSelectedSubjects(List<ControlledVocabularyValue> selectedSubjects) {
        this.selectedSubjects = selectedSubjects;
    }

    public Dataverse getLinkingDataverse() {
        return linkingDataverse;
    }

    public void setLinkingDataverse(Dataverse linkingDataverse) {
        this.linkingDataverse = linkingDataverse;
    }

    public List<SelectItem> getLinkingDVSelectItems() {
        return linkingDVSelectItems;
    }

    public void setLinkingDVSelectItems(List<SelectItem> linkingDVSelectItems) {
        this.linkingDVSelectItems = linkingDVSelectItems;
    }

    public Long getLinkingDataverseId() {
        return linkingDataverseId;
    }

    public void setLinkingDataverseId(Long linkingDataverseId) {
        this.linkingDataverseId = linkingDataverseId;
    }

    public List<Dataverse> getDataversesForLinking() {
        return dataversesForLinking;
    }

    public void setDataversesForLinking(List<Dataverse> dataversesForLinking) {

        this.dataversesForLinking = dataversesForLinking;
    }

    private List<ControlledVocabularyValue> dataverseSubjectControlledVocabularyValues;

    public List<ControlledVocabularyValue> getDataverseSubjectControlledVocabularyValues() {
        return dataverseSubjectControlledVocabularyValues;
    }

    public void setDataverseSubjectControlledVocabularyValues(List<ControlledVocabularyValue> dataverseSubjectControlledVocabularyValues) {
        this.dataverseSubjectControlledVocabularyValues = dataverseSubjectControlledVocabularyValues;
    }

    private void updateDataverseSubjectSelectItems() {
        DatasetFieldType subjectDatasetField = datasetFieldService.findByName(DatasetFieldConstant.subject);
        setDataverseSubjectControlledVocabularyValues(controlledVocabularyValueServiceBean.findByDatasetFieldTypeId(subjectDatasetField.getId()));
    }
    
    public LinkMode getLinkMode() {
        return linkMode;
    }

    public void setLinkMode(LinkMode linkMode) {
        this.linkMode = linkMode;
    }
    
    public boolean showLinkingPopup() {
        String testquery = "";
        if (session.getUser() == null) {
            return false;
        }
        if (dataverse == null) {
            return false;
        }
        if (query != null) {
            testquery = query;
        }

        return (session.getUser().isSuperuser() && (dataverse.getOwner() != null || !testquery.isEmpty()));
    }
    
    public void setupLinkingPopup (String popupSetting){
        if (popupSetting.equals("link")){
            setLinkMode(LinkMode.LINKDATAVERSE);           
        } else {
            setLinkMode(LinkMode.SAVEDSEARCH); 
        }
        updateLinkableDataverses();
    }

    public void updateLinkableDataverses() {
        dataversesForLinking = new ArrayList<>();
        linkingDVSelectItems = new ArrayList<>();
        
        //Since only a super user function add all dvs
        dataversesForLinking = dataverseService.findAll();// permissionService.getDataversesUserHasPermissionOn(session.getUser(), Permission.PublishDataverse);
        
        /*
        List<DataverseRole> roles = dataverseRoleServiceBean.getDataverseRolesByPermission(Permission.PublishDataverse, dataverse.getId());
        List<String> types = new ArrayList();
        types.add("Dataverse");
        for (Long dvIdAsInt : permissionService.getDvObjectIdsUserHasRoleOn(session.getUser(), roles, types, false)) {
            dataversesForLinking.add(dataverseService.find(dvIdAsInt));
        }*/
        
        //for linking - make sure the link hasn't occurred and its not int the tree
        if (this.linkMode.equals(LinkMode.LINKDATAVERSE)) {
        
            // remove this and it's parent tree
            dataversesForLinking.remove(dataverse);
            Dataverse testDV = dataverse;
            while(testDV.getOwner() != null){
                dataversesForLinking.remove(testDV.getOwner());
                testDV = testDV.getOwner();
            }                
            
            for (Dataverse removeLinked : linkingService.findLinkingDataverses(dataverse.getId())) {
                dataversesForLinking.remove(removeLinked);
            }
        } else{
            //for saved search add all

        }

        for (Dataverse selectDV : dataversesForLinking) {
            linkingDVSelectItems.add(new SelectItem(selectDV.getId(), selectDV.getDisplayName()));
        }

        if (!dataversesForLinking.isEmpty() && dataversesForLinking.size() == 1 && dataversesForLinking.get(0) != null) {
            linkingDataverse = dataversesForLinking.get(0);
            linkingDataverseId = linkingDataverse.getId();
        }
    }

    public void updateSelectedLinkingDV(ValueChangeEvent event) {
        linkingDataverseId = (Long) event.getNewValue();
    }

    public String getSchemaOrgDcatJson () {
        String repositoryURL    = SystemConfig.getDataverseSiteUrlStatic();
        String repositoryName   = BrandingUtil.getInstallationBrandName();
        String repositoryContact= settingsWrapper.getSupportTeamEmail();
        String description      = settingsWrapper.get(":dcat_repositoryDescription");
        String language         = settingsWrapper.get(":dcat_repository_language");
        String country          = settingsWrapper.get(":dcat_repository_country");
        String reserarchArea    = settingsWrapper.get(":dcat_repository_research_area");
        String prev_policy      = settingsWrapper.get(":dcat_repository_prev_policy");
        String terms            = settingsWrapper.get(":dcat_repository_terms");
        String license          = settingsWrapper.get(":dcat_repository_license");
        String orgName          = settingsWrapper.get(":dcat_repository_org_name");
        String access_terms     = settingsWrapper.get(":dcat_repository_access_terms"); 
        String certification    = settingsWrapper.get(":dcat_repository_certification");
        JsonObject oaiSchJson       = JsonObject.EMPTY_JSON_OBJECT;
        JsonObject siteMapSchJson   = JsonObject.EMPTY_JSON_OBJECT;
        JsonObject oaiDcatJson      = JsonObject.EMPTY_JSON_OBJECT;
        JsonObject licenseDcatJson  = JsonObject.EMPTY_JSON_OBJECT;
        JsonObject termsDcatJson    = JsonObject.EMPTY_JSON_OBJECT;
        JsonObject preservDcatJson  = JsonObject.EMPTY_JSON_OBJECT;
        JsonObject siteMapDcatJson  = JsonObject.EMPTY_JSON_OBJECT;
        

        if (systemConfig.isOAIServerEnabled ()) {
            JsonObjectBuilder oaiBuilder= Json.createObjectBuilder()
                        .add("@type", "schema:Offer")
                        .add("schema:itemOffered", Json.createObjectBuilder()
                            .add("@type", "schema:WebAPI")
                            .add("schema:url", repositoryURL + "/oai")
                            .add("schema:documentation", "https://www.openarchives.org/OAI/2.0/guidelines-static-repository.htm"));
            oaiSchJson= oaiBuilder.build();
        }
        try {
            URL url = new URL(repositoryURL + "/sitemap/sitemap.xml");
            HttpURLConnection huc = (HttpURLConnection) url.openConnection();
            int responseCode = huc.getResponseCode();
            if (responseCode== 200) {
                JsonObjectBuilder siteMapBuilder= Json.createObjectBuilder()
                        .add("@type", "schema:Offer")
                        .add("schema:itemOffered", Json.createObjectBuilder()
                            .add("@type", "schema:WebAPI")
                            .add("schema:url", repositoryURL + "/sitemap.xml")
                            .add("schema:documentation", "https://www.sitemaps.org/protocol.html"));
                siteMapSchJson= siteMapBuilder.build();
            }
        } catch (IOException iOException) {
            // Nothing to do 
        }

        JsonObjectBuilder builder = Json.createObjectBuilder()
                .add("@context", Json.createObjectBuilder()
                    .add("dcat", "http://www.w3.org/ns/dcat#")
                    .add("dct", "http://purl.org/dc/terms/")
                    .add("foaf", "http://xmlns.com/foaf/0.1/")
                    .add("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
                    .add("rdfs", "http://www.w3.org/2000/01/rdf-schema#")
                    .add("schema", "http://schema.org/")
                    .add("vcard", "http://www.w3.org/2006/vcard/ns#")
                    .add("xsd", "http://www.w3.org/2001/XMLSchema#")
                    .add("dqv", "http://www.w3.org/ns/dqv#")
                    .add("oa", "http://www.w3.org/ns/oa#")
                    .add("premis", "http://www.loc.gov/premis/rdf/v3/"))
                .add("@type", Json.createArrayBuilder()
                    .add("dcat:Catalog")
                    .add("foaf:Project")
                    .add("schema:DataCatalog")
                    .add("schema:Project"));
        if (!StringUtils.isEmpty(repositoryURL)) {
            builder= builder
                .add("@id",repositoryURL)
                .add("foaf:homepage", repositoryURL)
                .add("dct:identifier", repositoryURL)    
                .add("schema:url",repositoryURL);
        }
        if (!StringUtils.isEmpty(repositoryName)) {
            builder= builder
                .add("foaf:name", repositoryName)
                .add("dct:title", repositoryName);
        }
        if (!StringUtils.isEmpty(description)) {
            builder= builder
                .add("dct:description", description);
        }
        if (!StringUtils.isEmpty(repositoryContact)) {
            builder= builder
                .add("dcat:contactPoint", repositoryContact);
        }
        if (!StringUtils.isEmpty(reserarchArea)) {
            builder= builder
                .add("dcat:theme", Json.createArrayBuilder()
                .add(reserarchArea));
        }
        if (!StringUtils.isEmpty(access_terms)) {
            builder= builder
                .add("dct:accessRights", access_terms);
        }
        if (!StringUtils.isEmpty(license)) {
            builder= builder
                .add("dct:license", license);
            JsonObjectBuilder licBuilder= Json.createObjectBuilder()
                        .add("@type", "dct:Policy")
                        .add("@id", license);
            licenseDcatJson= licBuilder.build();
        }
        if (!StringUtils.isEmpty(certification) && !StringUtils.isEmpty(repositoryURL) ) {
            String [] certificationParts= certification.split("\\|");
            if (certificationParts[0].equals("CoreTrustSeal")) {
                builder= builder
                .add("dqv:hasQualityAnnotation", Json.createObjectBuilder()
                    .add("@type", "dqv:QualityCertificate")
                    .add("oa:hasTarget", repositoryURL)
                    .add("oa:hasBody", "https://amt.coretrustseal.org/certificates")
                    .add("oa:motivatedBy", "dqv:qualityAssessment")
                    .add("dct:creator", "CoreTrustSeal"));
            }
        }
        if (systemConfig.isOAIServerEnabled ()) {
            JsonObjectBuilder oaiBuilder= Json.createObjectBuilder()
                        .add("@type", "dcat:DataService")
                        .add("dcat:endpointURL", repositoryURL + "oai")
                        .add("dct:conformsTo", "https://www.openarchives.org/OAI/2.0/guidelines-static-repository.htm");
            oaiDcatJson= oaiBuilder.build();
        }
        if (!StringUtils.isEmpty(terms)) {
            JsonObjectBuilder termsBuilder= Json.createObjectBuilder()
                        .add("@type", "dct:accrualPolicy")
                        .add("@id", terms);
            termsDcatJson= termsBuilder.build();
        }
        if (!StringUtils.isEmpty(prev_policy)) {
            JsonObjectBuilder preservBuilder= Json.createObjectBuilder()
                        .add("@type", "premis:PreservationPolicy")
                        .add("@id", prev_policy)
                        .add("rdfs:seeAlso", "https://w3id.org/fair/fip/latest/Metadata-preservation-policy");
            preservDcatJson= preservBuilder.build();
        }
        if (!StringUtils.isEmpty(repositoryName)) {
            builder= builder
                .add("schema:name",repositoryName);
        }
        if (!StringUtils.isEmpty(description)) {
            builder= builder
                .add("schema:description",description);
        }
        if (!StringUtils.isEmpty(repositoryContact)) {
            builder= builder
                .add("schema:contactPoint",repositoryContact);
        }
        if (!StringUtils.isEmpty(reserarchArea)) {
            builder= builder
                .add("schema:keywords", Json.createArrayBuilder()
                    .add(reserarchArea));
        }
        if (!StringUtils.isEmpty(access_terms)) {
            builder= builder
                .add("schema:conditionsOfAccess", access_terms);
        }
        if (!StringUtils.isEmpty(license)) {
            builder= builder
                .add("schema:license", license);
        }
        if (!StringUtils.isEmpty(certification)) {
            String [] certificationParts= certification.split("\\|");
            if (certificationParts.length== 3 && certificationParts[0].equals("CoreTrustSeal"))
                builder= builder
                .add("schema:hasCertification", Json.createObjectBuilder()
                    .add("@type", "schema:Certification")
                    .add("schema:url", "https://amt.coretrustseal.org/certificates")
                    .add("schema:certificationStatus", "schema:CertificationActive")
                    .add("schema:issuedBy", Json.createObjectBuilder()
                        .add("@type", "schema:Organization")
                        .add("schema:name", "CoreTrustSeal")
                        .add("schema:url", "https://www.coretrustseal.org"))
                    .add("schema:auditDate", "2023-04-18")
                    .add("schema:expires", "2026-04-17"));
        }
        if (!StringUtils.isEmpty(terms) && !StringUtils.isEmpty(prev_policy)) {
            builder=builder
                .add("schema:publishingPrinciples", Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                        .add("@type", "schema:CreativeWork")
                        .add("schema:url", terms)
                        .add("schema:additionalType", "dct:Policy"))
                    .add(Json.createObjectBuilder()
                        .add("@type", "schema:CreativeWork")
                        .add("schema:url", prev_policy)
                        .add("schema:additionalType", "premis:PreservationPolicy")));
        } else if (!StringUtils.isEmpty(terms)) {
            builder=builder
                        .add("schema:publishingPrinciples", Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                        .add("@type", "schema:CreativeWork")
                        .add("schema:url", terms)
                        .add("schema:additionalType", "dct:Policy")));            
        } else if (!StringUtils.isEmpty(prev_policy)) {
            builder=builder
                .add("schema:publishingPrinciples", Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                        .add("@type", "schema:CreativeWork")
                        .add("schema:url", prev_policy)
                        .add("schema:additionalType", "premis:PreservationPolicy")));
        }
        if (!StringUtils.isEmpty(orgName) && !StringUtils.isEmpty(country)) {
            builder= builder
                .add("schema:publisher", Json.createObjectBuilder()
                    .add("@type", "schema:Organization")
                    .add("schema:name", orgName)
                    .add("schema:address", Json.createObjectBuilder()
                        .add("@type", "schema:PostalAddress")
                        .add("schema:addressCountry", country)));
        }
        if (!StringUtils.isEmpty(language)) {
            builder= builder
                .add("schema:inLanguage", language);
        }
        builder= builder
                .add("dct:conformsTo", Json.createArrayBuilder()
                    .add(termsDcatJson)
                    .add(licenseDcatJson)
                    .add(preservDcatJson)
                    .add(Json.createObjectBuilder()
                        .add("@type", "dct:Standard")
                        .add("@id", "http://www.dcc.ac.uk/resources/metadata-standards/dcat-data-catalog-vocabulary")
                        .add("rdfs:seeAlso", "https://w3id.org/fair/fip/latest/Metadata-schema"))
                    .add(Json.createObjectBuilder()
                        .add("@type", "dct:Standard")
                        .add("@id", "https://www.dcc.ac.uk/resources/metadata-standards/oai-ore-open-archives-initiative-object-reuse-and-exchange")
                        .add("rdfs:seeAlso", "https://w3id.org/fair/fip/latest/Metadata-schema"))
                    .add(Json.createObjectBuilder()
                        .add("@type", "dct:Standard")
                        .add("@id", "https://www.dcc.ac.uk/resources/metadata-standards/datacite-metadata-schema")
                        .add("rdfs:seeAlso", "https://w3id.org/fair/fip/latest/Metadata-schema"))
                    .add(Json.createObjectBuilder()
                        .add("@type", "dct:Standard")
                        .add("@id", "https://guidelines.openaire.eu/en/latest/data/use_of_datacite.html")
                        .add("rdfs:seeAlso", "https://w3id.org/fair/fip/latest/Metadata-schema"))
                    .add(Json.createObjectBuilder()
                        .add("@type", "dct:Standard")
                        .add("@id", "https://json-ld.org")
                        .add("rdfs:seeAlso", "https://w3id.org/fair/fip/latest/Metadata-schema"))
                    .add(Json.createObjectBuilder()
                        .add("@type", "dct:Standard")
                        .add("@id", "https://www.dcc.ac.uk/resources/metadata-standards/ddi-data-documentation-initiative")
                        .add("rdfs:seeAlso", "https://w3id.org/fair/fip/latest/Metadata-schema"))
                    .add(Json.createObjectBuilder()
                        .add("@type", "dct:Standard")
                        .add("@id", "https://www.dublincore.org/specifications/dublin-core/dcmi-terms")
                        .add("rdfs:seeAlso", "https://w3id.org/fair/fip/latest/Metadata-schema"))
                    .add(Json.createObjectBuilder()
                        .add("@type", "dct:Standard")
                        .add("@id", "https://ddialliance.org/Specification/DDI-Codebook/2.5/")
                        .add("rdfs:seeAlso", "https://w3id.org/fair/fip/latest/Metadata-schema"))
                    .add(Json.createObjectBuilder()
                        .add("@type", "dct:Standard")
                        .add("@id", "https://www.json.org")
                        .add("rdfs:seeAlso", "https://w3id.org/fair/fip/latest/Metadata-schema"))
                    .add(Json.createObjectBuilder()
                        .add("@type", "dct:Standard")
                        .add("@id", "https://doi.datacite.org")
                        .add("rdfs:seeAlso", "https://w3id.org/fair/fip/latest/Identifier-service")));
        if (!StringUtils.isEmpty(orgName) && !StringUtils.isEmpty(country)) {
            builder= builder
                .add("dct:publisher", Json.createObjectBuilder()
                    .add("@type", Json.createArrayBuilder()
                        .add("foaf:Agent")
                        .add("vcard:Kind"))
                    .add("foaf:name", orgName)
                    .add("vcard:fn", orgName)
                    .add("vcard:country-name", country));
        }
        if (!StringUtils.isEmpty(language)) {
            builder= builder
                .add("dct:language", language);
        }
        builder= builder
                .add("dcat:service", Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                        .add("@type", "dcat:DataService")
                        .add("dcat:endpointURL", repositoryURL + "/resources/json/.well-known/api-catalog")
                        .add("dct:conformsTo", "https://signposting.org/FAIRiCat"))
                    .add(Json.createObjectBuilder()
                        .add("@type", "dcat:DataService")
                        .add("dcat:endpointURL", repositoryURL + "/api")
                                .add("dct:conformsTo", "https://guides.dataverse.org/en/latest/api/search.html"))
                    .add(Json.createObjectBuilder()
                        .add("@type", "dcat:DataService")
                        .add("dcat:endpointURL", repositoryURL + "/api")
                                .add("dct:conformsTo", "https://guides.dataverse.org/en/latest/api/dataaccess.html"))
                    .add(Json.createObjectBuilder()
                        .add("@type", "dcat:DataService")
                        .add("dcat:endpointURL", repositoryURL + "/api")
                                .add("dct:conformsTo", "https://guides.dataverse.org/en/latest/api/native-api.html"))
                    .add(Json.createObjectBuilder()
                        .add("@type", "dcat:DataService")
                        .add("dcat:endpointURL", repositoryURL + "/api")
                                .add("dct:conformsTo", "https://guides.dataverse.org/en/latest/developers/dataset-semantic-metadata-api.html"))
                    .add(Json.createObjectBuilder()
                        .add("@type", "dcat:DataService")
                        .add("dcat:endpointURL", repositoryURL + "/api")
                                .add("dct:conformsTo", "https://guides.dataverse.org/en/latest/developers/s3-direct-upload-api.html"))
                    .add(Json.createObjectBuilder()
                        .add("@type", "dcat:DataService")
                        .add("dcat:endpointURL", repositoryURL + "/api")
                                .add("dct:conformsTo", "https://guides.dataverse.org/en/latest/developers/globus-api.html"))
                    .add(Json.createObjectBuilder()
                        .add("@type", "dcat:DataService")
                        .add("dcat:endpointURL", repositoryURL + "/api")
                                .add("dct:conformsTo", "https://guides.dataverse.org/en/latest/api/metrics.html"))
                    .add(Json.createObjectBuilder()
                        .add("@type", "dcat:DataService")
                        .add("dcat:endpointURL", repositoryURL + "/api")
                                .add("dct:conformsTo", "http://swordapp.org/"))
                    .add(oaiDcatJson)
                    .add(siteMapDcatJson));
        builder= builder
                .add("schema:offers", Json.createArrayBuilder()   
                    .add(Json.createObjectBuilder()
                        .add("@type", "schema:Offer")
                        .add("schema:itemOffered", Json.createObjectBuilder()
                            .add("@type", "schema:WebAPI")
                            .add("schema:url", repositoryURL + "/resources/json/.well-known/api-catalog")
                            .add("schema:documentation", "https://signposting.org/FAIRiCat")))
                    .add(Json.createObjectBuilder()
                        .add("@type", "schema:Offer")
                        .add("schema:itemOffered", Json.createObjectBuilder()
                            .add("@type", "schema:WebAPI")
                            .add("schema:url", repositoryURL + "/api")
                            .add("schema:documentation", "https://guides.dataverse.org/en/latest/api/search.html")))
                    .add(Json.createObjectBuilder()
                        .add("@type", "schema:Offer")
                        .add("schema:itemOffered", Json.createObjectBuilder()
                            .add("@type", "schema:WebAPI")
                            .add("schema:url", repositoryURL + "/api")
                            .add("schema:documentation", "https://guides.dataverse.org/en/latest/api/dataaccess.html")))
                    .add(Json.createObjectBuilder()
                        .add("@type", "schema:Offer")
                        .add("schema:itemOffered", Json.createObjectBuilder()
                            .add("@type", "schema:WebAPI")
                            .add("schema:url", repositoryURL + "/api")
                            .add("schema:documentation", "https://guides.dataverse.org/en/latest/api/native-api.html")))
                    .add(Json.createObjectBuilder()
                        .add("@type", "schema:Offer")
                        .add("schema:itemOffered", Json.createObjectBuilder()
                            .add("@type", "schema:WebAPI")
                            .add("schema:url", repositoryURL + "/api")
                            .add("schema:documentation", "https://guides.dataverse.org/en/latest/developers/dataset-semantic-metadata-api.html")))
                    .add(Json.createObjectBuilder()
                        .add("@type", "schema:Offer")
                        .add("schema:itemOffered", Json.createObjectBuilder()
                            .add("@type", "schema:WebAPI")
                            .add("schema:url", repositoryURL + "/api")
                            .add("schema:documentation", "https://guides.dataverse.org/en/latest/developers/dataset-migration-api.html")))
                    .add(Json.createObjectBuilder()
                        .add("@type", "schema:Offer")
                        .add("schema:itemOffered", Json.createObjectBuilder()
                            .add("@type", "schema:WebAPI")
                            .add("schema:url", repositoryURL + "/api")
                            .add("schema:documentation", "https://guides.dataverse.org/en/latest/developers/s3-direct-upload-api.html")))
                    .add(Json.createObjectBuilder()
                        .add("@type", "schema:Offer")
                        .add("schema:itemOffered", Json.createObjectBuilder()
                            .add("@type", "schema:WebAPI")
                            .add("schema:url", repositoryURL + "/api")
                            .add("schema:documentation", "https://guides.dataverse.org/en/latest/developers/globus-api.html")))
                    .add(Json.createObjectBuilder()
                        .add("@type", "schema:Offer")
                        .add("schema:itemOffered", Json.createObjectBuilder()
                            .add("@type", "schema:WebAPI")
                            .add("schema:url", repositoryURL + "/api")
                            .add("schema:documentation", "https://guides.dataverse.org/en/latest/api/metrics.html")))
                    .add(Json.createObjectBuilder()
                        .add("@type", "schema:Offer")
                        .add("schema:itemOffered", Json.createObjectBuilder()
                            .add("@type", "schema:WebAPI")
                            .add("schema:url", repositoryURL + "/api")
                            .add("schema:documentation", "http://swordapp.org/")))
                    .add(oaiSchJson)
                    .add(siteMapSchJson)
                    .add(Json.createObjectBuilder()
                        .add("@type", "schema:Offer")
                        .add("schema:itemOffered", Json.createObjectBuilder()
                            .add("@type", "schema:Service")
                            .add("schema:serviceOutput", Json.createObjectBuilder()
                                .add("schema:identifier", "https://doi.datacite.org"))
                            .add("schema:serviceType", "https://w3id.org/fair/fip/latest/Identifier-service")))
                    .add(Json.createObjectBuilder()
                        .add("@type", "schema:Offer")
                        .add("schema:itemOffered", Json.createObjectBuilder()
                            .add("@type", "schema:Service")
                            .add("schema:serviceOutput", Json.createObjectBuilder()
                                .add("schema:identifier", "http://www.dcc.ac.uk/resources/metadata-standards/dcat-data-catalog-vocabulary"))
                            .add("schema:serviceType", "https://w3id.org/fair/fip/latest/Metadata-schema")))
                    .add(Json.createObjectBuilder()
                        .add("@type", "schema:Offer")
                        .add("schema:itemOffered", Json.createObjectBuilder()
                            .add("@type", "schema:Service")
                            .add("schema:serviceOutput", Json.createObjectBuilder()
                                .add("schema:identifier", "https://www.dcc.ac.uk/resources/metadata-standards/oai-ore-open-archives-initiative-object-reuse-and-exchange"))
                            .add("schema:serviceType", "https://w3id.org/fair/fip/latest/Metadata-schema")))
                    .add(Json.createObjectBuilder()
                        .add("@type", "schema:Offer")
                        .add("schema:itemOffered", Json.createObjectBuilder()
                            .add("@type", "schema:Service")
                            .add("schema:serviceOutput", Json.createObjectBuilder()
                                .add("schema:identifier", "https://www.dcc.ac.uk/resources/metadata-standards/datacite-metadata-schema"))
                            .add("schema:serviceType", "https://w3id.org/fair/fip/latest/Metadata-schema")))
                    .add(Json.createObjectBuilder()
                        .add("@type", "schema:Offer")
                        .add("schema:itemOffered", Json.createObjectBuilder()
                            .add("@type", "schema:Service")
                            .add("schema:serviceOutput", Json.createObjectBuilder()
                                .add("schema:identifier", "https://guidelines.openaire.eu/en/latest/data/use_of_datacite.html"))
                            .add("schema:serviceType", "https://w3id.org/fair/fip/latest/Metadata-schema")))
                    .add(Json.createObjectBuilder()
                        .add("@type", "schema:Offer")
                        .add("schema:itemOffered", Json.createObjectBuilder()
                            .add("@type", "schema:Service")
                            .add("schema:serviceOutput", Json.createObjectBuilder()
                                .add("schema:identifier", "https://json-ld.org"))
                            .add("schema:serviceType", "https://w3id.org/fair/fip/latest/Metadata-schema")))
                    .add(Json.createObjectBuilder()
                        .add("@type", "schema:Offer")
                        .add("schema:itemOffered", Json.createObjectBuilder()
                            .add("@type", "schema:Service")
                            .add("schema:serviceOutput", Json.createObjectBuilder()
                                .add("schema:identifier", "https://www.dcc.ac.uk/resources/metadata-standards/ddi-data-documentation-initiative"))
                            .add("schema:serviceType", "https://w3id.org/fair/fip/latest/Metadata-schema")))
                    .add(Json.createObjectBuilder()
                        .add("@type", "schema:Offer")
                        .add("schema:itemOffered", Json.createObjectBuilder()
                            .add("@type", "schema:Service")
                            .add("schema:serviceOutput", Json.createObjectBuilder()
                                .add("schema:identifier", "https://www.dublincore.org/specifications/dublin-core/dcmi-terms"))
                            .add("schema:serviceType", "https://w3id.org/fair/fip/latest/Metadata-schema")))
                    .add(Json.createObjectBuilder()
                        .add("@type", "schema:Offer")
                        .add("schema:itemOffered", Json.createObjectBuilder()
                            .add("@type", "schema:Service")
                            .add("schema:serviceOutput", Json.createObjectBuilder()
                                .add("schema:identifier", "https://ddialliance.org/Specification/DDI-Codebook/2.5/"))
                            .add("schema:serviceType", "https://w3id.org/fair/fip/latest/Metadata-schema")))
                    .add(Json.createObjectBuilder()
                        .add("@type", "schema:Offer")
                        .add("schema:itemOffered", Json.createObjectBuilder()
                            .add("@type", "schema:Service")
                            .add("schema:serviceOutput", Json.createObjectBuilder()
                                .add("schema:identifier", "https://www.json.org"))
                            .add("schema:serviceType", "https://w3id.org/fair/fip/latest/Metadata-schema"))));
        
        return builder.build().toString();
    }
    
    public Dataverse getDataverse() {
        return dataverse;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }
    
    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }

    public String getAlias() { return this.alias; }
    public void setAlias(String alias) { this.alias = alias; }    

    public EditMode getEditMode() {
        return editMode;
    }

    public void setEditMode(EditMode editMode) {
        this.editMode = editMode;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public void updateOwnerDataverse() {
        if (dataverse.getOwner() != null && dataverse.getOwner().getId() != null) {
            ownerId = dataverse.getOwner().getId();
            logger.info("New host dataverse id: " + ownerId);
            // discard the dataverse already created:
            dataverse = new Dataverse();
            // initialize a new new dataverse:
            init();
            dataverseHeaderFragment.initBreadcrumbs(dataverse);
        }
    }
    
    public String init() {
        //System.out.println("_YE_OLDE_QUERY_COUNTER_");  // for debug purposes
        // Check for rate limit exceeded. Must be done before anything else to prevent unnecessary processing.
        if (!cacheFactory.checkRate(session.getUser(), new CheckRateLimitForCollectionPageCommand(null,null))) {
            return navigationWrapper.tooManyRequests();
        }
        if (this.getAlias() != null || this.getId() != null || this.getOwnerId() == null) {// view mode for a dataverse
            if (this.getAlias() != null) {
                dataverse = dataverseService.findByAlias(this.getAlias());
            } else if (this.getId() != null) {
                dataverse = dataverseService.find(this.getId());
            } else {
                try {
                    dataverse = settingsWrapper.getRootDataverse();
                } catch (EJBException e) {
                    // @todo handle case with no root dataverse (a fresh installation) with message about using API to create the root 
                    dataverse = null;
                }
            }

            // check if dv exists and user has permission
            if (dataverse == null) {
                return permissionsWrapper.notFound();
            }
            if (!dataverse.isReleased() && !permissionService.on(dataverse).has(Permission.ViewUnpublishedDataverse)) {
                // the permission lookup above should probably be moved into the permissionsWrapper -- L.A. 5.7
                return permissionsWrapper.notAuthorized();
            }

            ownerId = dataverse.getOwner() != null ? dataverse.getOwner().getId() : null;
        } else { // ownerId != null; create mode for a new child dataverse
            editMode = EditMode.CREATE;
            dataverse.setOwner(dataverseService.find( this.getOwnerId()));
            if (dataverse.getOwner() == null) {
                return  permissionsWrapper.notFound();
            } else if (!permissionService.on(dataverse.getOwner()).has(Permission.AddDataverse)) {
                // the permission lookup above should probably be moved into the permissionsWrapper -- L.A. 5.7
                return permissionsWrapper.notAuthorized();            
            }

            // set defaults - contact e-mail and affiliation from user
            dataverse.getDataverseContacts().add(new DataverseContact(dataverse, session.getUser().getDisplayInfo().getEmailAddress()));
            dataverse.setAffiliation(session.getUser().getDisplayInfo().getAffiliation());
            setupForGeneralInfoEdit();
            // FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Create New Dataverse", " - Create a new dataverse that will be a child dataverse of the parent you clicked from. Asterisks indicate required fields."));
            if (dataverse.getName() == null) {
                dataverse.setName(DataverseUtil.getSuggestedDataverseNameOnCreate(session.getUser()));
            }
        }

        return null;
    }

    public void initFeaturedDataverses() {
        List<Dataverse> featuredSource = new ArrayList<>();
        List<Dataverse> featuredTarget = new ArrayList<>();
        featuredSource.addAll(dataverseService.findAllPublishedByOwnerId(dataverse.getId()));
        featuredSource.addAll(linkingService.findLinkedDataverses(dataverse.getId()));
        List<DataverseFeaturedDataverse> featuredList = featuredDataverseService.findByDataverseId(dataverse.getId());
        for (DataverseFeaturedDataverse dfd : featuredList) {
            Dataverse fd = dfd.getFeaturedDataverse();
            featuredTarget.add(fd);
            featuredSource.remove(fd);
        }
        featuredDataverses = new DualListModel<>(featuredSource, featuredTarget);

    }

    public void initFacets() {
        List<DatasetFieldType> facetsSource = new ArrayList<>();
        List<DatasetFieldType> facetsTarget = new ArrayList<>();
        facetsSource.addAll(datasetFieldService.findAllFacetableFieldTypes());
        List<DataverseFacet> facetsList = dataverseFacetService.findByDataverseId(dataverse.getFacetRootId());
        for (DataverseFacet dvFacet : facetsList) {
            DatasetFieldType dsfType = dvFacet.getDatasetFieldType();
            facetsTarget.add(dsfType);
            facetsSource.remove(dsfType);
        }
        facets = new DualListModel<>(facetsSource, facetsTarget);
        facetMetadataBlockId = null;
    }

    private void setupForGeneralInfoEdit() {
        updateDataverseSubjectSelectItems();
        initFacets();
        refreshAllMetadataBlocks();
    }

    private Long facetMetadataBlockId;

    public Long getFacetMetadataBlockId() {
        return facetMetadataBlockId;
    }

    public void setFacetMetadataBlockId(Long facetMetadataBlockId) {
        this.facetMetadataBlockId = facetMetadataBlockId;
    }

    public void changeFacetsMetadataBlock() {
        if (facetMetadataBlockId == null) {
            facets.setSource(datasetFieldService.findAllFacetableFieldTypes());
        } else {
            facets.setSource(datasetFieldService.findFacetableFieldTypesByMetadataBlock(facetMetadataBlockId));
        }

        facets.getSource().removeAll(facets.getTarget());
    }

    public void toggleFacetRoot() {
        if (!dataverse.isFacetRoot()) {
            initFacets();
        }
    }

    public void onFacetTransfer(TransferEvent event) {
        for (Object item : event.getItems()) {
            DatasetFieldType facet = (DatasetFieldType) item;
            if (facetMetadataBlockId != null && !facetMetadataBlockId.equals(facet.getMetadataBlock().getId())) {
                facets.getSource().remove(facet);
            }
        }
    }

    private List<Dataverse> carouselFeaturedDataverses = null;
    
    public List<Dataverse> getCarouselFeaturedDataverses() {
        if (carouselFeaturedDataverses != null) {
            return carouselFeaturedDataverses;
        }
        carouselFeaturedDataverses = featuredDataverseService.findByDataverseIdQuick(dataverse.getId());/*new ArrayList();
        
        List<DataverseFeaturedDataverse> featuredList = featuredDataverseService.findByDataverseId(dataverse.getId());
        for (DataverseFeaturedDataverse dfd : featuredList) {
            Dataverse fd = dfd.getFeaturedDataverse();
            retList.add(fd);
        }*/
        
        return carouselFeaturedDataverses;
    }

    public List getContents() {
        List contentsList = dataverseService.findByOwnerId(dataverse.getId());
        contentsList.addAll(datasetService.findByOwnerId(dataverse.getId()));
        return contentsList;
    }

    public void edit(EditMode editMode) {
        this.editMode = editMode;
        if (editMode == EditMode.INFO) {
            setupForGeneralInfoEdit();
            JH.addMessage(FacesMessage.SEVERITY_INFO, BundleUtil.getStringFromBundle("dataverse.edit.msg"), BundleUtil.getStringFromBundle("dataverse.edit.detailmsg"));
        } else if (editMode == EditMode.FEATURED) {
            initFeaturedDataverses();
        }

    }

    public void refresh() {

    }

    private boolean openMetadataBlock;

    public boolean isOpenMetadataBlock() {
        return openMetadataBlock;
    }

    public void setOpenMetadataBlock(boolean openMetadataBlock) {
        this.openMetadataBlock = openMetadataBlock;
    }

    private boolean editInputLevel;

    public boolean isEditInputLevel() {
        return editInputLevel;
    }

    public void setEditInputLevel(boolean editInputLevel) {
        this.editInputLevel = editInputLevel;
    }

    public void showDatasetFieldTypes(Long mdbId) {
        showDatasetFieldTypes(mdbId, true);
    }

    public void showDatasetFieldTypes(Long mdbId, boolean allowEdit) {
        for (MetadataBlock mdb : allMetadataBlocks) {
            if (mdb.getId().equals(mdbId)) {
                mdb.setShowDatasetFieldTypes(true);
                openMetadataBlock = true;
            }
        }
        setEditInputLevel(allowEdit);
    }

    public void hideDatasetFieldTypes(Long mdbId) {
        for (MetadataBlock mdb : allMetadataBlocks) {
            if (mdb.getId().equals(mdbId)) {
                mdb.setShowDatasetFieldTypes(false);
                openMetadataBlock = false;
            }
        }
        setEditInputLevel(false);
    }

    public void updateInclude(Long mdbId, long dsftId) {
        List<DatasetFieldType> childDSFT = new ArrayList<>();

        for (MetadataBlock mdb : allMetadataBlocks) {
            if (mdb.getId().equals(mdbId)) {
                for (DatasetFieldType dsftTest : mdb.getDatasetFieldTypes()) {
                    if (dsftTest.getId().equals(dsftId)) {
                        dsftTest.setOptionSelectItems(resetSelectItems(dsftTest));
                        if ((dsftTest.isHasParent() && !dsftTest.getParentDatasetFieldType().isInclude()) || (!dsftTest.isHasParent() && !dsftTest.isInclude())) {
                            dsftTest.setRequiredDV(false);
                        }
                        if (dsftTest.isHasChildren()) {
                            childDSFT.addAll(dsftTest.getChildDatasetFieldTypes());
                        }
                    }
                }
            }
        }
        if (!childDSFT.isEmpty()) {
            for (DatasetFieldType dsftUpdate : childDSFT) {
                for (MetadataBlock mdb : allMetadataBlocks) {
                    if (mdb.getId().equals(mdbId)) {
                        for (DatasetFieldType dsftTest : mdb.getDatasetFieldTypes()) {
                            if (dsftTest.getId().equals(dsftUpdate.getId())) {
                                dsftTest.setOptionSelectItems(resetSelectItems(dsftTest));
                            }
                        }
                    }
                }
            }
        }
        PrimeFaces.current().executeScript("scrollAfterUpdate();");
    }

    public List<SelectItem> resetSelectItems(DatasetFieldType typeIn) {
        List<SelectItem> retList = new ArrayList<>();
        if ((typeIn.isHasParent() && typeIn.getParentDatasetFieldType().isInclude()) || (!typeIn.isHasParent() && typeIn.isInclude())) {
                SelectItem requiredItem = new SelectItem();
                requiredItem.setLabel(BundleUtil.getStringFromBundle("dataverse.item.required"));
                requiredItem.setValue(true);
                retList.add(requiredItem);
                SelectItem optional = new SelectItem();
                // When parent field is not required and child is; default level is "Conditionally Required"
                if (typeIn.isRequired() && typeIn.isHasParent() && !typeIn.getParentDatasetFieldType().isRequired()) {
                    optional.setLabel(BundleUtil.getStringFromBundle("dataverse.item.required.conditional"));
                } else {
                    optional.setLabel(BundleUtil.getStringFromBundle("dataverse.item.optional"));                    
                }
                optional.setValue(false);
                retList.add(optional);
        } else {
            SelectItem hidden = new SelectItem();
            hidden.setLabel(BundleUtil.getStringFromBundle("dataverse.item.hidden"));
            hidden.setValue(false);
            hidden.setDisabled(true);
            retList.add(hidden);
        }
        return retList;
    }

    public void updateRequiredDatasetFieldTypes(Long mdbId, Long dsftId, boolean inVal) {
        for (MetadataBlock mdb : allMetadataBlocks) {
            if (mdb.getId().equals(mdbId)) {
                for (DatasetFieldType dsft : mdb.getDatasetFieldTypes()) {
                    if (dsft.getId().equals(dsftId)) {
                        dsft.setRequiredDV(!inVal);
                    }
                }
            }
        }
    }

    public void updateOptionsRadio(Long mdbId, Long dsftId) {

        for (MetadataBlock mdb : allMetadataBlocks) {
            if (mdb.getId().equals(mdbId)) {
                for (DatasetFieldType dsft : mdb.getDatasetFieldTypes()) {
                    if (dsft.getId().equals(dsftId)) {
                        dsft.setOptionSelectItems(resetSelectItems(dsft));
                    }
                }
            }
        }
    }


    public String save() {
        List<DataverseFieldTypeInputLevel> listDFTIL = new ArrayList<>();
        if (editMode != null && ( editMode.equals(EditMode.INFO) || editMode.equals(EditMode.CREATE))) {

            List<MetadataBlock> selectedBlocks = new ArrayList<>();
            if (dataverse.isMetadataBlockRoot()) {
                dataverse.getMetadataBlocks().clear();
            }

            for (MetadataBlock mdb : this.allMetadataBlocks) {
                if (dataverse.isMetadataBlockRoot() && (mdb.isSelected() || mdb.isRequired())) {
                    selectedBlocks.add(mdb);
                    for (DatasetFieldType dsft : mdb.getDatasetFieldTypes()) {
                        if (!dsft.isChild()) {
                            // Save input level for parent field
                            saveInputLevels(listDFTIL, dsft, dataverse);
                            
                            // Handle child fields
                            if (dsft.isHasChildren()) {
                                for (DatasetFieldType child : dsft.getChildDatasetFieldTypes()) {
                                    saveInputLevels(listDFTIL, child, dataverse);
                                }
                            }
                        }
                    }
                }
            }

            if (!selectedBlocks.isEmpty()) {
                dataverse.setMetadataBlocks(selectedBlocks);
            }

            if (!dataverse.isFacetRoot()) {
                facets.getTarget().clear();
            }

        }

        Command<Dataverse> cmd = null;
        //TODO change to Create - for now the page is expecting INFO instead.
        Boolean create;
        if (dataverse.getId() == null) {
            if (session.getUser().isAuthenticated()) {
                if (dataverse.getOwner() == null || dataverse.getOwner().getId() == null) {
                    dataverse.setOwner(ownerId != null ? dataverseService.find(ownerId) : null);
                }
                create = Boolean.TRUE;
                cmd = new CreateDataverseCommand(dataverse, dvRequestService.getDataverseRequest(), facets.getTarget(), listDFTIL);
            } else {
                JH.addMessage(FacesMessage.SEVERITY_FATAL, BundleUtil.getStringFromBundle("dataverse.create.authenticatedUsersOnly"));
                return null;
            }
        } else {
            create = Boolean.FALSE;
            if (editMode != null && editMode.equals(EditMode.FEATURED)) {
                cmd = new UpdateDataverseCommand(dataverse, null, featuredDataverses.getTarget(), dvRequestService.getDataverseRequest(), null);
            } else {
                cmd = new UpdateDataverseCommand(dataverse, facets.getTarget(), null, dvRequestService.getDataverseRequest(), listDFTIL);                
            }
        }

        try {
            dataverse = commandEngine.submit(cmd);
            if (session.getUser() instanceof AuthenticatedUser) {
                if (create) {
                    userNotificationService.sendNotification((AuthenticatedUser) session.getUser(), dataverse.getCreateDate(), Type.CREATEDV, dataverse.getId());
                }
            }
        
            String message;
            if (editMode != null && editMode.equals(EditMode.FEATURED)) {
                message = BundleUtil.getStringFromBundle("dataverse.feature.update");
            } else {
                message = (create) ? BundleUtil.getStringFromBundle("dataverse.create.success", Arrays.asList(settingsWrapper.getGuidesBaseUrl(), settingsWrapper.getGuidesVersion())) : BundleUtil.getStringFromBundle("dataverse.update.success");
            }
            JsfHelper.addSuccessMessage(message);
            
            editMode = null;
            return returnRedirect();            
            

        } /*catch (CommandException ex) {
            logger.log(Level.SEVERE, "Unexpected Exception calling dataverse command", ex);
            String errMsg = create ? BundleUtil.getStringFromBundle("dataverse.create.failure") : BundleUtil.getStringFromBundle("dataverse.update.failure");
            JH.addMessage(FacesMessage.SEVERITY_FATAL, errMsg);
            return null;
        }*/ catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected Exception calling dataverse command", e);
            String errMsg = create ? BundleUtil.getStringFromBundle("dataverse.create.failure") : BundleUtil.getStringFromBundle("dataverse.update.failure");
            
            String failureMessage = e.getMessage() == null 
                        ? errMsg
                        : e.getMessage();
            JsfHelper.addErrorMessage(failureMessage);
            
            return null;
        }
    }
    
    public String cancel() {
        // reset values
        dataverse = dataverseService.find(dataverse.getId());
        ownerId = dataverse.getOwner() != null ? dataverse.getOwner().getId() : null;
        editMode = null;
        return "/dataverse.xhtml?alias=" + dataverse.getAlias() + "&faces-redirect=true";
    }

    public boolean isRootDataverse() {        
        return dataverse.getOwner() == null;
    }

    public Dataverse getOwner() {
        return (ownerId != null) ? dataverseService.find(ownerId) : null;
    }

    // METHODS for Dataverse Setup
    public boolean isInheritMetadataBlockFromParent() {
        return !dataverse.isMetadataBlockRoot();
    }

    public void setInheritMetadataBlockFromParent(boolean inheritMetadataBlockFromParent) {
        dataverse.setMetadataBlockRoot(!inheritMetadataBlockFromParent);
    }

    public void editMetadataBlocks() {
        if (!dataverse.isMetadataBlockRoot()) {
            refreshAllMetadataBlocks();
        }
    }

    public void editMetadataBlocks(boolean checkVal) {
        setInheritMetadataBlockFromParent(checkVal);
        if (!dataverse.isMetadataBlockRoot()) {
            refreshAllMetadataBlocks();
        }
    }
    
    public String resetToInherit() {

        setInheritMetadataBlockFromParent(true);
        refreshAllMetadataBlocks();
        return null;
    }

    public void cancelMetadataBlocks() {
        setInheritMetadataBlockFromParent(false);
    }

    public boolean isInheritFacetFromParent() {
        return !dataverse.isFacetRoot();
    }

    public void setInheritFacetFromParent(boolean inheritFacetFromParent) {
        dataverse.setFacetRoot(!inheritFacetFromParent);
    }

    public DualListModel<DatasetFieldType> getFacets() {
        return facets;
    }

    public void setFacets(DualListModel<DatasetFieldType> facets) {
        this.facets = facets;
    }

    public DualListModel<Dataverse> getFeaturedDataverses() {
        return featuredDataverses;
    }

    public void setFeaturedDataverses(DualListModel<Dataverse> featuredDataverses) {
        this.featuredDataverses = featuredDataverses;
    }

    public String saveLinkedDataverse() {

        if (linkingDataverseId == null) {
            JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataverse.link.select"));
            return "";
        }

        linkingDataverse = dataverseService.find(linkingDataverseId);

        LinkDataverseCommand cmd = new LinkDataverseCommand(dvRequestService.getDataverseRequest(), linkingDataverse, dataverse);
        try {
            commandEngine.submit(cmd);
        } catch (CommandException ex) {
            List<String> args = Arrays.asList(dataverse.getDisplayName(),linkingDataverse.getDisplayName());
            String msg = BundleUtil.getStringFromBundle("dataverse.link.error", args);
            logger.log(Level.SEVERE, "{0} {1}", new Object[]{msg, ex});
            JsfHelper.addErrorMessage(msg);
            return returnRedirect();
        }
        
        JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataverse.linked.success.wait", getSuccessMessageArguments()));
        return returnRedirect();        
    }
    
    private List<String> getSuccessMessageArguments() {
        List<String> arguments = new ArrayList<>();
        arguments.add(StringEscapeUtils.escapeHtml4(dataverse.getDisplayName()));
        String linkString = "<a href=\"/dataverse/" + linkingDataverse.getAlias() + "\">" + StringEscapeUtils.escapeHtml4(linkingDataverse.getDisplayName()) + "</a>";
        arguments.add(linkString);
        return arguments;
    }

    @Deprecated
    private SavedSearch createSavedOfCurrentDataverse(AuthenticatedUser savedSearchCreator) {
        /**
         * Please note that we are relying on the fact that the Solr ID of a
         * dataverse never changes, unlike datasets and files, which will change
         * from "dataset_10_draft" to "dataset_10" when published, for example.
         */
        String queryForCurrentDataverse = SearchFields.ID + ":" + IndexServiceBean.solrDocIdentifierDataverse + dataverse.getId();
        SavedSearch savedSearchToPersist = new SavedSearch(queryForCurrentDataverse, linkingDataverse, savedSearchCreator);
        SavedSearch savedSearchCreated = savedSearchService.add(savedSearchToPersist);
        return savedSearchCreated;
    }

    private SavedSearch createSavedSearchForChildren(AuthenticatedUser savedSearchCreator) {
        String wildcardQuery = "*";
        SavedSearch savedSearchToPersist = new SavedSearch(wildcardQuery, linkingDataverse, savedSearchCreator);
        String dataversePath = dataverseService.determineDataversePath(dataverse);
        String filterDownToSubtree = SearchFields.SUBTREE + ":\"" + dataversePath + "\"";
        SavedSearchFilterQuery filterDownToSubtreeFilterQuery = new SavedSearchFilterQuery(filterDownToSubtree, savedSearchToPersist);
        savedSearchToPersist.setSavedSearchFilterQueries(Arrays.asList(filterDownToSubtreeFilterQuery));
        SavedSearch savedSearchCreated = savedSearchService.add(savedSearchToPersist);
        return savedSearchCreated;
    }

     public String saveSavedSearch() {
        if (linkingDataverseId == null) {
            JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataverse.link.select"));
            return "";
        }
        linkingDataverse = dataverseService.find(linkingDataverseId);

        AuthenticatedUser savedSearchCreator = getAuthenticatedUser();
        if (savedSearchCreator == null) {
            String msg = BundleUtil.getStringFromBundle("dataverse.search.user");
            logger.severe(msg);
            JsfHelper.addErrorMessage(msg);
            return returnRedirect();
        }

        SavedSearch savedSearch = new SavedSearch(query, linkingDataverse, savedSearchCreator);
        savedSearch.setSavedSearchFilterQueries(new ArrayList<>());
        for (String filterQuery : filterQueries) {
            /**
             * @todo Why are there null's here anyway? Turn on debug and figure
             * this out.
             */
            if (filterQuery != null && !filterQuery.isEmpty()) {
                SavedSearchFilterQuery ssfq = new SavedSearchFilterQuery(filterQuery,savedSearch);
                savedSearch.getSavedSearchFilterQueries().add(ssfq);
            }
        }
        CreateSavedSearchCommand cmd = new CreateSavedSearchCommand(dvRequestService.getDataverseRequest(), linkingDataverse, savedSearch);
        try {
            commandEngine.submit(cmd);

            List<String> arguments = new ArrayList<>();           
            String linkString = "<a href=\"/dataverse/" + linkingDataverse.getAlias() + "\">" + StringEscapeUtils.escapeHtml4(linkingDataverse.getDisplayName()) + "</a>";
            arguments.add(linkString);
            String successMessageString = BundleUtil.getStringFromBundle("dataverse.saved.search.success", arguments);
            JsfHelper.addSuccessMessage(successMessageString);
            return returnRedirect();
        } catch (CommandException ex) {
            String msg = "There was a problem linking this search to yours: " + ex;
            logger.severe(msg);
            JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataverse.saved.search.failure") + " " +  ex);
            return returnRedirect();
        }
    }

    private AuthenticatedUser getAuthenticatedUser() {
        User user = session.getUser();
        if (user.isAuthenticated()) {
            return (AuthenticatedUser) user;
        } else {
            return null;
        }
    }

    public String releaseDataverse() {
        if (session.getUser() instanceof AuthenticatedUser) {
            PublishDataverseCommand cmd = new PublishDataverseCommand(dvRequestService.getDataverseRequest(), dataverse);
            try {
                commandEngine.submit(cmd);
                JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataverse.publish.success"));

            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Unexpected Exception calling  publish dataverse command", ex);
                String failureMessage = ex.getMessage() == null 
                        ? BundleUtil.getStringFromBundle("dataverse.publish.failure")
                        : ex.getMessage();
                JsfHelper.addErrorMessage(failureMessage);

            }
        } else {
            JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataverse.release.authenticatedUsersOnly"));
        }
        return returnRedirect();

    }

    public String deleteDataverse() {
        DeleteDataverseCommand cmd = new DeleteDataverseCommand(dvRequestService.getDataverseRequest(), dataverse);
        try {
            commandEngine.submit(cmd);
            JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataverse.delete.success"));
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unexpected Exception calling  delete dataverse command", ex);
            JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataverse.delete.failure"));
        }
        return "/dataverse.xhtml?alias=" + dataverse.getOwner().getAlias() + "&faces-redirect=true";
    }

    public String getMetadataBlockPreview(MetadataBlock mdb, int numberOfItems) {
        /// for beta, we will just preview the first n fields
        StringBuilder mdbPreview = new StringBuilder();
        int count = 0;
        for (DatasetFieldType dsfType : mdb.getDatasetFieldTypes()) {
            if (!dsfType.isChild()) {
                if (count != 0) {
                    mdbPreview.append(", ");
                    if (count == numberOfItems) {
                        mdbPreview.append("etc.");
                        break;
                    }
                }

                mdbPreview.append(dsfType.getDisplayName());
                count++;
            }
        }

        return mdbPreview.toString();
    }

    public Boolean isEmptyDataverse() {
        return !dataverseService.hasData(dataverse);
    }
    private List<MetadataBlock> allMetadataBlocks;

    public List<MetadataBlock> getAllMetadataBlocks() {
        return this.allMetadataBlocks;
    }

    public void setAllMetadataBlocks(List<MetadataBlock> inBlocks) {
        this.allMetadataBlocks = inBlocks;
    }

    private void refreshAllMetadataBlocks() {
        Long dataverseIdForInputLevel = dataverse.getId();
        List<MetadataBlock> retList = new ArrayList<>();

        List<MetadataBlock> availableBlocks = new ArrayList<>();
        //Add System level blocks
        availableBlocks.addAll(dataverseService.findSystemMetadataBlocks());

        Dataverse testDV = dataverse;
        //Add blocks associated with DV
        availableBlocks.addAll(dataverseService.findMetadataBlocksByDataverseId(dataverse.getId()));

        //Add blocks associated with dv going up inheritance tree
        while (testDV.getOwner() != null) {
            availableBlocks.addAll(dataverseService.findMetadataBlocksByDataverseId(testDV.getOwner().getId()));
            testDV = testDV.getOwner();
        }

        for (MetadataBlock mdb : availableBlocks) {
            mdb.setSelected(false);
            mdb.setShowDatasetFieldTypes(false);
            if (!dataverse.isMetadataBlockRoot() && dataverse.getOwner() != null) {
                dataverseIdForInputLevel = dataverse.getMetadataRootId();
                for (MetadataBlock mdbTest : dataverse.getOwner().getMetadataBlocks()) {
                    if (mdb.equals(mdbTest)) {
                        mdb.setSelected(true);
                    }
                }
            } else {
                for (MetadataBlock mdbTest : dataverse.getMetadataBlocks(true)) {
                    if (mdb.equals(mdbTest)) {
                        mdb.setSelected(true);
                    }
                }
            }

            for (DatasetFieldType dsft : mdb.getDatasetFieldTypes()) {
                if (!dsft.isChild()) {
                    loadInputLevels(dsft, dataverseIdForInputLevel);
                    dsft.setOptionSelectItems(resetSelectItems(dsft));
                    if (dsft.isHasChildren()) {
                        for (DatasetFieldType child : dsft.getChildDatasetFieldTypes()) {
                            loadInputLevels(child, dataverseIdForInputLevel);
                            child.setOptionSelectItems(resetSelectItems(child));
                        }
                    }
                }
            }            
            retList.add(mdb);
        }
        setAllMetadataBlocks(retList);
    }

    private void loadInputLevels(DatasetFieldType dsft, Long dataverseIdForInputLevel) {
        DataverseFieldTypeInputLevel dsfIl = dataverseFieldTypeInputLevelService
            .findByDataverseIdDatasetFieldTypeId(dataverseIdForInputLevel, dsft.getId());
        
        if (dsfIl != null) {
            dsft.setRequiredDV(dsfIl.isRequired());
            dsft.setInclude(dsfIl.isInclude());
            dsft.setLocalDisplayOnCreate(dsfIl.getDisplayOnCreate());
        } else {
            // If there is no input level, use the default values
            dsft.setRequiredDV(dsft.isRequired());
            dsft.setInclude(true);
        }
    }

    public void validateAlias(FacesContext context, UIComponent toValidate, Object value) {
        if (!StringUtils.isEmpty((String) value)) {
            String alias = (String) value;

            boolean aliasFound = false;
            Dataverse dv = dataverseService.findByAlias(alias);
            if (editMode == DataversePage.EditMode.CREATE) {
                if (dv != null) {
                    aliasFound = true;
                }
            } else {
                if (dv != null && !dv.getId().equals(dataverse.getId())) {
                    aliasFound = true;
                }
            }
            if (aliasFound) {
                ((UIInput) toValidate).setValid(false);
                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("dataverse.alias"), BundleUtil.getStringFromBundle("dataverse.alias.taken"));
                context.addMessage(toValidate.getClientId(context), message);
            }
        }
    }
    
    private String returnRedirect(){
        return "/dataverse.xhtml?alias=" + dataverse.getAlias() + "&faces-redirect=true";  
    }
    
    private Map<String, Integer> numberOfFacets = new HashMap<>();
    
    public int getNumberOfFacets(String name, int defaultValue) {
        Integer numFacets = numberOfFacets.get(name);
        if (numFacets == null) {
            numberOfFacets.put(name, defaultValue);
            numFacets = defaultValue;
        }
        return numFacets;
    }
    
    public void incrementFacets(String name, int incrementNum) {
        Integer numFacets = numberOfFacets.get(name);
        if (numFacets == null) {
            numFacets = incrementNum;
        }
        numberOfFacets.put(name, numFacets + incrementNum);
    }
    
    private String query;
    private List<String> filterQueries = new ArrayList<>();
    private List<FacetCategory> facetCategoryList = new ArrayList<>();
    private String selectedTypesString;
    private String sortField;
    private SearchIncludeFragment.SortOrder sortOrder;
    private String searchFieldType = SearchFields.TYPE;
    private String searchFieldSubtree = SearchFields.SUBTREE;
    
    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<String> getFilterQueries() {
        return filterQueries;
    }

    public void setFilterQueries(List<String> filterQueries) {
        this.filterQueries = filterQueries;
    }

    public List<FacetCategory> getFacetCategoryList() {
        return facetCategoryList;
    }

    public void setFacetCategoryList(List<FacetCategory> facetCategoryList) {
        this.facetCategoryList = facetCategoryList;
    }
    
    private int searchResultsCount = 0;
    
    public int getSearchResultsCount() {
        return searchResultsCount;
    }

    public void setSearchResultsCount(int searchResultsCount) {
        this.searchResultsCount = searchResultsCount;
    }

    public String getSelectedTypesString() {
        return selectedTypesString;
    }

    public void setSelectedTypesString(String selectedTypesString) {
        this.selectedTypesString = selectedTypesString;
    }
    
    public String getSortField() {
        return sortField;
    }

    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    public String getSortOrder() {
        if (sortOrder != null) {
            return sortOrder.toString();
        } else {
            return null;
        }
    }

    /**
     * Allow only valid values to be set.
     *
     * Rather than passing in a String and converting it to an enum in this
     * method we could write a converter:
     * http://stackoverflow.com/questions/8609378/jsf-2-0-view-parameters-to-pass-objects
     */
    public void setSortOrder(String sortOrderSupplied) {
        if (sortOrderSupplied != null) {
            if (sortOrderSupplied.equals(SearchIncludeFragment.SortOrder.asc.toString())) {
                this.sortOrder = SearchIncludeFragment.SortOrder.asc;
            }
            if (sortOrderSupplied.equals(SearchIncludeFragment.SortOrder.desc.toString())) {
                this.sortOrder = SearchIncludeFragment.SortOrder.desc;
            }
        }
    }
    
    public String getSearchFieldType() {
        return searchFieldType;
    }

    public void setSearchFieldType(String searchFieldType) {
        this.searchFieldType = searchFieldType;
    }

    public String getSearchFieldSubtree() {
        return searchFieldSubtree;
    }

    public void setSearchFieldSubtree(String searchFieldSubtree) {
        this.searchFieldSubtree = searchFieldSubtree;
    }
    
    public List<Dataverse> completeHostDataverseMenuList(String query) {
        if (session.getUser().isAuthenticated()) {
            return dataverseService.filterDataversesForHosting(query, dvRequestService.getDataverseRequest());
        } else {
            return null;
        }
    }
    
    public Set<Entry<String, String>> getStorageDriverOptions() {
    	HashMap<String, String> drivers =new HashMap<String, String>();
    	drivers.putAll(DataAccess.getStorageDriverLabels());
    	//Add an entry for the default (inherited from an ancestor or the system default)
    	drivers.put(getDefaultStorageDriverLabel(), DataAccess.UNDEFINED_STORAGE_DRIVER_IDENTIFIER);
    	return drivers.entrySet();
    }
    
    public String getDefaultStorageDriverLabel() {
    	String storageDriverId = DataAccess.DEFAULT_STORAGE_DRIVER_IDENTIFIER;
    	Dataverse parent = dataverse.getOwner();
    	boolean fromAncestor=false;
    	if(parent != null) {
    		storageDriverId = parent.getEffectiveStorageDriverId();
    		//recurse dataverse chain to root and if any have a storagedriver set, fromAncestor is true
    	    while(parent!=null) {
    	    	if(!parent.getStorageDriverId().equals(DataAccess.UNDEFINED_STORAGE_DRIVER_IDENTIFIER)) {
    	    		fromAncestor=true;
    	    		break;
    	    	}
    	    	parent=parent.getOwner();
    	    }
    	}
   		String label = DataAccess.getStorageDriverLabelFor(storageDriverId);
   		if(fromAncestor) {
   			label = label + " " + BundleUtil.getStringFromBundle("dataverse.inherited");
   		} else {
   			label = label + " " + BundleUtil.getStringFromBundle("dataverse.default");
   		}
   		return label;
    }
    
    public Set<Entry<String, String>> getMetadataLanguages() {
        return settingsWrapper.getMetadataLanguages(this.dataverse).entrySet();
    }
    
    private Set<Entry<String, String>> curationLabelSetOptions = null; 
    
    public Set<Entry<String, String>> getCurationLabelSetOptions() {
        if (curationLabelSetOptions == null) {
            HashMap<String, String> setNames = new HashMap<String, String>();
            Set<String> allowedSetNames = systemConfig.getCurationLabels().keySet();
            if (allowedSetNames.size() > 0) {
                // Add an entry for the default (inherited from an ancestor or the system
                // default)
                String inheritedLabelSet = getCurationLabelSetNameLabel();
                if (!StringUtils.isBlank(inheritedLabelSet)) {
                    setNames.put(inheritedLabelSet, SystemConfig.DEFAULTCURATIONLABELSET);
                }
                // Add an entry for disabled
                setNames.put(BundleUtil.getStringFromBundle("dataverse.curationLabels.disabled"), SystemConfig.CURATIONLABELSDISABLED);

                allowedSetNames.forEach(name -> {
                    String localizedName = DatasetUtil.getLocaleCurationStatusLabelFromString(name) ;
                    setNames.put(localizedName,name);
                });
            }
            curationLabelSetOptions = setNames.entrySet();
        }
        return curationLabelSetOptions;
    }

    public String getCurationLabelSetNameLabel() {
        Dataverse parent = dataverse.getOwner();
        String setName = null;
        boolean fromAncestor = false;
        if (parent != null) {
            setName = parent.getEffectiveCurationLabelSetName();
            // recurse dataverse chain to root and if any have a curation label set name set,
            // fromAncestor is true
            while (parent != null) {
                if (!parent.getCurationLabelSetName().equals(SystemConfig.DEFAULTCURATIONLABELSET)) {
                    fromAncestor = true;
                    break;
                }
                parent = parent.getOwner();
            }
        }
        if (setName != null) {
            if (fromAncestor) {
                setName = setName + " " + BundleUtil.getStringFromBundle("dataverse.inherited");
            } else {
                setName = setName + " " + BundleUtil.getStringFromBundle("dataverse.default");
            }
        }
        return setName;
    }

    public Set<Entry<String, String>> getGuestbookEntryOptions() {
        return settingsWrapper.getGuestbookEntryOptions(this.dataverse).entrySet();
    }

    public Set<Entry<String, String>> getPidProviderOptions() {
        PidProvider defaultPidProvider = pidProviderFactoryBean.getDefaultPidGenerator();
        Set<String> providerIds = PidUtil.getManagedProviderIds();
        Set<Entry<String, String>> options = new HashSet<Entry<String, String>>();
        if (providerIds.size() > 1) {

            String label = null;
            if (this.dataverse.getOwner() != null && this.dataverse.getOwner().getEffectivePidGenerator()!= null) {
                PidProvider inheritedPidProvider = this.dataverse.getOwner().getEffectivePidGenerator();
                label = inheritedPidProvider.getLabel() + " " + BundleUtil.getStringFromBundle("dataverse.inherited") + ": "
                        + inheritedPidProvider.getProtocol() + ":" + inheritedPidProvider.getAuthority()
                        + inheritedPidProvider.getSeparator() + inheritedPidProvider.getShoulder();
            } else {
                label = defaultPidProvider.getLabel() +  " " + BundleUtil.getStringFromBundle("dataverse.default") + ": "
                        + defaultPidProvider.getProtocol() + ":" + defaultPidProvider.getAuthority()
                        + defaultPidProvider.getSeparator() + defaultPidProvider.getShoulder();
            }
            Entry<String, String> option = new AbstractMap.SimpleEntry<String, String>("default", label);
            options.add(option);
        }
        for (String providerId : providerIds) {
            PidProvider pidProvider = PidUtil.getPidProvider(providerId);
            String label = pidProvider.getLabel() + ": " + pidProvider.getProtocol() + ":" + pidProvider.getAuthority()
                    + pidProvider.getSeparator() + pidProvider.getShoulder();
            Entry<String, String> option = new AbstractMap.SimpleEntry<String, String>(providerId, label);
            options.add(option);
        }
        return options;
    }

    public void updateDisplayOnCreate(Long mdbId, Long dsftId, boolean currentValue) {
        for (MetadataBlock mdb : allMetadataBlocks) {
            if (mdb.getId().equals(mdbId)) {
                for (DatasetFieldType dsft : mdb.getDatasetFieldTypes()) {
                    if (dsft.getId().equals(dsftId)) {
                        // Update value in memory
                        dsft.setLocalDisplayOnCreate(!currentValue);
                        
                        // Update or create input level
                        DataverseFieldTypeInputLevel existingLevel = dataverseFieldTypeInputLevelService
                            .findByDataverseIdDatasetFieldTypeId(dataverse.getId(), dsftId);
                        
                        if (existingLevel != null) {
                            existingLevel.setDisplayOnCreate(!currentValue);
                            dataverseFieldTypeInputLevelService.save(existingLevel);
                        } else {
                            DataverseFieldTypeInputLevel newLevel = new DataverseFieldTypeInputLevel(
                                dsft, 
                                dataverse, 
                                dsft.isRequiredDV(),
                                true,  // default include
                                !currentValue  // new value of displayOnCreate
                            );
                            dataverseFieldTypeInputLevelService.save(newLevel);
                        }
                    }
                }
            }
        }
    }

    private void saveInputLevels(List<DataverseFieldTypeInputLevel> listDFTIL, DatasetFieldType dsft, Dataverse dataverse) {
        // If the field already has an input level, update it
        DataverseFieldTypeInputLevel existingLevel = dataverseFieldTypeInputLevelService
            .findByDataverseIdDatasetFieldTypeId(dataverse.getId(), dsft.getId());
        
        if (existingLevel != null) {
            existingLevel.setDisplayOnCreate(dsft.getLocalDisplayOnCreate());
            existingLevel.setInclude(dsft.isInclude());
            existingLevel.setRequired(dsft.isRequiredDV());
            listDFTIL.add(existingLevel);
        } else if (dsft.isInclude() || (dsft.getLocalDisplayOnCreate()!=null) || dsft.isRequiredDV()) {
            // Only create new input level if there is any specific configuration
            listDFTIL.add(new DataverseFieldTypeInputLevel(
                dsft, 
                dataverse, 
                dsft.isRequiredDV(), 
                dsft.isInclude(), 
                dsft.getLocalDisplayOnCreate()
            ));
        }
    }
}
