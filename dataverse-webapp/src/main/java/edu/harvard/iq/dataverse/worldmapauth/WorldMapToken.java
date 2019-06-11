/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.worldmapauth;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Random;
import java.util.logging.Logger;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 *
 * @author raprasad
 */
@Entity
@Table(name="worldmapauth_token"
     , indexes = {@Index(name = "token_value",  columnList="token", unique = true)
                , @Index(columnList="application_id")
		, @Index(columnList="datafile_id")
		, @Index(columnList="dataverseuser_id")
     })
public class WorldMapToken implements java.io.Serializable {   
    
    @Transient
    public static final String GEOCONNECT_TOKEN_KEY = "GEOCONNECT_TOKEN";
    @Transient
    public static final long MAX_HOURS_TOKEN_CAN_BE_USED = 10;
    
    private static final Logger logger = Logger.getLogger(WorldMapToken.class.getCanonicalName());

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; 

    @Column(nullable=true)  
    private String token;       // save it to get an id, then set token

    @ManyToOne
    @JoinColumn(nullable=false)
    private TokenApplicationType application;

    @ManyToOne
    @JoinColumn(nullable=false)
    private AuthenticatedUser dataverseUser;

    @ManyToOne
    @JoinColumn(nullable=false)
    private DataFile dataFile;


    @Column(nullable=false)
    private boolean hasExpired;

    @Column(nullable=false)
    private Timestamp lastRefreshTime;

    @Column(nullable=false)
    private Timestamp modified;

    @Column(nullable=false)
    private Timestamp created;
    
    public WorldMapToken(){
        this.setLastRefreshTime(this.getCurrentTimestamp());
        this.setCreated();
        this.setModified();
    }
        /**
     * Get property token.
     * @return String, value of property token.
     */
    public String getToken() {
         return this.token;
    }

    public Long getId(){
        return this.id;
    }
    /**
     * Set property token.
     * @param token new value of property token.
     */
    public boolean setToken(){
        if (this.token != null){
            return false;
        }
        if ((this.dataFile==null)||(this.dataverseUser==null)){
            return false;
        }
        MessageDigest md;   
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            logger.severe("Failed to set token for 'Map It' request!!!");
            return false;
        }
        md.update(this.getCurrentTimestamp().toString().getBytes());
        md.update(this.dataFile.toString().getBytes());
        md.update(this.dataverseUser.toString().getBytes());
        Random rand = new Random();
        Integer rnd_int = rand.nextInt();
        md.update(rnd_int.byteValue());
        
        byte[] mdbytes = md.digest();
        StringBuilder sb = new StringBuilder("");
        for (int i = 0; i < mdbytes.length; i++) {
            sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        this.token = sb.toString();
        //this.token = md.toString();
        return true;
    }


    /**
     * Get property application.
     * @return TokenApplicationType, value of property application.
     */
    public TokenApplicationType getApplication() {
            return this.application;
    }

    /**
     * Set property application.
     * @param application new value of property application.
     */
    public void setApplication(TokenApplicationType application) {
            this.application = application;
    }


    /**
     * Get property dataverseUser.
     * @return DataverseUser, value of property dataverseUser.
     */
    public AuthenticatedUser getDataverseUser() {
            return this.dataverseUser;
    }

    /**
     * Set property dataverseUser.
     * @param dataverseUser new value of property dataverseUser.
     */
    public void setDataverseUser(AuthenticatedUser dataverseUser) {
            this.dataverseUser = dataverseUser;
    }


    /**
     * Get property datafile.
     * @return DataFile, value of property datafile.
     */
    public DataFile getDatafile() {
            return this.dataFile;
    }

    /**
     * Set property datafile.
     * @param datafile new value of property datafile.
     */
    public void setDatafile(DataFile dataFile) {
            this.dataFile = dataFile;
    }


    /**
     * Get property hasExpired.
     * @return boolean, value of property hasExpired.
     */
    public boolean getHasExpired() {
        return this.hasExpired;
    }

    /**
     * Set property hasExpired.
     * @param hasExpired new value of property hasExpired.
     */
    public void setHasExpired(boolean hasExpired) {
        this.hasExpired = hasExpired;
    }


    /**
     * Get property lastRefreshTime.
     * @return Timestamp, value of property lastRefreshTime.
     */
    public Timestamp getLastRefreshTime() {
       return this.lastRefreshTime;
    }

    /**
     * Set property lastRefreshTime.
     * @param lastRefreshTime new value of property lastRefreshTime.
     */
    public void setLastRefreshTime(Timestamp lastRefreshTime) {
        this.lastRefreshTime = this.getCurrentTimestamp();
    }


    /**
     * Get property modified.
     * @return Timestamp, value of property modified.
     */
    public Timestamp getModified() {
            return this.modified;
    }

    /**
     * Set property modified.
     */
    public void setModified() {
        this.modified = this.getCurrentTimestamp();
    }


    /**
     * Get property created.
     * @return Timestamp, value of property created.
     */
    public Timestamp getCreated() {
            return this.created;
    }


    /**
     * Set property created.
     */
    public void setCreated() {
            this.created = this.getCurrentTimestamp();
    }

    public boolean hasTokenExpired(){
        logger.fine("hasTokenExpired");
 //       long currentTime = this.getCurrentTimestamp();//new Date().getTime();
        return this.hasTokenExpired(this.getCurrentTimestamp());
    }
    
    
  private long getElapsedHours(Timestamp currentTime, Timestamp oldTime){
       
       // If values are null, send back an elapsed time
       if ((currentTime==null)||(oldTime==null)){
            return this.application.getTimeLimitSeconds() + 100000;
       }
       
        long milliseconds1 = oldTime.getTime();
        long milliseconds2 = currentTime.getTime();

        long diff = milliseconds2 - milliseconds1;
        //long diffSeconds = diff / 1000;
        //long diffMinutes = diff / (60 * 1000);
        long diffHours = diff / (60 * 60 * 1000);
        //long diffDays = diff / (24 * 60 * 60 * 1000);

        return diffHours;
    }
  
   private long getElapsedSeconds(Timestamp currentTime, Timestamp oldTime){
       
       // If values are null, send back an elapsed time
       if ((currentTime==null)||(oldTime==null)){
            return this.application.getTimeLimitSeconds() + 100000;
       }
       
        long milliseconds1 = oldTime.getTime();
        long milliseconds2 = currentTime.getTime();

        long diff = milliseconds2 - milliseconds1;
        long diffSeconds = diff / 1000;
        //long diffMinutes = diff / (60 * 1000);
        //long diffHours = diff / (60 * 60 * 1000);
        //long diffDays = diff / (24 * 60 * 60 * 1000);

        return diffSeconds;
    }
    
    public boolean hasTokenExpired(Timestamp currentTimestamp){
        logger.fine("hasTokenExpired (w/timestamp)");

        if (this.getHasExpired()){
            return true;
        }
        
        if ((currentTimestamp==null)||(this.lastRefreshTime==null)){            
            this.expireToken();
            return true;
        }
         logger.fine("currentTimestamp: " + currentTimestamp);
         logger.fine("lastRefreshTime: " + lastRefreshTime);

        //System.out.println("  ..pre diff: "+ currentTimestamp);
        long hours = this.getElapsedHours(currentTimestamp, this.created);
        logger.fine("elapsed hours: " + hours);

        if (hours > MAX_HOURS_TOKEN_CAN_BE_USED){
            this.expireToken();
            return true;
        }
        
        long diffSeconds = this.getElapsedSeconds(currentTimestamp, this.lastRefreshTime);
        //System.out.println("  ..diffSeconds: "+ diffSeconds);
        //logger.fine("this.application.getTimeLimitSeconds: "+ this.application.getTimeLimitSeconds());
        if (diffSeconds > this.application.getTimeLimitSeconds()){
            this.expireToken();
            return true;
        }
        return false;
                
    }
    
    public void expireToken(){
        this.setHasExpired(true);
    }
    
    
    public Timestamp getCurrentTimestamp(){
        return new Timestamp(new Date().getTime());
    }
    
    public boolean refreshToken(){
        
        if (this.getHasExpired()){
            return false;
        }
        Timestamp currentTime = this.getCurrentTimestamp();
        if (this.hasTokenExpired(currentTime)){
            return false;
        }
        this.setLastRefreshTime(currentTime);
        return true;
    }
}
