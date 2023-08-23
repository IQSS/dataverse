package edu.harvard.iq.dataverse.search.savedsearch;

import java.io.Serializable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(indexes = {@Index(columnList="savedsearch_id")})
public class SavedSearchFilterQuery implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String filterQuery;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(nullable = false)
    private SavedSearch savedSearch;

    /**
     * This default constructor is only here to prevent this error at
     * deployment:
     *
     * The instance creation method
     * [edu.harvard.iq.dataverse.search.savedsearch.SavedSearchFilterQuery.<Default Constructor>],
     * with no parameters, does not exist, or is not accessible.
     *
     * Don't use it.
     */
    @Deprecated
    public SavedSearchFilterQuery() {
    }

    public SavedSearchFilterQuery(String filterQuery, SavedSearch savedSearch) {
        this.filterQuery = filterQuery;
        this.savedSearch = savedSearch;
    }

    @Override
    public String toString() {
        return "SavedSearchFilterQuery{" + "id=" + id + ", filterQuery=" + filterQuery + '}';
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFilterQuery() {
        return filterQuery;
    }

    public void setFilterQuery(String filterQuery) {
        this.filterQuery = filterQuery;
    }

    public SavedSearch getSavedSearch() {
        return savedSearch;
    }

    public void setSavedSearch(SavedSearch savedSearch) {
        this.savedSearch = savedSearch;
    }

}
