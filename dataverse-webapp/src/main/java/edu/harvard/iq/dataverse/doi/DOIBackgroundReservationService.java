package edu.harvard.iq.dataverse.doi;

import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.globalid.DOIDataCiteServiceBean;
import edu.harvard.iq.dataverse.persistence.GlobalId;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.search.index.IndexServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.apache.commons.lang.math.NumberUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.DependsOn;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class designed to reserve doi's in the background.
 */
@Startup
@Singleton
@DependsOn("StartupFlywayMigrator")
public class DOIBackgroundReservationService {

    private static final Logger logger = Logger.getLogger(DOIBackgroundReservationService.class.getCanonicalName());

    private SettingsServiceBean settingsServiceBean;
    private DatasetRepository datasetRepository;
    private DatasetDao datasetDao;
    private DOIDataCiteServiceBean doiDataCiteService;
    private IndexServiceBean indexServiceBean;

    private final Timer timer = new Timer();

    public DOIBackgroundReservationService() {
    }

    @Inject
    public DOIBackgroundReservationService(SettingsServiceBean settingsServiceBean, DatasetRepository datasetRepository,
                                           DatasetDao datasetDao, DOIDataCiteServiceBean doiDataCiteService,
                                           IndexServiceBean indexServiceBean) {
        this.settingsServiceBean = settingsServiceBean;
        this.datasetRepository = datasetRepository;
        this.datasetDao = datasetDao;
        this.doiDataCiteService = doiDataCiteService;
        this.indexServiceBean = indexServiceBean;
    }

    @PostConstruct
    void startReservation() {
        reserveDoiPeriodically(timer);
    }

    @PreDestroy
    void preDestroy() {
        timer.cancel();
    }

    /**
     * Creates a timer which will reserve doi's in interval provided by 'DoiBackgroundReservationInterval'.
     * Run method has to be wrapped in Try otherwise Transaction rollback could destroy timer setup.
     */
    void reserveDoiPeriodically(Timer timer) {
        String provider = settingsServiceBean.getValueForKey(SettingsServiceBean.Key.DoiProvider);

        if (provider.equals("DataCite")) {

            Option<Long> intervalInMs = Option
                    .of(settingsServiceBean.getValueForKey(SettingsServiceBean.Key.DoiBackgroundReservationInterval))
                    .filter(NumberUtils::isNumber)
                    .map(Long::parseLong);

            if (intervalInMs.isEmpty() || intervalInMs.get() < 0) {
                return;
            }

                timer.schedule(new TimerTask() {
                                   @Override
                                   public void run() {
                                       Try.run(() -> registerDataCiteIdentifier());
                                   }
                               }
                        , 0, intervalInMs.get());

        }

    }

    void registerDataCiteIdentifier() {
        List<Dataset> nonReservedDatasets = datasetRepository.findByNonRegisteredIdentifier();

        for (Dataset nonReservedDataset : nonReservedDatasets) {
            int attempts = 0;

            GlobalId globalId = nonReservedDataset.getGlobalId();

            while (doiDataCiteService.alreadyExists(globalId) && attempts < 10) {
                globalId = new GlobalId(nonReservedDataset.getProtocol(),
                                        nonReservedDataset.getAuthority(),
                                        datasetDao.generateDatasetIdentifier(nonReservedDataset));
                attempts++;
            }

            Dataset refreshedDataset = datasetRepository.save(nonReservedDataset);
            refreshedDataset.setIdentifier(globalId.getIdentifier());

            Try.of(() -> doiDataCiteService.createIdentifier(refreshedDataset))
               .onFailure(throwable -> logger.log(Level.INFO, "Identifier could not be reserved", throwable))
               .onSuccess(s -> {
                   refreshedDataset.setGlobalIdCreateTime(new Timestamp(new Date().getTime()));
                   refreshedDataset.setIdentifierRegistered(true);
                   datasetRepository.save(refreshedDataset);
                   indexServiceBean.asyncIndexDataset(refreshedDataset, false);
               });
        }
    }
}
