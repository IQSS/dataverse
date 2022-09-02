package edu.harvard.iq.dataverse.persistence.datafile;

import edu.harvard.iq.dataverse.persistence.JpaRepository;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalTool.Type;

import javax.ejb.Stateless;
import javax.persistence.TypedQuery;
import java.util.List;

@Stateless
public class ExternalToolRepository extends JpaRepository<Long, ExternalTool> {

    // -------------------- CONSTRUCTORS --------------------

    public ExternalToolRepository() {
        super(ExternalTool.class);
    }

    // -------------------- LOGIC --------------------

    public List<ExternalTool> findByType(Type type, String contentType) {

        // If contentType==null, get all tools of the given ExternalTool.Type
        TypedQuery<ExternalTool> typedQuery = em.createQuery(
                "SELECT OBJECT(o) FROM ExternalTool AS o WHERE o.type = :type"
                        + (contentType != null ? " AND o.contentType = :contentType" : ""),
                ExternalTool.class);
        typedQuery.setParameter("type", type);
        if (contentType != null) {
            typedQuery.setParameter("contentType", contentType);
        }
        return typedQuery.getResultList();
    }
}
