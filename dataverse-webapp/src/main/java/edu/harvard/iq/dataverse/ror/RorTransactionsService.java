package edu.harvard.iq.dataverse.ror;

import edu.harvard.iq.dataverse.persistence.ror.RorData;
import edu.harvard.iq.dataverse.persistence.ror.RorDataRepository;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.Set;

/**
 * Class dedicated only for ability to control transactions.
 */
@Stateless
public class RorTransactionsService {

    private RorDataRepository rorRepository;

    // -------------------- CONSTRUCTORS --------------------

    public RorTransactionsService() { }

    @Inject
    public RorTransactionsService(RorDataRepository rorRepository) {
        this.rorRepository = rorRepository;
    }

    // -------------------- PUBLIC --------------------

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void truncateAll() {
        rorRepository.truncateAll();
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void saveMany(Set<RorData> entities) {
        entities.forEach(e -> rorRepository.save(e));
    }
}
