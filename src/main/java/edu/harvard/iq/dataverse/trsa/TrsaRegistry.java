package edu.harvard.iq.dataverse.trsa;

import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.ULocale;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
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
@Table(name = "trsa_registry", catalog = "dvndb", schema = "public")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "TrsaRegistry.findAll", query = "SELECT t FROM TrsaRegistry t"),
    @NamedQuery(name = "TrsaRegistry.findByInstallation", query = "SELECT t FROM TrsaRegistry t WHERE t.installation = :installation"),
    @NamedQuery(name = "TrsaRegistry.findByEmail", query = "SELECT t FROM TrsaRegistry t WHERE t.email = :email"),
    @NamedQuery(name = "TrsaRegistry.findByDataverseurl", query = "SELECT t FROM TrsaRegistry t WHERE t.dataverseurl = :dataverseurl"),
    @NamedQuery(name = "TrsaRegistry.findByApitoken", query = "SELECT t FROM TrsaRegistry t WHERE t.apitoken = :apitoken"),
    @NamedQuery(name = "TrsaRegistry.findByDatastoragelocation", query = "SELECT t FROM TrsaRegistry t WHERE t.datastoragelocation = :datastoragelocation"),
    @NamedQuery(name = "TrsaRegistry.findByDataaccessinfo", query = "SELECT t FROM TrsaRegistry t WHERE t.dataaccessinfo = :dataaccessinfo"),
    @NamedQuery(name = "TrsaRegistry.findByNotaryserviceurl", query = "SELECT t FROM TrsaRegistry t WHERE t.notaryserviceurl = :notaryserviceurl"),
    @NamedQuery(name = "TrsaRegistry.findBySafeserviceurl", query = "SELECT t FROM TrsaRegistry t WHERE t.safeserviceurl = :safeserviceurl"),
    @NamedQuery(name = "TrsaRegistry.findByRegistertime", query = "SELECT t FROM TrsaRegistry t WHERE t.registertime = :registertime"),
    @NamedQuery(name = "TrsaRegistry.findByDisabled", query = "SELECT t FROM TrsaRegistry t WHERE t.disabled = :disabled"),
    @NamedQuery(name = "TrsaRegistry.findByExpiretime", query = "SELECT t FROM TrsaRegistry t WHERE t.expiretime = :expiretime"),
    @NamedQuery(name = "TrsaRegistry.findById", query = "SELECT t FROM TrsaRegistry t WHERE t.id = :id")})
public class TrsaRegistry implements Serializable {
    
    private static final Logger logger = Logger.getLogger(TrsaRegistry.class.getName());
    
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
    private String dataverseurl;
    
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(nullable = false, length = 255)
    private String apitoken;
    
    
    @Basic(optional = false)
    @NotNull
    @Size(min=1, max = 255)
    @Column(nullable = false, length = 255)
    private String datastoragelocation;
    
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
    
    @Basic(optional = false)
    @NotNull
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String safeserviceurl;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date registertime;
    
    private Boolean disabled;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date expiretime;


    public TrsaRegistry() {
    }

    public TrsaRegistry(Long id) {
        this.id = id;
    }

    public TrsaRegistry(Long id, String installation, String email, 
            String dataverseurl, String apitoken, String datastoragelocation,
            String dataaccessinfo, String notaryserviceurl, 
            String safeserviceurl) {
        this.id = id;
        this.installation = installation;
        this.email = email;
        this.dataverseurl = dataverseurl;
        this.apitoken = apitoken;
        this.datastoragelocation = datastoragelocation;
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

    public String getDataverseurl() {
        return dataverseurl;
    }

    public void setDataverseurl(String dataverseurl) {
        this.dataverseurl = dataverseurl;
    }

    public String getApitoken() {
        return apitoken;
    }

    public void setApitoken(String apitoken) {
        this.apitoken = apitoken;
    }

    public String getDatastoragelocation() {
        return datastoragelocation;
    }

    public void setDatastoragelocation(String datastoragelocation) {
        this.datastoragelocation = datastoragelocation;
    }

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

    public String getSafeserviceurl() {
        return safeserviceurl;
    }

    public void setSafeserviceurl(String safeserviceurl) {
        this.safeserviceurl = safeserviceurl;
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
        if (!(object instanceof TrsaRegistry)) {
            return false;
        }
        TrsaRegistry other = (TrsaRegistry) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.trsa.TrsaRegistry[ id=" + id + " ]";
    }
    
    
    public JsonObjectBuilder toJson() {
        JsonObjectBuilder jab = Json.createObjectBuilder();
        return jab.add("id", getId())
                .add("installation", getInstallation())
                .add("email", getEmail())
                .add("dataverseurl", getDataverseurl())
                .add("apitoken", getApitoken())
                .add("datastoragelocation", getDatastoragelocation())
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
