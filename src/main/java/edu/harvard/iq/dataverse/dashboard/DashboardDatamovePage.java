package edu.harvard.iq.dataverse.dashboard;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.SettingsWrapper;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.UnforcedCommandException;
import edu.harvard.iq.dataverse.engine.command.impl.MoveDatasetCommand;
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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;

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
    @Inject
    SettingsWrapper settingsWrapper;

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
                BundleUtil.getStringFromBundle("dashboard.card.datamove.message", Arrays.asList(settingsWrapper.getGuidesBaseUrl(), settingsWrapper.getGuidesVersion()))));
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
            // Since we never get here, we aren't bothering to move this English to the bundle.
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
            HttpServletRequest httpServletRequest = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
            DataverseRequest dataverseRequest = new DataverseRequest(authUser, httpServletRequest);
            commandEngine.submit(new MoveDatasetCommand(
                    dataverseRequest, ds, target, false
            ));
            
            logger.info("Moved " + dsPersistentId + " from " + srcAlias + " to " + dstAlias);
            
            JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dashboard.card.datamove.message.success", arguments));
        }
        catch (CommandException e) {
            logger.log(Level.SEVERE,"Unable to move "+ dsPersistentId + " from " + srcAlias + " to " + dstAlias, e);
            arguments.add(e.getLocalizedMessage());
            if (e instanceof UnforcedCommandException) {
                String guidesBaseUrl = settingsWrapper.getGuidesBaseUrl();
                String version = settingsWrapper.getGuidesVersion();
                // Suggest using the API to force the move.
                arguments.add(BundleUtil.getStringFromBundle("dashboard.card.datamove.dataset.command.error.unforced.suggestForce", Arrays.asList(guidesBaseUrl, version)));
            } else {
                String emptyStringNoDetails = "";
                arguments.add(emptyStringNoDetails);
            }
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    BundleUtil.getStringFromBundle("dashboard.card.datamove.message.failure.summary"),
                    BundleUtil.getStringFromBundle("dashboard.card.datamove.message.failure.details", arguments)));
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
