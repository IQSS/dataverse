package edu.harvard.iq.dataverse.dataverse.template;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.persistence.dataset.Template;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseContact;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import io.vavr.control.Try;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
@Transactional(TransactionMode.ROLLBACK)
public class TemplateServiceIT extends WebappArquillianDeployment {

    private String TEST_DATAVERSE_ALIAS = "test-dataverse-alias";

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @Inject
    private TemplateService templateService;

    @Inject
    private DataverseSession dataverseSession;

    @Inject
    private DataverseServiceBean dataverseService;

    @Before
    public void setUp() {
        createSessionUser();

        Dataverse dataverse = prepareDataverse();
        em.persist(dataverse);
    }

    @Test
    public void shouldSuccessfullyUpdateDataverseTemplate() {
        //given
        Dataverse dataverse = dataverseService.findByAlias(TEST_DATAVERSE_ALIAS);
        Template template = prepareTemplate();
        template.setDataverse(dataverse);
        dataverse.setTemplates(Collections.singletonList(template));

        em.persist(template);

        //when
        Try<Dataverse> savedDataverse = templateService.updateTemplateInheritance(dataverse, false);

        //then
        assertFalse(savedDataverse.isFailure());
        assertTrue(savedDataverse.get().isTemplateRoot());

    }

    @Test
    public void shouldSuccessfullyDeleteTemplate() {
        //given
        Dataverse dataverse = dataverseService.findByAlias(TEST_DATAVERSE_ALIAS);
        Template template = prepareTemplate();
        template.setDataverse(dataverse);

        dataverse.setTemplates(Lists.newArrayList(template));
        dataverse.setDefaultTemplate(template);

        em.persist(template);

        //when
        Try<Dataverse> affectedDataverse = templateService.deleteTemplate(dataverse, template);

        //then
        assertFalse(affectedDataverse.isFailure());
        assertTrue(affectedDataverse.get().getTemplates().isEmpty());
        assertNull(affectedDataverse.get().getDefaultTemplate());
        assertNull(em.find(Template.class, template.getId()));
    }

    @Test
    public void shouldSuccessfullyCloneTemplate() {
        //given
        Dataverse dataverse = dataverseService.findByAlias(TEST_DATAVERSE_ALIAS);
        Template template = prepareTemplate();
        template.setDataverse(dataverse);

        dataverse.setTemplates(Lists.newArrayList(template));
        dataverse.setDefaultTemplate(template);

        em.persist(template);

        //when
        Try<Template> clonedTemplate = templateService.cloneTemplate(template, dataverse);

        //then
        assertFalse(clonedTemplate.isFailure());
        assertTrue(dataverse.getTemplates().contains(clonedTemplate.get()));
        assertEquals(2, dataverse.getTemplates().size());
        assertTrue(clonedTemplate.get().getName().equalsIgnoreCase("copy of " + template.getName()));
    }

    @Test
    public void makeTemplateDefaultForDataverse() {
        //given
        Dataverse dataverse = dataverseService.findByAlias(TEST_DATAVERSE_ALIAS);
        Template template = prepareTemplate();
        template.setDataverse(dataverse);

        dataverse.setTemplates(Lists.newArrayList(template));

        em.persist(template);

        //when
        templateService.makeTemplateDefaultForDataverse(dataverse, template);

        //then
        assertEquals(dataverse.getDefaultTemplate(), template);

    }

    @Test
    public void removeDataverseDefaultTemplate() {
        //given
        Dataverse dataverse = dataverseService.findByAlias(TEST_DATAVERSE_ALIAS);
        Template template = prepareTemplate();
        template.setDataverse(dataverse);

        dataverse.setTemplates(Lists.newArrayList(template));
        dataverse.setDefaultTemplate(template);

        em.persist(template);

        //when
        templateService.removeDataverseDefaultTemplate(dataverse);

        //then
        assertNull(dataverse.getDefaultTemplate());
    }

    @Test
    public void updateDefaultTemplates_ForInheritedValue() {
        //given
        Dataverse dataverse = dataverseService.findByAlias(TEST_DATAVERSE_ALIAS);
        Dataverse rootDataverse = dataverseService.findRootDataverse();
        Template template = prepareTemplate();
        template.setDataverse(rootDataverse);
        rootDataverse.setDefaultTemplate(template);

        em.persist(template);

        //when
        templateService.updateTemplateInheritance(dataverse, true);

        //then
        assertEquals(dataverse.getDefaultTemplate(), template);

    }

    @Test
    public void updateDefaultTemplates_ForNonInheritedValue() {
        //given
        Dataverse dataverse = dataverseService.findByAlias(TEST_DATAVERSE_ALIAS);
        Dataverse rootDataverse = dataverseService.findRootDataverse();
        Template template = prepareTemplate();
        rootDataverse.setTemplates(Lists.newArrayList(template));
        template.setDataverse(rootDataverse);
        dataverse.setDefaultTemplate(template);

        em.persist(template);

        //when
        templateService.updateTemplateInheritance(dataverse, false);

        //then
        assertNull(dataverse.getDefaultTemplate());

    }

    @Test
    public void retrieveDataverseNamesWithDefaultTemplate() {
        //given
        Dataverse dataverse = dataverseService.findByAlias(TEST_DATAVERSE_ALIAS);
        Template template = prepareTemplate();
        template.setDataverse(dataverse);

        dataverse.setTemplates(Lists.newArrayList(template));
        dataverse.setDefaultTemplate(template);

        em.persist(template);
        //when
        em.flush();

        List<String> dataverseNames = templateService.retrieveDataverseNamesWithDefaultTemplate(template.getId());

        //then
        assertEquals(1, dataverseNames.size());
        assertTrue(dataverseNames.contains("test dataverse"));

    }

    // -------------------- PRIVATE --------------------

    private Dataverse prepareDataverse() {
        Dataverse dataverse = new Dataverse();
        dataverse.setOwner(dataverseService.findRootDataverse());
        dataverse.setCreateDate(new Timestamp(new Date().getTime()));
        dataverse.setModificationTime(new Timestamp(new Date().getTime()));
        dataverse.setDataverseContacts(prepareDataverseContact());
        dataverse.setDataverseType(Dataverse.DataverseType.JOURNALS);
        dataverse.setAlias(TEST_DATAVERSE_ALIAS);
        dataverse.setName("test dataverse");

        return dataverse;
    }

    private Template prepareTemplate() {
        Template template = new Template();
        template.setName("nice template");
        template.setCreateTime(new Timestamp(new Date().getTime()));

        return template;
    }

    private List<DataverseContact> prepareDataverseContact() {
        DataverseContact dataverseContact = new DataverseContact();
        dataverseContact.setContactEmail("test@gmail.com");

        ArrayList<DataverseContact> contacts = new ArrayList<>();
        contacts.add(dataverseContact);
        return contacts;
    }

    private void createSessionUser() {
        AuthenticatedUser user = createUser();
        em.persist(user);
        dataverseSession.setUser(user);
    }

    private AuthenticatedUser createUser() {
        AuthenticatedUser user = new AuthenticatedUser();
        user.setSuperuser(true);
        user.setLastName("Banan");
        user.setEmail("test@gmail.com");
        user.setUserIdentifier("TERMINATOR");
        user.setFirstName("Anakin");
        user.setCreatedTime(Timestamp.valueOf(LocalDateTime.of(2019, 1, 1, 1, 1)));
        return user;
    }
}
