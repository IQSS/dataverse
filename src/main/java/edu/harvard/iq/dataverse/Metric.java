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

    @Column(nullable = false, unique = true)
    private String metricName;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String metricValue;

    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date lastCalledDate;

    @Transient
    private static final String separator = "_";

    @Deprecated
    public Metric() {
    }

    //For monthly metrics
    public Metric(String metricTitle, String yyyymm, String metricValue) {
        this.metricName = generateMetricName(metricTitle, yyyymm);
        this.metricValue = metricValue;
        this.lastCalledDate = new Timestamp(new Date().getTime());
    }

    //For all-time metrics
    public Metric(String metricName, String metricValue) {
        this.metricName = metricName;
        this.metricValue = metricValue;
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

    public String getMetricDateString() {
        return metricName.substring(metricName.indexOf(separator) + 1);
    }

    public String getMetricTitle() {
        int monthSeperatorIndex = metricName.indexOf(separator);
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

    public static String generateMetricName(String title, String dateString) {
        if (title.contains(separator) || dateString.contains(separator)) {
            throw new IllegalArgumentException("Metric title or date contains character reserved for seperator");
        }
        if (separator.contains("-")) {
            throw new IllegalArgumentException("Metric seperator cannot be '-', value reserved for dates");
        }
        return title + separator + dateString;
    }

}
