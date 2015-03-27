package edu.harvard.iq.dataverse.search.savedsearch;

import edu.harvard.iq.dataverse.Dataverse;
import java.io.Serializable;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

@Entity
public class SavedSearch implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Must be non-null but a "*" (wildcard) can be used.
     *
     * https://wiki.apache.org/solr/CommonQueryParameters#q
     */
    @Column(nullable = false)
    private String query;

    /**
     * "fq" stands for Filter Query:
     * https://wiki.apache.org/solr/CommonQueryParameters#fq
     */
    @OneToMany(mappedBy = "savedSearch", cascade = CascadeType.ALL)
    private List<SavedSearchFilterQuery> savedSearchFilterQueries;

    /**
     * Saved searches are associated with a Dataverse, not a user.
     */
    @JoinColumn(nullable = false)
    private Dataverse definitionPoint;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Dataverse getDefinitionPoint() {
        return definitionPoint;
    }

    public void setDefinitionPoint(Dataverse definitionPoint) {
        this.definitionPoint = definitionPoint;
    }

    public List<SavedSearchFilterQuery> getSavedSearchFilterQueries() {
        return savedSearchFilterQueries;
    }

    public void setSavedSearchFilterQueries(List<SavedSearchFilterQuery> savedSearchFilterQueries) {
        this.savedSearchFilterQueries = savedSearchFilterQueries;
    }

}
