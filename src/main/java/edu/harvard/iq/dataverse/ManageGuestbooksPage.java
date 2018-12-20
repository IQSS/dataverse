package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteGuestbookCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseGuestbookRootCommand;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author skraffmiller
 */
@ViewScoped
@Named
public class ManageGuestbooksPage implements java.io.Serializable {
    private static final Logger logger = Logger.getLogger(ManageGuestbooksPage.class.getCanonicalName());

    @EJB
    DataverseServiceBean dvService;

    @EJB
    GuestbookResponseServiceBean guestbookResponseService;

    @EJB
    GuestbookServiceBean guestbookService;

    @EJB
    EjbDataverseEngine engineService;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    EntityManager em;

    @Inject
    DataversePage dvpage;

    @Inject
    GuestbookPage guestbookPage;

    @Inject
    DataverseSession session;

    @Inject
    DataverseRequestServiceBean dvRequestService;

    @Inject
    PermissionsWrapper permissionsWrapper;

    private List<Guestbook> guestbooks;
    private Dataverse dataverse;
    private Long dataverseId;
    private boolean inheritGuestbooksValue;
    private boolean displayDownloadAll = false;
    private Guestbook selectedGuestbook = null;

    public String init() {
        dataverse = dvService.find(dataverseId);

        if (dataverse == null) {
            return permissionsWrapper.notFound();
        }
        if (!permissionsWrapper.canIssueCommand(dataverse, UpdateDataverseCommand.class)) {
            return permissionsWrapper.notAuthorized();
        }

        Long totalResponses = guestbookResponseService.findCountAll(dataverseId);
        if(totalResponses.intValue() > 0){
            displayDownloadAll = true;
            FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_INFO, 
                            BundleUtil.getStringFromBundle("dataset.manageGuestbooks.tip.title"), 
                            BundleUtil.getStringFromBundle("dataset.manageGuestbooks.tip.downloadascsv")));

        }

        dvpage.setDataverse(dataverse);

        guestbooks = new LinkedList<>();
        setInheritGuestbooksValue(!dataverse.isGuestbookRoot());
        if (inheritGuestbooksValue && dataverse.getOwner() != null) {
            for (Guestbook pg : dataverse.getParentGuestbooks()) {
                pg.setUsageCount(guestbookService.findCountUsages(pg.getId(), dataverseId));
                pg.setResponseCount(guestbookResponseService.findCountByGuestbookId(pg.getId(), dataverseId));
                guestbooks.add(pg);
            }
        }
        for (Guestbook cg : dataverse.getGuestbooks()) {
            cg.setDeletable(true);
            cg.setUsageCount(guestbookService.findCountUsages(cg.getId(), dataverseId));
            if (!(guestbookService.findCountUsages(cg.getId(), null) == 0)) {
                cg.setDeletable(false);
            }
            cg.setResponseCount(guestbookResponseService.findCountByGuestbookId(cg.getId() , dataverseId));
            if (!(guestbookResponseService.findCountByGuestbookId(cg.getId() , null) == 0)) {
                cg.setDeletable(false);
            }
            cg.setDataverse(dataverse);
            guestbooks.add(cg);
        }
        return null;
    }

    /* 
      replaced by the "streamResponsesByDataverse(), below
    public void downloadResponsesByDataverse(){
        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) ctx.getExternalContext().getResponse();
        response.setContentType("text/comma-separated-values");
        String fileNameString = "attachment;filename=" + getFileName();
        response.setHeader("Content-Disposition", fileNameString);
        String converted = convertResponsesToCommaDelimited(guestbookResponseService.findArrayByDataverseId(dataverseId));
        try {
            ServletOutputStream out = response.getOutputStream();
            out.write(converted.getBytes());
            out.flush();
            ctx.responseComplete();
        } catch (Exception e) {

        }
    }*/
    /*private final String SEPARATOR = ",";
    private final String END_OF_LINE = "\n";


    private String convertResponsesToCommaDelimited(List<Object[]> guestbookResponses) {

        StringBuilder sb = new StringBuilder();
        sb.append("Guestbook, Dataset, Date, Type, File Name,  File id, User Name, Email, Institution, Position, Custom Questions");
        sb.append(END_OF_LINE);
        for (Object[] array : guestbookResponses) {
            sb.append(array[0]);
            sb.append(SEPARATOR);
            sb.append(array[1]);
            sb.append(SEPARATOR);
            sb.append(array[2]);
            sb.append(SEPARATOR);
            sb.append(array[3]);
            sb.append(SEPARATOR);
            sb.append(array[4]);
            sb.append(SEPARATOR);
            sb.append(array[5]);
            sb.append(SEPARATOR);
            sb.append(array[6] == null ? "" : array[6]);
            sb.append(SEPARATOR);
            sb.append(array[7] == null ? "" : array[7]);
            sb.append(SEPARATOR);
            sb.append(array[8] == null ? "" : array[8]);
            sb.append(SEPARATOR);
            sb.append(array[9] == null ? "" : array[9]);
            if(array[10] != null){
                List <Object[]> responses = (List<Object[]>) array[10];
                for (Object[] response: responses){
                    sb.append(SEPARATOR);
                    sb.append(response[0]);
                    sb.append(SEPARATOR);
                    sb.append(response[1] == null ? "" : response[1]);
                }
            }
            sb.append(END_OF_LINE);
        }
        return sb.toString();
    }*/
    
    public void streamResponsesByDataverse(){
        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) ctx.getExternalContext().getResponse();
        response.setContentType("text/comma-separated-values");
        String fileNameString = "attachment;filename=" + getFileName();
        response.setHeader("Content-Disposition", fileNameString);
        try {
            ServletOutputStream out = response.getOutputStream();
            guestbookResponseService.streamResponsesByDataverseIdAndGuestbookId(out, dataverseId, null);
            out.flush();
            ctx.responseComplete();
        } catch (Exception e) {
            logger.warning("Failed to stream collected guestbook responses for dataverse "+dataverseId);
        }
    }

    /* This method does not appear to be needed; the ManageGuestbooksPage does not
       offer to download collected responses by dataverse and guestbook... 
       (that is done from the guestbook-responses page)
    public void downloadResponsesByDataverseAndGuestbook(){
        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) ctx.getExternalContext().getResponse();
        response.setContentType("text/comma-separated-values");
        String fileNameString = "attachment;filename=" + getFileName();
        response.setHeader("Content-Disposition", fileNameString);
        //selectedGuestbook
        String converted = convertResponsesToCommaDelimited(guestbookResponseService.findArrayByDataverseIdAndGuestbookId(dataverseId, selectedGuestbook.getId()));
        try {
            ServletOutputStream out = response.getOutputStream();
            out.write(converted.getBytes());
            out.flush();
            ctx.responseComplete();
        } catch (Exception e) {

        }
    }*/

    private String getFileName(){
       // The fix below replaces any spaces in the name of the dataverse with underscores;
       // without it, the filename was chopped off (by the browser??), and the user 
       // was getting the file name "Foo", instead of "Foo and Bar in Social Sciences.csv". -- L.A.
       return  dataverse.getName().replace(' ', '_') + "_GuestbookReponses.csv";
    }
    
    public void deleteGuestbook() {
        if (selectedGuestbook != null) {
            guestbooks.remove(selectedGuestbook);
            dataverse.getGuestbooks().remove(selectedGuestbook);
            try {
                engineService.submit(new DeleteGuestbookCommand(dvRequestService.getDataverseRequest(), getDataverse(), selectedGuestbook));
                JsfHelper.addFlashMessage(BundleUtil.getStringFromBundle("dataset.manageGuestbooks.message.deleteSuccess"));
            } catch (CommandException ex) {
                String failMessage = BundleUtil.getStringFromBundle("dataset.manageGuestbooks.message.deleteFailure");
                JH.addMessage(FacesMessage.SEVERITY_FATAL, failMessage);
            }
        } else {
            System.out.print("Selected Guestbook is null");
        }
    }

    public void saveDataverse(ActionEvent e) {
        saveDataverse("", "");
    }

    public String enableGuestbook(Guestbook selectedGuestbook) {
        selectedGuestbook.setEnabled(true);
        saveDataverse("dataset.manageGuestbooks.message.enableSuccess", "dataset.manageGuestbooks.message.enableFailure");
        return "";
    }

    public String disableGuestbook(Guestbook selectedGuestbook) {
        selectedGuestbook.setEnabled(false);
        saveDataverse("dataset.manageGuestbooks.message.disableSuccess", "dataset.manageGuestbooks.message.disableFailure");
        return "";
    }


    private void saveDataverse(String successMessage, String failureMessage) {
        if (successMessage.isEmpty()) {
            successMessage = "dataset.manageGuestbooks.message.editSuccess";
        }
        if (failureMessage.isEmpty()) {
            failureMessage = "dataset.manageGuestbooks.message.editFailure";
        }
        try {
            engineService.submit(new UpdateDataverseCommand(getDataverse(), null, null, dvRequestService.getDataverseRequest(), null));
            JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle(successMessage));
        } catch (CommandException ex) {
            JH.addMessage(FacesMessage.SEVERITY_FATAL, BundleUtil.getStringFromBundle(failureMessage));
        }

    }

    public List<Guestbook> getGuestbooks() {
        return guestbooks;
    }

    public void setGuestbooks(List<Guestbook> guestbooks) {
        this.guestbooks = guestbooks;
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

    public boolean isInheritGuestbooksValue() {
        return inheritGuestbooksValue;
    }

    public void setInheritGuestbooksValue(boolean inheritGuestbooksValue) {
        this.inheritGuestbooksValue = inheritGuestbooksValue;
    }

    public Guestbook getSelectedGuestbook() {
        return selectedGuestbook;
    }

    public void setSelectedGuestbook(Guestbook selectedGuestbook) {
        this.selectedGuestbook = selectedGuestbook;
    }

    public void viewSelectedGuestbook(Guestbook selectedGuestbook) {
        this.selectedGuestbook = selectedGuestbook;
        guestbookPage.setGuestbook(selectedGuestbook);
    }

    public boolean isDisplayDownloadAll() {
        return displayDownloadAll;
    }

    public void setDisplayDownloadAll(boolean displayDownloadAll) {
        this.displayDownloadAll = displayDownloadAll;
    }

    public String updateGuestbooksRoot(javax.faces.event.AjaxBehaviorEvent event) throws javax.faces.event.AbortProcessingException {
        try {
            dataverse = engineService.submit(
                    new UpdateDataverseGuestbookRootCommand(!isInheritGuestbooksValue(),
                                                            dvRequestService.getDataverseRequest(),
                                                            getDataverse()));
            init();
            return "";
        } catch (CommandException ex) {
            Logger.getLogger(ManageGuestbooksPage.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    }
}
