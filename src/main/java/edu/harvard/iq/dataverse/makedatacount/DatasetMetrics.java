package edu.harvard.iq.dataverse.makedatacount;

import edu.harvard.iq.dataverse.Dataset;
import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

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
public class DatasetMetrics {

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
    private Long viewsTotal;

    /**
     * See viewsTotal.
     */
    @Column(nullable = true)
    private Long viewsUnique;

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
    private Long downloadsTotal;

    /**
     * See downloadTotal.
     */
    @Column(nullable = true)
    private Long downloadsUnique;

    /**
     * Internally, one dataset can cite another using "Related Dataset" but we
     * expect citations to come from Crossref as well.
     */
    @Column(nullable = true)
    private Long citations;
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

    public Long getViewsTotal() {
        return viewsTotal;
    }

    public void setViewsTotal(Long viewsTotal) {
        this.viewsTotal = viewsTotal;
    }

    public Long getViewsUnique() {
        return viewsUnique;
    }

    public void setViewsUnique(Long viewsUnique) {
        this.viewsUnique = viewsUnique;
    }

    public Long getDownloadsTotal() {
        return downloadsTotal;
    }

    public void setDownloadsTotal(Long downloadsTotal) {
        this.downloadsTotal = downloadsTotal;
    }

    public Long getDownloadsUnique() {
        return downloadsUnique;
    }

    public void setDownloadsUnique(Long downloadsUnique) {
        this.downloadsUnique = downloadsUnique;
    }

    public Long getCitations() {
        return citations;
    }

    public void setCitations(Long citations) {
        this.citations = citations;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

}
