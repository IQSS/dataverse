/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

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

    @ManyToOne(optional=true)
    @JoinColumn(name="dataverse_id", nullable=true)
    private Dataverse dataverse;
    
    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date lastCalledDate;

    @Deprecated
    public Metric() {
    }

    //For monthly and day metrics
    /**
     * 
     * @param name - metric name
     * @param dayString - how many days (day metric only)
     * @param dataLocation - local, remote, all
     * @param d - the parent dataverse
     * @param value - the value to cache
     */
    public Metric(String name, String dayString, String dataLocation, Dataverse d, String value) {
        this.name = name;
        this.valueJson = value;
        this.dataLocation = dataLocation;
        this.dayString = dayString;
        this.lastCalledDate = new Timestamp(new Date().getTime());
        this.dataverse = d;
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

    public Dataverse getDataverse() {
        return dataverse;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }

}
