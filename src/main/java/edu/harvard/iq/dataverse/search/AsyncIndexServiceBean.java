// Author: Eryk Kulikowski @ KU Leuven (2023). Apache 2.0 License

package edu.harvard.iq.dataverse.search;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.solr.client.solrj.SolrServerException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.batch.util.LoggingUtil;
import fish.payara.cluster.Clustered;
import fish.payara.cluster.DistributedLockType;

@Named
@Singleton
@Clustered(lock = DistributedLockType.LOCK_NONE)
public class AsyncIndexServiceBean {

    @EJB
    IndexServiceBean indexService;

    @Inject
    @ConfigProperty(name = "dataverse.search.index.maxpoolsize", defaultValue = "4")
    private Integer maximumPoolSize;

    private ThreadPoolExecutor blockingExecutor;

    @PostConstruct
    protected void setup() {
        blockingExecutor = getBlockingThreadPoolExecutor();
    }

    private static final Logger logger = Logger.getLogger(AsyncIndexServiceBean.class.getCanonicalName());

    // The following two variables are only used in the synchronized getNextToIndex
    // method and do not need to be synchronized themselves. We use concurrent
    // variants just in case...

    // nextToIndex contains datasets mapped by dataset id that were added for future
    // indexing while the indexing was already ongoing for a given dataset
    // (if there already was a dataset scheduled for indexing, it is overwritten and
    // only the most recently requested version is kept in the map)
    private static final Map<Long, Dataset> NEXT_TO_INDEX = new ConcurrentHashMap<>();
    // indexingNow is a set of dataset ids of datasets being indexed asynchronously
    // right now
    private static final Map<Long, Boolean> INDEXING_NOW = new ConcurrentHashMap<>(); // it is used as set

    // When you pass null as Dataset parameter to this method, it indicates that the
    // indexing of the dataset with "id" has finished
    // Pass non-null Dataset to schedule it for indexing
    synchronized private static Dataset getNextToIndex(Long id, Dataset d) {
        if (d == null) { // -> indexing of the dataset with id has finished
            Dataset next = NEXT_TO_INDEX.remove(id);
            if (next == null) { // -> no new indexing jobs were requested while indexing was ongoing
                // the job can be stopped now
                INDEXING_NOW.remove(id);
            }
            return next;
        }
        // index job is requested for a non-null dataset
        if (INDEXING_NOW.containsKey(id)) { // -> indexing job is already ongoing, and a new job should not be started
            NEXT_TO_INDEX.put(id, d);
            // by the current thread -> return null
            return null;
        }
        // otherwise, start a new job
        INDEXING_NOW.put(id, true);
        return d;
    }

    /**
     * Indexes a dataset asynchronously on a blocking thread pool executor. The
     * executor has the maximum pool size as configured in microprofile setting
     * "dataverse.search.index.maxpoolsize" with a default value of 4. When all
     * threads are in use, this method will block until a thread becomes available.
     * Otherwise, the indexing is executed immediately in the background on an
     * available (or new) thread.
     * 
     * Note that the commands implement a synchronized skipping mechanism. When an
     * indexing job is already running for a given dataset in the background, the
     * new command will not index that dataset, but will delegate the execution to
     * the already running job. The running job will pick up the requested indexing
     * once that it is finished with the ongoing indexing. If another indexing is
     * requested before the ongoing indexing is finished, only the indexing that is
     * requested most recently will be picked up for the next indexing.
     * 
     * In other words: we can have at most one indexing ongoing for the given
     * dataset, and at most one (most recent) request for reindexing of the same
     * dataset. All requests that come between the most recent one and the ongoing
     * one are skipped for the optimization reasons. For a more in depth discussion,
     * see the pull request: https://github.com/IQSS/dataverse/pull/9558
     * 
     * @param dataset                The dataset to be indexed.
     * @param doNormalSolrDocCleanUp Flag for normal Solr doc clean up.
     */
    public void asyncIndexDataset(Dataset dataset, boolean doNormalSolrDocCleanUp) {
        Runnable command = getIndexingRunnable(dataset, doNormalSolrDocCleanUp);
        blockingExecutor.execute(command);
    }

    private ThreadPoolExecutor getBlockingThreadPoolExecutor() {
        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(maximumPoolSize);
        ThreadPoolExecutor blockingExecutor = new ThreadPoolExecutor(1, maximumPoolSize, 2, TimeUnit.MINUTES, queue);
        blockingExecutor.setRejectedExecutionHandler(new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
                try {
                    executor.getQueue().put(runnable);
                    if (executor.isShutdown()) {
                        throw new RejectedExecutionException("Indexing blocking executor is shutdown");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RejectedExecutionException("Indexing blocking executor is interrupted", e);
                }
            }
        });
        return blockingExecutor;
    }

    private Runnable getIndexingRunnable(final Dataset dataset, final boolean doNormalSolrDocCleanUp) {
        return new Runnable() {
            @Override
            public void run() {
                Long id = dataset.getId();
                Dataset next = getNextToIndex(id, dataset);
                // if there is an ongoing index job for this dataset, next is null (ongoing
                // index job will reindex the newest version after current indexing finishes)
                while (next != null) {
                    logger.fine("indexing dataset " + id);
                    try {
                        indexService.indexDataset(next, doNormalSolrDocCleanUp);
                    } catch (SolrServerException | IOException e) {
                        String failureLogText = "Indexing failed. You can kickoff a re-index of this dataset with: \r\n curl http://localhost:8080/api/admin/index/datasets/"
                                + next.getId().toString();
                        failureLogText += "\r\n" + e.getLocalizedMessage();
                        LoggingUtil.writeOnSuccessFailureLog(null, failureLogText, next);
                    }
                    next = getNextToIndex(id, null);
                    // if during the indexing no new job was requested, next is null and loop can be
                    // stopped
                }
            }
        };
    }
}
