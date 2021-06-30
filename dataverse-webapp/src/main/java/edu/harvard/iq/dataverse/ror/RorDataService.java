package edu.harvard.iq.dataverse.ror;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import edu.harvard.iq.dataverse.api.dto.RorEntryDTO;
import edu.harvard.iq.dataverse.interceptors.SuperuserRequired;
import edu.harvard.iq.dataverse.persistence.ror.RorData;
import edu.harvard.iq.dataverse.util.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Stateless
@TransactionManagement(TransactionManagementType.BEAN) // We don't want to start txs automatically here
public class RorDataService {

    private static final Logger logger = LoggerFactory.getLogger(RorDataService.class);

    private RorConverter rorConverter;

    private RorTransactionsService rorTransactionsService;

    private static int DEFAULT_BATCH_SIZE_FOR_TX = 100;
    private int batchSizeForTx = DEFAULT_BATCH_SIZE_FOR_TX;

    // -------------------- CONSTRUCTORS --------------------

    public RorDataService() {
    }

    @Inject
    public RorDataService(RorConverter rorConverter, RorTransactionsService rorTransactionsService) {
        this.rorConverter = rorConverter;
        this.rorTransactionsService = rorTransactionsService;
    }

    public RorDataService(RorConverter rorConverter, RorTransactionsService rorTransactionsService, int transactionBatchSize) {
        this.rorConverter = rorConverter;
        this.rorTransactionsService = rorTransactionsService;
        batchSizeForTx = transactionBatchSize;
    }

    // -------------------- LOGIC --------------------

    /**
     * Method dedicated for refreshing ror data, dropping old data and then adding fresh entries.
     */
    @SuperuserRequired
    public UpdateResult refreshRorData(File file, FormDataContentDisposition header) {
        File processed = selectFileToProcess(file, header);
        UpdateResult updateResult = new UpdateResult();
        try (FileReader fileReader = new FileReader(processed);
             JsonReader jsonReader = new JsonReader(fileReader)) {
            jsonReader.beginArray();

            Gson gson = new Gson();
            RorEntryDTO rorEntry;
            int count = 0;
            Set<RorData> toSave = new HashSet<>();
            while (jsonReader.hasNext()) {
                count++;
                rorEntry = gson.fromJson(jsonReader, RorEntryDTO.class);
                updateResult.update(rorEntry);

                if (count == 1) {
                    rorTransactionsService.truncateAll();
                }

                if (count % batchSizeForTx == 0) {
                    rorTransactionsService.saveMany(toSave);
                    updateResult.getSavedRorData().addAll(toSave);
                    toSave.clear();
                }
                toSave.add(rorConverter.toEntity(rorEntry));
            }
            rorTransactionsService.saveMany(toSave);
            updateResult.getSavedRorData().addAll(toSave);

            jsonReader.endArray();
        } catch (IOException ioe) {
            logger.warn("Exception while processing input file", ioe);
        } finally {
            processed.delete();
        }
        return updateResult;
    }

    // -------------------- PRIVATE --------------------

    private File selectFileToProcess(File file, FormDataContentDisposition header) {
        if (hasExtension(header.getFileName(), ".zip")) {
            return decompressJson(file);
        } else if (hasExtension(header.getFileName(), ".json")) {
            return file;
        } else {
            throw new IllegalArgumentException("No valid file uploaded (only .json or zipped .json");
        }
    }

    private File decompressJson(File zipped) {
        File decompressed = null;
        try (FileInputStream fileInputStream = new FileInputStream(zipped);
             ZipInputStream zipInputStream = new ZipInputStream(fileInputStream)) {
            ZipEntry currentEntry;
            while ((currentEntry = zipInputStream.getNextEntry()) != null) {
                if (!currentEntry.isDirectory() && hasExtension(currentEntry.getName(), ".json")) {
                    break;
                }
                zipInputStream.closeEntry();
            }
            if (currentEntry != null) {
                decompressed = FileUtil.inputStreamToFile(zipInputStream, 8192);
                zipInputStream.closeEntry();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            zipped.delete();
        }

        return decompressed;
    }

    private boolean hasExtension(String fileName, String extension) {
        return StringUtils.isNotBlank(fileName) && fileName.toLowerCase().endsWith(extension);
    }

    // -------------------- INNER CLASSES --------------------

    public static class UpdateResult {
        private Integer total = 0;
        private SortedMap<String, Integer> stats = new TreeMap<>();
        private List<RorData> savedRorData = new ArrayList<>();

        // -------------------- GETTERS --------------------

        public Integer getTotal() {
            return total;
        }

        public SortedMap<String, Integer> getStats() {
            return stats;
        }

        public List<RorData> getSavedRorData() {
            return savedRorData;
        }

        // -------------------- LOGIC --------------------

        public void update(RorEntryDTO entryDTO) {
            String countryName = entryDTO.getCountry().getCountryName();
            Integer countryTotal = stats.get(countryName);
            stats.put(countryName, countryTotal != null ? countryTotal + 1 : 1);
            total++;
        }
    }
}
