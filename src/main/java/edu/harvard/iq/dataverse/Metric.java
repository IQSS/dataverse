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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    
    @Column( nullable = false )
    private String metricName;

    @Column
    private long metricValue;
    
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date lastCalledDate;

    private static final String seperator = "_";

    @Deprecated
    public Metric() {  
    }
    
    public Metric(String metricTitle, String yyyymm, long metricValue) {  
        this.metricName = generateMetricName(metricTitle, yyyymm); 
        this.metricValue = metricValue;
        this.lastCalledDate = new Timestamp(new Date().getTime()); //MAD: SHOULD I BE GENERATING THIS IN CODE?
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

    public String getMetricDateString() {
        return metricName.substring(metricName.indexOf(seperator)+1);
    }
    
    public String getMetricTitle() {
        return metricName.substring(0,metricName.indexOf(seperator));
    }

    /**
     * @return the metricValue
     */
    public long getMetricValue() {
        return metricValue;
    }

    /**
     * @param metricValue the metricValue to set
     */
    public void setMetricValue(long metricValue) {
        this.metricValue = metricValue;
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
    
    //MAD: Should this live in a util?
    public static String generateMetricName(String title, String dateString) {
        if(title.contains(seperator) || dateString.contains(seperator)) {
            throw new IllegalArgumentException("Metric title or date contains character reserved for seperator");
        }
        if(seperator.contains("-")) {
            throw new IllegalArgumentException("Metric seperator cannot be '-', value reserved for dates");
        }
        return title + seperator + dateString;
    }
    
}
