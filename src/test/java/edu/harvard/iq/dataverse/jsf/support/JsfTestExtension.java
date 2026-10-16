package edu.harvard.iq.dataverse.jsf.support;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.Video;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * Runs each {@link JsfTest} method in the browser chosen by
 * {@link JsfTestConfig#browser()}. (The Maven build runs the whole suite once
 * per browser, back to back.)
 *
 * The browser is launched — and logged in, see {@link AuthSetup} — once per
 * test run, on first use. Every test invocation then gets its own browser
 * context started from the saved session, with a trace, a video and a final
 * full-page screenshot written to target/playwright/&lt;Class&gt;-&lt;method&gt;-&lt;browser&gt;/.
 * After the test, pass or fail, its {@link Cleanup} tasks run.
 */
public class JsfTestExtension implements TestTemplateInvocationContextProvider {

    static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(JsfTestExtension.class);

    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        return AnnotationSupport.isAnnotated(context.getTestMethod(), JsfTest.class);
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
        return Stream.of(new BrowserInvocation(JsfTestConfig.browser()));
    }

    private record BrowserInvocation(String browserName) implements TestTemplateInvocationContext {

        @Override
        public String getDisplayName(int invocationIndex) {
            return browserName;
        }

        @Override
        public List<Extension> getAdditionalExtensions() {
            return List.of(new BrowserTestLifecycle(browserName));
        }
    }

    /** Sets up and tears down one test invocation in one browser. */
    private static final class BrowserTestLifecycle implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

        private static final Logger logger = Logger.getLogger(BrowserTestLifecycle.class.getCanonicalName());

        private final String browserName;
        private Path outputDir;
        private BrowserContext context;
        private Page page;
        private Cleanup cleanup;
        private JsfFixtures fixtures;

        BrowserTestLifecycle(String browserName) {
            this.browserName = browserName;
        }

        @Override
        public void beforeEach(ExtensionContext extensionContext) {
            BrowserSession session = extensionContext.getRoot().getStore(NAMESPACE).getOrComputeIfAbsent(
                    "browser:" + browserName, key -> BrowserSession.start(browserName), BrowserSession.class);
            Browser browser = session.browser();

            outputDir = JsfTestConfig.resultsDir().resolve(extensionContext.getRequiredTestClass().getSimpleName()
                    + "-" + extensionContext.getRequiredTestMethod().getName() + "-" + browserName);
            deleteRecursively(outputDir);

            context = browser.newContext(new Browser.NewContextOptions()
                    .setBaseURL(JsfTestConfig.baseUrl())
                    .setViewportSize(1280, 720)
                    .setStorageStatePath(AuthFile.path(browserName))
                    .setRecordVideoDir(outputDir)
                    .setRecordVideoSize(1280, 720));
            context.tracing().start(new Tracing.StartOptions()
                    .setTitle(extensionContext.getDisplayName())
                    .setScreenshots(true)
                    .setSnapshots(true));
            page = context.newPage();
            cleanup = new Cleanup();
            fixtures = new JsfFixtures(page, browser, browserName, cleanup);
        }

        @Override
        public void afterEach(ExtensionContext extensionContext) {
            if (context == null) {
                return;
            }
            try {
                page.screenshot(new Page.ScreenshotOptions()
                        .setPath(outputDir.resolve("test-finished.png"))
                        .setFullPage(true));
            } catch (RuntimeException e) {
                logger.log(Level.WARNING, "could not take the final screenshot", e);
            }

            for (String failure : cleanup.runAll(context::newPage)) {
                // Don't fail the test for a cleanup problem — but make it
                // visible in the report so leaks get noticed and fixed.
                extensionContext.publishReportEntry("cleanup failed", failure);
                logger.warning("cleanup failed — " + failure);
            }

            Video video = page.video();
            try {
                context.tracing().stop(new Tracing.StopOptions().setPath(outputDir.resolve("trace.zip")));
            } finally {
                context.close();
            }
            if (video != null) {
                video.saveAs(outputDir.resolve("video.webm"));
                video.delete();
            }
        }

        @Override
        public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
            Class<?> type = parameterContext.getParameter().getType();
            return type == Page.class || type == JsfFixtures.class || type == Cleanup.class;
        }

        @Override
        public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
            Class<?> type = parameterContext.getParameter().getType();
            if (type == Page.class) {
                return page;
            }
            if (type == Cleanup.class) {
                return cleanup;
            }
            return fixtures;
        }

        private static void deleteRecursively(Path dir) {
            if (!Files.exists(dir)) {
                return;
            }
            try (Stream<Path> paths = Files.walk(dir)) {
                for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(p);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
