package edu.harvard.iq.dataverse.bannersandmessages.messages;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.bannersandmessages.messages.dto.DataverseMessagesMapper;
import edu.harvard.iq.dataverse.bannersandmessages.messages.dto.DataverseTextMessageDto;
import edu.harvard.iq.dataverse.bannersandmessages.validation.DataverseTextMessageValidator;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author tjanek
 */
@Stateless
@Named
public class DataverseTextMessageServiceBean implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(DataverseTextMessageServiceBean.class.getCanonicalName());

    @Inject
    private DataverseMessagesMapper mapper;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    
    public DataverseTextMessageServiceBean() {
    }

    public DataverseTextMessageServiceBean(EntityManager em, DataverseMessagesMapper mapper) {
        this.em = em;
        this.mapper = mapper;
    }

    public DataverseTextMessageDto newTextMessage(Long dataverseId) {
        return mapper.mapToNewTextMessage(dataverseId);
    }

    public DataverseTextMessageDto getTextMessage(Long textMessageId) {
        DataverseTextMessage textMessage = em.find(DataverseTextMessage.class, textMessageId);
        return mapper.mapToDto(textMessage);
    }

    public void deactivate(Long textMessageId) {
        DataverseTextMessage textMessage = em.find(DataverseTextMessage.class, textMessageId);
        textMessage.deactivate();
        em.merge(textMessage);
    }

    public void delete(Long textMessageId) {
        DataverseTextMessage textMessage = em.find(DataverseTextMessage.class, textMessageId);
        em.remove(textMessage);
    }

    public void save(DataverseTextMessageDto messageDto) {

        DataverseTextMessageValidator.validateEndDate(messageDto.getFromTime(), messageDto.getToTime());

        DataverseTextMessage textMessage = new DataverseTextMessage();

        textMessage.setActive(messageDto.isActive());
        Dataverse dataverse = em.find(Dataverse.class, messageDto.getDataverseId());
        if (dataverse == null) {
            throw new IllegalArgumentException("Dataverse doesn't exist:" + messageDto.getDataverseId());
        }
        textMessage.setDataverse(dataverse);
        textMessage.setFromTime(messageDto.getFromTime());
        textMessage.setToTime(messageDto.getToTime());

        textMessage.getDataverseLocalizedMessages().clear();
        messageDto.getDataverseLocalizedMessage().forEach(lm -> {
            textMessage.addLocalizedMessage(lm.getLocale(), lm.getMessage());
        });

        em.merge(textMessage);
    }

    public List<String> getTextMessagesForDataverse(Long dataverseId, String localeCode) {
        if (dataverseId == null) {
            return Lists.newArrayList();
        }
        logger.info("Getting text messages for dataverse: " + dataverseId);
        List<String> messages = em.createNativeQuery("select r.message from (select distinct dvtml.message, dvtm.totime  from\n" +
                "  dataversetextmessage dvtm\n" +
                "  join dataverselocalizedmessage dvtml on dvtml.dataversetextmessage_id = dvtm.id\n" +
                "  where\n" +
                "    dvtm.active = true and\n" +
                "    dvtml.locale = ? and\n" +
                "    ? between dvtm.fromtime and dvtm.totime and\n" +
                "    dvtm.dataverse_id in (with recursive dv_roots as (\n" +
                "    select\n" +
                "        dv.id,\n" +
                "        dv.owner_id,\n" +
                "        d2.allowmessagesbanners\n" +
                "    from dvobject dv\n" +
                "      join dataverse d2 on dv.id = d2.id\n" +
                "    where\n" +
                "        dv.id = ?\n" +
                "        union all\n" +
                "        select\n" +
                "               dv2.id,\n" +
                "               dv2.owner_id,\n" +
                "               d2.allowmessagesbanners\n" +
                "        from dvobject dv2\n" +
                "               join dataverse d2 on dv2.id = d2.id\n" +
                "               join dv_roots on dv_roots.owner_id = dv2.id\n" +
                "    )\n" +
                "    select id from dv_roots dr where dr.allowmessagesbanners = true) order by dvtm.totime asc) r")
                .setParameter(1, localeCode)
                .setParameter(2, LocalDateTime.now())
                .setParameter(3, dataverseId)
                .getResultList();
        return messages;
    }

    public List<DataverseTextMessage> fetchAllTextMessagesForDataverse(long dataverseId) {
        return em.createQuery("select dtm FROM DataverseTextMessage as dtm " +
                "join fetch DataverseLocalizedMessage " +
                "where dtm.dataverse.id = :dataverseid")
                .setParameter("dataverseid", dataverseId)
                .getResultList();
    }

    /**
     * Fetches history of messages for dataverse with paging
     * (paging is offset based so it will not offer the best performance if there will be a lot of records)
     *
     * @param dataverseId
     * @param firstResult
     * @param maxResult
     * @return List<DataverseTextMessage>
     */
    public List<DataverseTextMessage> fetchTextMessagesForDataverseWithPaging(long dataverseId, int firstResult, int maxResult) {
        return em.createQuery("select dtm FROM DataverseTextMessage as dtm " +
                "join fetch DataverseLocalizedMessage  " +
                "where dtm.dataverse.id = :dataverseid order by dtm.id DESC ")
                .setParameter("dataverseid", dataverseId)
                .setFirstResult(firstResult)
                .setMaxResults(maxResult)
                .getResultList();
    }

    public Long countMessagesForDataverse(long dataverseId) {
        return em.createQuery("select count(dtm.id) FROM DataverseTextMessage as dtm " +
                "where dtm.dataverse.id = :dataverseid", Long.class)
                .setParameter("dataverseid", dataverseId)
                .getSingleResult();
    }
}
