
package edu.harvard.iq.dataverse.harvest.server.xoai;

import com.lyncode.xoai.dataprovider.handlers.helpers.ResumptionTokenHelper;
import com.lyncode.xoai.model.oaipmh.ResumptionToken;
import static java.lang.Math.round;
import static com.google.common.base.Predicates.isNull;

/**
 *
 * @author Leonid Andreev
 * Dataverse's own version of the XOAI ResumptionTokenHelper
 * Fixes the issue with the offset cursor: the OAI validation spec 
 * insists that it starts with 0, while the XOAI implementation uses 1 
 * as the initial offset. 
 */
public class XresumptionTokenHelper {
        
    private ResumptionToken.Value current;
    private long maxPerPage;
    private Long totalResults;

    public XresumptionTokenHelper(ResumptionToken.Value current, long maxPerPage) {
        this.current = current;
        this.maxPerPage = maxPerPage;
    }

    public XresumptionTokenHelper withTotalResults(long totalResults) {
        this.totalResults = totalResults;
        return this;
    }

    public ResumptionToken resolve (boolean hasMoreResults) {
        if (isInitialOffset() && !hasMoreResults) return null;
        else {
            if (hasMoreResults) {
                ResumptionToken.Value next = current.next(maxPerPage);
                return populate(new ResumptionToken(next));
            } else {
                ResumptionToken resumptionToken = new ResumptionToken();
                resumptionToken.withCursor(round((current.getOffset()) / maxPerPage));
                if (totalResults != null)
                    resumptionToken.withCompleteListSize(totalResults);
                return resumptionToken;
            }
        }
    }

    private boolean isInitialOffset() {
        return isNull().apply(current.getOffset()) || current.getOffset() == 0;
    }

    private ResumptionToken populate(ResumptionToken resumptionToken) {
        if (totalResults != null)
            resumptionToken.withCompleteListSize(totalResults);
        resumptionToken.withCursor(round((resumptionToken.getValue().getOffset() - maxPerPage)/ maxPerPage));
        return resumptionToken;
    }
    
    
}
