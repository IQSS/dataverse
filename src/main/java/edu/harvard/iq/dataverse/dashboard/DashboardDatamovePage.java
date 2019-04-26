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
import javax.faces.component.UIInput;
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
  
    @Inject
    DataverseSession session;
    @Inject
    PermissionsWrapper permissionsWrapper;
    @EJB
    EjbDataverseEngine commandEngine;

    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DataverseServiceBean dataverseService;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    private static final Logger logger = Logger.getLogger(DashboardDatamovePage.class.getCanonicalName());

    private AuthenticatedUser authUser = null;

    // source dataset

    public UIInput getSelectedDatasetMenu() {
        return selectedDatasetMenu;
    }

    public void setSelectedDatasetMenu(UIInput selectedDatasetMenu) {
        this.selectedDatasetMenu = selectedDatasetMenu;
    }

    UIInput selectedDatasetMenu;

    public Dataset getSelectedSourceDataset() {
        return selectedSourceDataset;
    }

    public void setSelectedSourceDataset(Dataset selectedSourceDataset) {
        this.selectedSourceDataset = selectedSourceDataset;
    }

    private Dataset selectedSourceDataset;


    public List<Dataset> completeSelectedDataset(String query) {
        return datasetService.filterByPidQuery(query);
    }
    
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
                BundleUtil.getStringFromBundle("dashboard.card.datamove.manage"), 
                BundleUtil.getStringFromBundle("dashboard.card.datamove.message")));
        return null;
    }
    
    public void move(){
        Dataset ds = selectedSourceDataset;
        String dsPersistentId = ds!=null?ds.getGlobalId().asString():null;
        String srcAlias = ds!=null?ds.getOwner().getAlias():null;

        Dataverse target = selectedDestinationDataverse;
        String dstAlias = target!=null?target.getAlias():null;

        if (ds == null || target == null) {
            // Move only works if both inputs are correct 
            // But if these inputs are required, we should never get here
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage("Please specify all fields"));
            return;
        }
        // Note that we do not check if the Dataset is already in target verse!
        //srcAlias.equals(dstAlias) // moving to the same verse makes no sense

        // construct arguments for message
        List<String> arguments = new ArrayList<>();
        arguments.add(ds!=null?ds.getDisplayName():"-");
        arguments.add(dsPersistentId!=null?dsPersistentId:"-");
        arguments.add(target!=null?target.getName():"-");

        // copied logic from Datasets API move
        //Command requires Super user - it will be tested by the command
        try {
            commandEngine.submit(new MoveDatasetCommand(
                new DataverseRequest(authUser, IpAddress.valueOf("127.0.0.1")), ds, target, false
                // Or: new DataverseRequest(authUser, (HttpServletRequest)null), ds, target, false
            ));
            
            logger.info("Moved " + dsPersistentId + " from " + srcAlias + " to " + dstAlias);
            
            JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dashboard.card.datamove.message.success", arguments));
        }
        catch (CommandException e) {
            logger.log(Level.SEVERE,"Unable to move "+ dsPersistentId + " from " + srcAlias + " to " + dstAlias, e);
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Failed to moved dataset",
                    BundleUtil.getStringFromBundle("dashboard.card.datamove.message.failure", arguments)));
        }
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
