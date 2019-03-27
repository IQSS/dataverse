package edu.harvard.iq.dataverse.dashboard;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.api.Admin;
import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.GrantSuperuserStatusCommand;
import edu.harvard.iq.dataverse.engine.command.impl.MoveDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RevokeAllRolesCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RevokeSuperuserStatusCommand;
import edu.harvard.iq.dataverse.mydata.Pager;
import edu.harvard.iq.dataverse.userdata.UserListMaker;
import edu.harvard.iq.dataverse.userdata.UserListResult;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;

import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ViewScoped
@Named("DashboardDatamovePage")
public class DashboardDatamovePage implements java.io.Serializable {
  
    @EJB
    AuthenticationServiceBean authenticationService;
    @EJB
    UserServiceBean userService;
    @Inject
    DataverseSession session;
    @Inject
    PermissionsWrapper permissionsWrapper;
    @EJB
    EjbDataverseEngine commandEngine;
    @Inject
    DataverseRequestServiceBean dvRequestService;

    // I need those, but not sure this is the way to do it
    @EJB DatasetServiceBean datasetService;
    @EJB DataverseServiceBean dataverseService;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    private static final Logger logger = Logger.getLogger(DashboardDatamovePage.class.getCanonicalName());

    private AuthenticatedUser authUser = null;

    public boolean isDisable() {
        return disable;
    }
    
    public void setDisable(boolean disable) {
        this.disable = disable;
    }
    
    private boolean disable=true;
    
    public String getSrcAlias() {
      return srcAlias;
    }
  
    public void setSrcAlias(String srcAlias) {
      this.srcAlias = srcAlias;
    }
  
    private String srcAlias;

    public String getDstAlias() {
        return dstAlias;
    }

    public void setDstAlias(String dstAlias) {
        this.dstAlias = dstAlias;
    }

    private String dstAlias;
    
    public List<String> getAliases() {
        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }

    private List<String> aliases = new ArrayList<String>();

    public List<String> getDsPersistentIds() {
      return dsPersistentIds;
    }
  
    public void setDsPersistentIds(List<String> dsPersistentIds) {
      this.dsPersistentIds = dsPersistentIds;
    }
  
    private List<String> dsPersistentIds = new ArrayList<String>();

    public String getDsPersistentId() {
      return dsPersistentId;
    }
  
    public void setDsPersistentId(String dsPersistentId) {
      this.dsPersistentId = dsPersistentId;
    }
  
    private String dsPersistentId;
  
    public String init() {

        if ((session.getUser() != null) && (session.getUser().isAuthenticated()) && (session.getUser().isSuperuser())) {
           authUser = (AuthenticatedUser) session.getUser();
            // initialize components
            for (Dataverse dataverse : dataverseService.findAll()) {
                aliases.add(dataverse.getAlias());
            }

            // Note that target verse selection should be with the autocomplete as when Linking
            // Dataset selection should also be improved!
        } else {
            return permissionsWrapper.notAuthorized();
            // redirect to login OR give some type ‘you must be logged in message'
        }

        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, BundleUtil.getStringFromBundle("dashboard.card.datamove.manage"), BundleUtil.getStringFromBundle("dashboard.card.datamove.message")));
        return null;
    }

    /** Move button should only be enables if both inputs are correct and Datset is not aalredy in target 
     */
    public void move(){
        //JsfHelper.addSuccessMessage("Move called for id: " + datasetId + " To: " + dataverseAlias);

        // copied logic from Datasets API move
        Dataset ds = datasetService.findByGlobalId(dsPersistentId);//datasetId);
        Dataverse target = dataverseService.findByAlias(dstAlias);//dataverseAlias);

        //Command requires Super user - it will be tested by the command
        try {
            commandEngine.submit(new MoveDatasetCommand(
                new DataverseRequest(authUser, IpAddress.valueOf("127.0.0.1")), ds, target, false
                // Or: new DataverseRequest(authUser, (HttpServletRequest)null), ds, target, false
            ));
            
            logger.info("Moved " + dsPersistentId + " from " + srcAlias + " to " + dstAlias);
            
            updateDsPersistentIds(); // moved ds should not be in list anymore
            updateDisabled();
        }
        catch (CommandException e) {
            logger.log(Level.SEVERE,"Unable to move "+ dsPersistentId + " from " + srcAlias + " to " + dstAlias, e);
        }
    }

    public void handleSrcChange(final ValueChangeEvent event){
        srcAlias = (String)event.getNewValue();

        //infoMsg("Selected source verse: " + srcAlias);

        updateDsPersistentIds();
        updateDisabled();
    }

    public void handleDsPersistentIdChange(final ValueChangeEvent event){
        dsPersistentId = (String)event.getNewValue();
        updateDisabled();
        
        //infoMsg("Selected dataset: " + dsPersistentId);
    }

    public void handleDstChange(final ValueChangeEvent event){
        dstAlias = (String)event.getNewValue();
        updateDisabled();
        //infoMsg("Selected destination verse: " + dstAlias + " move disable=" + isDisable());
    }
    
    private void updateDsPersistentIds() {
        dsPersistentId = null;
        dsPersistentIds.clear();
        Dataverse srcDv = dataverseService.findByAlias(srcAlias);
        for(Dataset dataset: datasetService.findByOwnerId(srcDv.getId())) {
          dsPersistentIds.add(dataset.getGlobalId().asString());
        }
    }
    
    private void updateDisabled() {
        setDisable (dsPersistentId==null || srcAlias==null || dstAlias==null // some required info is missing
            || srcAlias.equals(dstAlias) // moving to the same verse makes no sense
        );
        if(!isDisable()) infoMsg("move "+ dsPersistentId + " from " + srcAlias + " to " + dstAlias);
        else infoMsg("select source and destination in order to move");
    }
    

    private void infoMsg(String msg) {
        FacesContext
            .getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, msg, ""));
    }

    public String getDataverseCount() {
        long count = em.createQuery("SELECT count(dv) FROM Dataverse dv", Long.class).getSingleResult();
        return NumberFormat.getInstance().format(count);
    }

    public String getDatasetCount() {
        long count = em.createQuery("SELECT count(ds) FROM Dataset ds", Long.class).getSingleResult();
        return NumberFormat.getInstance().format(count);
    }

}
