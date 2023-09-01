package edu.harvard.iq.dataverse.authorization.users;

import edu.harvard.iq.dataverse.Cart;
import edu.harvard.iq.dataverse.DatasetLock;
import edu.harvard.iq.dataverse.UserNotification.Type;
import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.validation.ValidateEmail;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserLookup;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2TokenData;
import edu.harvard.iq.dataverse.userdata.UserUtil;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.OrcidOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibAuthenticationProvider;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;
import static edu.harvard.iq.dataverse.util.StringUtil.nonEmpty;
import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotBlank;

/**
 * When adding an attribute to this class, be sure to update the following:
 * 
 *  (1) AuthenticatedUser.toJSON() - within this class   (REQUIRED)
 *  (2) UserServiceBean.getUserListCore() - native SQL query
 *  (3) UserServiceBean.createAuthenticatedUserForView() - add values to a detached AuthenticatedUser object
 * 
 * @author rmp553
 */
@NamedQueries({
    @NamedQuery( name="AuthenticatedUser.findAll",
                query="select au from AuthenticatedUser au"),
    @NamedQuery( name="AuthenticatedUser.findSuperUsers",
                query="SELECT au FROM AuthenticatedUser au WHERE au.superuser = TRUE"),
    @NamedQuery( name="AuthenticatedUser.findByIdentifier",
                query="select au from AuthenticatedUser au WHERE LOWER(au.userIdentifier)=LOWER(:identifier)"),
    @NamedQuery( name="AuthenticatedUser.findByEmail",
                query="select au from AuthenticatedUser au WHERE LOWER(au.email)=LOWER(:email)"),
    @NamedQuery( name="AuthenticatedUser.countOfIdentifier",
                query="SELECT COUNT(a) FROM AuthenticatedUser a WHERE LOWER(a.userIdentifier)=LOWER(:identifier)"),
    @NamedQuery( name="AuthenticatedUser.filter",
                query="select au from AuthenticatedUser au WHERE ("
                        + "LOWER(au.userIdentifier) like LOWER(:query) OR "
                        + "lower(concat(au.firstName,' ',au.lastName)) like lower(:query))"),
    @NamedQuery( name="AuthenticatedUser.findAdminUser",
                query="select au from AuthenticatedUser au WHERE "
                        + "au.superuser = true "
                        + "order by au.id")
    
})
@Entity
public class AuthenticatedUser implements User, Serializable {
    
    public static final String IDENTIFIER_PREFIX = "@";
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    /**
     * @todo Shouldn't there be some constraints on what the userIdentifier is
     * allowed to be? It can't be as restrictive as the "userName" field on
     * BuiltinUser because we can't predict what Shibboleth Identity Providers
     * (IdPs) will send (typically in the "eppn" SAML assertion) but perhaps
     * spaces, for example, should be disallowed. Right now "elisah.da mota" can
     * be persisted as a userIdentifier per
     * https://github.com/IQSS/dataverse/issues/2945
     */
    @NotNull
    @Column(nullable = false, unique=true)
    private String userIdentifier;

    @ValidateEmail(message = "{user.invalidEmail}")
    @NotNull
    @Column(nullable = false, unique=true)
    private String email;
    private String affiliation;
    private String position;
    
    @NotBlank(message = "{user.lastName}")
    private String lastName;
    
    @NotBlank(message = "{user.firstName}")
    private String firstName;
    
    @Column(nullable = true)
    private Timestamp emailConfirmed;
 
    @Column(nullable=false)
    private Timestamp createdTime;
    
    @Column(nullable=true)
    private Timestamp lastLoginTime;    // last user login timestamp

    @Column(nullable=true)
    private Timestamp lastApiUseTime;   // last API use with user's token
    
    @Transient
    private Cart cart;
    
    private boolean superuser;

    @Column(nullable=false)
    private boolean deactivated;

    @Column(nullable=true)
    private Timestamp deactivatedTime;

    @Column(columnDefinition="TEXT", nullable=true)
    private String mutedEmails;

    @Column(columnDefinition="TEXT", nullable=true)
    private String mutedNotifications;
    
    @Transient
    private Set<Type> mutedEmailsSet = new HashSet<>();
    
    @Transient
    private Set<Type> mutedNotificationsSet = new HashSet<>();

    @PrePersist
    void prePersist() {
        mutedNotifications = Type.toStringValue(mutedNotificationsSet);
        mutedEmails = Type.toStringValue(mutedEmailsSet);
    }
    
    @PostLoad
    public void initialize() {
        mutedNotificationsSet = Type.tokenizeToSet(mutedNotifications);
        mutedEmailsSet = Type.tokenizeToSet(mutedEmails);
    }

    /**
     * @todo Consider storing a hash of *all* potentially interesting Shibboleth
     * attribute key/value pairs, not just the Identity Provider (IdP).
     */
    @Transient
    private String shibIdentityProvider;

    @Override
    public String getIdentifier() {
        return IDENTIFIER_PREFIX + userIdentifier;
    }

    @OneToMany(mappedBy = "user", cascade={CascadeType.REMOVE})
    private List<UserNotification> notifications;

    public List<UserNotification> getUserNotifications() {
        return notifications;
    }

    public void setUserNotifications(List<UserNotification> notifications) {
        this.notifications = notifications;
    }
    
    @OneToMany(mappedBy = "requestor", cascade={CascadeType.REMOVE})
    private List<UserNotification> requests;

    public List<UserNotification> getUserRequests() {
        return requests;
    }

    public void setUserRequestss(List<UserNotification> requests) {
        this.requests = requests;
    }

    
    @OneToMany(mappedBy = "user", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<DatasetLock> datasetLocks;
	
    public List<DatasetLock> getDatasetLocks() {
        return datasetLocks;
    }

    public void setDatasetLocks(List<DatasetLock> datasetLocks) {
        this.datasetLocks = datasetLocks;
    }

    @OneToMany(mappedBy = "user", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<OAuth2TokenData> oAuth2TokenDatas;

    @Override
    public AuthenticatedUserDisplayInfo getDisplayInfo() {
        return new AuthenticatedUserDisplayInfo(firstName, lastName, email, affiliation, position);
    }
    
    /**
     * Takes the passed info object and updated the internal fields according to it.
     * @param inf the info from which we update the fields.
    */
    public void applyDisplayInfo( AuthenticatedUserDisplayInfo inf ) {
        setFirstName(inf.getFirstName());
        setLastName(inf.getLastName());
        if ( nonEmpty(inf.getEmailAddress()) ) {
            setEmail(inf.getEmailAddress());
        }
        if ( nonEmpty(inf.getAffiliation()) ) {
            setAffiliation( inf.getAffiliation() );
        }
        if ( nonEmpty(inf.getPosition()) ) {
            setPosition( inf.getPosition());
        }
    }

    // For Shib users, set "email confirmed" timestamp on login.
    public void updateEmailConfirmedToNow() {
        if (ShibAuthenticationProvider.PROVIDER_ID.equals(this.getAuthenticatedUserLookup().getAuthenticationProviderId())) {
            Timestamp emailConfirmedNow = new Timestamp(new Date().getTime());
            this.setEmailConfirmed(emailConfirmedNow);
        }
    }

    //For User List Admin dashboard
    @Transient
    private String roles;
    
    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }
    
    //For User List Admin dashboard - AuthenticatedProviderId
    @Transient
    private String authProviderId;    

    public String getAuthProviderId() {
        return authProviderId;
    }

    public void setAuthProviderId(String authProviderId) {
        this.authProviderId = authProviderId;
    }
    
    
    @Transient
    private String authProviderFactoryAlias;    

    public String getAuthProviderFactoryAlias() {
        return authProviderFactoryAlias;
    }

    public void setAuthProviderFactoryAlias(String authProviderFactoryAlias) {
        this.authProviderFactoryAlias = authProviderFactoryAlias;
    }
    
    
    
    @Override
    public boolean isAuthenticated() { return true; }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserIdentifier() {
        return userIdentifier;
    }

    public void setUserIdentifier(String userIdentifier) {
        this.userIdentifier = userIdentifier;
    }

    public String getName() {
        return firstName + " " + lastName;
    }

    public String getEmail() {
        return email;
    }

    //Stripping spaces to continue support of #2945
    public void setEmail(String email) {
        this.email = email.trim();
    }

    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public Timestamp getEmailConfirmed() {
        return emailConfirmed;
    }

    public void setEmailConfirmed(Timestamp emailConfirmed) {
        this.emailConfirmed = emailConfirmed;
    }

    @Override
    public boolean isSuperuser() {
        return superuser;
    }

    public void setSuperuser(boolean superuser) {
        this.superuser = superuser;
    }

    @Override
    public boolean isDeactivated() {
        return deactivated;
    }

    public void setDeactivated(boolean deactivated) {
        this.deactivated = deactivated;
    }

    public Timestamp getDeactivatedTime() {
        return deactivatedTime;
    }

    public void setDeactivatedTime(Timestamp deactivatedTime) {
        this.deactivatedTime = deactivatedTime;
    }

    @OneToOne(mappedBy = "authenticatedUser")
    private AuthenticatedUserLookup authenticatedUserLookup;

    public AuthenticatedUserLookup getAuthenticatedUserLookup() {
        return authenticatedUserLookup;
    }

    public void setAuthenticatedUserLookup(AuthenticatedUserLookup authenticatedUserLookup) {
        this.authenticatedUserLookup = authenticatedUserLookup;
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
        if (!(object instanceof AuthenticatedUser)) {
            return false;
        }
        AuthenticatedUser other = (AuthenticatedUser) object;
        return Objects.equals(getId(), other.getId());
    }    

    public String getShibIdentityProvider() {
        return shibIdentityProvider;
    }

    public void setShibIdentityProvider(String shibIdentityProvider) {
        this.shibIdentityProvider = shibIdentityProvider;
    }
    
    public JsonObjectBuilder toJson() {
        //JsonObjectBuilder authenicatedUserJson = Json.createObjectBuilder();
        
        NullSafeJsonBuilder authenicatedUserJson = NullSafeJsonBuilder.jsonObjectBuilder();
         
        authenicatedUserJson.add("id", this.id);
        authenicatedUserJson.add("userIdentifier", this.userIdentifier);
        authenicatedUserJson.add("lastName", this.lastName);
        authenicatedUserJson.add("firstName", this.firstName);
        authenicatedUserJson.add("email", this.email);
        authenicatedUserJson.add("affiliation", UserUtil.getStringOrNull(this.affiliation));
        authenicatedUserJson.add("position", UserUtil.getStringOrNull(this.position));
        authenicatedUserJson.add("isSuperuser", this.superuser);
              
        authenicatedUserJson.add("authenticationProvider", this.authProviderFactoryAlias);   
        authenicatedUserJson.add("roles", UserUtil.getStringOrNull(this.roles));
        
        authenicatedUserJson.add("createdTime", UserUtil.getTimestampStringOrNull(this.createdTime));
        authenicatedUserJson.add("lastLoginTime", UserUtil.getTimestampStringOrNull(this.lastLoginTime));
        authenicatedUserJson.add("lastApiUseTime", UserUtil.getTimestampStringOrNull(this.lastApiUseTime));

        authenicatedUserJson.add("deactivated", this.deactivated);
        authenicatedUserJson.add("deactivatedTime", UserUtil.getTimestampStringOrNull(this.deactivatedTime));
        authenicatedUserJson.add("mutedEmails", JsonPrinter.enumsToJson(this.mutedEmailsSet));
        authenicatedUserJson.add("mutedNotifications", JsonPrinter.enumsToJson(this.mutedNotificationsSet));

        return authenicatedUserJson;
    }
    
     /**
     * May be used for translating API field names.  
     * 
     * Should match order of "toJson()" method
     * 
     * @return 
     */
    public static JsonObjectBuilder getBundleStrings(){
     
           return Json.createObjectBuilder()                   
                .add("userId", BundleUtil.getStringFromBundle("dashboard.list_users.tbl_header.userId"))
                .add("userIdentifier", BundleUtil.getStringFromBundle("dashboard.list_users.tbl_header.userIdentifier"))
                .add("lastName", BundleUtil.getStringFromBundle("dashboard.list_users.tbl_header.lastName"))
                .add("firstName", BundleUtil.getStringFromBundle("dashboard.list_users.tbl_header.firstName"))
                .add("email", BundleUtil.getStringFromBundle("dashboard.list_users.tbl_header.email"))
                .add("affiliation", BundleUtil.getStringFromBundle("dashboard.list_users.tbl_header.affiliation"))
                .add("position", BundleUtil.getStringFromBundle("dashboard.list_users.tbl_header.position"))
                .add("isSuperuser", BundleUtil.getStringFromBundle("dashboard.list_users.tbl_header.isSuperuser"))
                
                .add("authenticationProvider", BundleUtil.getStringFromBundle("dashboard.list_users.tbl_header.authProviderFactoryAlias"))
                .add("roles", BundleUtil.getStringFromBundle("dashboard.list_users.tbl_header.roles"))
                   
                .add("createdTime", BundleUtil.getStringFromBundle("dashboard.list_users.tbl_header.createdTime"))
                .add("lastLoginTime", BundleUtil.getStringFromBundle("dashboard.list_users.tbl_header.lastLoginTime"))
                .add("lastApiUseTime", BundleUtil.getStringFromBundle("dashboard.list_users.tbl_header.lastApiUseTime"))
                ;
                       
    }
    
    @Override
    public String toString() {
        return "[AuthenticatedUser identifier:" + getIdentifier() + "]";
    }
    
    public String getSortByString() {
        return this.getLastName() + " " + this.getFirstName() + " " + this.getUserIdentifier();
    }
    
    /**
     * 
     * @param lastLoginTime 
     */
    public void setLastLoginTime(Timestamp lastLoginTime){
        
        this.lastLoginTime = lastLoginTime;
    }
    
    /**
     * @param lastLoginTime
     */
    public Timestamp getLastLoginTime(){
        return this.lastLoginTime;
    }
    
    
    public void setCreatedTime(Timestamp createdTime){
        this.createdTime = createdTime;
    }
    
    public Timestamp getCreatedTime(){
        return this.createdTime;
    }

    
    /**
     * 
     * @param lastApiUseTime 
     */
    public void setLastApiUseTime(Timestamp lastApiUseTime){        
        this.lastApiUseTime = lastApiUseTime;
    }
    
    /**
     * 
     * @param lastApiUseTime
     */
    public Timestamp getLastApiUseTime(){
        
        return this.lastApiUseTime;
    }

    public String getOrcidId() {
        String authProviderId = getAuthenticatedUserLookup().getAuthenticationProviderId();
        if (OrcidOAuth2AP.PROVIDER_ID_PRODUCTION.equals(authProviderId)) {
            return getAuthenticatedUserLookup().getPersistentUserId();
        }
        return null;
    }
    
    public Cart getCart() {
        if (cart == null){
            cart = new Cart();
        }
        return cart;
    }
    
    public void setCart(Cart cart) {
        this.cart = cart;
    }

    public Set<Type> getMutedEmails() {
        return mutedEmailsSet;
    }

    public void setMutedEmails(Set<Type> mutedEmails) {
        this.mutedEmailsSet = mutedEmails;
        this.mutedEmails = Type.toStringValue(mutedEmails);
    }

    public Set<Type> getMutedNotifications() {
        return mutedNotificationsSet;
    }

    public void setMutedNotifications(Set<Type> mutedNotifications) {
        this.mutedNotificationsSet = mutedNotifications;
        this.mutedNotifications = Type.toStringValue(mutedNotifications);
    }
    
    public boolean hasEmailMuted(Type type) {
        if (this.mutedEmailsSet == null || type == null) {
            return false;
        }
        return this.mutedEmailsSet.contains(type);
    }
    
    public boolean hasNotificationMuted(Type type) {
        if (this.mutedNotificationsSet == null || type == null) {
            return false;
        }
        return this.mutedNotificationsSet.contains(type);
    }
}
