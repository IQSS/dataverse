/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.userdata;

import java.sql.Timestamp;
import java.util.List;

/**
 *
 * @author rmp553
 */
public class SingleUserView {
 
	private Integer rowNum;

	private Integer id;

	private String userIdentifier;

	private String lastName;

	private String firstName;

	private String email;

	private String affiliation;

	private Boolean isSuperuser;

	private String position;

	private Timestamp modificationTime;

	private String roles;


        
    /*
     * Constructor
     */
    public SingleUserView(Object[] dbRowValues, String roles, Integer rowNum){

        this.id = (int)dbRowValues[0];
        this.userIdentifier = (String)dbRowValues[1];
        this.lastName = UserUtil.getStringOrNull(dbRowValues[2]);
        this.firstName = UserUtil.getStringOrNull(dbRowValues[3]);
        this.email = UserUtil.getStringOrNull(dbRowValues[4]);
        this.affiliation = UserUtil.getStringOrNull(dbRowValues[5]);
        this.isSuperuser = (boolean)dbRowValues[6];
        this.position = UserUtil.getStringOrNull(dbRowValues[7]);
        this.modificationTime = UserUtil.getTimestampOrNull(dbRowValues[8]);
        this.roles = roles;
      
        this.rowNum = rowNum;
    }

    /**
     *  Set rowNum
     *  @param rowNum
     */
    public void setRowNum(Integer rowNum){
        this.rowNum = rowNum;
    }

    /**
     *  Get for rowNum
     *  @return Integer
     */
    public Integer getRowNum(){
        return this.rowNum;
    }
    

    /**
     *  Set id
     *  @param id
     */
    public void setId(Integer id){
        this.id = id;
    }

    /**
     *  Get for id
     *  @return Integer
     */
    public Integer getId(){
        return this.id;
    }
    

    /**
     *  Set userIdentifier
     *  @param userIdentifier
     */
    public void setUserIdentifier(String userIdentifier){
        this.userIdentifier = userIdentifier;
    }

    /**
     *  Get for userIdentifier
     *  @return String
     */
    public String getUserIdentifier(){
        return this.userIdentifier;
    }
    

    /**
     *  Set lastName
     *  @param lastName
     */
    public void setLastName(String lastName){
        this.lastName = lastName;
    }

    /**
     *  Get for lastName
     *  @return String
     */
    public String getLastName(){
        return this.lastName;
    }
    

    /**
     *  Set firstName
     *  @param firstName
     */
    public void setFirstName(String firstName){
        this.firstName = firstName;
    }

    /**
     *  Get for firstName
     *  @return String
     */
    public String getFirstName(){
        return this.firstName;
    }
    

    /**
     *  Set email
     *  @param email
     */
    public void setEmail(String email){
        this.email = email;
    }

    /**
     *  Get for email
     *  @return String
     */
    public String getEmail(){
        return this.email;
    }
    

    /**
     *  Set affiliation
     *  @param affiliation
     */
    public void setAffiliation(String affiliation){
        this.affiliation = affiliation;
    }

    /**
     *  Get for affiliation
     *  @return String
     */
    public String getAffiliation(){
        return this.affiliation;
    }
    

    /**
     *  Set isSuperuser
     *  @param isSuperuser
     */
    public void setIsSuperuser(Boolean isSuperuser){
        this.isSuperuser = isSuperuser;
    }

    /**
     *  Get for isSuperuser
     *  @return Boolean
     */
    public Boolean getIsSuperuser(){
        return this.isSuperuser;
    }
    

    /**
     *  Set position
     *  @param position
     */
    public void setPosition(String position){
        this.position = position;
    }

    /**
     *  Get for position
     *  @return String
     */
    public String getPosition(){
        return this.position;
    }
    

    /**
     *  Set modificationTime
     *  @param modificationTime
     */
    public void setModificationTime(Timestamp modificationTime){
        this.modificationTime = modificationTime;
    }

    /**
     *  Get for modificationTime
     *  @return Timestamp
     */
    public Timestamp getModificationTime(){
        return this.modificationTime;
    }
    

    /**
     *  Set roles
     *  @param roles
     */
    public void setRoles(String roles){
        this.roles = roles;
    }

    /**
     *  Get for roles
     *  @return String
     */
    public String getRoles(){
        return this.roles;
    }
    
}
