package edu.harvard.iq.dataverse.dashboard;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.SettingsWrapper;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.MoveDataverseCommand;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIInput;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

@ViewScoped
@Named("DashboardMoveDataversePage")
public class DashboardMoveDataversePage implements java.io.Serializable {
  
    @Inject
    DataverseSession session;
    @Inject
    PermissionsWrapper permissionsWrapper;
    @EJB
    EjbDataverseEngine commandEngine;
    @EJB
    DataverseServiceBean dataverseService;
    @Inject
    SettingsWrapper settingsWrapper;
    
    private static final Logger logger = Logger.getLogger(DashboardMoveDataversePage.class.getCanonicalName());

    private AuthenticatedUser authUser = null;

    // source dataverse

    public UIInput getSelectedSourceDataverseMenu() {
        return selectedSourceDataverseMenu;
    }

    public void setSelectedSourceDataverseMenu(UIInput selectedSourceDataverseMenu) {
        this.selectedSourceDataverseMenu = selectedSourceDataverseMenu;
    }

    UIInput selectedSourceDataverseMenu;

    public Dataverse getSelectedSourceDataverse() {
        return selectedSourceDataverse;
    }

    public void setSelectedSourceDataverse(Dataverse selectedSourceDataverse) {
        this.selectedSourceDataverse = selectedSourceDataverse;
    }

    private Dataverse selectedSourceDataverse;
    
    // destination dataverse

    public UIInput getSelectedDataverseMenu() {
        return selectedDataverseMenu;
    }

    public void setSelectedDataverseMenu(UIInput selectedDataverseMenu) {
        this.selectedDataverseMenu = selectedDataverseMenu;
    }

    UIInput selectedDataverseMenu;

    public Dataverse getSelectedDestinationDataverse() {
        return selectedDestinationDataverse;
    }

    public void setSelectedDestinationDataverse(Dataverse selectedDestinationDataverse) {
        this.selectedDestinationDataverse = selectedDestinationDataverse;
    }

    private Dataverse selectedDestinationDataverse;

    public List<Dataverse> completeSelectedDataverse(String query) {
        return dataverseService.filterByAliasQuery(query);
    }

    public String init() {

        if ((session.getUser() != null) && (session.getUser().isAuthenticated()) && (session.getUser().isSuperuser())) {
           authUser = (AuthenticatedUser) session.getUser();
            // initialize components, if any need it
        } else {
            return permissionsWrapper.notAuthorized();
            // redirect to login OR give some type of ‘you must be logged in' message
        }

        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(FacesMessage.SEVERITY_INFO, 
                BundleUtil.getStringFromBundle("dashboard.move.dataverse.message.summary"), 
                BundleUtil.getStringFromBundle("dashboard.move.dataverse.message.detail", Arrays.asList(settingsWrapper.getGuidesBaseUrl(), settingsWrapper.getGuidesVersion()))));
        return null;
    }
    
    public void move(){
        Dataverse dvSource = selectedSourceDataverse;
        String srcAlias = dvSource != null ? dvSource.getAlias() : null;

        Dataverse target = selectedDestinationDataverse;
        String dstAlias = target != null ? target.getAlias() : null;

        if (dvSource == null || target == null) {
            // Move only works if both inputs are correct 
            // But if these inputs are required, we should never get here
            // Since we never get here, we aren't bothering to move this English to the bundle.
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage("Please specify all fields"));
            return;
        }

        // construct arguments for message
        List<String> arguments = new ArrayList<>();
        arguments.add(dvSource != null ? dvSource.getName() : "-");
        arguments.add(target != null ? target.getName() : "-");

        // copied logic from Dataverse API move
        //Command requires Super user - it will be tested by the command
        try {
            HttpServletRequest httpServletRequest = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
            DataverseRequest dataverseRequest = new DataverseRequest(authUser, httpServletRequest);
            commandEngine.submit(new MoveDataverseCommand(
                    dataverseRequest, dvSource, target, false
            ));
            
            logger.info("Moved " + srcAlias + " to " + dstAlias);
            
            JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dashboard.move.dataverse.message.success", arguments));
        }
        catch (CommandException e) {
            logger.log(Level.SEVERE,"Unable to move "+ srcAlias + " to " + dstAlias, e);
            arguments.add(e.getLocalizedMessage());
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    BundleUtil.getStringFromBundle("dashboard.move.dataverse.message.failure.summary"),
                    BundleUtil.getStringFromBundle("dashboard.move.dataverse.message.failure.details", arguments)));
        }
    }

    public String getDataverseCount() {
        long count = dataverseService.getDataverseCount();
        return NumberFormat.getInstance().format(count);
    }

}
