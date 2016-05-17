/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClient;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClientServiceBean;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
 
    private List<HarvestingClient> configuredHarvestingClients;
    private Dataverse dataverse;
    private Long dataverseId = null;
    private HarvestingClient selectedClient;
    
    
    public void init() {
        if (dataverseId != null) {
            setDataverse(dataverseService.find(getDataverseId())); 
        } else {
            setDataverse(dataverseService.findRootDataverse());
        }
        configuredHarvestingClients = harvestingClientService.getAllHarvestingClients();
        harvestTypeRadio = harvestTypeRadioOAI;
        //selectedClient = new HarvestingClient();
        
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Harvesting Client Settings", JH.localize("harvestclients.toptip")));
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
    
    public HarvestingClient getSelectedClient() {
        return selectedClient; 
    }
    
    public void addClient(ActionEvent ae) {
        
    }
    
    public void runHarvest(HarvestingClient harvestingClient) {
        
    }
    
    public void editClient(HarvestingClient harvestingClient) {
        
    }
    
    
    public void deleteClient() {
        
    }
    
    //public void validateNickname(FacesContext context, UIComponent toValidate, Object rawValue) {
    public boolean validateNickname() {

        if ( !StringUtils.isEmpty(getNewNickname()) ) {

            if (! Pattern.matches("^[a-zA-Z0-9\\_\\-]+$", getNewNickname()) ) {
                //input.setValid(false);
                FacesContext.getCurrentInstance().addMessage(getNewClientNicknameInputField().getClientId(),
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "", JH.localize("harvestclients.newClientDialog.nickname.invalid")));
                return false;

                // If it passes the regex test, check 
            } else if ( harvestingClientService.findByNickname(getNewNickname()) != null ) {
                //input.setValid(false);
                FacesContext.getCurrentInstance().addMessage(getNewClientNicknameInputField().getClientId(),
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "", JH.localize("harvestclients.newClientDialog.nickname.alreadyused")));
                return false;
            }
            return true;
        } 
        
        // Nickname field is empty:
        FacesContext.getCurrentInstance().addMessage(getNewClientNicknameInputField().getClientId(),
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "", JH.localize("harvestclients.newClientDialog.nickname.required")));
        return false;
    }
    
    public boolean validateServerUrlOAI() {          
        if (!StringUtils.isEmpty(getNewHarvestingUrl())) {
            if (false) {
                FacesContext.getCurrentInstance().addMessage(getNewClientUrlInputField().getClientId(),
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "", getNewHarvestingUrl() + ": " + JH.localize("harvestclients.newClientDialog.url.invalid")));
                return false;
            } else {
                return true;
            }
        } 
        FacesContext.getCurrentInstance().addMessage(getNewClientUrlInputField().getClientId(),
            new FacesMessage(FacesMessage.SEVERITY_ERROR, "", getNewHarvestingUrl() + ": " + JH.localize("harvestclients.newClientDialog.url.required")));
        return false;
    }
    
    public void validateInitialSettings() {
        if (isHarvestTypeOAI()) {
            boolean nicknameValidated = validateNickname();
            boolean urlValidated = validateServerUrlOAI();
            
            if (nicknameValidated && urlValidated) {
                // We want to run both validation tests; this is why 
                // we are not doing "if ((validateNickname() && validateServerUrlOAI())"
                // in the line above. -- L.A. 4.4 May 2016.
                
                setInitialSettingsValidated(true);
            }
            // (and if not - it stays set to false)
        }
    }
    
    /*
    public void validateClientUrl(FacesContext context, UIComponent toValidate, Object rawValue) {
        String value = (String) rawValue;
        UIInput input = (UIInput) toValidate;
        input.setValid(true); 

        // ... 
    }
    */
    
    /*
     * Values and methods for creating a new harvesting client: 
    */
    
    private int harvestTypeRadio; // 1 = OAI; 2 = Nesstar
    private static int harvestTypeRadioOAI = 1;
    private static int harvestTypeRadioNesstar = 2; 
    
    UIInput newClientNicknameInputField;
    UIInput newClientUrlInputField;
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
    
    private String newHarvestingScheduleDayOfWeek = "";
    private String newHarvestingScheduleTimeOfDay = "";
    
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
        
        this.newHarvestingScheduleDayOfWeek = "";
        this.newHarvestingScheduleTimeOfDay = "";
        
        this.harvestingScheduleRadioAMPM = harvestingScheduleRadioAM;

        setOaiSetsSelectItems(new ArrayList<>());
        setOaiMetadataFormatSelectItems(new ArrayList<>());
        getOaiMetadataFormatSelectItems().add(new SelectItem("oai_dc", "oai_dc"));
        
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
    
    private List<SelectItem> oaiSetsSelectItems;

    public List<SelectItem> getOaiSetsSelectItems() {
        return oaiSetsSelectItems;
    }

    public void setOaiSetsSelectItems(List<SelectItem> oaiSetsSelectItems) {
        this.oaiSetsSelectItems = oaiSetsSelectItems;
    }
    
    private List<SelectItem> oaiMetadataFormatSelectItems;

    public List<SelectItem> getOaiMetadataFormatSelectItems() {
        return oaiMetadataFormatSelectItems;
    }

    public void setOaiMetadataFormatSelectItems(List<SelectItem> oaiMetadataFormatSelectItems) {
        this.oaiMetadataFormatSelectItems = oaiMetadataFormatSelectItems;
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
    
    private List<SelectItem> daysOfWeekSelectItems = null;
    
    public List<SelectItem> getDaysOfWeekSelectItems() {
        if (this.daysOfWeekSelectItems == null) {
            List<String> weekDays = Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
            this.daysOfWeekSelectItems = new ArrayList<>();
            for (int i = 0; i < weekDays.size(); i++) {
                this.daysOfWeekSelectItems.add(new SelectItem(weekDays.get(i), weekDays.get(i)));
            }
        }
        
        return this.daysOfWeekSelectItems;
    }
    
    public void setDaysOfWeekSelectItems(List<SelectItem> daysOfWeekSelectItems) {
        this.daysOfWeekSelectItems = daysOfWeekSelectItems;
    }
}
