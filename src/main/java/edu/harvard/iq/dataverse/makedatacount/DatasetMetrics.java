package edu.harvard.iq.dataverse.makedatacount;

import edu.harvard.iq.dataverse.Dataset;
import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;

/**
 * Cached versions of views, downloads, and citations to show in the UI and API.
 * The latest data is in log files and will be aggregated.
 *
 * The principle we decided on in tech hours on 2018-12-04 is "If we're sending
 * it to the DataCite hub, we should store it in our database as well." For
 * example, since we're sending "country" to DataCite in the SUSHI reports,
 * we'll store it, even if we don't a breakdown per country in our UI.
 *
 * TODO: We probably need to change views, downloads, and citations to be
 * nullable to distinguish between 0 downloads and an unknown number of
 * downloads for a given month.
 */
@Entity
public class DatasetMetrics implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    /**
     * TODO: add OneToMany on the other side.
     *
     * TODO: This releationship should probably be on datasetVersion instead,
     * even though DataCite hub may not be ready to process it.
     */
    @NotNull
    @JoinColumn(nullable = false)
    @ManyToOne
    private Dataset dataset;

    /**
     * Granularity by month should be sufficient.
     *
     * The spec says a breakdown per day is not supported: "Reporting of usage
     * broken down by day is not supported in this release of the Code of
     * Practice for Research Data Usage Metrics."
     * https://www.projectcounter.org/code-of-practice-rd-sections/3-technical-specifications-reports/
     */
    @Column(nullable = true)
    private String monthYear;

    /**
     * Views, but note that DataONE's formula is "Views = Investigations -
     * Requests". TODO: Discuss.
     *
     * We may try to populate view from Google Analytics, just like how Zenodo
     * extracted data from Piwik: "From 2013, we have been using a self-hosted
     * Piwik instance to track usage statistics for zenodo.org. We were able to
     * extract record views and downloads from this Piwik instance."
     * https://help.zenodo.org/#statistics
     */
    @Column(nullable = true)
    private Long viewsTotalRegular;

    /**
     * See viewsTotal.
     */
    @Column(nullable = true)
    private Long viewsUniqueRegular;
    
    @Column(nullable = true)
    private Long viewsTotalMachine;
    
    @Column(nullable = true)
    private Long viewsUniqueMachine;

    /**
     * Yes, downloads are also stored in the guestbook table. New downloads will
     * be written both here and there. The guestbook table records IP address
     * but here we only store the country code.
     *
     * TODO: Write code to process the historical downloads from the guestbook
     * table into this table, converting IP addresses into country codes. Maybe
     * an API endpoint for this?
     */
    @Column(nullable = true)
    private Long downloadsTotalRegular;

    /**
     * See downloadTotal.
     */
    @Column(nullable = true)
    private Long downloadsUniqueRegular;

    
    @Column(nullable = true)
    private Long downloadsTotalMachine;

    /**
     * See downloadTotal.
     */
    @Column(nullable = true)
    private Long downloadsUniqueMachine;
    /**
     * 2 character country code.
     *
     * TODO: Indicate which standard we are using? Would 3 character codes be
     * better?
     *
     * For an example of sending various metric types (total-dataset-requests,
     * unique-dataset-investigations, etc) for a given month (2018-04) per
     * country (DK, US, etc.) see
     * https://github.com/CDLUC3/counter-processor/blob/5ce045a09931fb680a32edcc561f88a407cccc8d/good_test.json#L893
     *
     * counter-processor uses GeoLite2 for IP lookups according to their
     * https://github.com/CDLUC3/counter-processor#download-the-free-ip-to-geolocation-database
     */
    @Column(nullable = true)
    private String countryCode;
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public String getMonthYear() {
        return monthYear;
    }

    public void setMonth(String monthYear) {
        this.monthYear = monthYear;
    }

    public Long getViewsTotalRegular() {
        return viewsTotalRegular;
    }

    public void setViewsTotalRegular(Long viewsTotalRegular) {
        this.viewsTotalRegular = viewsTotalRegular;
    }

    public Long getViewsUniqueRegular() {
        return viewsUniqueRegular;
    }

    public void setViewsUniqueRegular(Long viewsUniqueRegular) {
        this.viewsUniqueRegular = viewsUniqueRegular;
    }

    public Long getDownloadsTotalRegular() {
        return downloadsTotalRegular;
    }

    public void setDownloadsTotalRegular(Long downloadsTotalRegular) {
        this.downloadsTotalRegular = downloadsTotalRegular;
    }

    public Long getDownloadsUniqueRegular() {
        return downloadsUniqueRegular;
    }

    public void setDownloadsUniqueRegular(Long downloadsUniqueRegular) {
        this.downloadsUniqueRegular = downloadsUniqueRegular;
    }
    
    public Long getViewsTotalMachine() {
        return viewsTotalMachine;
    }

    public void setViewsTotalMachine(Long viewsTotalMachine) {
        this.viewsTotalMachine = viewsTotalMachine;
    }

    public Long getViewsUniqueMachine() {
        return viewsUniqueMachine;
    }

    public void setViewsUniqueMachine(Long viewsUniqueMachine) {
        this.viewsUniqueMachine = viewsUniqueMachine;
    }

    public Long getDownloadsTotalMachine() {
        return downloadsTotalMachine;
    }

    public void setDownloadsTotalMachine(Long downloadsTotalMachine) {
        this.downloadsTotalMachine= downloadsTotalMachine;
    }

    public Long getDownloadsUniqueMachine() {
        return downloadsUniqueMachine;
    }

    public void setDownloadsUniqueMachine(Long downloadsUniqueMachine) {
        this.downloadsUniqueMachine = downloadsUniqueMachine;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = ((countryCode == null) ? null : countryCode.toLowerCase());
    }
    
    @Transient private Long viewsUnique;
    @Transient private Long viewsTotal;
    @Transient private Long downloadsUnique;
    @Transient private Long downloadsTotal;

    public Long getViewsUnique() {
        return viewsUnique;
    }

    public void setViewsUnique(Long viewsUnique) {
        this.viewsUnique = viewsUnique;
    }

    public Long getViewsTotal() {
        return viewsTotal;
    }

    public void setViewsTotal(Long viewsTotal) {
        this.viewsTotal = viewsTotal;
    }

    public Long getDownloadsUnique() {
        return downloadsUnique;
    }

    public void setDownloadsUnique(Long downloadsUnique) {
        this.downloadsUnique = downloadsUnique;
    }

    public Long getDownloadsTotal() {
        return downloadsTotal;
    }

    public void setDownloadsTotal(Long downloadsTotal) {
        this.downloadsTotal = downloadsTotal;
    }
    
    public void initCounts(){
        this.setDownloadsTotalMachine(new Long(0));
        this.setDownloadsTotalRegular(new Long(0));
        this.setDownloadsUniqueMachine(new Long(0));
        this.setDownloadsUniqueRegular(new Long(0));
        this.setViewsTotalMachine(new Long(0));
        this.setViewsTotalRegular(new Long(0));
        this.setViewsUniqueMachine(new Long(0));
        this.setViewsUniqueRegular(new Long(0));
    }

}
