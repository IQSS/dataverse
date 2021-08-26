package edu.harvard.iq.dataverse.guestbook;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseCommand;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.guestbook.Guestbook;
import edu.harvard.iq.dataverse.util.JsfHelper;
import io.vavr.control.Try;
import org.apache.commons.lang.StringUtils;
import org.omnifaces.cdi.ViewScoped;

import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author skraffmiller
 */
@ViewScoped
@Named
public class ManageGuestbooksPage implements java.io.Serializable {
    private static final Logger logger = Logger.getLogger(ManageGuestbooksPage.class.getCanonicalName());

    private DataverseDao dvService;
    private GuestbookResponseServiceBean guestbookResponseService;
    private GuestbookServiceBean guestbookService;
    private PermissionsWrapper permissionsWrapper;
    private ManageGuestbooksService manageGuestbooksService;
    private DataverseSession dataverseSession;

    private List<Guestbook> guestbooks;
    private Dataverse dataverse;
    private Long dataverseId;
    private boolean inheritGuestbooksValue = true;
    private boolean displayDownloadAll = false;
    private Guestbook selectedGuestbook = null;

    // -------------------- CONSTRUCTORS --------------------
    @Deprecated
    public ManageGuestbooksPage() {
    }

    @Inject
    public ManageGuestbooksPage(DataverseDao dvService, GuestbookResponseServiceBean guestbookResponseService,
                                GuestbookServiceBean guestbookService,
                                PermissionsWrapper permissionsWrapper,
                                ManageGuestbooksService manageGuestbooksService,
                                DataverseSession dataverseSession) {
        this.dvService = dvService;
        this.guestbookResponseService = guestbookResponseService;
        this.guestbookService = guestbookService;
        this.permissionsWrapper = permissionsWrapper;
        this.manageGuestbooksService = manageGuestbooksService;
        this.dataverseSession = dataverseSession;
    }

    // -------------------- GETTERS --------------------
    private String getFileName() {
        // The fix below replaces any spaces in the name of the dataverse with underscores;
        // without it, the filename was chopped off (by the browser??), and the user
        // was getting the file name "Foo", instead of "Foo and Bar in Social Sciences.csv". -- L.A.
        return dataverse.getName().replace(' ', '_') + "_GuestbookReponses.csv";
    }

    public List<Guestbook> getGuestbooks() {
        return guestbooks;
    }

    public Dataverse getDataverse() {
        return dataverse;
    }

    public Long getDataverseId() {
        return dataverseId;
    }

    public boolean isInheritGuestbooksValue() {
        return inheritGuestbooksValue;
    }

    public Guestbook getSelectedGuestbook() {
        return selectedGuestbook;
    }

    public boolean isDisplayDownloadAll() {
        return displayDownloadAll;
    }

    // -------------------- LOGIC --------------------

    public String init() {
        dataverse = dvService.find(dataverseId);

        if (dataverse == null) {
            return permissionsWrapper.notFound();
        }
        if (!permissionsWrapper.canIssueCommand(dataverse, UpdateDataverseCommand.class)) {
            return permissionsWrapper.notAuthorized();
        }

        Long totalResponses = guestbookResponseService.findCountAll(dataverseId);

        dataverseSession.getUser().isSuperuser();
        if (totalResponses.intValue() > 0 && dataverseSession.getUser().isSuperuser()) {
            displayDownloadAll = true;
        }

        loadGuestbooks();

        return null;
    }

    public void streamResponsesByDataverse() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) ctx.getExternalContext().getResponse();
        response.setContentType("text/comma-separated-values");
        try {
            String fileName = URLEncoder.encode(getFileName(), StandardCharsets.UTF_8.toString());
            response.setHeader("Content-Disposition", "attachment; filename*=utf-8''"+ fileName + "; filename="+fileName);

            ServletOutputStream out = response.getOutputStream();
            guestbookResponseService.streamResponsesByDataverseIdAndGuestbookId(out, dataverseId, null);
            out.flush();
            ctx.responseComplete();
        } catch (Exception e) {
            logger.warning("Failed to stream collected guestbook responses for dataverse " + dataverseId);
        }
    }

    public void deleteGuestbook() {
        Try.of(() -> manageGuestbooksService.deleteGuestbook(selectedGuestbook.getId()))
                .onSuccess(dv -> guestbooks.remove(selectedGuestbook))
                .onSuccess(dv -> JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataset.manageGuestbooks.message.deleteSuccess")))
                .onFailure(throwable -> JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataset.manageGuestbooks.message.deleteFailure"), StringUtils.EMPTY));
    }

    public void enableGuestbook(Guestbook gb) {
        Try.of(() -> manageGuestbooksService.enableGuestbook(gb.getId()))
                .onSuccess(guestbook -> guestbooks.stream().filter(g -> g.getId().equals(gb.getId())).findFirst().ifPresent(g -> g.setEnabled(true)))
                .onSuccess(guestbook -> JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataset.manageGuestbooks.message.enableSuccess")))
                .onFailure(throwable -> JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataset.manageGuestbooks.message.enableFailure"), StringUtils.EMPTY));
    }

    public void disableGuestbook(Guestbook gb) {
        Try.of(() -> manageGuestbooksService.disableGuestbook(gb.getId()))
                .onSuccess(guestbook -> guestbooks.stream().filter(g -> g.getId().equals(gb.getId())).findFirst().ifPresent(g -> g.setEnabled(false)))
                .onSuccess(guestbook -> JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataset.manageGuestbooks.message.disableSuccess")))
                .onFailure(throwable -> JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataset.manageGuestbooks.message.disableFailure"), StringUtils.EMPTY));
    }

    public void updateGuestbooksRoot(javax.faces.event.AjaxBehaviorEvent event) throws javax.faces.event.AbortProcessingException {

        Try.of(() -> manageGuestbooksService.updateAllowGuestbooksFromRootStatus(dataverse.getId(), !inheritGuestbooksValue))
                .onSuccess(dv -> manageInheritedGuestbooks())
                .onFailure(throwable -> Logger.getLogger(ManageGuestbooksPage.class.getName()).log(Level.SEVERE, null, throwable));
    }

    // -------------------- PRIVATE ---------------------
    private void loadGuestbooks() {
        guestbooks = new LinkedList<>();
        if (!dataverse.isGuestbookRoot()) {
            setInheritGuestbooksValue(true);
            loadInheritedGuestbooks();
        }
        addDataverseGuestbooks();
    }

    private void manageInheritedGuestbooks() {
        if (inheritGuestbooksValue) {
            loadInheritedGuestbooks();
        } else {
            unloadInheritedGuestbooks();
        }
    }

    private void addDataverseGuestbooks() {
        for (Guestbook cg : dataverse.getGuestbooks()) {
            cg.setDeletable(true);
            cg.setUsageCount(guestbookService.findCountUsages(cg.getId(), dataverseId));
            if (!(guestbookService.findCountUsages(cg.getId(), null) == 0)) {
                cg.setDeletable(false);
            }
            cg.setResponseCount(guestbookResponseService.findCountByGuestbookId(cg.getId(), dataverseId));
            if (!(guestbookResponseService.findCountByGuestbookId(cg.getId(), null) == 0)) {
                cg.setDeletable(false);
            }
            cg.setDataverse(dataverse);
            guestbooks.add(cg);
        }
    }

    private void loadInheritedGuestbooks() {
        for (Guestbook pg : dataverse.getParentGuestbooks()) {
            pg.setUsageCount(guestbookService.findCountUsages(pg.getId(), dataverseId));
            pg.setResponseCount(guestbookResponseService.findCountByGuestbookId(pg.getId(), dataverseId));
            guestbooks.add(pg);
        }
    }

    private void unloadInheritedGuestbooks() {
        guestbooks.removeIf(guestbook -> !guestbook.getDataverse().getId().equals(dataverseId));
    }

    // -------------------- SETTERS --------------------

    public void setGuestbooks(List<Guestbook> guestbooks) {
        this.guestbooks = guestbooks;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    public void setDataverseId(Long dataverseId) {
        this.dataverseId = dataverseId;
    }

    public void setInheritGuestbooksValue(boolean inheritGuestbooksValue) {
        this.inheritGuestbooksValue = inheritGuestbooksValue;
    }

    public void setSelectedGuestbook(Guestbook selectedGuestbook) {
        this.selectedGuestbook = selectedGuestbook;
    }

    public void viewSelectedGuestbook(Guestbook selectedGuestbook) {
        this.selectedGuestbook = selectedGuestbook;
    }

    public void setDisplayDownloadAll(boolean displayDownloadAll) {
        this.displayDownloadAll = displayDownloadAll;
    }
}
