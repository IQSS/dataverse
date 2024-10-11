package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CustomizationFilesServletTest {
    private static final String EN_FILE_CONTENT = "Some content";
    private static final String PL_FILE_CONTENT = "Jakaś zawartość";

    private static final String PL = "pl";
    private static final String IT = "it";

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private SettingsServiceBean settingsService;

    @Mock
    private DataverseSession dataverseSession;

    @InjectMocks
    private CustomizationFilesServlet servlet;

    private File fileEn;
    private File filePl;
    private File tempDirectory;

    private StringWriter writer;

    // -------------------- TESTS ---------------------

    @BeforeEach
    public void setUp() throws IOException {
        setUpTestFiles();
        setUpMocks();
    }

    @AfterEach
    public void tearDown() {
        fileEn.delete();
        filePl.delete();
        tempDirectory.delete();
    }

    @Test
    public void shouldUseLocalizedFooterFileWhenAvailable() {
        // given
        when(dataverseSession.getLocaleCode()).thenReturn(PL);

        // when
        servlet.processRequest(request, response);

        // then
        assertThat(writer.toString(), containsString(PL_FILE_CONTENT));
    }

    @Test
    public void shouldFallBackToEnglishFooterWhenLocalizedIsNotAvailable() {
        // given
        when(dataverseSession.getLocaleCode()).thenReturn(IT);

        // when
        servlet.processRequest(request, response);

        // then
        assertThat(writer.toString(), containsString(EN_FILE_CONTENT));
    }

    // -------------------- PRIVATE ---------------------

    private void setUpTestFiles() throws IOException {
        Path directory = Files.createTempDirectory(randomAlphabetic(8));
        tempDirectory = directory.toFile();
        FileName fileName = createFileName();
        fileEn = createFile(directory, fileName, EN_FILE_CONTENT);
        filePl = createFile(directory, fileName.withInfix("_" + PL), PL_FILE_CONTENT);
    }

    private void setUpMocks() throws IOException {
        when(request.getParameter("customFileType")).thenReturn("footer");
        when(settingsService.getValueForKey(Key.FooterCustomizationFile)).thenReturn(fileEn.getAbsolutePath());
        writer = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(writer));
    }

    private FileName createFileName() {
        return new FileName(randomAlphabetic(8), "." + randomAlphabetic(3));
    }

    private File createFile(Path directory, FileName fileName, String content) {
        try {
            Path filePath = directory.toAbsolutePath().resolve(fileName.toString());
            File file = filePath.toFile();
            file.createNewFile();
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(content);
            }
            return file;
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    // -------------------- INNER CLASSES ---------------------

    private static class FileName {
        private final String name;
        private final String infix;
        private final String extension;

        public FileName(String name, String infix, String extension) {
            this.name = name;
            this.infix = infix;
            this.extension = extension;
        }

        public FileName(String name, String extension) {
            this(name, StringUtils.EMPTY, extension);
        }

        public FileName withInfix(String infix) {
            return new FileName(this.name, infix, this.extension);
        }

        public String toString() {
            return name + infix + extension;
        }
    }
}