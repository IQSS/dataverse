/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseCommand;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;

import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

/**
 *
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
    DataverseServiceBean dvService;
    
    @EJB
    SystemConfig systemConfig;
    
    @Inject
    PermissionsWrapper permissionsWrapper;
    
    private Long guestbookId;
    
    private Long dataverseId;


    private Guestbook guestbook;
    
    private Dataverse dataverse;

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

        guestbook.setResponseCount(guestbookResponseService.findCountByGuestbookId(guestbookId, dataverseId));

        logger.info("Guestbook responses count: " + guestbook.getResponseCount());
        responsesAsArray = guestbookResponseService.findArrayByGuestbookIdAndDataverseId(guestbookId, dataverseId, systemConfig.getGuestbookResponsesPageDisplayLimit());

        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                BundleUtil.getStringFromBundle("dataset.guestbooksResponses.tip.title"),
                BundleUtil.getStringFromBundle("dataset.guestbooksResponses.tip.downloadascsv")));

        return null;
    }
    
    private String getFileName(){
       // The fix below replaces any spaces in the name of the dataverse with underscores;
       // without it, the filename was chopped off (by the browser??), and the user 
       // was getting the file name "Foo", instead of "Foo and Bar in Social Sciences.csv". -- L.A. 
       return  dataverse.getName().replace(' ', '_') + "_" + guestbook.getId() + "_GuestbookReponses.csv";
    }

    public void streamResponsesByDataverseAndGuestbook(){
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
            logger.warning("Failed to stream collected guestbook responses for guestbook " + guestbookId + ", dataverse "+dataverseId);
        }
    }
    
    /* 
      The methods below have been replaced with the "streamResponsesByDataverseAndGuestbook()", above -- L.A.
    public void downloadResponsesByDataverseAndGuestbook(){
        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) ctx.getExternalContext().getResponse();
        response.setContentType("text/comma-separated-values");
        String fileNameString = "attachment;filename=" + getFileName();
        response.setHeader("Content-Disposition", fileNameString);
        //selectedGuestbook
        String converted = convertResponsesToTabDelimited(guestbookResponseService.findArrayByDataverseIdAndGuestbookId(dataverseId, guestbookId));
        try {
            ServletOutputStream out = response.getOutputStream();
            out.write(converted.getBytes());                                                                                                                                                                                                                                                                                                                     
            out.flush();
            ctx.responseComplete();
        } catch (Exception e) {

        }
    } 
    
    
    private final String SEPARATOR = ",";
    private final String END_OF_LINE = "\n";

    
    private String convertResponsesToTabDelimited(List<Object[]> guestbookResponses) {

        StringBuilder sb = new StringBuilder();
        sb.append("Guestbook, Dataset, Date, Type, File Name, File Id, User Name, Email, Institution, Position, Custom Questions");
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
            sb.append(array[5] == null ? "" : array[5]);
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
    } */
    
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

    /*public List<GuestbookResponse> getResponses() {
        return responses;
    }

    public void setResponses(List<GuestbookResponse> responses) {
        this.responses = responses;
    }*/

}
