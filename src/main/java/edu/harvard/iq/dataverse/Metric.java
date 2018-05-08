/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

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
        @Index(columnList = "id") //MAD: UNSURE ABOUT USE OF THIS ANNOTATION AND MY CUSTOMIZATION
}) 
public class Metric implements Serializable {
    
//    CREATE TABLE METRIC (
//    id integer NOT NULL,
//    metricName character varying(255) NOT NULL,
//    metricMonth integer NOT NULL,
//    metricYear integer NOT NULL,
//    metricValue integer NOT NULL,
//    calledDate timestamp without time zone NOT NULL --Will use for non-month metrics

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    
    @Column( nullable = false )
    private String metricName;
    
    @Column
    private int metricMonth;
    
    @Column
    private int metricYear;
    
    @Column
    private int metricValue;
    
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date calledDate;


    @Deprecated
    public Metric() {  
    }
    
    public Metric(String metricName, int metricMonth, int metricYear, int metricValue) {  
        this.metricName = metricName;
        this.metricMonth = metricMonth;
        this.metricYear = metricYear;
        this.metricValue = metricValue;
        this.calledDate = new Timestamp(new Date().getTime()); //MAD: SHOULD I BE GENERATING THIS IN CODE?
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

    /**
     * @return the metricName
     */
    public String getMetricName() {
        return metricName;
    }

    /**
     * @param metricName the metricName to set
     */
    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    /**
     * @return the metricMonth
     */
    public int getMetricMonth() {
        return metricMonth;
    }

    /**
     * @param metricMonth the metricMonth to set
     */
    public void setMetricMonth(int metricMonth) {
        this.metricMonth = metricMonth;
    }

    /**
     * @return the metricYear
     */
    public int getMetricYear() {
        return metricYear;
    }

    /**
     * @param metricYear the metricYear to set
     */
    public void setMetricYear(int metricYear) {
        this.metricYear = metricYear;
    }

    /**
     * @return the metricValue
     */
    public int getMetricValue() {
        return metricValue;
    }

    /**
     * @param metricValue the metricValue to set
     */
    public void setMetricValue(int metricValue) {
        this.metricValue = metricValue;
    }

    /**
     * @return the calledDate
     */
    public Date getCalledDate() {
        return calledDate;
    }

    /**
     * @param calledDate the calledDate to set
     */
    public void setCalledDate(Date calledDate) {
        this.calledDate = calledDate;
    }
    
    
}
