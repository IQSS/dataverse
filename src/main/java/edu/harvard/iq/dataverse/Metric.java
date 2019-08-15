/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author madunlap
 */
@Entity
@Table(indexes = {
    @Index(columnList = "id")
})
public class Metric implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private int id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String valueJson;
        
    @Column(columnDefinition = "TEXT", nullable = true)
    private String dataLocation;
    
    @Column(columnDefinition = "TEXT", nullable = true)
    private String dayString;

    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date lastCalledDate;

    @Deprecated
    public Metric() {
    }

    //For monthly and day metrics
    
    public Metric(String name, String dayString, String dataLocation, String value) throws IOException {
        if(null == name || null == value) {
            throw new IOException("A created metric must have a metricName and metricValue");
        }
        this.name = name;
        this.valueJson = value;
        this.dataLocation = dataLocation;
        this.dayString = dayString;
        this.lastCalledDate = new Timestamp(new Date().getTime());
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }

    public String getDateString() {
        return dayString;
    }

    public String getDataLocation() {
        return dataLocation;
    }
    
    public String getName() {
        return name;
    }

    /**
     * @return the valueJson
     */
    public String getValueJson() {
        return valueJson;
    }

    /**
     * @param metricValue the valueJson to set
     */
    public void setValueJson(String metricValue) {
        this.valueJson = metricValue;
    }

    /**
     * @return the calledDate
     */
    public Date getLastCalledDate() {
        return lastCalledDate;
    }

    /**
     * @param calledDate the calledDate to set
     */
    public void setLastCalledDate(Date calledDate) {
        this.lastCalledDate = calledDate;
    }

}
