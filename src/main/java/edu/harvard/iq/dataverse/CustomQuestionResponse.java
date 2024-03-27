/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.util.List;
import jakarta.faces.model.SelectItem;
import jakarta.persistence.*;

/**
 *
 * @author skraffmiller
 */
@Entity
@Table(indexes = {
        @Index(columnList = "guestbookresponse_id")
})
public class CustomQuestionResponse implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(nullable = false)
    private GuestbookResponse guestbookResponse;

    @ManyToOne
    @JoinColumn(nullable = false)
    private CustomQuestion customQuestion;
    
    @Column(name = "response", columnDefinition = "TEXT", nullable = true)
    private String response;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    public GuestbookResponse getGuestbookResponse() {
        return guestbookResponse;
    }

    public void setGuestbookResponse(GuestbookResponse guestbookResponse) {
        this.guestbookResponse = guestbookResponse;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
    
    
    public CustomQuestion getCustomQuestion() {
        return customQuestion;
    }

    public void setCustomQuestion(CustomQuestion customQuestion) {
        this.customQuestion = customQuestion;
    }
    
    @Transient
    private List<SelectItem> responseSelectItems;

    public List<SelectItem> getResponseSelectItems() {
        return responseSelectItems;
    }

    public void setResponseSelectItems(List<SelectItem> responseSelectItems) {
        this.responseSelectItems = responseSelectItems;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof CustomQuestionResponse)) {
            return false;
        }
        CustomQuestionResponse other = (CustomQuestionResponse) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dvn.core.vdc.CustomQuestionResponse[ id=" + id + " ]";
    }

}

