package edu.harvard.iq.dataverse.makedatacount;

import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.util.List;

@Named
@Stateless
public class MakeDataCountProcessStateServiceBean {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    protected EntityManager em;

    public MakeDataCountProcessState getMakeDataCountProcessState(String yearMonth) {
        validateYearMonth(yearMonth);
        MakeDataCountProcessState mdcps = null;
        String queryStr = "SELECT d FROM MakeDataCountProcessState d WHERE d.yearMonth = '" + yearMonth + "' ";
        Query query = em.createQuery(queryStr);
        List resultList = query.getResultList();
        if (resultList.size() > 1) {
            throw new EJBException("More than one MakeDataCount Process State record found for YearMonth " + yearMonth + ".");
        }
        if (resultList.size() == 1) {
            mdcps = (MakeDataCountProcessState) resultList.get(0);
        }
        return mdcps;
    }

    public MakeDataCountProcessState setMakeDataCountProcessState(String yearMonth, String state) {
        MakeDataCountProcessState mdcps = getMakeDataCountProcessState(yearMonth);
        if (mdcps == null) {
            mdcps = new MakeDataCountProcessState(yearMonth, state);
        } else {
            mdcps.setState(state);
        }
        return em.merge(mdcps);
    }

    public boolean deleteMakeDataCountProcessState(String yearMonth) {
        MakeDataCountProcessState mdcps = getMakeDataCountProcessState(yearMonth);
        if (mdcps == null) {
            return false;
        } else {
            em.remove(mdcps);
            em.flush();
            return true;
        }
    }

    private void validateYearMonth(String yearMonth) {
        // Check yearMonth format. either yyyy-mm or yyyy-mm-dd
        if (yearMonth == null || (!yearMonth.matches("\\d{4}-\\d{2}") && !yearMonth.matches("\\d{4}-\\d{2}-\\d{2}"))) {
            throw new IllegalArgumentException("YEAR-MONTH date format must be either yyyy-mm or yyyy-mm-dd");
        }
    }
}
