/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.util.LinkedList;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

/**
 *
 * @author skraffmi
 */
@ViewScoped
@Named("guestbookResponsesPage")
public class GuestbookResponsesPage implements java.io.Serializable {

    @EJB
    GuestbookServiceBean guestbookService;

    @EJB
    GuestbookResponseServiceBean guestbookResponseService;
    
    @EJB
    DataverseServiceBean dvService;
    
    private Long guestbookId;
    
    private Long dataverseId;


    private Guestbook guestbook;
    
    private Dataverse dataverse;



    private List<GuestbookResponse> responses;
    
    public void init() {
        guestbook = guestbookService.find(guestbookId);
        dataverse = dvService.find(dataverseId);
        if(guestbook != null){
            responses = guestbookResponseService.findAllByGuestbookId(guestbookId);
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

    public List<GuestbookResponse> getResponses() {
        return responses;
    }

    public void setResponses(List<GuestbookResponse> responses) {
        this.responses = responses;
    }

}
