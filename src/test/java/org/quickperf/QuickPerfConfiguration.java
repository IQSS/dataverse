package org.quickperf;

import org.quickperf.config.SpecifiableGlobalAnnotations;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;

import static org.quickperf.sql.annotation.SqlAnnotationBuilder.disableSameSelectTypesWithDifferentParamValues;
import static org.quickperf.sql.annotation.SqlAnnotationBuilder.expectJdbcBatching;
import static org.quickperf.sql.annotation.SqlAnnotationBuilder.expectNoConnectionLeak;

/**
 * Default database-access contract applied to <b>every</b> QuickPerf test.
 *
 * <p>IMPORTANT: this class MUST live in the {@code org.quickperf} package,
 * otherwise QuickPerf will not pick it up.
 *
 * <p>Once {@code @QuickPerfTest} is added to the {@code @JpaPerformanceTest}
 * meta-annotation, these three rules automatically cover the default DB access
 * layer of all data services exercised by performance tests - no per-test
 * wiring needed:
 * <ul>
 *   <li>N+1 detection: fails when the same SELECT type is issued with
 *       different bind parameter values (e.g. one lazy query per file).</li>
 *   <li>JDBC batching: bulk writes must use batched roundtrips.</li>
 *   <li>No connection leaks.</li>
 * </ul>
 *
 * <p><b>Known caveat - IDENTITY id generation:</b> ~90 Dataverse entities use
 * {@code GenerationType.IDENTITY}, which prevents JDBC batching of INSERTs
 * (the driver must fetch the generated key per row). A test method that
 * persists entities may therefore fail {@code expectJdbcBatching()}.
 * If CI shows noise from fixture inserts, scope batching down to bulk-write
 * tests only and cancel it per-method with
 * {@code @ExpectJdbcBatching(batchSize = 0)}.
 *
 * <p>Escape hatches at method level (from the QuickPerf wiki):
 * <ul>
 *   <li>{@code @EnableSameSelectTypesWithDifferentParamValues} cancels N+1 detection</li>
 *   <li>{@code @ExpectJdbcBatching(batchSize = 0)} cancels the batching expectation</li>
 *   <li>{@code @DisableQuickPerf} / {@code @DisableGlobalAnnotations} disables everything temporarily</li>
 * </ul>
 */
public class QuickPerfConfiguration implements SpecifiableGlobalAnnotations {

    @Override
    public Collection<Annotation> specifyAnnotationsAppliedOnEachTest() {
        return Arrays.asList(
                // Can reveal N+1 selects:
                // https://blog.jooq.org/2017/12/18/the-cost-of-jdbc-server-roundtrips/
                disableSameSelectTypesWithDifferentParamValues(),

                // Bulk writes (tabular ingest, harvesting) must use JDBC batching
                expectJdbcBatching(),

                // No leaked connections
                expectNoConnectionLeak()
        );
    }
}
