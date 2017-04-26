/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.*;

/**
 *
 * @author skraffmiller
 */
@Entity
@Table(indexes = {
        @Index(columnList = "guestbook_id"),
        @Index(columnList = "datafile_id"),
        @Index(columnList = "dataset_id")
})
public class GuestbookResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(nullable=false)
    private Guestbook guestbook;
    
    @ManyToOne
    @JoinColumn(nullable=false)
    private DataFile dataFile;
    
    @ManyToOne
    @JoinColumn(nullable=false)
    private Dataset dataset;
    
    @ManyToOne
    @JoinColumn(nullable=true)
    private DatasetVersion datasetVersion;

    @ManyToOne
    @JoinColumn(nullable=true)
    private AuthenticatedUser authenticatedUser;

    @OneToMany(mappedBy="guestbookResponse",cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST},orphanRemoval=true)
    @OrderBy ("id")
    private List<CustomQuestionResponse> customQuestionResponses;


    private String name;
    private String email;
    private String institution;
    private String position;
    private String downloadtype;
    private String sessionId;
        
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date responseTime;
    
    /*
    Transient Values carry non-written information 
    that will assist in the download process
    - selected file ids is a comma delimited list that contains the file ids for multiple download
    - fileFormat tells the download api which format a subsettable file should be downloaded as
    - writeResponse is set to false when dataset version is draft.
    */
    
    @Transient
    private String selectedFileIds;
    
    @Transient 
    private String fileFormat;
    
    @Transient 
    private boolean writeResponse = true;

    public boolean isWriteResponse() {
        return writeResponse;
    }

    public void setWriteResponse(boolean writeResponse) {
        this.writeResponse = writeResponse;
    }

    public String getSelectedFileIds() {
        return selectedFileIds;
    }

    public void setSelectedFileIds(String selectedFileIds) {
        this.selectedFileIds = selectedFileIds;
    }
    
    
    public String getFileFormat() {
        return fileFormat;
    }

    public void setFileFormat(String downloadFormat) {
        this.fileFormat = downloadFormat;
    }

    public GuestbookResponse(){
        
    }
    
    public GuestbookResponse(GuestbookResponse source){
        //makes a clone of a response for adding of studyfiles in case of multiple downloads
        this.setName(source.getName());
        this.setEmail(source.getEmail());
        this.setInstitution(source.getInstitution());
        this.setPosition(source.getPosition());
        this.setResponseTime(source.getResponseTime());
        this.setDataset(source.getDataset());
        this.setDatasetVersion(source.getDatasetVersion());
        this.setAuthenticatedUser(source.getAuthenticatedUser());
        this.setSessionId(source.getSessionId());
        List <CustomQuestionResponse> customQuestionResponses = new ArrayList<>();
        if (!source.getCustomQuestionResponses().isEmpty()){
            for (CustomQuestionResponse customQuestionResponse : source.getCustomQuestionResponses() ){
                CustomQuestionResponse customQuestionResponseAdd = new CustomQuestionResponse();
                customQuestionResponseAdd.setResponse(customQuestionResponse.getResponse());  
                customQuestionResponseAdd.setCustomQuestion(customQuestionResponse.getCustomQuestion());
                customQuestionResponseAdd.setGuestbookResponse(this);
                customQuestionResponses.add(customQuestionResponseAdd);
            }           
        }
        this.setCustomQuestionResponses(customQuestionResponses);
        this.setGuestbook(source.getGuestbook());
    }
    
    
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Guestbook getGuestbook() {
        return guestbook;
    }

    public void setGuestbook(Guestbook guestbook) {
        this.guestbook = guestbook;
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    public Date getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(Date responseTime) {
        this.responseTime = responseTime;
    }

    public String getResponseDate() {
        return new SimpleDateFormat("MMMM d, yyyy").format(responseTime);
    }
    
    public String getResponseDateForDisplay(){
        return null; //    SimpleDateFormat("yyyy").format(new Timestamp(new Date().getTime()));
    }
    

    public List<CustomQuestionResponse> getCustomQuestionResponses() {
        return customQuestionResponses;
    }

    public void setCustomQuestionResponses(List<CustomQuestionResponse> customQuestionResponses) {
        this.customQuestionResponses = customQuestionResponses;
    }
    
    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public DataFile getDataFile() {
        return dataFile;
    }

    public void setDataFile(DataFile dataFile) {
        this.dataFile = dataFile;
    }
       
    public DatasetVersion getDatasetVersion() {
        return datasetVersion;
    }

    public void setDatasetVersion(DatasetVersion datasetVersion) {
        this.datasetVersion = datasetVersion;
    }

    public AuthenticatedUser getAuthenticatedUser() {
        return authenticatedUser;
    }

    public void setAuthenticatedUser(AuthenticatedUser authenticatedUser) {
        this.authenticatedUser = authenticatedUser;
    }
    
    public String getDownloadtype() {
        return downloadtype;
    }

    public void setDownloadtype(String downloadtype) {
        this.downloadtype = downloadtype;
    }
    
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
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
        if (!(object instanceof GuestbookResponse)) {
            return false;
        }
        GuestbookResponse other = (GuestbookResponse) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dvn.core.vdc.GuestBookResponse[ id=" + id + " ]";
    }
    
}

