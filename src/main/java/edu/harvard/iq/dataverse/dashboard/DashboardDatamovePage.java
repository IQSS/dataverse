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

    private static final Logger logger = Logger.getLogger(DashboardDatamovePage.class.getCanonicalName());

    private AuthenticatedUser authUser = null;

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    private String datasetId;

    public String getDataverseAlias() {
        return dataverseAlias;
    }

    public void setDataverseAlias(String dataverseAlias) {
        this.dataverseAlias = dataverseAlias;
    }

    private String dataverseAlias;
   
    // TODO use alias selection...

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
    
    public String init() {

        if ((session.getUser() != null) && (session.getUser().isAuthenticated()) && (session.getUser().isSuperuser())) {
           authUser = (AuthenticatedUser) session.getUser();
            // initialize components here
            for (Dataverse dataverse : dataverseService.findAll()) {
                aliases.add(dataverse.getAlias());
            }

            // Note that target verse selection should be with the autocomplete as when Linking
            // Dataset selection should also be improved!
        } else {
            return permissionsWrapper.notAuthorized();
            // redirect to login OR give some type ‘you must be logged in message'
        }

        return null;
    }

    /** Move button should only be enables if both inputs are correct and Datset is not aalredy in target 
     */
    public void move(){
        // TODO move
        JsfHelper.addSuccessMessage("Move called for id: " + datasetId + " To: " + dataverseAlias);

        /* */
        // copied logic from Datasets API move
        Dataset ds = datasetService.findByGlobalId(datasetId);
        Dataverse target = dataverseService.findByAlias(dstAlias);//dataverseAlias);
        
        //ds.getDisplayName()
        //List<Long> dsIds = datasetService.findIdsByOwnerId(target.getId());

        //Command requires Super user - it will be tested by the command
        try {
            commandEngine.submit(new MoveDatasetCommand(
                new DataverseRequest(authUser, IpAddress.valueOf("127.0.0.1")), ds, target, false
                // Or: new DataverseRequest(authUser, (HttpServletRequest)null), ds, target, false
            ));
          
            // TODO Log something!
            logger.info("Moved " + datasetId + " to " + dstAlias);
        }
        catch (CommandException e) {
            logger.log(Level.SEVERE,"Unable to move", e);
            //e.printStackTrace();
        }
        /* */
    }

    public void handleDstChange(final ValueChangeEvent event){
        dstAlias = (String)event.getNewValue();
        infoMsg("Selected destination verse: " + dstAlias);
    }

    private void infoMsg(String msg) {
        FacesContext
            .getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, msg, ""));
    }

    /** 
     * Number of total users
     * @return 
     */
    public String getUserCount() {

        return NumberFormat.getInstance().format(userService.getTotalUserCount());
    }

    /** 
     * Number of total Superusers
     * @return 
     */
    public Long getSuperUserCount() {
        
        return userService.getSuperUserCount();
    }

}
