package edu.harvard.iq.dataverse.util;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamRegistryTest {
    
    private static final byte[] PAYLOAD = "payload".getBytes(StandardCharsets.UTF_8);
    
    @Nested
    class Tracking {
        
        @Test
        void wrapsAndRegisters() throws IOException {
            CountingInputStream original = new CountingInputStream();
            try (StreamRegistry registry = new StreamRegistry("test")) {
                InputStream tracked = registry.track(original);
                
                assertNotSame(original, tracked);
                assertEquals(1, registry.openCount());
                assertArrayEquals(PAYLOAD, tracked.readAllBytes());
            }
        }
        
        @Test
        void closeDeregisters() throws IOException {
            CountingInputStream original = new CountingInputStream();
            try (StreamRegistry registry = new StreamRegistry("test")) {
                InputStream tracked = registry.track(original);
                tracked.close();
                
                assertEquals(0, registry.openCount());
                assertEquals(1, original.closeCount);
            }
        }
        
        @Test
        void closeIsIdempotentPerStream() throws IOException {
            CountingInputStream original = new CountingInputStream();
            try (StreamRegistry registry = new StreamRegistry("test")) {
                InputStream tracked = registry.track(original);
                tracked.close();
                tracked.close();
                
                assertEquals(1, original.closeCount);
            }
        }
        
        @Test
        void registryClosesForgottenStreams() {
            CountingInputStream forgotten = new CountingInputStream();
            try (StreamRegistry registry = new StreamRegistry("test")) {
                registry.track(forgotten);
                assertEquals(1, registry.openCount());
            }
            assertEquals(1, forgotten.closeCount);
        }
        
        @Test
        void transferToIsDelegated() throws IOException {
            CountingInputStream original = new CountingInputStream();
            ByteArrayOutputStream sink = new ByteArrayOutputStream();
            try (StreamRegistry registry = new StreamRegistry("test")) {
                long copied = registry.track(original).transferTo(sink);
                
                assertEquals(PAYLOAD.length, copied);
                assertArrayEquals(PAYLOAD, sink.toByteArray());
                assertEquals(1, original.transferToCount);
            }
        }
        
        @Test
        void rejectsNull() {
            try (StreamRegistry registry = new StreamRegistry("test")) {
                assertThrows(NullPointerException.class, () -> registry.track(null));
            }
        }
    }
    
    @Nested
    class Registering {
        
        @Test
        void returnsSameInstanceAndClosesIt() {
            CountingCloseable resource = new CountingCloseable();
            try (StreamRegistry registry = new StreamRegistry("test")) {
                assertSame(resource, registry.register(resource));
                assertEquals(1, registry.openCount());
            }
            assertEquals(1, resource.closeCount);
        }
        
        @Test
        void rejectsNull() {
            try (StreamRegistry registry = new StreamRegistry("test")) {
                assertThrows(NullPointerException.class, () -> registry.register(null));
            }
        }
    }
    
    @Nested
    class Lifecycle {
        
        @Test
        void closeTwiceClosesResourcesOnce() {
            CountingCloseable resource = new CountingCloseable();
            StreamRegistry registry = new StreamRegistry("test");
            registry.register(resource);
            
            registry.close();
            registry.close();
            
            assertEquals(1, resource.closeCount);
            assertEquals(0, registry.openCount());
        }
        
        @Test
        void trackAfterCloseFailsAndClosesStream() {
            CountingInputStream orphan = new CountingInputStream();
            StreamRegistry registry = new StreamRegistry("test");
            registry.close();
            
            assertThrows(IllegalStateException.class, () -> registry.track(orphan));
            assertEquals(1, orphan.closeCount);
        }
        
        @Test
        void registerAfterCloseFails() {
            StreamRegistry registry = new StreamRegistry("test");
            registry.close();
            
            assertThrows(IllegalStateException.class, () -> registry.register(new CountingCloseable()));
        }
        
        @Test
        void failureDuringCloseIsContained() {
            CountingCloseable healthy = new CountingCloseable();
            StreamRegistry registry = new StreamRegistry("test");
            registry.register(() -> {
                throw new IOException("boom");
            });
            registry.register(healthy);
            
            registry.close();
            
            assertEquals(1, healthy.closeCount);
        }
    }
    
    @Test
    void identitySetUsesReferenceEquality() {
        Set<String> set = StreamRegistry.newIdentitySet();
        set.add(new String("same"));
        set.add(new String("same"));
        
        assertEquals(2, set.size());
    }
    
    @Test
    void isThreadSafe() throws InterruptedException {
        int threads = 8;
        int streamsPerThread = 100;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        
        try (StreamRegistry registry = new StreamRegistry("concurrent", 16)) {
            for (int t = 0; t < threads; t++) {
                pool.submit(() -> {
                    start.await();
                    for (int i = 0; i < streamsPerThread; i++) {
                        registry.track(new CountingInputStream()).close();
                    }
                    return null;
                });
            }
            start.countDown();
            pool.shutdown();
            
            assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));
            assertEquals(0, registry.openCount());
        }
    }
    
    private static final class CountingInputStream extends ByteArrayInputStream {
        
        private int closeCount;
        private int transferToCount;
        
        private CountingInputStream() {
            super(PAYLOAD);
        }
        
        @Override
        public long transferTo(OutputStream out) throws IOException {
            transferToCount++;
            return super.transferTo(out);
        }
        
        @Override
        public void close() throws IOException {
            closeCount++;
            super.close();
        }
    }
    
    private static final class CountingCloseable implements Closeable {
        
        private int closeCount;
        
        @Override
        public void close() {
            closeCount++;
        }
    }
}