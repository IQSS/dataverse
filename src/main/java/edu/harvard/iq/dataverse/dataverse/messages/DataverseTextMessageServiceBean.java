package edu.harvard.iq.dataverse.dataverse.messages;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseLocaleBean;
import edu.harvard.iq.dataverse.dataverse.messages.dto.DataverseMessagesMapper;
import edu.harvard.iq.dataverse.dataverse.messages.dto.DataverseTextMessageDto;

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

    DataverseTextMessageServiceBean() {
    }

    DataverseTextMessageServiceBean(EntityManager em, DataverseMessagesMapper mapper) {
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
        // TODO: validate

        DataverseTextMessage textMessage = new DataverseTextMessage();
        textMessage.setActive(messageDto.isActive());
        textMessage.setDataverse(em.find(Dataverse.class, messageDto.getDataverseId()));
        textMessage.setFromTime(messageDto.getFromTime());
        textMessage.setToTime(messageDto.getToTime());

        messageDto.getDataverseLocalizedMessage().forEach(lm -> {
            textMessage.addLocalizedMessage(lm.getLocale(), lm.getMessage());
        });

        em.persist(textMessage);
    }

    public List<String> getTextMessagesForDataverse(Long dataverseId) {
        logger.info("Getting text messages for dataverse: " + dataverseId);
        DataverseLocaleBean locale = new DataverseLocaleBean();
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
                .setParameter(1, locale.getLocaleCode())
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
}
