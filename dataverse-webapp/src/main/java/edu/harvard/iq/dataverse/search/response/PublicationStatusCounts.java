package edu.harvard.iq.dataverse.search.response;

import edu.harvard.iq.dataverse.search.query.SearchPublicationStatus;

import java.util.Objects;

/**
 * Model class that contains number of dvObjects with
 * some publication status in search response.
 * <p>
 * Be aware that sum of all counts is not equal to all
 * dvObjects as some dvObjects can have multiple publication
 * statuses.
 * 
 * @author madryk
 */
public class PublicationStatusCounts {

    private long inReviewCount;
    private long unpublishedCount;
    private long publishedCount;
    private long draftCount;
    private long deaccessionedCount;


    // -------------------- CONSTRUCTORS --------------------

    public PublicationStatusCounts(long inReviewCount, long unpublishedCount, long publishedCount, long draftCount,
            long deaccessionedCount) {
        super();
        this.inReviewCount = inReviewCount;
        this.unpublishedCount = unpublishedCount;
        this.publishedCount = publishedCount;
        this.draftCount = draftCount;
        this.deaccessionedCount = deaccessionedCount;
    }

    // -------------------- GETTERS --------------------

    public long getInReviewCount() {
        return inReviewCount;
    }

    public long getUnpublishedCount() {
        return unpublishedCount;
    }

    public long getPublishedCount() {
        return publishedCount;
    }

    public long getDraftCount() {
        return draftCount;
    }

    public long getDeaccessionedCount() {
        return deaccessionedCount;
    }

    // -------------------- LOGIC --------------------

    /**
     * Modifies count number for publication status associated with
     * the given {@link SearchPublicationStatus}.
     */
    public void setCountByPublicationStatus(SearchPublicationStatus status, long count) {
        switch (status) {
        case PUBLISHED:
            publishedCount = count;
            break;
        case UNPUBLISHED:
            unpublishedCount = count;
            break;
        case DRAFT:
            draftCount = count;
            break;
        case IN_REVIEW:
            inReviewCount = count;
            break;
        case DEACCESSIONED:
            deaccessionedCount = count;
            break;
        default:
            break;
        }
    }
    
    /**
     * Returns {@link PublicationStatusCounts} with all counts set
     * to zero.
     */
    public static PublicationStatusCounts emptyPublicationStatusCounts() {
        return new PublicationStatusCounts(0, 0, 0, 0, 0);
    }

    // -------------------- hashCode & equals --------------------

    @Override
    public int hashCode() {
        return Objects.hash(deaccessionedCount, draftCount, inReviewCount, publishedCount, unpublishedCount);
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PublicationStatusCounts other = (PublicationStatusCounts) obj;
        return deaccessionedCount == other.deaccessionedCount && draftCount == other.draftCount
                && inReviewCount == other.inReviewCount && publishedCount == other.publishedCount
                && unpublishedCount == other.unpublishedCount;
    }

    // -------------------- toString --------------------

    @Override
    public String toString() {
        return "PublicationStatusCounts [inReviewCount=" + inReviewCount + ", unpublishedCount=" + unpublishedCount
                + ", publishedCount=" + publishedCount + ", draftCount=" + draftCount + ", deaccessionedCount="
                + deaccessionedCount + "]";
    }

}
