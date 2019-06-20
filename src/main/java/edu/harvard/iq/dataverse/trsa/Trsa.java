package edu.harvard.iq.dataverse.trsa;

import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.ULocale;
import edu.harvard.iq.dataverse.Dataset;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author asone
 */
@Entity
@Table(name = "trsa")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Trsa.findAll", query = "SELECT t FROM Trsa t"),
    @NamedQuery(name = "Trsa.findByInstallation", query = "SELECT t FROM Trsa t WHERE t.installation = :installation"),
    @NamedQuery(name = "Trsa.findByEmail", query = "SELECT t FROM Trsa t WHERE t.email = :email"),
    @NamedQuery(name = "Trsa.findByDatafileserverurl", query = "SELECT t FROM Trsa t WHERE t.datafileserverurl = :datafileserverurl"),
    @NamedQuery(name = "Trsa.findByDataaccessinfo", query = "SELECT t FROM Trsa t WHERE t.dataaccessinfo = :dataaccessinfo"),
    @NamedQuery(name = "Trsa.findByNotaryserviceurl", query = "SELECT t FROM Trsa t WHERE t.notaryserviceurl = :notaryserviceurl"),
    @NamedQuery(name = "Trsa.findByTrsaurl", query = "SELECT t FROM Trsa t WHERE t.trsaurl = :trsaurl"),
    @NamedQuery(name = "Trsa.findByRegistertime", query = "SELECT t FROM Trsa t WHERE t.registertime = :registertime"),
    @NamedQuery(name = "Trsa.findByDisabled", query = "SELECT t FROM Trsa t WHERE t.disabled = :disabled"),
    @NamedQuery(name = "Trsa.findByExpiretime", query = "SELECT t FROM Trsa t WHERE t.expiretime = :expiretime"),
    @NamedQuery(name = "Trsa.findById", query = "SELECT t FROM Trsa t WHERE t.id = :id")})
public class Trsa implements Serializable {
    
    private static final Logger logger = Logger.getLogger(Trsa.class.getName());
    
    public static Integer DEFAULT_VALID_PERIOD=1;

    private static final long serialVersionUID = 1L;
    
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "ID")
    private Long id;
    
    
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(nullable = false, length = 255)
    private String installation;
    // @Pattern(regexp="[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?", message="Invalid email")//if the field contains email address consider using this annotation to enforce field validation
    
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(nullable = false, length = 255)
    private String email;
    
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(nullable = false, length = 255)
    private String datafileserverurl;
    
//    @Basic(optional = false)
//    @NotNull
//    @Size(min = 1, max = 255)
//    @Column(nullable = false, length = 255)
//    private String apitoken;
    
    
//    @Basic(optional = false)
//    @NotNull
//    @Size(min=1, max = 255)
//    @Column(nullable = false, length = 255)
//    private String datastoragelocation;
    
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(nullable = false, length = 255)
    private String dataaccessinfo;
    
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(nullable = false, length = 255)
    private String notaryserviceurl;
    
    @Basic(optional = true)
    @Size(max = 255)
    @Column(nullable = true, length = 255)
    private String trsaurl;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date registertime;
    
    private Boolean disabled;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date expiretime;
    
    
    
    @OneToMany (mappedBy="trsa", cascade={CascadeType.MERGE, CascadeType.REMOVE}, orphanRemoval=true)
    private List<Dataset> trsaCoupledDatasets;

    public List<Dataset> getTrsaCoupledDatasets() {
        return this.trsaCoupledDatasets;
    }

    public void setTrsaCoupledDatasets(List<Dataset> trsaCoupledDatasets) {
        this.trsaCoupledDatasets = trsaCoupledDatasets;
    }
    


    public Trsa() {
    }

    public Trsa(Long id) {
        this.id = id;
    }

    public Trsa(Long id, 
            String installation, 
            String email, 
            String datafileserverurl,
            String dataaccessinfo, 
            String notaryserviceurl
        ) {
        this.id = id;
        this.installation = installation;
        this.email = email;
        this.datafileserverurl = datafileserverurl;
        this.dataaccessinfo = dataaccessinfo;
        this.notaryserviceurl = notaryserviceurl;
        this.registertime = new Timestamp(new Date().getTime());
        this.expiretime = generateExpireTimestamp();
        this.disabled=false;
    }

    public String getInstallation() {
        return installation;
    }

    public void setInstallation(String installation) {
        this.installation = installation;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDatafileserverurl() {
        return datafileserverurl;
    }

    public void setDatafileserverurl(String datafileserverurl) {
        this.datafileserverurl = datafileserverurl;
    }

//    public String getApitoken() {
//        return apitoken;
//    }
//
//    public void setApitoken(String apitoken) {
//        this.apitoken = apitoken;
//    }
//
//    public String getDatastoragelocation() {
//        return datastoragelocation;
//    }
//
//    public void setDatastoragelocation(String datastoragelocation) {
//        this.datastoragelocation = datastoragelocation;
//    }

    public String getDataaccessinfo() {
        return dataaccessinfo;
    }

    public void setDataaccessinfo(String dataaccessinfo) {
        this.dataaccessinfo = dataaccessinfo;
    }

    public String getNotaryserviceurl() {
        return notaryserviceurl;
    }

    public void setNotaryserviceurl(String notaryserviceurl) {
        this.notaryserviceurl = notaryserviceurl;
    }

    public String getTrsaurl() {
        return trsaurl;
    }

    public void setTrsaurl(String trsaurl) {
        this.trsaurl = trsaurl;
    }

    public Date getRegistertime() {
        return registertime;
    }

    public void setRegistertime(Date registertime) {
        this.registertime = registertime;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public Date getExpiretime() {
        return expiretime;
    }

    public void setExpiretime(Date expiretime) {
        this.expiretime = expiretime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
        if (!(object instanceof Trsa)) {
            return false;
        }
        Trsa other = (Trsa) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.trsa.Trsa[ id=" + id + " ]";
    }
    
    
    public JsonObjectBuilder toJson() {
        JsonObjectBuilder jab = Json.createObjectBuilder();
        return jab.add("id", getId())
                .add("installation", getInstallation())
                .add("email", getEmail())
                .add("datafileserverurl", getDatafileserverurl())
                .add("dataaccessinfo", getDataaccessinfo())
                .add("notaryserviceurl", getNotaryserviceurl())
                .add("registertime", getRegistertime().toString())
                .add("expiretime", getExpiretime().toString())
                ;
        
//        jab.add(DISPLAY_NAME, getDisplayName());
//        jab.add(DESCRIPTION, getDescription());
//        jab.add(TYPE, getType().text);
//        jab.add(TOOL_URL, getToolUrl());
//        jab.add(TOOL_PARAMETERS, getToolParameters());
    }
    
    private Date generateExpireTimestamp(){
        return generateExpireTimestamp(null);
    }
    
    
    private Date generateExpireTimestamp(Integer year){
        if (year==null){
            year = DEFAULT_VALID_PERIOD;
        }
        Date baseline= this.registertime;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(baseline.getTime());
        cal.add(Calendar.YEAR, year);
        return new Date(cal.getTime().getTime());
        
    }
}
