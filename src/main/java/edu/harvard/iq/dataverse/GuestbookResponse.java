
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.externaltools.ExternalTool;
import edu.harvard.iq.dataverse.util.BundleUtil;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

/**
 *
 * @author skraffmiller
 */
@NamedStoredProcedureQuery(
        name = "GuestbookResponse.estimateGuestBookResponseTableSize",
        procedureName = "estimateGuestBookResponseTableSize",
        parameters = {
            @StoredProcedureParameter(mode = ParameterMode.OUT, type = Long.class)
        }
)
@Entity
@Table(indexes = {
        @Index(columnList = "guestbook_id"),
        @Index(columnList = "datafile_id"),
        @Index(columnList = "dataset_id")
})

@NamedQueries(
        @NamedQuery(name = "GuestbookResponse.findByAuthenticatedUserId",
                query = "SELECT gbr FROM GuestbookResponse gbr WHERE gbr.authenticatedUser.id=:authenticatedUserId")
)

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

    @OneToMany(mappedBy="guestbookResponse",cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST},fetch = FetchType.LAZY)
    //private FileAccessRequest fileAccessRequest;
    private List<FileAccessRequest> fileAccessRequests;
     
    @OneToMany(mappedBy="guestbookResponse",cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST},orphanRemoval=true)
    @OrderBy ("id")
    private List<CustomQuestionResponse> customQuestionResponses;

    @Size(max = 255, message = "{guestbook.response.nameLength}")
    private String name;

    // TODO: Consider using EMailValidator as well.
    @Size(max = 255, message = "{guestbook.response.nameLength}")
    private String email;

    @Size(max = 255, message = "{guestbook.response.nameLength}")
    private String institution;

    @Size(max = 255, message = "{guestbook.response.nameLength}")
    private String position;
    
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date responseTime;

    private String sessionId;
    private String eventType;

    /** Event Types - there are four pre-defined values in use.
     * The type can also be the name of a previewer/explore tool
     */
    
    public static final String ACCESS_REQUEST = "AccessRequest";
    public static final String DOWNLOAD = "Download";
    static final String SUBSET = "Subset";
    static final String EXPLORE = "Explore";

    /*
    Transient Values carry non-written information 
    that will assist in the download process
    - writeResponse is set to false when dataset version is draft.
    - selected file ids is a comma delimited list that contains the file ids for multiple download
    - fileFormat tells the download api which format a subsettable file should be downloaded as

    */
      
    @Transient 
    private boolean writeResponse = true;

    @Transient
    private String selectedFileIds;
    
    @Transient 
    private String fileFormat;

    /**
     * This transient variable is a place to temporarily retrieve the
     * ExternalTool object from the popup when the popup is required on the
     * dataset page. TODO: Some day, investigate if it can be removed.
     */
    @Transient
    private ExternalTool externalTool;

    
    public boolean isWriteResponse() {
        return writeResponse;
    }

    public void setWriteResponse(boolean writeResponse) {
        this.writeResponse = writeResponse;
    }

    public String getSelectedFileIds(){
        return this.selectedFileIds;
    }
    
    public void setSelectedFileIds(String selectedFileIds) {
        this.selectedFileIds = selectedFileIds;
    }
    
    public String getFileFormat() {
        return this.fileFormat;
    }

    public void setFileFormat(String downloadFormat) {
        this.fileFormat = downloadFormat;
    }
    
    public ExternalTool getExternalTool() {
        return externalTool;
    }

    public void setExternalTool(ExternalTool externalTool) {
        this.externalTool = externalTool;
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

    public List<CustomQuestionResponse> getCustomQuestionResponses() {
        return customQuestionResponses;
    }

    public void setCustomQuestionResponses(List<CustomQuestionResponse> customQuestionResponses) {
        this.customQuestionResponses = customQuestionResponses;
    }
    
    public List<FileAccessRequest> getFileAccessRequests(){
        return fileAccessRequests;
    }

    public void setFileAccessRequest(List<FileAccessRequest> fARs){
        this.fileAccessRequests = fARs;
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
    
    public String getEventType() {
        return this.eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
        
    }
    
    public String getSessionId() {
        return this.sessionId;
    }

    public void setSessionId(String sessionId) {
        
        this.sessionId= sessionId;
    }
    
    public String toHtmlFormattedResponse() {

        StringBuilder sb = new StringBuilder();

        sb.append(BundleUtil.getStringFromBundle("dataset.guestbookResponse.id") + ": " + getId() + "<br>\n");
        sb.append(BundleUtil.getStringFromBundle("dataset.guestbookResponse.date") + ": " + getResponseDate() + "<br>\n");
        sb.append(BundleUtil.getStringFromBundle("dataset.guestbookResponse.respondent") + "<br><ul style=\"list-style-type:none;\">\n<li>"
                + BundleUtil.getStringFromBundle("name") + ": " + getName() + "</li>\n<li>");
        sb.append("  " + BundleUtil.getStringFromBundle("email") + ": " + getEmail() + "</li>\n<li>");
        sb.append(
                "  " + BundleUtil.getStringFromBundle("institution") + ": " + wrapNullAnswer(getInstitution()) + "</li>\n<li>");
        sb.append("  " + BundleUtil.getStringFromBundle("position") + ": " + wrapNullAnswer(getPosition()) + "</li></ul>\n");
        sb.append(BundleUtil.getStringFromBundle("dataset.guestbookResponse.guestbook.additionalQuestions")
                + ":<ul style=\"list-style-type:none;\">\n");

        for (CustomQuestionResponse cqr : getCustomQuestionResponses()) {
            sb.append("<li>" + BundleUtil.getStringFromBundle("dataset.guestbookResponse.question") + ": "
                    + cqr.getCustomQuestion().getQuestionString() + "<br>"
                    + BundleUtil.getStringFromBundle("dataset.guestbookResponse.answer") + ": "
                    + wrapNullAnswer(cqr.getResponse()) + "</li>\n");
        }
        sb.append("</ul>");
        return sb.toString();
    }
    
    private String wrapNullAnswer(String answer) {
        //This assumes we don't have to distinguish null from when the user actually answers "(No Reponse)". The db still has the real value
        if (answer == null) {
            return BundleUtil.getStringFromBundle("dataset.guestbookResponse.noResponse");
        }
        return answer;
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

