package edu.harvard.iq.dataverse.util;

import java.io.Closeable;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Keeps track of {@link Closeable} resources handed out to code we do not control,
 * so they are released even when that code forgets to close them.
 * <p>
 * The typical use is a scope that lends streams to a plugin, a script, or any other caller whose diligence cannot
 * be assumed: resources are tracked as they are handed out, deregister themselves when closed properly, and
 * whatever remains is released when the registry closes.
 * <p>
 * Callers are therefore free to close or not close, and to close more than once, without leaking file descriptors
 * or storage connections.
 * <p>
 * Thread-safe: the registry may be used from several threads at once. The individual streams it hands out are not
 * made thread-safe by being tracked.
 * <p>
 * Example usage:
 * <pre>{@code
 * try (StreamRegistry registry = new StreamRegistry("bulk-operation " + correlationId, 16)) {
 *     plugin.doSomethingWith(registry.track(storage.open(...)));
 * } // anything the plugin left open is closed here
 * }</pre>
 */
public final class StreamRegistry implements AutoCloseable {
    
    private static final Logger logger = Logger.getLogger(StreamRegistry.class.getCanonicalName());
    
    /**
     * Creates the kind of set this registry needs:
     * a) identity-based, because streams are not required to provide meaningful {@code equals}/{@code hashCode}, and
     * b) synchronized because {@link Collections#newSetFromMap(java.util.Map)} over an {@link IdentityHashMap} is not.
     *
     * @return a mutable, thread-safe, identity-based set
     */
    public static <T> Set<T> newIdentitySet() {
        return Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));
    }
    
    private final Set<Closeable> openResources = newIdentitySet();
    private final String label;
    private final int warnThreshold;
    
    private boolean closed = false;
    private volatile boolean warned = false;
    
    /**
     * @param label identifies this registry in log records, e.g. a correlation id
     */
    public StreamRegistry(String label) {
        this(label, 0);
    }
    
    /**
     * @param label identifies this registry in log records, e.g. a correlation id
     * @param warnThreshold log once when more than this many resources are open simultaneously.
     *                      This is pure observability and imposes no limit. Zero or negative disables the warning.
     */
    public StreamRegistry(String label, int warnThreshold) {
        this.label = label == null ? "" : label;
        this.warnThreshold = warnThreshold;
    }
    
    /**
     * Wraps a stream so that closing it also deregisters it here, and registers it.
     * Until then, the registry keeps it alive and will close it on {@link #close()}.
     *
     * @return the wrapped stream (close this one, not the original)
     * @throws IllegalStateException if this registry is already closed; the given stream is then closed for you
     */
    public InputStream track(InputStream stream) {
        Objects.requireNonNull(stream, "stream must not be null");
        TrackedInputStream tracked = new TrackedInputStream(stream);
        try {
            add(tracked);
        } catch (RuntimeException ex) {
            closeQuietly(stream);
            throw ex;
        }
        return tracked;
    }
    
    /**
     * Registers a resource without wrapping it.
     * <p>
     * It is closed when this registry closes, whether or not the caller closed it first.
     * (Implementations of {@link Closeable} are required to tolerate repeated closing.)
     * <p>
     * Unlike {@link #track(InputStream)} this cannot deregister on close, so the registry holds the reference for
     * its whole lifetime. Prefer {@link #track} where a wrapper is acceptable.
     *
     * @return the very same resource, for chaining
     * @throws IllegalStateException if this registry is already closed
     */
    public <T extends Closeable> T register(T resource) {
        Objects.requireNonNull(resource, "resource must not be null");
        add(resource);
        return resource;
    }
    
    /** Number of tracked resources not yet closed. Intended for diagnostics and tests. */
    public int openCount() {
        synchronized (openResources) {
            return openResources.size();
        }
    }
    
    /**
     * Closes everything still open.
     * <p>
     * Never throws: failures are logged, as a caller at scope exit can rarely act on them. Idempotent.
     */
    @Override
    public void close() {
        List<Closeable> remaining;
        synchronized (openResources) {
            if (closed) {
                return;
            }
            closed = true;
            remaining = new ArrayList<>(openResources);
            openResources.clear();
        }
        if (!remaining.isEmpty()) {
            logger.log(Level.FINE, () -> prefix() + "releasing " + remaining.size()
                + " resource(s) the consumer left open");
        }
        remaining.forEach(StreamRegistry::closeQuietly);
    }
    
    private void add(Closeable resource) {
        int count;
        synchronized (openResources) {
            if (closed) {
                throw new IllegalStateException(prefix() + "registry is already closed");
            }
            openResources.add(resource);
            count = openResources.size();
        }
        if (warnThreshold > 0 && count > warnThreshold && !warned) {
            warned = true;
            logger.log(Level.WARNING, () -> prefix() + count + " resources are open at once; they will be "
                + "released when this scope ends, but the consumer may be retaining more handles than intended");
        }
    }
    
    private String prefix() {
        return label.isEmpty() ? "" : "[" + label + "] ";
    }
    
    private static void closeQuietly(Closeable resource) {
        try {
            resource.close();
        } catch (IOException ex) {
            logger.log(Level.WARNING, ex, () -> "Could not close " + resource.getClass().getName());
        }
    }
    
    /** Deregisters itself on close, so a well-behaved consumer keeps the registry empty. */
    @SuppressWarnings("java:S4929")
    private final class TrackedInputStream extends FilterInputStream {
        
        private boolean streamClosed = false;
        
        private TrackedInputStream(InputStream in) {
            super(in);
        }
        
        /**
         * Delegates instead of inheriting loop, so that fast paths of the wrapped stream survive the wrapping.
         * Notably, the stream returned by {@link java.nio.file.Files#newInputStream} can transfer at
         * channel level, which the default implementation in {@link InputStream} would silently replace.
         * (We extend from FilterInputStream which will provide the default implementation, not the wrapped stream!)
         */
        @Override
        public long transferTo(OutputStream out) throws IOException {
            return in.transferTo(out);
        }
        
        @Override
        public void close() throws IOException {
            if (streamClosed) {
                return;
            }
            streamClosed = true;
            synchronized (openResources) {
                openResources.remove(this);
            }
            super.close();
        }
    }
}