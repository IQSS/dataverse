package edu.harvard.iq.dataverse.harvest.client;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
@Transactional(TransactionMode.ROLLBACK)
public class HarvestingClientsServiceIT extends WebappArquillianDeployment {
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @Inject
    private HarvestingClientsService harvestingClientsService;
    @Inject
    private HarvestingClientDao harvestingClientService;
    @Inject
    private DataverseSession dataverseSession;
    @EJB
    private AuthenticationServiceBean authenticationServiceBean;
    @Inject
    private DataverseDao dataverseDao;
    @Inject
    private HarvestingClientDao harvestingClientDao;

    @Before
    public void setUp() {
        dataverseSession.setUser(authenticationServiceBean.getAdminUser());
    }

    @Test
    public void shouldCreateHarvestingClient() {
        // given
        HarvestingClient newHarvestingClient = createHarvestingClient();

        // when
        harvestingClientsService.createHarvestingClient(newHarvestingClient);

        // then
        HarvestingClient dbHarvestingClient = harvestingClientService.findByNickname("newNickname");
        assertNotNull(dbHarvestingClient);

        assertEquals(dataverseDao.findByAlias("ownmetadatablocks").getId(), dbHarvestingClient.getDataverse().getId());
        assertEquals("http://localhost:8080/oai", dbHarvestingClient.getHarvestingUrl());
        assertEquals("newOaiSet", dbHarvestingClient.getHarvestingSet());
        assertEquals("newMetadataFormat", dbHarvestingClient.getMetadataPrefix());
        assertEquals("newHarvestingStyle", dbHarvestingClient.getHarvestStyle());
        assertTrue(dbHarvestingClient.isScheduled());
        assertEquals(HarvestingClient.SCHEDULE_PERIOD_WEEKLY, dbHarvestingClient.getSchedulePeriod());
        assertEquals(7, dbHarvestingClient.getScheduleDayOfWeek().intValue());
        assertEquals(12, dbHarvestingClient.getScheduleHourOfDay().intValue());
    }

    @Test
    public void shouldUpdateHarvestingClient() {
        // given
        HarvestingClient newHarvestingClient = createHarvestingClient();
        em.persist(newHarvestingClient);

        // when
        newHarvestingClient.setName("updatedNickname");
        newHarvestingClient.setHarvestingSet("updatedOaiSet");
        newHarvestingClient.setSchedulePeriod(HarvestingClient.SCHEDULE_PERIOD_DAILY);

        harvestingClientsService.updateHarvestingClient(newHarvestingClient);

        // then
        HarvestingClient dbHarvestingClient = harvestingClientService.findByNickname("updatedNickname");
        assertNotNull(dbHarvestingClient);

        assertEquals(dataverseDao.findByAlias("ownmetadatablocks").getId(), dbHarvestingClient.getDataverse().getId());
        assertEquals("http://localhost:8080/oai", dbHarvestingClient.getHarvestingUrl());
        assertEquals("updatedOaiSet", dbHarvestingClient.getHarvestingSet());
        assertEquals("newMetadataFormat", dbHarvestingClient.getMetadataPrefix());
        assertEquals("newHarvestingStyle", dbHarvestingClient.getHarvestStyle());
        assertTrue(dbHarvestingClient.isScheduled());
        assertEquals(HarvestingClient.SCHEDULE_PERIOD_DAILY, dbHarvestingClient.getSchedulePeriod());
        assertEquals(7, dbHarvestingClient.getScheduleDayOfWeek().intValue());
        assertEquals(12, dbHarvestingClient.getScheduleHourOfDay().intValue());
    }

    // -------------------- PRIVATE ---------------------
    private HarvestingClient createHarvestingClient() {
        HarvestingClient newHarvestingClient = new HarvestingClient();
        newHarvestingClient.setName("newNickname");
        newHarvestingClient.setDataverse(dataverseDao.findByAlias("ownmetadatablocks"));
        newHarvestingClient.setHarvestingUrl("http://localhost:8080/oai");
        newHarvestingClient.setHarvestingSet("newOaiSet");
        newHarvestingClient.setMetadataPrefix("newMetadataFormat");
        newHarvestingClient.setHarvestStyle("newHarvestingStyle");
        newHarvestingClient.setScheduled(true);
        newHarvestingClient.setSchedulePeriod(HarvestingClient.SCHEDULE_PERIOD_WEEKLY);
        newHarvestingClient.setScheduleDayOfWeek(7);
        newHarvestingClient.setScheduleHourOfDay(12);
        return newHarvestingClient;
    }
}
