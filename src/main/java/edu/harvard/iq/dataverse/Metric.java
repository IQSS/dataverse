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
import javax.persistence.Transient;

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
    private String metricName;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String metricValue;
        
    @Column(columnDefinition = "TEXT", nullable = true)
    private String metricDataLocation;
    
    //MAD: Keeping as text for support of pastDays and toMonth?
    @Column(columnDefinition = "TEXT", nullable = true)
    private String metricDayString;

    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date lastCalledDate;

    @Transient
    public static final String SEPARATOR = "_"; //MAD: do we need to specify more seperators?

    @Deprecated
    public Metric() {
    }

    //For monthly and day metrics
    public Metric(String metricTitle, String dayString, String dataLocation, String metricValue) {
        this.metricName = metricTitle;
        this.metricValue = metricValue;
        this.metricDataLocation = dataLocation;
        this.metricDayString = dayString;
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

    //MAD: Refactor all of these to not have "metric" in the method name
    public String getMetricDateString() {
        return metricDayString;
        //return metricName.substring(metricName.indexOf(SEPARATOR) + 1); //MAD: Is this going to blow up with adding query params
    }

    public String getMetricDataLocation() {
        return metricDataLocation;
    }
    
    public String getMetricTitle() {
        int monthSeperatorIndex = metricName.indexOf(SEPARATOR);
        if (monthSeperatorIndex >= 0) {
            return metricName.substring(0, monthSeperatorIndex);
        }
        return metricName;
    }

    /**
     * @return the metricValue
     */
    public String getMetricValue() {
        return metricValue;
    }

    /**
     * @param metricValue the metricValue to set
     */
    public void setMetricValue(String metricValue) {
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

//    public static String generateMetricName(String title, String dateString) {
//        if (title.contains(SEPARATOR) || dateString.contains(SEPARATOR)) {
//            throw new IllegalArgumentException("Metric title or date contains character reserved for seperator");
//        }
//        if (SEPARATOR.contains("-")) {
//            throw new IllegalArgumentException("Metric seperator cannot be '-', value reserved for dates");
//        }
//        return title + SEPARATOR + dateString;
//    }

}
