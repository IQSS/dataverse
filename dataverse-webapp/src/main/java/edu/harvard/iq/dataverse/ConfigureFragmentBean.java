package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.externaltools.ExternalToolHandler;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalTool;
import edu.harvard.iq.dataverse.persistence.user.ApiToken;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.User;
import org.omnifaces.cdi.ViewScoped;

import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.inject.Inject;
import javax.inject.Named;

import java.util.logging.Logger;

import static edu.harvard.iq.dataverse.util.JsfHelper.JH;

/**
 * This bean is mainly for keeping track of which file the user selected to run external tools on.
 * Also for creating an alert across Dataset and DataFile page, and making it easy to get the file-specific handler for a tool.
 *
 * @author madunlap
 */

@ViewScoped
@Named
public class ConfigureFragmentBean implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(ConfigureFragmentBean.class.getName());

    private ExternalTool tool = null;
    private Long fileId = null;

    @EJB
    DataFileServiceBean datafileService;
    @Inject
    DataverseSession session;
    @EJB
    AuthenticationServiceBean authService;
    @EJB
    private ExternalToolHandler externalToolHandler;

    public String configureExternalAlert() {
        JH.addMessage(FacesMessage.SEVERITY_WARN, tool.getDisplayName(), BundleUtil.getStringFromBundle("file.configure.launchMessage.details") + " " + tool.getDisplayName() + ".");
        return "";
    }

    /**
     * @param setTool the tool to set
     */
    public void setConfigurePopupTool(ExternalTool setTool) {
        tool = setTool;
    }

    /**
     * @return the Tool
     */
    public ExternalTool getConfigurePopupTool() {
        return tool;
    }

    public String getToolUrlWithQueryParams() {
        if (fileId == null) {
            //on first UI load, method is called before fileId is set. There may be a better way to handle this
            return null;
        }

        DataFile dataFile = datafileService.find(fileId);

        ApiToken apiToken = new ApiToken();
        User user = session.getUser();
        if (user instanceof AuthenticatedUser) {
            apiToken = authService.findApiTokenByUser((AuthenticatedUser) user);
        }
        
        return externalToolHandler.buildToolUrlWithQueryParams(tool, dataFile, apiToken);
    }

    public void setConfigureFileId(Long setFileId) {
        fileId = setFileId;
    }
}
