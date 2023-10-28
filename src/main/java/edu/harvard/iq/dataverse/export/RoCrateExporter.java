package edu.harvard.iq.dataverse.export;

import com.google.auto.service.AutoService;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.rocrate.RoCrateManager;
import edu.harvard.iq.dataverse.util.BundleUtil;
import io.gdcc.spi.export.ExportDataProvider;
import io.gdcc.spi.export.ExportException;
import io.gdcc.spi.export.Exporter;
import jakarta.ws.rs.core.MediaType;

import java.io.FileInputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Exports the dataset metadata as an RO-Crate JSON using the metadatablocks of the dataset as the schema
 * of the RO-Crate. It means, that all data that is present in the dataset in Dataverse is exported as is without
 * mapping it to the Schema.org vocabulary, which is the default schema for RO-Crates.
 *
 * This class has been extracted from the ARP project (https://science-research-data.hu/en) in the frame of
 * FAIR-IMPACT's 1st Open Call "Enabling FAIR Signposting and RO-Crate for content/metadata discovery and consumption".
 *
 * @author Balázs E. Pataki <balazs.pataki@sztaki.hu>, SZTAKI, Department of Distributed Systems, https://dsd.sztaki.hu
 * @author Norbert Finta <norbert.finta@sztaki.hu>, SZTAKI, Department of Distributed Systems, https://dsd.sztaki.hu
 * @version 1.0
 */
@AutoService(Exporter.class)
public class RoCrateExporter implements Exporter
{
    @Override
    public void exportDataset(ExportDataProvider exportDataProvider, OutputStream outputStream) throws ExportException
    {
        // The exporter is not loaded as an EJB by the ExporterService but is initialized directly, which means
        // we cannot inject EJB-s with the usual @EJB annotation. Instead, we look them up at runtime
        var roCrateManager = CDI.current().select(RoCrateManager.class).get();
        var datasetService = CDI.current().select(DatasetServiceBean.class).get();

        // exportDataProvider doesn't provide the Dataset object only the JSON representation, so we get this to
        // read the ID of the Dataset and then load it via the datasetService
        var json = exportDataProvider.getDatasetJson();
        var ds = datasetService.find(Long.valueOf(json.get("id").toString()));

        // Get the version specified in the json. Note: it is actually always the latest version, one cannot
        // export metadata of older versions.
        var versionId = Long.valueOf(json.getJsonObject("datasetVersion").get("id").toString());
        var version = ds.getVersionFromId(versionId);

        // Now we can creat the RO-Crate using the roCrateManager
        String roCratePath = roCrateManager.getRoCratePath(version);
        if (!Files.exists(Paths.get(roCratePath))) {
            try {
                roCrateManager.createOrUpdateRoCrate(version);
            } catch (Exception e) {
                throw new ExportException(e.getMessage());
            }
        }

        try (FileInputStream fis = new FileInputStream(roCratePath)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (Exception e) {
            throw new ExportException(e.getMessage());
        }
    }

    @Override
    public String getFormatName()
    {
        return "rocrate";
    }

    @Override
    public String getDisplayName(Locale locale)
    {
        String displayName = BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.rocrate",locale);
        return Optional.ofNullable(displayName).orElse("RO-Crate");
    }

    @Override
    public Boolean isHarvestable()
    {
        return true;
    }

    @Override
    public Boolean isAvailableToUsers()
    {
        return true;
    }

    @Override
    public String getMediaType()
    {
        return MediaType.APPLICATION_JSON;
    }
}
