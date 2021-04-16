/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateHarvestingClientCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateHarvestingClientCommand;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClient;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClientServiceBean;
import edu.harvard.iq.dataverse.harvest.server.OAIRecord;
import edu.harvard.iq.dataverse.harvest.server.OAIRecordServiceBean;
import edu.harvard.iq.dataverse.harvest.server.OAISet;
import edu.harvard.iq.dataverse.harvest.server.OAISetServiceBean;
import edu.harvard.iq.dataverse.harvest.server.OaiSetException;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import edu.harvard.iq.dataverse.util.SystemConfig;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author Leonid Andreev
 */
@ViewScoped
@Named
public class HarvestingSetsPage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(HarvestingSetsPage.class.getCanonicalName());

    @Inject
    DataverseSession session;
    @EJB
    AuthenticationServiceBean authSvc;
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    OAISetServiceBean oaiSetService;
    @EJB
    OAIRecordServiceBean oaiRecordService;
            
    @EJB
    EjbDataverseEngine engineService;
    @EJB
    SystemConfig systemConfig; 
    
    @Inject
    DataverseRequestServiceBean dvRequestService;
    @Inject
    NavigationWrapper navigationWrapper;
 
    private List<OAISet> configuredHarvestingSets;
    private OAISet selectedSet;
    private boolean setSpecValidated = false;
    private boolean setQueryValidated = false;
    private int setQueryResult = -1;
    
    public enum PageMode {

        VIEW, CREATE, EDIT
    }  
    private PageMode pageMode = PageMode.VIEW; 
    
    private int oaiServerStatusRadio; 
    
    private static final int oaiServerStatusRadioDisabled = 0;
    private static final int oaiServerStatusRadioEnabled = 1;
    private UIInput newSetSpecInputField;
    private UIInput newSetQueryInputField;

    private String newSetSpec = "";
    private String newSetDescription = "";
    private String newSetQuery = "";
    
    public String getNewSetSpec() {
        return newSetSpec;
    }
    
    public void setNewSetSpec(String newSetSpec) {
        this.newSetSpec = newSetSpec;
    }
    
    public String getNewSetDescription() {
        return newSetDescription;
    }
    
    public void setNewSetDescription(String newSetDescription) {
        this.newSetDescription = newSetDescription;
    }
    
    public String getNewSetQuery() {
        return newSetQuery;
    }
    
    public void setNewSetQuery(String newSetQuery) {
        this.newSetQuery = newSetQuery;
    }
     
    public int getOaiServerStatusRadio() {
        return this.oaiServerStatusRadio;
    }
    
    public void setOaiServerStatusRadio(int oaiServerStatusRadio) {
        this.oaiServerStatusRadio = oaiServerStatusRadio;
    }
    
    public String init() {
        if (!isSessionUserAuthenticated()) {
            return "/loginpage.xhtml" + navigationWrapper.getRedirectPage();
        } else if (!isSuperUser()) {
            return navigationWrapper.notAuthorized();
        }
        
        configuredHarvestingSets = oaiSetService.findAll();
        pageMode = PageMode.VIEW;
        
        if (isHarvestingServerEnabled()) {
            oaiServerStatusRadio = oaiServerStatusRadioEnabled;
            checkIfDefaultSetExists();
        } else {
            oaiServerStatusRadio = oaiServerStatusRadioDisabled;
        }
                
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, BundleUtil.getStringFromBundle("harvestserver.title"), BundleUtil.getStringFromBundle("harvestserver.toptip")));
        return null; 
    }
    
    private void checkIfDefaultSetExists() {
        OAISet defaultSet = oaiSetService.findDefaultSet();
        if (defaultSet == null) {
            createDefaultSet();
        }
    }
    
    public List<OAISet> getConfiguredOAISets() {
        return configuredHarvestingSets; 
    }
    
    public void setConfiguredOAISets(List<OAISet> oaiSets) {
        configuredHarvestingSets = oaiSets; 
    }
    
    public boolean isHasNamedOAISets() {
        List<OAISet> namedSets = oaiSetService.findAllNamedSets();
        return namedSets != null && namedSets.size() > 0;
    }
    
    public boolean isHarvestingServerEnabled() {
        return systemConfig.isOAIServerEnabled();
    }
    
    public void toggleHarvestingServerStatus() {
        if (isHarvestingServerEnabled()) {
            systemConfig.disableOAIServer();
        } else {
            systemConfig.enableOAIServer();
            JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("harvestserver.service.enable.success"));
            checkIfDefaultSetExists();
        }
    }
    
    public UIInput getNewSetSpecInputField() {
        return newSetSpecInputField;
    }

    public void setNewSetSpecInputField(UIInput newSetSpecInputField) {
        this.newSetSpecInputField = newSetSpecInputField;
    }
    
    public UIInput getNewSetQueryInputField() {
        return newSetQueryInputField;
    }

    public void setNewSetQueryInputField(UIInput newSetQueryInputField) {
        this.newSetQueryInputField = newSetQueryInputField;
    }
    
    public void disableHarvestingServer() {
        systemConfig.disableOAIServer();
    }
    
    public void setSelectedSet(OAISet oaiSet) {
        selectedSet = oaiSet; 
    }
    
    public OAISet getSelectedSet() {
        return selectedSet; 
    }
    
    // init method when the user clicks 'add new set':
    public void initNewSet(ActionEvent ae) {
        
        this.newSetSpec = "";
        this.newSetDescription = "";
        this.newSetQuery = "";
        
        this.pageMode = PageMode.CREATE;
        this.setSpecValidated = false;
        this.setQueryValidated = false;
        this.setQueryResult = -1;
        
    }
    
    // init method when the user clicks 'edit existing set':
    public void editSet(OAISet oaiSet) {
        this.newSetSpec = oaiSet.getSpec();
        this.newSetDescription = oaiSet.getDescription();
        this.newSetQuery = oaiSet.getDefinition();
        
        this.pageMode = PageMode.EDIT;
        this.setSpecValidated = false;
        this.setQueryValidated = false;
        this.setQueryResult = -1;
        
        if (oaiSet.isDefaultSet()) {
            setSetQueryValidated(true);
        }
        
        setSelectedSet(oaiSet);
    }
    
    public void createSet(ActionEvent ae) {
        
        OAISet newOaiSet = new OAISet();
        
        
        newOaiSet.setSpec(getNewSetSpec());
        newOaiSet.setName(getNewSetSpec());
        newOaiSet.setDescription(getNewSetDescription());
        newOaiSet.setDefinition(getNewSetQuery());
        
        boolean success = false;
        
        try {
            oaiSetService.save(newOaiSet);
            configuredHarvestingSets = oaiSetService.findAll();  
            String successMessage = BundleUtil.getStringFromBundle("harvestserver.newSetDialog.success");
            successMessage = successMessage.replace("{0}", newOaiSet.getSpec());
            JsfHelper.addSuccessMessage(successMessage);
            success = true;

        } catch (Exception ex) {
            JH.addMessage(FacesMessage.SEVERITY_FATAL, BundleUtil.getStringFromBundle("harvest.oaicreate.fail"));
             logger.log(Level.SEVERE, "Failed to create OAI set" + ex.getMessage(), ex);
        }
        
        if (success) {
            OAISet savedSet = oaiSetService.findBySpec(getNewSetSpec());
            if (savedSet != null) {
                runSetExport(savedSet);
                configuredHarvestingSets = oaiSetService.findAll(); 
            }
        }
        
        setPageMode(HarvestingSetsPage.PageMode.VIEW);        
    }
    
    // this saves an existing set that the user has edited: 
    
    public void saveSet(ActionEvent ae) {
        
        OAISet oaiSet = getSelectedSet(); 
        
        if (oaiSet == null) {
            // TODO: 
            // tell the user somehow that the set cannot be saved, and advise
            // them to save the settings they have entered. 
        }
        
        // Note that the nickname is not editable:
        oaiSet.setDefinition(getNewSetQuery());
        oaiSet.setDescription(getNewSetDescription());
        
        // will try to save it now:
        boolean success = false; 
        
        try {
            oaiSetService.save(oaiSet);
            configuredHarvestingSets = oaiSetService.findAll(); 
            JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("harvest.oaiupdate.success",   Arrays.asList(oaiSet.isDefaultSet() ? "default" : oaiSet.getSpec())));
            success = true;

        } catch (Exception ex) {
            JH.addMessage(FacesMessage.SEVERITY_FATAL, BundleUtil.getStringFromBundle("harvest.oaiupdate.fail"));
             logger.log(Level.SEVERE, "Failed to update OAI set." + ex.getMessage(), ex);
        }
        
        if (success) {
            OAISet createdSet = oaiSetService.findBySpec(getNewSetSpec());
            if (createdSet != null) {
                runSetExport(createdSet);
                configuredHarvestingSets = oaiSetService.findAll(); 
            }
        }
        
        setPageMode(HarvestingSetsPage.PageMode.VIEW);

        
    }
    
    public void deleteSet() {
        if (selectedSet != null) {
            logger.info("proceeding to delete harvesting set "+ selectedSet.getSpec());
            try {
                oaiSetService.setDeleteInProgress(selectedSet.getId());
                oaiSetService.remove(selectedSet.getId());
                selectedSet = null; 

                configuredHarvestingSets = oaiSetService.findAll();
                JsfHelper.addInfoMessage(BundleUtil.getStringFromBundle("harvestserver.tab.header.action.delete.infomessage"));
            } catch (Exception ex) {
                String failMessage = BundleUtil.getStringFromBundle("harvest.delete.fail")+ex.getMessage();
                JH.addMessage(FacesMessage.SEVERITY_FATAL, failMessage);
            }
        } else {
            logger.warning("Delete called, with a null selected harvesting set!");
        }
        
    }
    
    private void createDefaultSet() {
        
        OAISet newOaiSet = new OAISet();
        
        
        newOaiSet.setSpec("");
        newOaiSet.setName("");
        // The default description of the default set. The admin will be  
        // able to modify it later, if necessary.
        newOaiSet.setDescription(BundleUtil.getStringFromBundle("harvestserver.newSetDialog.setdescription.default"));
        newOaiSet.setDefinition("");
        
        boolean success = false;
        
        try {
            oaiSetService.save(newOaiSet);
            configuredHarvestingSets = oaiSetService.findAll();  
            success = true;

        } catch (Exception ex) {
            // should be a warning perhaps??
            JH.addMessage(FacesMessage.SEVERITY_FATAL, BundleUtil.getStringFromBundle("harvest.oaicreate.defaultset.fail"));
            logger.log(Level.SEVERE, "Failed to create the Default OAI set" + ex.getMessage(), ex);
        }
        
        if (success) {
            OAISet savedSet = oaiSetService.findBySpec(getNewSetSpec());
            if (savedSet != null) {
                runSetExport(savedSet);
                configuredHarvestingSets = oaiSetService.findAll(); 
            }
        }
        
        setPageMode(HarvestingSetsPage.PageMode.VIEW);        
    }
    
    public boolean isSetSpecValidated() {
        return this.setSpecValidated;
    }
    
    public void setSetSpecValidated(boolean validated) {
        this.setSpecValidated = validated;
    }
    
    public boolean isSetQueryValidated() {
        return this.setQueryValidated;
    }
    
    public void setSetQueryValidated(boolean validated) {
        this.setQueryValidated = validated;
    }
    
    public int getSetQueryResult() {
        return this.setQueryResult;
    }
    
    public void setSetQueryResult(int setQueryResult) {
        this.setQueryResult = setQueryResult;
    }
    
    public PageMode getPageMode() {
        return this.pageMode;
    } 
    
    public void setPageMode(PageMode pageMode) {
        this.pageMode = pageMode;
    }
    
    public boolean isCreateMode() {
        return PageMode.CREATE == this.pageMode;
    }
    
    public boolean isEditMode() {
        return PageMode.EDIT == this.pageMode;
    }
    
    public boolean isViewMode() {
        return PageMode.VIEW == this.pageMode;
    }
    
    public boolean isSessionUserAuthenticated() {
        
        if (session == null) {
            return false;
        }
        
        if (session.getUser() == null) {
            return false;
        }
        
        if (session.getUser().isAuthenticated()) {
            return true;
        }
        
        return false;
    }
    
    public int getSetInfoNumOfDatasets(OAISet oaiSet) {
        if (oaiSet.isDefaultSet()) {
            return getSetInfoNumOfExported(oaiSet);
        }
        
        String query = oaiSet.getDefinition();
        
        try {
            int num = oaiSetService.validateDefinitionQuery(query);
            if (num > -1) {
                return num;
            }
        } catch (OaiSetException ose) {
            // do notghin - will return zero.
        }
        return 0;
    }
    
    public int getSetInfoNumOfExported(OAISet oaiSet) {
        List<OAIRecord> records = oaiRecordService.findActiveOaiRecordsBySetName(oaiSet.getSpec());
        
        if (records == null || records.isEmpty()) {
            return 0; 
        }
        
        return records.size();
        
    }
    
    public int getSetInfoNumOfDeleted(OAISet oaiSet) {
        List<OAIRecord> records = oaiRecordService.findDeletedOaiRecordsBySetName(oaiSet.getSpec());
        
        if (records == null || records.isEmpty()) {
            return 0; 
        }
        
        return records.size();
        
    }
    
    public void validateSetQuery() {
        int datasetsFound = 0;
        try {
            datasetsFound = oaiSetService.validateDefinitionQuery(getNewSetQuery());
        } catch (OaiSetException ose) {
            FacesContext.getCurrentInstance().addMessage(getNewSetQueryInputField().getClientId(),
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "", BundleUtil.getStringFromBundle("harvest.search.failed")+ose.getMessage()));
            setSetQueryValidated(false);
            return;
        }
        
        setSetQueryValidated(true);
        setSetQueryResult(datasetsFound);
        
    }
    
    public void backToQuery() {
        setSetQueryValidated(false);
    }
    
    /* 
     
        version of validateSetSpec() that's not component-driven (must be called explicitly 
        with action="#{harvestingSetsPage.validateSetSpec}")
    
    
    public void validateSetSpec() {

        if ( !StringUtils.isEmpty(getNewSetSpec()) ) {

            if (! Pattern.matches("^[a-zA-Z0-9\\_\\-]+$", getNewSetSpec()) ) {
                //input.setValid(false);
                FacesContext.getCurrentInstance().addMessage(getNewSetSpecInputField().getClientId(),
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "", BundleUtil.getStringFromBundle("harvestserver.newSetDialog.setspec.invalid")));
                setSetSpecValidated(false);
                return;

                // If it passes the regex test, check 
            } else if ( oaiSetService.findBySpec(getNewSetSpec()) != null ) {
                //input.setValid(false);
                FacesContext.getCurrentInstance().addMessage(getNewSetSpecInputField().getClientId(),
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "", BundleUtil.getStringFromBundle("harvestserver.newSetDialog.setspec.alreadyused")));
                setSetSpecValidated(false);
                return;
            }
            setSetSpecValidated(true);
            return;
        } 
        
        // Nickname field is empty:
        FacesContext.getCurrentInstance().addMessage(getNewSetSpecInputField().getClientId(),
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "", BundleUtil.getStringFromBundle("harvestserver.newSetDialog.setspec.required")));
        setSetSpecValidated(false);
        return;
    }*/
    
    public void validateSetSpec(FacesContext context, UIComponent toValidate, Object rawValue) {
        String value = (String) rawValue;
        UIInput input = (UIInput) toValidate;
        input.setValid(true); // Optimistic approach

        if (context.getExternalContext().getRequestParameterMap().get("DO_VALIDATION") != null) {
            
            if (!StringUtils.isEmpty(value)) {
                if (value.length() > 30){
                    input.setValid(false);
                    context.addMessage(toValidate.getClientId(),
                            new FacesMessage(FacesMessage.SEVERITY_ERROR, "", BundleUtil.getStringFromBundle("harvestserver.newSetDialog.setspec.sizelimit")));
                    return;
                    
                }
                if (!Pattern.matches("^[a-zA-Z0-9\\_\\-]+$", value)) {
                    input.setValid(false);
                    context.addMessage(toValidate.getClientId(),
                            new FacesMessage(FacesMessage.SEVERITY_ERROR, "", BundleUtil.getStringFromBundle("harvestserver.newSetDialog.setspec.invalid")));
                    return;

                    // If it passes the regex test, check 
                } else if (oaiSetService.findBySpec(value) != null) {
                    input.setValid(false);
                    context.addMessage(toValidate.getClientId(),
                            new FacesMessage(FacesMessage.SEVERITY_ERROR, "", BundleUtil.getStringFromBundle("harvestserver.newSetDialog.setspec.alreadyused")));
                    return;
                }

                // set spec looks legit!
                return;
            }
            
            // the field can't be left empty either: 
            input.setValid(false);
            context.addMessage(toValidate.getClientId(),
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "", BundleUtil.getStringFromBundle("harvestserver.newSetDialog.setspec.required")));

        }
        
        // no validation requested - so we're cool!
    }
    
    // this will re-export the set in the background, asynchronously:
    public void startSetExport(OAISet oaiSet) {
        try {          
            runSetExport(oaiSet);
        } catch (Exception ex) {
            String failMessage = BundleUtil.getStringFromBundle("harvest.reexport.fail");
            JH.addMessage(FacesMessage.SEVERITY_FATAL, failMessage);
            return;
        } 
                
        String successMessage = BundleUtil.getStringFromBundle("harvestserver.actions.runreexport.success");
        successMessage = successMessage.replace("{0}", oaiSet.getSpec());
        JsfHelper.addSuccessMessage(successMessage);
        configuredHarvestingSets = oaiSetService.findAll(); 
    }
    
    public void runSetExport(OAISet oaiSet) {
        oaiSetService.setUpdateInProgress(oaiSet.getId());
        oaiSetService.exportOaiSetAsync(oaiSet);
    }
    
    public boolean isSuperUser() {
        return session.getUser().isSuperuser();
    }
}