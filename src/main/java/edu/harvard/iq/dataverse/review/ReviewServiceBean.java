package edu.harvard.iq.dataverse.review;

import java.io.Serializable;
import java.util.List;
import java.util.logging.Logger;

import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.json.JsonObjectBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

@Stateless
@Named
public class ReviewServiceBean implements Serializable {

    private static final Logger logger = Logger.getLogger(ReviewServiceBean.class.getCanonicalName());

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public List<Review> findAll() {
        TypedQuery<Review> typedQuery = em.createQuery("SELECT OBJECT(r) FROM Review AS r", Review.class);
        return typedQuery.getResultList();
    }

    public Review find(long id) {
        TypedQuery<Review> typedQuery = em.createQuery("SELECT OBJECT(r) FROM Review AS r WHERE r.id = :id",
                Review.class);
        typedQuery.setParameter("id", id);
        try {
            return typedQuery.getSingleResult();
        } catch (NoResultException | NonUniqueResultException ex) {
            return null;
        }
    }

    public Review save(Review review) {
        if (review.getId() == null) {
            em.persist(review);
            return review;
        } else {
            return em.merge(review);
        }
    }

    // TODO consider moving to JsonPrinter
    public static JsonObjectBuilder toJson(Review review) {
        JsonObjectBuilder jsonObjectBuilder = new NullSafeJsonBuilder();
        if (review != null) {
            jsonObjectBuilder.add("toString", review.toString());
        }
        return jsonObjectBuilder;
    }

}
