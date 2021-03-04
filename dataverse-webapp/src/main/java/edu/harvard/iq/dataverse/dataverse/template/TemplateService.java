package edu.harvard.iq.dataverse.dataverse.template;

import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.engine.command.impl.CreateTemplateCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteTemplateCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseTemplateCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseTemplateRootCommand;
import edu.harvard.iq.dataverse.persistence.dataset.Template;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import io.vavr.control.Try;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class TemplateService {

    private static final Logger logger = Logger.getLogger(TemplateService.class.getCanonicalName());

    private EjbDataverseEngine engineService;
    private DataverseRequestServiceBean dvRequestService;
    private TemplateDao templateDao;

    private Clock clock = Clock.systemDefaultZone();

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public TemplateService() {
    }

    @Inject
    public TemplateService(EjbDataverseEngine engineService, DataverseRequestServiceBean dvRequestService,
                           TemplateDao templateDao) {
        this.engineService = engineService;
        this.dvRequestService = dvRequestService;
        this.templateDao = templateDao;
    }

    // -------------------- LOGIC --------------------

    public Try<Template> createTemplate(Dataverse dataverse, Template template) {
        template.setCreateTime(Timestamp.valueOf(LocalDateTime.now(clock)));
        template.setUsageCount(0L);
        dataverse.getTemplates().add(template);

        return Try.of(() -> engineService.submit(new CreateTemplateCommand(template, dvRequestService.getDataverseRequest(), dataverse)))
                .onFailure(throwable -> logger.log(Level.SEVERE, null, throwable));
    }

    public Try<Template> updateTemplate(Dataverse dataverse, Template template) {
        return Try.of(() -> engineService.submit(new UpdateDataverseTemplateCommand(dataverse, template, dvRequestService.getDataverseRequest())))
                .onFailure(throwable -> logger.log(Level.SEVERE, null, throwable));
    }

    /**
     * Updates dataverse regarding if it is a templateRoot or not.
     */
    public Try<Dataverse> updateTemplateInheritance(Dataverse dataverse, boolean inheritTemplatesValue) {

        updateDataverseTemplates(inheritTemplatesValue, dataverse);

        return Try.of(() -> engineService.submit(new UpdateDataverseTemplateRootCommand(!inheritTemplatesValue, dvRequestService.getDataverseRequest(), dataverse)))
                .onFailure(throwable -> logger.log(Level.SEVERE, null, throwable));
    }

    public Try<Dataverse> deleteTemplate(Dataverse dataverse, Template templateToDelete) {

        if (dataverse.getDefaultTemplate() != null && dataverse.getDefaultTemplate().equals(templateToDelete)) {
            dataverse.setDefaultTemplate(null);
        }

        dataverse.getTemplates().remove(templateToDelete);

        return Try.of(() -> engineService.submit(new DeleteTemplateCommand(dvRequestService.getDataverseRequest(),
                dataverse, templateToDelete, templateDao.findDataversesByDefaultTemplateId(templateToDelete.getId()))))
                .onFailure(throwable -> logger.log(Level.SEVERE, throwable.getMessage(), throwable));
    }

    /**
     * Creates a copy of the given template and returns that copy.
     * <p>In order to clone a template within a dataverse use
     * this method and pass the result to {@link #mergeIntoDataverse}.
     */
    public Template copyTemplate(Template toCopy) {
        Template copy = toCopy.cloneNewTemplate();
        copy.setName(BundleUtil.getStringFromBundle("page.copy") + " " + toCopy.getName());
        copy.setUsageCount(0L);
        copy.setCreateTime(Timestamp.valueOf(LocalDateTime.now(clock)));

        return copy;
    }

    /**
     * Merges the given template into the given dataverse.
     * <p>Could be used with {@link #copyTemplate} in order to clone
     * a template within a dataverse.
     */
    public Try<Template> mergeIntoDataverse(Dataverse dataverse, Template toMerge) {
        toMerge.setDataverse(dataverse);
        dataverse.getTemplates().add(toMerge);

        Try<Template> createdTemplate = Try.of(
                () -> engineService.submit(new CreateTemplateCommand(toMerge, dvRequestService.getDataverseRequest(), dataverse)))
                .onFailure(t -> logger.log(Level.SEVERE, t.getMessage(), t));

        updateDataverse(dataverse);
        return createdTemplate;
    }

    public Try<Dataverse> makeTemplateDefaultForDataverse(Dataverse dataverse, Template template) {
        dataverse.setDefaultTemplate(template);

        return updateDataverse(dataverse);
    }

    public Try<Dataverse> removeDataverseDefaultTemplate(Dataverse dataverse) {
        dataverse.setDefaultTemplate(null);

        return updateDataverse(dataverse);
    }

    public List<String> retrieveDataverseNamesWithDefaultTemplate(long templateId) {
        return templateDao.findDataverseNamesByDefaultTemplateId(templateId);
    }

    // -------------------- PRIVATE --------------------

    /**
     * Cleans up dataverse default templates regarding parent inheritance.
     * <p/>
     * Inheritance = true - if current dataverse didn't have default template but parent did,
     * the parent's default template is now also current dataverse default template.
     * <p/>
     * Inheritance = false - if current dataverse had default template and it was parent's template, dataverse won't have default template anymore.
     */
    private void updateDataverseTemplates(boolean isInheritTemplatesValue, Dataverse dataverse) {
        if (isInheritTemplatesValue && !isDataverseHasDefaultTemplate(dataverse) && isDataverseHasDefaultTemplate(dataverse.getOwner())) {
            dataverse.setDefaultTemplate(dataverse.getOwner().getDefaultTemplate());
        }

        if (!isInheritTemplatesValue && isDataverseHasDefaultTemplate(dataverse)) {
            dataverse.getParentTemplates().stream()
                    .filter(template -> template.equals(dataverse.getDefaultTemplate()))
                    .findAny()
                    .ifPresent(template -> dataverse.setDefaultTemplate(null));
        }
    }

    private boolean isDataverseHasDefaultTemplate(Dataverse dataverse) {
        return dataverse.getDefaultTemplate() != null;
    }

    private Try<Dataverse> updateDataverse(Dataverse dataverse) {
        return Try.of(() -> engineService.submit(new UpdateDataverseCommand(dataverse, null, null,
                                                                            dvRequestService.getDataverseRequest(), null)));
    }

    // -------------------- SETTERS --------------------

    public void setClock(Clock clock) {
        this.clock = clock;
    }
}
