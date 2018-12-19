/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateHarvestingClientCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteHarvestingClientCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateHarvestingClientCommand;
import edu.harvard.iq.dataverse.harvest.client.HarvesterServiceBean;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClient;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClientServiceBean;
import edu.harvard.iq.dataverse.harvest.client.oai.OaiHandler;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.timer.DataverseTimerServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import java.util.ArrayList;
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
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author Leonid Andreev
 */
@ViewScoped
@Named
public class HarvestingClientsPage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(HarvestingClientsPage.class.getCanonicalName());

    @Inject
    DataverseSession session;
    @EJB
    AuthenticationServiceBean authSvc;
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    HarvestingClientServiceBean harvestingClientService; 
    @EJB
    HarvesterServiceBean harvesterService;
    @EJB
    DatasetServiceBean datasetService;
    @EJB
    IndexServiceBean indexService;
    @EJB
    EjbDataverseEngine engineService;
    @EJB
    DataverseTimerServiceBean dataverseTimerService;
    @Inject
    DataverseRequestServiceBean dvRequestService;
    @Inject
    NavigationWrapper navigationWrapper;
 
    private List<HarvestingClient> configuredHarvestingClients;
    private Dataverse dataverse;
    private Long dataverseId = null;
    private HarvestingClient selectedClient;
    
    //private static final String solrDocIdentifierDataset = "dataset_";
    
    public enum PageMode {

        VIEW, CREATE, EDIT, DELETE
    }  
    private PageMode pageMode = PageMode.VIEW; 
    
    public enum CreateStep {
        ONE, TWO, THREE, FOUR
    }
    
    private CreateStep createStep = CreateStep.ONE;
    
    private Dataverse selectedDestinationDataverse;

    public void setSelectedDestinationDataverse(Dataverse dv) {
        this.selectedDestinationDataverse = dv;
    }

    public Dataverse getSelectedDestinationDataverse() {
        return this.selectedDestinationDataverse;
    }
    
    public List<Dataverse> completeSelectedDataverse(String query) {
        return dataverseService.filterByAliasQuery(query);
    }
    
    public String init() {
        if (!isSessionUserAuthenticated()) {
            return "/loginpage.xhtml" + navigationWrapper.getRedirectPage();
        } else if (!isSuperUser()) {
            return navigationWrapper.notAuthorized(); 
        }
        
        if (dataverseId != null) {
            setDataverse(dataverseService.find(getDataverseId()));
            if (getDataverse() == null) {
                return navigationWrapper.notFound();
            }
        } else {
            setDataverse(dataverseService.findRootDataverse());
        }
        
        configuredHarvestingClients = harvestingClientService.getAllHarvestingClients();
        
        pageMode = PageMode.VIEW;
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, BundleUtil.getStringFromBundle("harvestclients.title"), BundleUtil.getStringFromBundle("harvestclients.toptip")));
        return null; 
    }
    
    public List<HarvestingClient> getConfiguredHarvestingClients() {
        return configuredHarvestingClients; 
    }
    
    public void setConfiguredHarvestingClients(List<HarvestingClient> configuredClients) {
        configuredHarvestingClients = configuredClients; 
    }
    
    public Dataverse getDataverse() {
        return dataverse;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    public Long getDataverseId() {
        return dataverseId;
    }

    public void setDataverseId(Long dataverseId) {
        this.dataverseId = dataverseId;
    }
    
    public void setSelectedClient(HarvestingClient harvestingClient) {
        selectedClient = harvestingClient; 
    }
    
    public void setClientForDelete(HarvestingClient harvestingClient) {
        selectedClient = harvestingClient;
        this.pageMode = PageMode.DELETE;
    }
    
    public HarvestingClient getSelectedClient() {
        return selectedClient; 
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
    
    public boolean isDeleteMode() {
        return PageMode.DELETE == this.pageMode;
    }
    
    public boolean isCreateStepOne() {
        return CreateStep.ONE == this.createStep;
    }
    
    public boolean isCreateStepTwo() {
        return CreateStep.TWO == this.createStep;
    }
    
    public boolean isCreateStepThree() {
        return CreateStep.THREE == this.createStep;
    }
    
    public boolean isCreateStepFour() {
        return CreateStep.FOUR == this.createStep;
    }
    
    
    public void runHarvest(HarvestingClient harvestingClient) {
        try {
            DataverseRequest dataverseRequest = new DataverseRequest(session.getUser(), (HttpServletRequest)null);
            harvesterService.doAsyncHarvest(dataverseRequest, harvestingClient);        
        } catch (Exception ex) {
            String failMessage = BundleUtil.getStringFromBundle("harvest.start.error");
            JH.addMessage(FacesMessage.SEVERITY_FATAL, failMessage);
            return;
        } 
                
        String successMessage = BundleUtil.getStringFromBundle("harvestclients.actions.runharvest.success");
        successMessage = successMessage.replace("{0}", harvestingClient.getName());
        JsfHelper.addSuccessMessage(successMessage);
        
        // refresh the harvesting clients list - we want this one to be showing
        // "inprogress"; and we want to be able to disable all the actions buttons
        // for it:
        // (looks like we need to sleep for a few milliseconds here, to make sure 
        // it has already been updated with the "inprogress" setting)
        try{Thread.sleep(500L);}catch(Exception e){}
        
        
        configuredHarvestingClients = harvestingClientService.getAllHarvestingClients();
        
        
    }
    
    public void editClient(HarvestingClient harvestingClient) {
        setSelectedClient(harvestingClient);
        
        this.newNickname = harvestingClient.getName();
        this.newHarvestingUrl = harvestingClient.getHarvestingUrl();
        this.initialSettingsValidated = false;
        
        // TODO: do we want to try and contact the server, again, to make 
        // sure the metadataformat and/or set are still supported? 
        // and if not, what do we do? 
        // alternatively, should we make these 2 fields not editable at all?
        
        this.newOaiSet = !StringUtils.isEmpty(harvestingClient.getHarvestingSet()) ? harvestingClient.getHarvestingSet() : "none";
        this.newMetadataFormat = harvestingClient.getMetadataPrefix();
        this.newHarvestingStyle = harvestingClient.getHarvestStyle();
        
        this.harvestTypeRadio = harvestTypeRadioOAI;
        
        if (harvestingClient.isScheduled()) {
            if (HarvestingClient.SCHEDULE_PERIOD_DAILY.equals(harvestingClient.getSchedulePeriod())) {
                this.harvestingScheduleRadio = harvestingScheduleRadioDaily;                
                setHourOfDayAMPMfromInteger(harvestingClient.getScheduleHourOfDay());
                
            } else if (HarvestingClient.SCHEDULE_PERIOD_WEEKLY.equals(harvestingClient.getSchedulePeriod())) {
                this.harvestingScheduleRadio = harvestingScheduleRadioWeekly;
                setHourOfDayAMPMfromInteger(harvestingClient.getScheduleHourOfDay());
                setWeekdayFromInteger(harvestingClient.getScheduleDayOfWeek());
                
            } else {
                // ok, the client is marked as "scheduled" - but the actual
                // schedule type is not specified. 
                // so we'll show it as unscheduled on the edit form:
                this.harvestingScheduleRadio = harvestingScheduleRadioNone;
                this.newHarvestingScheduleDayOfWeek = "Sunday";
                this.newHarvestingScheduleTimeOfDay = "12";
                this.harvestingScheduleRadioAMPM = harvestingScheduleRadioAM;
            }
        } else {
            this.harvestingScheduleRadio = harvestingScheduleRadioNone;
            // unscheduled; but we populate this values to act as the defaults 
            // if they decide to schedule it and toggle the form to show the 
            // time and/or day pulldowns:
            this.newHarvestingScheduleDayOfWeek = "Sunday";
            this.newHarvestingScheduleTimeOfDay = "12";
            this.harvestingScheduleRadioAMPM = harvestingScheduleRadioAM;
        }        
        
        this.createStep = CreateStep.ONE;
        this.pageMode = PageMode.EDIT;
        
    }
    
    
    public void deleteClient() {
        if (selectedClient != null) {
            
            //configuredHarvestingClients.remove(selectedClient);
            
            logger.info("proceeding to delete harvesting client "+selectedClient.getName());
            try {
                harvestingClientService.setDeleteInProgress(selectedClient.getId());
                
                //engineService.submit(new DeleteHarvestingClientCommand(dvRequestService.getDataverseRequest(), selectedClient));
                harvestingClientService.deleteClient(selectedClient.getId());
                JsfHelper.addInfoMessage(BundleUtil.getStringFromBundle("harvestclients.tab.header.action.delete.infomessage"));
                
            //} catch (CommandException ex) {
            //    String failMessage = "Selected harvesting client cannot be deleted.";
            //    JH.addMessage(FacesMessage.SEVERITY_FATAL, failMessage);
            } catch (Exception ex) {
                String failMessage = BundleUtil.getStringFromBundle("harvest.delete.error")+ex.getMessage();
                JH.addMessage(FacesMessage.SEVERITY_FATAL, failMessage);
            }
        } else {
            logger.warning("Delete called, with a null selected harvesting client");
        }
        
        selectedClient = null; 
        configuredHarvestingClients = harvestingClientService.getAllHarvestingClients();
        this.pageMode = PageMode.VIEW;
        
    }
    
    public void createClient(ActionEvent ae) {
        
        HarvestingClient newHarvestingClient = new HarvestingClient(); // will be set as type OAI by default
        
        newHarvestingClient.setName(newNickname);
        
        if (getSelectedDestinationDataverse() == null) {
            JsfHelper.JH.addMessage(FacesMessage.SEVERITY_ERROR,BundleUtil.getStringFromBundle("harvest.create.error"));
        }
        
        newHarvestingClient.setDataverse(getSelectedDestinationDataverse());
        if (getSelectedDestinationDataverse().getHarvestingClientConfigs() == null) {
            getSelectedDestinationDataverse().setHarvestingClientConfigs(new ArrayList<>());
        }
        getSelectedDestinationDataverse().getHarvestingClientConfigs().add(newHarvestingClient);
        
        newHarvestingClient.setHarvestingUrl(newHarvestingUrl);
        if (!StringUtils.isEmpty(newOaiSet)) {
            newHarvestingClient.setHarvestingSet(newOaiSet);
        }
        newHarvestingClient.setMetadataPrefix(newMetadataFormat);
        newHarvestingClient.setHarvestStyle(newHarvestingStyle);
        
        if (isNewHarvestingScheduled()) {
            newHarvestingClient.setScheduled(true);
            
            if (isNewHarvestingScheduledWeekly()) {
                newHarvestingClient.setSchedulePeriod(HarvestingClient.SCHEDULE_PERIOD_WEEKLY);
                if (getWeekDayNumber() == null) {
                    // create a "week day is required..." error message, etc. 
                    // but we may be better off not even giving them an opportunity 
                    // to leave the field blank - ?
                }
                newHarvestingClient.setScheduleDayOfWeek(getWeekDayNumber());
            } else {
                newHarvestingClient.setSchedulePeriod(HarvestingClient.SCHEDULE_PERIOD_DAILY);
            }
            
            if (getHourOfDay() == null) {
                // see the comment above, about the day of week. same here.
            }
            newHarvestingClient.setScheduleHourOfDay(getHourOfDay());
        }
        
        // make default archive url (used to generate links pointing back to the 
        // archival sources, when harvested datasets are displayed in search results),
        // from the harvesting url:
        newHarvestingClient.setArchiveUrl(makeDefaultArchiveUrl());
        // set default description - they can customize it as they see fit:
        newHarvestingClient.setArchiveDescription(BundleUtil.getStringFromBundle("harvestclients.viewEditDialog.archiveDescription.default.generic"));
        
        
        // will try to save it now:
        
        try {
            newHarvestingClient = engineService.submit( new CreateHarvestingClientCommand(dvRequestService.getDataverseRequest(), newHarvestingClient));
            
            configuredHarvestingClients = harvestingClientService.getAllHarvestingClients();
            
            // NO, we no longer create timers here. It is the job of the Mother Timer!
            //dataverseTimerService.createHarvestTimer(newHarvestingClient);
            
            String successMessage = BundleUtil.getStringFromBundle("harvestclients.newClientDialog.success");
            successMessage = successMessage.replace("{0}", newHarvestingClient.getName());
            JsfHelper.addSuccessMessage(successMessage);

        } /* TODO: (?) add a dedicated "NameAlreadyExists" exception for the 
             create client command? 
          catch ( CreateHarvestingClientCommand.NicknameAlreadyExistsException naee ) {
            FacesContext.getCurrentInstance().addMessage(newHarvestingClient.getName(),
                           new FacesMessage( FacesMessage.SEVERITY_ERROR, naee.getMessage(), null));

        }*/ catch (CommandException ex) {
            logger.log(Level.WARNING, "Harvesting client creation command failed", ex);
            JsfHelper.JH.addMessage(FacesMessage.SEVERITY_ERROR,
                    BundleUtil.getStringFromBundle("harvest.createCommand.error"),
                                    ex.getMessage());
        } catch (Exception ex) {
            JH.addMessage(FacesMessage.SEVERITY_FATAL, BundleUtil.getStringFromBundle("harvest.create.fail"));
            logger.log(Level.SEVERE, "Harvesting client creation failed (reason unknown)." + ex.getMessage(), ex);
        }
        setPageMode(PageMode.VIEW);

        
    }
    
    // this saves an existing client that the user has edited: 
    
    public void saveClient(ActionEvent ae) {
        
        HarvestingClient harvestingClient = getSelectedClient(); 
        
        if (harvestingClient == null) {
            // TODO: 
            // tell the user somehow that the client cannot be saved, and advise
            // them to save the settings they have entered. 
            // as of now - we will show an error message, but only after the 
            // edit form has been closed.        
        }
        
        // nickname is not editable for existing clients:
        //harvestingClient.setName(newNickname);
        harvestingClient.setHarvestingUrl(newHarvestingUrl);
        harvestingClient.setHarvestingSet(newOaiSet);
        harvestingClient.setMetadataPrefix(newMetadataFormat);
        harvestingClient.setHarvestStyle(newHarvestingStyle);
        
        if (isNewHarvestingScheduled()) {
            harvestingClient.setScheduled(true);
            
            if (isNewHarvestingScheduledWeekly()) {
                harvestingClient.setSchedulePeriod(HarvestingClient.SCHEDULE_PERIOD_WEEKLY);
                if (getWeekDayNumber() == null) {
                    // create a "week day is required..." error message, etc. 
                    // but we may be better off not even giving them an opportunity 
                    // to leave the field blank - ?
                }
                harvestingClient.setScheduleDayOfWeek(getWeekDayNumber());
            } else {
                harvestingClient.setSchedulePeriod(HarvestingClient.SCHEDULE_PERIOD_DAILY);
            }
            
            if (getHourOfDay() == null) {
                // see the comment above, about the day of week. same here.
            }
            harvestingClient.setScheduleHourOfDay(getHourOfDay());
        } else {
            harvestingClient.setScheduled(false);
        }
        
        // will try to save it now:
        
        try {
            harvestingClient = engineService.submit( new UpdateHarvestingClientCommand(dvRequestService.getDataverseRequest(), harvestingClient));
            
            configuredHarvestingClients = harvestingClientService.getAllHarvestingClients();
            
            if (!harvestingClient.isScheduled()) {
                dataverseTimerService.removeHarvestTimer(harvestingClient);
            }
            JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("harvest.update.success") + harvestingClient.getName());

        } catch (CommandException ex) {
            logger.log(Level.WARNING, "Failed to save harvesting client", ex);
            JsfHelper.JH.addMessage(FacesMessage.SEVERITY_ERROR,
                    BundleUtil.getStringFromBundle("harvest.save.failure1"),
                                    ex.getMessage());
        } catch (Exception ex) {
            JH.addMessage(FacesMessage.SEVERITY_FATAL, BundleUtil.getStringFromBundle("harvest.save.failure2"));
             logger.log(Level.SEVERE, "Failed to save harvesting client (reason unknown)." + ex.getMessage(), ex);
        }
        setPageMode(PageMode.VIEW);

        
    }
    
    public void validateMetadataFormat(FacesContext context, UIComponent toValidate, Object rawValue) {
        String value = (String) rawValue;
        UIInput input = (UIInput) toValidate;
        input.setValid(true); // Optimistic approach

        // metadataFormats are selected from a pulldown that's populated with 
        // the values returned by the remote OAI server. 
        // the only validation we want is to make sure the select one from the 
        // menu. 
        if (context.getExternalContext().getRequestParameterMap().get("DO_VALIDATION") != null
                && StringUtils.isEmpty(value)) {

            input.setValid(false);
            context.addMessage(toValidate.getClientId(),
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "", BundleUtil.getStringFromBundle("harvestclients.newClientDialog.oaiMetadataFormat.required")));

        }
    }
    
    public boolean validateNickname() {

        if ( !StringUtils.isEmpty(getNewNickname()) ) {

            if (getNewNickname().length() > 30 || (!Pattern.matches("^[a-zA-Z0-9\\_\\-]+$", getNewNickname())) ) {
                //input.setValid(false);
                FacesContext.getCurrentInstance().addMessage(getNewClientNicknameInputField().getClientId(),
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "", BundleUtil.getStringFromBundle("harvestclients.newClientDialog.nickname.invalid")));
                return false;

                // If it passes the regex test, check 
            } else if ( harvestingClientService.findByNickname(getNewNickname()) != null ) {
                //input.setValid(false);
                FacesContext.getCurrentInstance().addMessage(getNewClientNicknameInputField().getClientId(),
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "", BundleUtil.getStringFromBundle("harvestclients.newClientDialog.nickname.alreadyused")));
                return false;
            }
            return true;
        } 
        
        // Nickname field is empty:
        FacesContext.getCurrentInstance().addMessage(getNewClientNicknameInputField().getClientId(),
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "", BundleUtil.getStringFromBundle("harvestclients.newClientDialog.nickname.required")));
        return false;
    }
    
    public boolean validateSelectedDataverse() {
        if (selectedDestinationDataverse == null) {
            FacesContext.getCurrentInstance().addMessage(getSelectedDataverseMenu().getClientId(),
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "", BundleUtil.getStringFromBundle("harvestclients.newClientDialog.dataverse.required")));
            return false;
        }
        return true;
    }
    
    public boolean validateServerUrlOAI() {
        if (!StringUtils.isEmpty(getNewHarvestingUrl())) {

            OaiHandler oaiHandler = new OaiHandler(getNewHarvestingUrl());
            boolean success = true;
            String message = null;

            // First, we'll try to obtain the list of supported metadata formats:
            try {
                List<String> formats = oaiHandler.runListMetadataFormats();
                if (formats != null && formats.size() > 0) {
                    createOaiMetadataFormatSelectItems(formats);
                } else {
                    success = false;
                    message = "received empty list from ListMetadataFormats";
                }

                // TODO: differentiate between different exceptions/failure scenarios } catch (OaiHandlerException ohee) {
            } catch (Exception ex) {
                success = false;
                message = "Failed to execute listmetadataformats; " + ex.getMessage();

            }

            if (success) {
                logger.info("metadataformats: success");
                logger.info(getOaiMetadataFormatSelectItems().size() + " metadata formats total.");
            } else {
                logger.info("metadataformats: failed;"+message);
            }
            // And if that worked, the list of sets provided:

            if (success) {
                try {
                    List<String> sets = oaiHandler.runListSets();
                    createOaiSetsSelectItems(sets);
                } catch (Exception ex) {
                    //success = false; 
                    // ok - we'll try and live without sets for now... 
                    // (since listMetadataFormats has succeeded earlier, may 
                    // be safe to assume that this OAI server is at least 
                    // somewhat functioning...)
                    // (XOAI ListSets buggy as well?)
                    message = "Failed to execute ListSets; " + ex.getMessage();
                    logger.warning(message);
                }
            }

            if (success) {
                return true;
            }

            FacesContext.getCurrentInstance().addMessage(getNewClientUrlInputField().getClientId(),
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "", getNewHarvestingUrl() + ": " + BundleUtil.getStringFromBundle("harvestclients.newClientDialog.url.invalid")));
            return false;

        }
        FacesContext.getCurrentInstance().addMessage(getNewClientUrlInputField().getClientId(),
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "", getNewHarvestingUrl() + ": " + BundleUtil.getStringFromBundle("harvestclients.newClientDialog.url.required")));
        return false;
    }
    
    public void validateInitialSettings() {
        if (isHarvestTypeOAI()) {
            boolean nicknameValidated = true; 
            boolean destinationDataverseValidated = true;
            if (isCreateMode()) {
                nicknameValidated = validateNickname();
                destinationDataverseValidated = validateSelectedDataverse();
            }
            boolean urlValidated = validateServerUrlOAI();
            
            if (nicknameValidated && destinationDataverseValidated && urlValidated) {
                // In Create mode we want to run all 3 validation tests; this is why 
                // we are not doing "if ((validateNickname() && validateServerUrlOAI())"
                // in the line above. -- L.A. 4.4 May 2016.
                
                setInitialSettingsValidated(true);
                this.createStep = CreateStep.TWO;
            }
            // (and if not - it stays set to false)
        }
    }
    
    public void backToStepOne() {
        this.initialSettingsValidated = false;
        this.createStep = CreateStep.ONE;
    }
    
    public void goToStepThree() {
        this.createStep = CreateStep.THREE;
    }
    
    public void backToStepTwo() {
        this.createStep = CreateStep.TWO;
    }
    
    public void goToStepFour() {
        this.createStep = CreateStep.FOUR;
    }
    
    public void backToStepThree() {
        this.createStep = CreateStep.THREE;
    }
    
    /*
     * Variables and methods for creating a new harvesting client: 
    */
    
    private int harvestTypeRadio; // 1 = OAI; 2 = Nesstar
    private static int harvestTypeRadioOAI = 1;
    private static int harvestTypeRadioNesstar = 2; 
    
    UIInput newClientNicknameInputField;
    UIInput newClientUrlInputField;
    UIInput hiddenInputField; 
    /*UISelectOne*/ UIInput metadataFormatMenu; 
    UIInput selectedDataverseMenu;
    
    private String newNickname = "";
    private String newHarvestingUrl = "";
    private boolean initialSettingsValidated = false;
    private String newOaiSet = "";
    private String newMetadataFormat = ""; 
    private String newHarvestingStyle = "";
    
    private int harvestingScheduleRadio; 
    
    private static final int harvestingScheduleRadioNone = 0;
    private static final int harvestingScheduleRadioDaily = 1;
    private static final int harvestingScheduleRadioWeekly = 2; 
    
    private String newHarvestingScheduleDayOfWeek = "Sunday";
    private String newHarvestingScheduleTimeOfDay = "12";
    
    private int harvestingScheduleRadioAMPM;
    private static final int harvestingScheduleRadioAM = 0;
    private static final int harvestingScheduleRadioPM = 1; 
    
    
    public void initNewClient(ActionEvent ae) {
        //this.selectedClient = new HarvestingClient();
        this.newNickname = "";
        this.newHarvestingUrl = "";
        this.initialSettingsValidated = false;
        this.newOaiSet = "";
        this.newMetadataFormat = "";
        this.newHarvestingStyle = HarvestingClient.HARVEST_STYLE_DATAVERSE;
        
        this.harvestTypeRadio = harvestTypeRadioOAI;
        this.harvestingScheduleRadio = harvestingScheduleRadioNone; 
        
        this.newHarvestingScheduleDayOfWeek = "Sunday";
        this.newHarvestingScheduleTimeOfDay = "12";
        
        this.harvestingScheduleRadioAMPM = harvestingScheduleRadioAM;
        
        this.pageMode = PageMode.CREATE;
        this.createStep = CreateStep.ONE;
        this.selectedDestinationDataverse = null;
        
    }
        
    public boolean isInitialSettingsValidated() {
        return this.initialSettingsValidated;
    }
    
    public void setInitialSettingsValidated(boolean validated) {
        this.initialSettingsValidated = validated;
    }
    
    
    public String getNewNickname() {
        return newNickname;
    }
    
    public void setNewNickname(String newNickname) {
        this.newNickname = newNickname;
    }
    
    public String getNewHarvestingUrl() {
        return newHarvestingUrl;
    }
    
    public void setNewHarvestingUrl(String newHarvestingUrl) {
        this.newHarvestingUrl = newHarvestingUrl;
    }
    
    public int getHarvestTypeRadio() {
        return this.harvestTypeRadio;
    }
    
    public void setHarvestTypeRadio(int harvestTypeRadio) {
        this.harvestTypeRadio = harvestTypeRadio;
    }
    
    public boolean isHarvestTypeOAI() {
        return harvestTypeRadioOAI == harvestTypeRadio;
    }
    
    public boolean isHarvestTypeNesstar() {
        return harvestTypeRadioNesstar == harvestTypeRadio;
    }
    
    public String getNewOaiSet() {
        return newOaiSet;
    }
    
    public void setNewOaiSet(String newOaiSet) {
        this.newOaiSet = newOaiSet;
    }
    
    public String getNewMetadataFormat() {
        return newMetadataFormat;
    }
    
    public void setNewMetadataFormat(String newMetadataFormat) {
        this.newMetadataFormat = newMetadataFormat;
    }
    
    public String getNewHarvestingStyle() {
        return newHarvestingStyle;
    }
    
    public void setNewHarvestingStyle(String newHarvestingStyle) {
        this.newHarvestingStyle = newHarvestingStyle;
    }
    
    public int getHarvestingScheduleRadio() {
        return this.harvestingScheduleRadio;
    }
    
    public void setHarvestingScheduleRadio(int harvestingScheduleRadio) {
        this.harvestingScheduleRadio = harvestingScheduleRadio;
    }
    
    public boolean isNewHarvestingScheduled() {
        return this.harvestingScheduleRadio != harvestingScheduleRadioNone;
    }
    
    public boolean isNewHarvestingScheduledWeekly() {
        return this.harvestingScheduleRadio == harvestingScheduleRadioWeekly;
    }
    
    public boolean isNewHarvestingScheduledDaily() {
        return this.harvestingScheduleRadio == harvestingScheduleRadioDaily;
    }
    
    public String getNewHarvestingScheduleDayOfWeek() {
        return newHarvestingScheduleDayOfWeek;
    }
    
    public void setNewHarvestingScheduleDayOfWeek(String newHarvestingScheduleDayOfWeek) {
        this.newHarvestingScheduleDayOfWeek = newHarvestingScheduleDayOfWeek;
    }
    
    public String getNewHarvestingScheduleTimeOfDay() {
        return newHarvestingScheduleTimeOfDay;
    }
    
    public void setNewHarvestingScheduleTimeOfDay(String newHarvestingScheduleTimeOfDay) {
        this.newHarvestingScheduleTimeOfDay = newHarvestingScheduleTimeOfDay;
    }
    
    public int getHarvestingScheduleRadioAMPM() {
        return this.harvestingScheduleRadioAMPM;
    }
    
    public void setHarvestingScheduleRadioAMPM(int harvestingScheduleRadioAMPM) {
        this.harvestingScheduleRadioAMPM = harvestingScheduleRadioAMPM;
    }
    
    public boolean isHarvestingScheduleTimeOfDayPM() {
        return getHarvestingScheduleRadioAMPM() == harvestingScheduleRadioPM;
    }
    
    public void toggleNewClientSchedule() {
        
    }

    
    public UIInput getNewClientNicknameInputField() {
        return newClientNicknameInputField;
    }

    public void setNewClientNicknameInputField(UIInput newClientInputField) {
        this.newClientNicknameInputField = newClientInputField;
    }

    public UIInput getNewClientUrlInputField() {
        return newClientUrlInputField;
    }

    public void setNewClientUrlInputField(UIInput newClientInputField) {
        this.newClientUrlInputField = newClientInputField;
    }
    
    public UIInput getHiddenInputField() {
        return hiddenInputField;
    }

    public void setHiddenInputField(UIInput hiddenInputField) {
        this.hiddenInputField = hiddenInputField;
    }
    
    public UIInput getMetadataFormatMenu() {
        return metadataFormatMenu;
    }

    public void setMetadataFormatMenu(UIInput metadataFormatMenu) {
        this.metadataFormatMenu = metadataFormatMenu;
    }
    
    public UIInput getSelectedDataverseMenu() {
        return selectedDataverseMenu;
    }

    public void setSelectedDataverseMenu(UIInput selectedDataverseMenu) {
        this.selectedDataverseMenu = selectedDataverseMenu;
    }
    
    private List<SelectItem> oaiSetsSelectItems;

    public List<SelectItem> getOaiSetsSelectItems() {
        return oaiSetsSelectItems;
    }

    public void setOaiSetsSelectItems(List<SelectItem> oaiSetsSelectItems) {
        this.oaiSetsSelectItems = oaiSetsSelectItems;
    }
    
    private void createOaiSetsSelectItems(List<String> setNames) {
        setOaiSetsSelectItems(new ArrayList<>());
        if (setNames != null) {
            for (String set: setNames) {
                if (!StringUtils.isEmpty(set)) {
                    getOaiSetsSelectItems().add(new SelectItem(set, set));
                }
            }
        }
    }
    
    private List<SelectItem> oaiMetadataFormatSelectItems;

    public List<SelectItem> getOaiMetadataFormatSelectItems() {
        return oaiMetadataFormatSelectItems;
    }

    public void setOaiMetadataFormatSelectItems(List<SelectItem> oaiMetadataFormatSelectItems) {
        this.oaiMetadataFormatSelectItems = oaiMetadataFormatSelectItems;
    }
    
    private void createOaiMetadataFormatSelectItems(List<String> formats) {
        setOaiMetadataFormatSelectItems(new ArrayList<>());
        if (formats != null) {
            for (String f: formats) {
                if (!StringUtils.isEmpty(f)) {
                    getOaiMetadataFormatSelectItems().add(new SelectItem(f, f));
                }
            }
        }
    }
    
    
    private List<SelectItem> harvestingStylesSelectItems = null; 
    
    public List<SelectItem> getHarvestingStylesSelectItems() {
        if (this.harvestingStylesSelectItems == null) {
            this.harvestingStylesSelectItems = new ArrayList<>(); 
            for (int i = 0; i < HarvestingClient.HARVEST_STYLE_LIST.size(); i++) {
                String style = HarvestingClient.HARVEST_STYLE_LIST.get(i);
                this.harvestingStylesSelectItems.add(new SelectItem(
                    style,
                    HarvestingClient.HARVEST_STYLE_INFOMAP.get(style)));
            }
        }
        return this.harvestingStylesSelectItems;
    }
    
    public void setHarvestingStylesSelectItems(List<SelectItem> harvestingStylesSelectItems) {
        this.harvestingStylesSelectItems = harvestingStylesSelectItems;
    }
    
    private List<String> weekDays = null; 
    private List<SelectItem> daysOfWeekSelectItems = null;
    
    public List<SelectItem> getDaysOfWeekSelectItems() {
        if (this.daysOfWeekSelectItems == null) {
            List<String> weekDays = getWeekDays();
            this.daysOfWeekSelectItems = new ArrayList<>();
            for (int i = 0; i < weekDays.size(); i++) {
                this.daysOfWeekSelectItems.add(new SelectItem(weekDays.get(i), weekDays.get(i)));
            }
        }
        
        return this.daysOfWeekSelectItems;
    }
    
    private List<String> getWeekDays() {
        if (weekDays == null) {
            weekDays = Arrays.asList("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday");
        }
        return weekDays;
    }
    
    private Integer getWeekDayNumber (String weekDayName) {
        List<String> weekDays = getWeekDays();
        int i = 0;
        for (String weekDayString: weekDays) {
            if (weekDayString.equals(weekDayName)) {
                return new Integer(i);
            }
            i++;
        }
        return null; 
    }
     
    private Integer getWeekDayNumber() {
        return getWeekDayNumber(getNewHarvestingScheduleDayOfWeek());
    }
    
    private void setWeekdayFromInteger(Integer weekday) {
        if (weekday == null || weekday < 1 || weekday > 7) {
            weekday = 0;  //set default to Sunday
        }
        this.newHarvestingScheduleDayOfWeek = getWeekDays().get(weekday);
    }
    
    private Integer getHourOfDay() {
        Integer hour = null; 
        if (getNewHarvestingScheduleTimeOfDay() != null) {
            try {
                hour = new Integer(getNewHarvestingScheduleTimeOfDay());
            } catch (Exception ex) {
                hour = null; 
            }
        }
        
        if (hour != null) {
            if (hour.intValue() == 12) {
                hour = 0; 
            }
            if (isHarvestingScheduleTimeOfDayPM()) {
                hour = hour + 12;
            }
        }
        
        return hour;
    }
    
    private void setHourOfDayAMPMfromInteger(Integer hour24) {
        if (hour24 == null || hour24.intValue() > 23) {
            hour24 = 0;
        }
        
        if (hour24.intValue() > 11) {
            hour24 = hour24.intValue() - 12;
            this.harvestingScheduleRadioAMPM = harvestingScheduleRadioPM;
        } else {
            this.harvestingScheduleRadioAMPM = harvestingScheduleRadioAM;
        }
        
        if (hour24.intValue() == 0) {
            this.newHarvestingScheduleTimeOfDay = "12";        
        } else {
            this.newHarvestingScheduleTimeOfDay = hour24.toString();
        }
    }
    
    private String makeDefaultArchiveUrl() {
        String archiveUrl = null; 
        
        if (getNewHarvestingUrl() != null) {
            int k = getNewHarvestingUrl().indexOf('/', 8);
            if (k > -1) {
                archiveUrl = getNewHarvestingUrl().substring(0, k);
            }
        }
        
        return archiveUrl; 
    }
    
    public void setDaysOfWeekSelectItems(List<SelectItem> daysOfWeekSelectItems) {
        this.daysOfWeekSelectItems = daysOfWeekSelectItems;
    }
    
    private List<SelectItem> hoursOfDaySelectItems = null;
    
    public List<SelectItem> getHoursOfDaySelectItems() {
        if (this.hoursOfDaySelectItems == null) {
            this.hoursOfDaySelectItems = new ArrayList<>();
            this.hoursOfDaySelectItems.add(new SelectItem( 12+"", "12:00"));
            for (int i = 1; i < 12; i++) {
                this.hoursOfDaySelectItems.add(new SelectItem(i+"", i+":00"));
            }
        }
        
        return this.hoursOfDaySelectItems;
    }
    
    public void setHoursOfDaySelectItems(List<SelectItem> hoursOfDaySelectItems) {
        this.hoursOfDaySelectItems = hoursOfDaySelectItems;
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
    
    public boolean isSuperUser() {
        return session.getUser().isSuperuser();
    }
}
