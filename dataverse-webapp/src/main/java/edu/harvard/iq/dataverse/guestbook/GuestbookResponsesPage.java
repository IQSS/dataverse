package edu.harvard.iq.dataverse.guestbook;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseCommand;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.guestbook.Guestbook;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import javax.faces.view.ViewScoped;

import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.logging.Logger;

/**
 * @author skraffmi
 */
@ViewScoped
@Named("guestbookResponsesPage")
public class GuestbookResponsesPage implements java.io.Serializable {
    private static final Logger logger = Logger.getLogger(GuestbookResponsesPage.class.getCanonicalName());

    @EJB
    GuestbookServiceBean guestbookService;

    @EJB
    GuestbookResponseServiceBean guestbookResponseService;

    @EJB
    DataverseDao dvService;

    @EJB
    SystemConfig systemConfig;

    @Inject
    SettingsServiceBean settingsService;

    @Inject
    PermissionsWrapper permissionsWrapper;

    private Long guestbookId;

    private Long dataverseId;


    private Guestbook guestbook;

    private Dataverse dataverse;

    private long displayLimit;

    private String redirectString = "";

    public String getRedirectString() {
        return redirectString;
    }

    public void setRedirectString(String redirectString) {
        this.redirectString = redirectString;
    }

    /*private List<GuestbookResponse> responses;*/
    private List<Object[]> responsesAsArray;

    public List<Object[]> getResponsesAsArray() {
        return responsesAsArray;
    }

    public void setResponsesAsArray(List<Object[]> responsesAsArray) {
        this.responsesAsArray = responsesAsArray;
    }

    public String init() {
        guestbook = guestbookService.find(guestbookId);
        dataverse = dvService.find(dataverseId);

        if (dataverse == null || guestbook == null) {
            return permissionsWrapper.notFound();
        }

        if (!permissionsWrapper.canIssueCommand(dataverse, UpdateDataverseCommand.class)) {
            return permissionsWrapper.notAuthorized();
        }

        displayLimit = settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.GuestbookResponsesPageDisplayLimit);

        guestbook.setResponseCount(guestbookResponseService.findCountByGuestbookId(guestbookId, dataverseId));

        logger.info("Guestbook responses count: " + guestbook.getResponseCount());
        responsesAsArray = guestbookResponseService.findArrayByGuestbookIdAndDataverseId(guestbookId, dataverseId, displayLimit);


        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                                                                            BundleUtil.getStringFromBundle("dataset.guestbooksResponses.tip.title"),
                                                                            BundleUtil.getStringFromBundle("dataset.guestbooksResponses.tip.downloadascsv")));

        return null;
    }

    private String getFileName() {
        // The fix below replaces any spaces in the name of the dataverse with underscores;
        // without it, the filename was chopped off (by the browser??), and the user
        // was getting the file name "Foo", instead of "Foo and Bar in Social Sciences.csv". -- L.A.
        return dataverse.getName().replace(' ', '_') + "_" + guestbook.getId() + "_GuestbookReponses.csv";
    }

    public void streamResponsesByDataverseAndGuestbook() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) ctx.getExternalContext().getResponse();
        response.setContentType("text/comma-separated-values");
        String fileNameString = "attachment;filename=" + getFileName();
        response.setHeader("Content-Disposition", fileNameString);
        try {
            ServletOutputStream out = response.getOutputStream();
            guestbookResponseService.streamResponsesByDataverseIdAndGuestbookId(out, dataverseId, guestbookId);
            out.flush();
            ctx.responseComplete();
        } catch (Exception e) {
            logger.warning("Failed to stream collected guestbook responses for guestbook " + guestbookId + ", dataverse " + dataverseId);
        }
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

    public Long getGuestbookId() {
        return guestbookId;
    }

    public void setGuestbookId(Long guestbookId) {
        this.guestbookId = guestbookId;
    }

    public Guestbook getGuestbook() {
        return guestbook;
    }

    public void setGuestbook(Guestbook guestbook) {
        this.guestbook = guestbook;
    }

    public long getDisplayLimit() {
        return displayLimit;
    }

}
