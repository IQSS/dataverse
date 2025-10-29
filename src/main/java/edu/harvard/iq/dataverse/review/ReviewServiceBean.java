package edu.harvard.iq.dataverse.review;

import java.io.Serializable;
import java.util.logging.Logger;

import jakarta.ejb.Stateless;
import jakarta.inject.Named;
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
            logger.info("Saving review: " + review);
            em.persist(review);
            logger.info("Review saved: " + review);
            return review;
        } else {
            logger.info("Updating review: " + review);
            return em.merge(review);
        }
    }

}
