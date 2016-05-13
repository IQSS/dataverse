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
import java.util.List;
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
@Named("HarvestingClientsPage")
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
    private HarvestingClient selectedClient = new HarvestingClient();
    private int harvestTypeRadio = 1; // 1 = OAI; 2 = Nesstar
    
    UIInput newClientNicknameInputField;
    UIInput newClientUrlInputField;
    
    public void init() {
        if (dataverseId != null) {
            setDataverse(dataverseService.find(getDataverseId())); 
        } else {
            setDataverse(dataverseService.findRootDataverse());
        }
        configuredHarvestingClients = harvestingClientService.getAllHarvestingClients();
        harvestTypeRadio = 1;
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
    
    public void initNewClient(ActionEvent ae) {
        //this.selectedClient = new HarvestingClient();
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
    
    public void validateNickname(FacesContext context, UIComponent toValidate, Object rawValue) {
        String value = (String) rawValue;
        UIInput input = (UIInput) toValidate;
        input.setValid(true); 

        if ( !StringUtils.isEmpty(value) ) {

            if (! Pattern.matches("^[a-zA-Z0-9\\_\\-]+$", value) ) {
                input.setValid(false);
                context.addMessage(toValidate.getClientId(),
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "", JH.localize("harvestclients.newClientDialog.nickname.invalid")));

                // If it passes the regex test, check 
            } else if ( harvestingClientService.findByNickname(value) != null ) {
                input.setValid(false);
                context.addMessage(toValidate.getClientId(),
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "", JH.localize("harvestclients.newClientDialog.nickname.alreadyused")));
            }
        }
    }
    
    public void testClientUrl(String serverUrl) {
        //if (!StringUtils.isEmpty(serverUrl)) {
            FacesContext.getCurrentInstance().addMessage(getNewClientUrlInputField().getClientId(),
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "", JH.localize("harvestclients.newClientDialog.url.invalid")));
        //}
    }
    
    public void validateClientUrl(FacesContext context, UIComponent toValidate, Object rawValue) {
        String value = (String) rawValue;
        UIInput input = (UIInput) toValidate;
        input.setValid(true); 

        // ... 
    }
    
    public int getHarvestTypeRadio() {
        return this.harvestTypeRadio;
    }
    
    public void setHarvestTypeRadio(int harvestTypeRadio) {
        this.harvestTypeRadio = harvestTypeRadio;
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
}
