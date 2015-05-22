package edu.harvard.iq.dataverse.search.savedsearch;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(indexes = {@Index(columnList="definitionpoint_id")
		, @Index(columnList="creator_id")})
public class SavedSearch implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Must be non-null but a "*" (wildcard) can be used.
     *
     * https://wiki.apache.org/solr/CommonQueryParameters#q
     */
    @Column(nullable = false, columnDefinition = "TEXT")
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

    @JoinColumn(nullable = false)
    private AuthenticatedUser creator;

    public List<String> getFilterQueriesAsStrings() {
        List<String> filterQueries = new ArrayList<>();
        for (SavedSearchFilterQuery filterQueryToAdd : getSavedSearchFilterQueries()) {
            filterQueries.add(filterQueryToAdd.getFilterQuery());
        }
        return filterQueries;
    }

    /**
     * This default constructor is only here to prevent this error at
     * deployment:
     *
     * Exception Description: The instance creation method
     * [edu.harvard.iq.dataverse.search.savedsearch.SavedSearch.<Default Constructor>],
     * with no parameters, does not exist, or is not accessible
     *
     * Don't use it.
     */
    @Deprecated
    public SavedSearch() {
    }

    public SavedSearch(String query, Dataverse definitionPoint, AuthenticatedUser creator) {
        this.query = query;
        this.definitionPoint = definitionPoint;
        this.creator = creator;
    }

    @Override
    public String toString() {
        return "SavedSearch{" + "id=" + id + ", query=" + query + ", savedSearchFilterQueries=" + savedSearchFilterQueries + ", definitionPoint=" + definitionPoint + '}';
    }

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

    public AuthenticatedUser getCreator() {
        return creator;
    }

    public void setCreator(AuthenticatedUser creator) {
        this.creator = creator;
    }

    public List<SavedSearchFilterQuery> getSavedSearchFilterQueries() {
        return savedSearchFilterQueries;
    }

    public void setSavedSearchFilterQueries(List<SavedSearchFilterQuery> savedSearchFilterQueries) {
        this.savedSearchFilterQueries = savedSearchFilterQueries;
    }

}
