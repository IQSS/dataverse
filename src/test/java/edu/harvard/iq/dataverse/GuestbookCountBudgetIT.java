package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.util.testing.fixtures.DatasetFixture;
import edu.harvard.iq.dataverse.util.testing.fixtures.DatasetFixtureBuilder;
import edu.harvard.iq.dataverse.util.testing.performance.JpaEntityManagerService;
import edu.harvard.iq.dataverse.util.testing.performance.JpaPerformanceTest;
import edu.harvard.iq.dataverse.util.testing.recipes.DatasetRecipe;
import edu.harvard.iq.dataverse.util.testing.recipes.DatasetTypeRecipe;
import edu.harvard.iq.dataverse.util.testing.recipes.FileRecipe;
import edu.harvard.iq.dataverse.util.testing.recipes.VersionRecipe;
import jakarta.persistence.EntityManager;
import net.ttddyy.dsproxy.QueryCountHolder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quickperf.sql.annotation.EnableSameSelectTypesWithDifferentParamValues;
import org.quickperf.sql.annotation.ExpectDelete;
import org.quickperf.sql.annotation.ExpectInsert;
import org.quickperf.sql.annotation.ExpectUpdate;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reproduction for the Manage Guestbooks page N+1: usage and response counts
 * are loaded with up to 4 queries per guestbook
 * ({@code findCountUsages} and {@code findCountByGuestbookId}, each with and
 * without a dataverse scope).
 *
 * <p>The fixture holds 5 guestbooks over 2 dataverses with an uneven response
 * spread (including a zero-response guestbook and cross-dataverse responses
 * exercising the owner scope). The test asserts the per-row pattern exceeds
 * the new-path budget (RED proof), the grouped counts stay within it, and
 * both produce identical per-guestbook counts.
 */
@JpaPerformanceTest
class GuestbookCountBudgetIT {

    static JpaEntityManagerService jpa;
    static Long dataverseId;
    static List<Long> guestbookIds;

    /** Fixed budget for the 4 grouped counts over 5 guestbooks. Calibrated, see IT output. */
    static final int COUNT_BUDGET = 4;

    @BeforeAll
    static void setUp() {
        jpa.start();

        DatasetFixture first = DatasetFixtureBuilder.builder().recipe(DatasetRecipe.of(
            DatasetTypeRecipe.dataset(),
            VersionRecipe.of(FileRecipe.regular(2))
        )).build();
        DatasetFixture second = DatasetFixtureBuilder.builder().recipe(DatasetRecipe.of(
            DatasetTypeRecipe.dataset(),
            VersionRecipe.of(FileRecipe.regular(1))
        )).build();

        jpa.inTransactionVoid(em -> {
            em.persist(first.datasetType());

            Dataverse firstDataverse = newDataverse(em, "gbdv1", "Guestbook Dataverse 1");
            Dataverse secondDataverse = newDataverse(em, "gbdv2", "Guestbook Dataverse 2");

            Dataset firstDataset = first.dataset();
            firstDataset.setDatasetType(first.datasetType());
            firstDataset.setOwner(firstDataverse);
            for (DataFile dataFile : first.dataFiles()) {
                dataFile.setOwner(firstDataset);
                em.persist(dataFile);
            }
            Dataset secondDataset = second.dataset();
            secondDataset.setDatasetType(first.datasetType());
            secondDataset.setOwner(secondDataverse);
            for (DataFile dataFile : second.dataFiles()) {
                dataFile.setOwner(secondDataset);
                em.persist(dataFile);
            }

            List<Guestbook> guestbooks = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                Guestbook guestbook = new Guestbook();
                guestbook.setDataverse(firstDataverse);
                guestbook.setName("Guestbook " + i);
                guestbook.setCreateTime(new java.util.Date());
                em.persist(guestbook);
                guestbooks.add(guestbook);
            }
            firstDataset.setGuestbook(guestbooks.get(0));
            secondDataset.setGuestbook(guestbooks.get(1));
            em.persist(firstDataset);
            em.persist(secondDataset);
            em.flush();

            DataFile firstFile = first.dataFiles().get(0);
            DataFile secondFile = second.dataFiles().get(0);
            respond(em, guestbooks.get(0), firstDataset, firstFile);
            respond(em, guestbooks.get(0), firstDataset, firstFile);
            respond(em, guestbooks.get(0), secondDataset, secondFile);
            respond(em, guestbooks.get(1), firstDataset, firstFile);
            respond(em, guestbooks.get(3), firstDataset, firstFile);
            respond(em, guestbooks.get(3), firstDataset, firstFile);
            respond(em, guestbooks.get(4), secondDataset, secondFile);
            em.flush();

            dataverseId = firstDataverse.getId();
            guestbookIds = new ArrayList<>();
            for (Guestbook guestbook : guestbooks) {
                guestbookIds.add(guestbook.getId());
            }
        });
    }

    @Test
    @DisplayName("guestbooks: grouped counts cost 4 SELECTs, not 4 per guestbook")
    @EnableSameSelectTypesWithDifferentParamValues
    @ExpectUpdate(0)
    @ExpectInsert(0)
    @ExpectDelete(0)
    void countsStayWithinBudget() {
        jpa.getEntityManagerFactory().getCache().evictAll();
        CountRun legacy = jpa.inTransaction(em -> {
            GuestbookServiceBean guestbooks = new GuestbookServiceBean();
            inject(guestbooks, em);
            GuestbookResponseServiceBean responses = new GuestbookResponseServiceBean();
            inject(responses, em);
            QueryCountHolder.clear();
            Map<Long, long[]> counts = new HashMap<>();
            for (Long guestbookId : guestbookIds) {
                counts.put(guestbookId, new long[]{
                    guestbooks.findCountUsages(guestbookId, dataverseId),
                    guestbooks.findCountUsages(guestbookId, null),
                    responses.findCountByGuestbookId(guestbookId, dataverseId),
                    responses.findCountByGuestbookId(guestbookId, null)});
            }
            return new CountRun(QueryCountHolder.getGrandTotal().getSelect(), counts);
        });
        jpa.getEntityManagerFactory().getCache().evictAll();
        CountRun fast = jpa.inTransaction(em -> {
            GuestbookServiceBean guestbooks = new GuestbookServiceBean();
            inject(guestbooks, em);
            GuestbookResponseServiceBean responses = new GuestbookResponseServiceBean();
            inject(responses, em);
            QueryCountHolder.clear();
            Map<Long, Long> usages = guestbooks.findCountUsagesByGuestbookIds(guestbookIds, dataverseId);
            Map<Long, Long> usagesGlobal = guestbooks.findCountUsagesByGuestbookIds(guestbookIds, null);
            Map<Long, Long> responseCounts = responses.findCountsByGuestbookIds(guestbookIds, dataverseId);
            Map<Long, Long> responseCountsGlobal = responses.findCountsByGuestbookIds(guestbookIds, null);
            Map<Long, long[]> counts = new HashMap<>();
            for (Long guestbookId : guestbookIds) {
                counts.put(guestbookId, new long[]{
                    usages.getOrDefault(guestbookId, 0L),
                    usagesGlobal.getOrDefault(guestbookId, 0L),
                    responseCounts.getOrDefault(guestbookId, 0L),
                    responseCountsGlobal.getOrDefault(guestbookId, 0L)});
            }
            return new CountRun(QueryCountHolder.getGrandTotal().getSelect(), counts);
        });
        System.out.println("[GuestbookCountBudgetIT] legacy SELECTs: " + legacy.selects
                + ", grouped SELECTs: " + fast.selects);

        assertTrue(legacy.selects > COUNT_BUDGET,
                "legacy path should exceed the new-path budget, but got " + legacy.selects);
        assertTrue(fast.selects <= COUNT_BUDGET,
                "expected at most " + COUNT_BUDGET + " SELECTs, but got " + fast.selects);
        assertEquals(legacy.counts.keySet(), fast.counts.keySet(), "same guestbooks counted");
        for (Long guestbookId : guestbookIds) {
            assertEquals(
                    java.util.Arrays.toString(legacy.counts.get(guestbookId)),
                    java.util.Arrays.toString(fast.counts.get(guestbookId)),
                    "identical counts for guestbook " + guestbookId);
        }
    }

    private record CountRun(long selects, Map<Long, long[]> counts) {
    }

    private static void inject(Object target, EntityManager em) {
        try {
            Field found = null;
            Class<?> type = target.getClass();
            while (type != null && found == null) {
                try {
                    found = type.getDeclaredField("em");
                } catch (NoSuchFieldException e) {
                    type = type.getSuperclass();
                }
            }
            if (found == null) {
                throw new NoSuchFieldException("em");
            }
            found.setAccessible(true);
            found.set(target, em);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Cannot inject EntityManager into " + target.getClass().getSimpleName(), e);
        }
    }

    private static Dataverse newDataverse(EntityManager em, String alias, String name) {
        Dataverse dataverse = new Dataverse();
        dataverse.setAlias(alias);
        dataverse.setName(name);
        dataverse.setDataverseType(Dataverse.DataverseType.UNCATEGORIZED);
        dataverse.getDataverseContacts().add(new DataverseContact(dataverse, alias + "@example.com"));
        dataverse.setCreateDate(new Timestamp(System.currentTimeMillis()));
        dataverse.setModificationTime(new Timestamp(System.currentTimeMillis()));
        em.persist(dataverse);
        return dataverse;
    }

    private static void respond(EntityManager em, Guestbook guestbook, Dataset dataset, DataFile dataFile) {
        GuestbookResponse response = new GuestbookResponse();
        response.setGuestbook(guestbook);
        response.setDataset(dataset);
        response.setDataFile(dataFile);
        em.persist(response);
    }
}
