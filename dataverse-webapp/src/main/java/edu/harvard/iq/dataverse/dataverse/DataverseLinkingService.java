package edu.harvard.iq.dataverse.dataverse;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.annotations.PermissionNeeded;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.interceptors.LoggedCall;
import edu.harvard.iq.dataverse.interceptors.Restricted;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.link.DataverseLinkingDataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.link.DataverseLinkingDataverseRepository;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.search.index.IndexServiceBean;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.inject.Inject;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

/**
 * @author skraffmiller
 */
@Stateless
public class DataverseLinkingService implements java.io.Serializable {

    @Inject
    private DataverseLinkingDataverseRepository dataverseLinkingDataverseRepository;
    @Inject
    private IndexServiceBean indexService;
    @Inject
    private DataverseSession session;

    // -------------------- LOGIC --------------------

    public List<Dataverse> findLinkedDataverses(Long linkingDataverseId) {
        return dataverseLinkingDataverseRepository.findByLinkingDataverseId(linkingDataverseId).stream()
                .map(DataverseLinkingDataverse::getDataverse)
                .collect(toList());
    }

    public List<Dataverse> findLinkingDataverses(Long dataverseId) {
        return dataverseLinkingDataverseRepository.findByDataverseId(dataverseId).stream()
                .map(DataverseLinkingDataverse::getLinkingDataverse)
                .collect(toList());
    }

    public DataverseLinkingDataverse findDataverseLinkingDataverse(Long dataverseId, Long linkingDataverseId) {
        return dataverseLinkingDataverseRepository.findByDataverseIdAndLinkingDataverseId(dataverseId, linkingDataverseId)
                .orElse(null);
    }

    public boolean alreadyLinked(Dataverse definitionPoint, Dataverse dataverseToLinkTo) {
        return dataverseLinkingDataverseRepository.findByDataverseIdAndLinkingDataverseId(dataverseToLinkTo.getId(), definitionPoint.getId())
                .isPresent();
    }

    /**
     * Operation to link one dataverse to the other.
     */
    @LoggedCall
    @Restricted(@PermissionNeeded(needs = {Permission.PublishDataset}))
    @TransactionAttribute(REQUIRES_NEW)
    public DataverseLinkingDataverse saveLinkedDataverse(
            @PermissionNeeded Dataverse dataverseToBeLinked, Dataverse dataverse) {

        if (!session.getUser().isSuperuser()) {
            throw new PermissionException("Link Dataverse can only be called by superusers.",
                                          Collections.singleton(Permission.PublishDataverse), dataverseToBeLinked);
        }
        if (dataverse.equals(dataverseToBeLinked)) {
            throw new IllegalCommandException("Can't link a dataverse to itself");
        }
        if (dataverse.getOwners().contains(dataverseToBeLinked)) {
            throw new IllegalCommandException("Can't link a dataverse to its parents");
        }

        DataverseLinkingDataverse dataverseLinkingDataverse = new DataverseLinkingDataverse();
        dataverseLinkingDataverse.setDataverse(dataverse);
        dataverseLinkingDataverse.setLinkingDataverse(dataverseToBeLinked);
        dataverseLinkingDataverse.setLinkCreateTime(new Timestamp(new Date().getTime()));
        dataverseLinkingDataverse = dataverseLinkingDataverseRepository.save(dataverseLinkingDataverse);
        indexService.indexDataverse(dataverse);
        return dataverseLinkingDataverse;
    }
}
