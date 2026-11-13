package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.dataset.DatasetType;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Guestbook;
import edu.harvard.iq.dataverse.CustomQuestion;
import edu.harvard.iq.dataverse.CustomQuestionValue;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Utility class to validate managed fields that should not be changed by users.
 */
public class ManagedFieldsValidator {
    private static final Logger logger = Logger.getLogger(ManagedFieldsValidator.class.getCanonicalName());

    public static void validateManagedFields(Dataset incoming, Dataset existing) throws JsonParseException {
        List<String> diffs = new ArrayList<>();
        if (incoming.getAuthority() != null && !Objects.equals(incoming.getAuthority(), existing.getAuthority())) {
            diffs.add("authority");
        }
        if (incoming.getProtocol() != null && !Objects.equals(incoming.getProtocol(), existing.getProtocol())) {
            diffs.add("protocol");
        }
        if (incoming.getIdentifier() != null && !Objects.equals(incoming.getIdentifier(), existing.getIdentifier())) {
            diffs.add("identifier");
        }
        if (incoming.getDatasetFileCountLimit() != null && !Objects.equals(incoming.getDatasetFileCountLimit(), existing.getDatasetFileCountLimit())) {
            diffs.add("datasetFileCountLimit");
        }
        if (incoming.getDatasetType() != null) {
            String incomingName = incoming.getDatasetType().getName();
            String existingName = existing.getDatasetType() != null ? existing.getDatasetType().getName() : null;
            if (!Objects.equals(incomingName, existingName)) {
                // If it's the default and existing is null, it's NOT a diff because it's the default.
                if (!(existingName == null && DatasetType.DEFAULT_DATASET_TYPE.equals(incomingName))) {
                    diffs.add("datasetType");
                }
            }
        }
        if (incoming.getStorageIdentifier() != null && !Objects.equals(incoming.getStorageIdentifier(), existing.getStorageIdentifier())) {
            diffs.add("storageIdentifier");
        }

        if (!diffs.isEmpty()) {
            String msg = "Managed fields cannot be changed: " + String.join(", ", diffs);
            logger.warning(msg);
            throw new JsonParseException(msg);
        }
    }

    public static void validateManagedFields(DatasetVersion incoming, DatasetVersion existing) throws JsonParseException {
        List<String> diffs = new ArrayList<>();

        if (incoming.getId() != null && !Objects.equals(incoming.getId(), existing.getId())) {
            diffs.add("id");
        }
        if (incoming.getVersionNumber() != null && !Objects.equals(incoming.getVersionNumber(), existing.getVersionNumber())) {
            diffs.add("versionNumber");
        }
        if (incoming.getMinorVersionNumber() != null && !Objects.equals(incoming.getMinorVersionNumber(), existing.getMinorVersionNumber())) {
            diffs.add("minorVersionNumber");
        }
        if (incoming.getVersionState() != null && !Objects.equals(incoming.getVersionState(), existing.getVersionState())) {
            diffs.add("versionState");
        }
        if (incoming.getUNF() != null && !Objects.equals(incoming.getUNF(), existing.getUNF())) {
            diffs.add("UNF");
        }
        if (incoming.getReleaseTime() != null && !timeMatch(incoming.getReleaseTime(), existing.getReleaseTime())) {
            diffs.add("releaseTime");
        }
        if (incoming.getCreateTime() != null && !timeMatch(incoming.getCreateTime(), existing.getCreateTime())) {
            diffs.add("createTime");
        }
        if (incoming.getLastUpdateTime() != null && !timeMatch(incoming.getLastUpdateTime(), existing.getLastUpdateTime())) {
            diffs.add("lastUpdateTime");
        }
        if (incoming.getArchiveTime() != null && !timeMatch(incoming.getArchiveTime(), existing.getArchiveTime())) {
            diffs.add("archiveTime");
        }
        if (incoming.getVersionNote() != null && !Objects.equals(incoming.getVersionNote(), existing.getVersionNote())) {
            diffs.add("versionNote");
        }
        if (incoming.getDeaccessionNote() != null && !Objects.equals(incoming.getDeaccessionNote(), existing.getDeaccessionNote())) {
            diffs.add("deaccessionNote");
        }
        if (incoming.getDeaccessionLink() != null && !Objects.equals(incoming.getDeaccessionLink(), existing.getDeaccessionLink())) {
            diffs.add("deaccessionLink");
        }

        if (!diffs.isEmpty()) {
            String msg = "Managed fields cannot be changed: " + String.join(", ", diffs);
            logger.warning(msg);
            throw new JsonParseException(msg);
        }
    }

    public static void validateManagedFields(Guestbook incoming, Guestbook existing) throws JsonParseException {
        List<String> diffs = new ArrayList<>();
        if (incoming.getId() != null && !Objects.equals(incoming.getId(), existing.getId())) {
            diffs.add("id");
        }
        if (incoming.getCreateTime() != null && !timeMatch(incoming.getCreateTime(), existing.getCreateTime())) {
            diffs.add("createTime");
        }
        
        // Validate nested custom questions IDs
        if (incoming.getCustomQuestions() != null) {
            Set<Long> existingQuestionIds = new HashSet<>();
            if (existing.getCustomQuestions() != null) {
                for (CustomQuestion cq : existing.getCustomQuestions()) {
                    if (cq.getId() != null) {
                        existingQuestionIds.add(cq.getId());
                    }
                }
            }
            for (CustomQuestion cq : incoming.getCustomQuestions()) {
                if (cq.getId() != null && !existingQuestionIds.contains(cq.getId())) {
                    diffs.add("customQuestion.id:" + cq.getId());
                }
                // Validate nested option values IDs
                if (cq.getCustomQuestionValues() != null) {
                    Set<Long> existingValueIds = new HashSet<>();
                    // We need to find the matching question in 'existing' to get its valid value IDs
                    CustomQuestion existingCq = null;
                    if (cq.getId() != null && existing.getCustomQuestions() != null) {
                        existingCq = existing.getCustomQuestions().stream()
                                .filter(e -> Objects.equals(e.getId(), cq.getId()))
                                .findFirst().orElse(null);
                    }
                    if (existingCq != null && existingCq.getCustomQuestionValues() != null) {
                        for (CustomQuestionValue cqv : existingCq.getCustomQuestionValues()) {
                            if (cqv.getId() != null) {
                                existingValueIds.add(cqv.getId());
                            }
                        }
                    }
                    for (CustomQuestionValue cqv : cq.getCustomQuestionValues()) {
                        if (cqv.getId() != null && !existingValueIds.contains(cqv.getId())) {
                            diffs.add("customQuestionValue.id:" + cqv.getId());
                        }
                    }
                }
            }
        }
        
        if (!diffs.isEmpty()) {
            String msg = "Managed fields cannot be changed: " + String.join(", ", diffs);
            logger.warning(msg);
            throw new JsonParseException(msg);
        }
    }

    private static boolean timeMatch(Date d1, Date d2) {
        if (d1 == null) {
            return d2 == null;
        }
        if (d2 == null) {
            return false;
        }
        return d1.getTime() == d2.getTime();
    }
}
