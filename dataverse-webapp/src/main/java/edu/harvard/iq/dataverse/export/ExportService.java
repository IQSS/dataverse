package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.error.DataverseError;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import io.vavr.control.Either;
import io.vavr.control.Try;
import org.apache.commons.lang.StringUtils;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


/**
 * Class responsible for managing exporters and mainly exporting.
 */
@Stateless
public class ExportService {

    private Instance<Exporter> exporters;
    private Map<ExporterType, Exporter> exportersMap = new HashMap<>();

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    ExportService() {
        //JEE requirement
    }

    @Inject
    public ExportService(Instance<Exporter> exporters) {
        this.exporters = exporters;
    }

    @PostConstruct
    void loadAllExporters() {

        exporters.iterator().forEachRemaining(exporter -> exportersMap.put(exporter.getExporterType(), exporter));

    }

    // -------------------- LOGIC --------------------

    /**
     * Exports datasetVersion with given exporter.
     *
     * @return {@code Error} if exporting failed or exporter was not found in the list of exporters.
     * <p>
     * {@code String} if exporting was a success.
     */
    public Either<DataverseError, String> exportDatasetVersionAsString(DatasetVersion datasetVersion, ExporterType exporter) {
        Optional<Exporter> loadedExporter = getExporter(exporter);

        if (loadedExporter.isPresent()) {

            String exportedDataset = Try.of(() -> loadedExporter.get()
                    .exportDataset(datasetVersion))
                    .onFailure(Throwable::printStackTrace)
                    .getOrElse(StringUtils.EMPTY);

            return exportedDataset.isEmpty() ?
                    Either.left(new DataverseError("Failed to export the dataset as " + exporter)) :
                    Either.right(exportedDataset);
        }

        return Either.left(new DataverseError(exporter + " was not found among exporter list"));
    }

    public Map<ExporterType, Exporter> getAllExporters() {
        return exportersMap;
    }

    /**
     * @return MediaType of given exporter or {@link Exporter#getMediaType()} default value.
     */
    public String getMediaType(ExporterType provider) {

        return getExporter(provider)
                .map(Exporter::getMediaType)
                .get();
    }

    // -------------------- PRIVATE --------------------

    private Optional<Exporter> getExporter(ExporterType exporter) {
        return Optional.ofNullable(exportersMap.get(exporter));
    }

}
