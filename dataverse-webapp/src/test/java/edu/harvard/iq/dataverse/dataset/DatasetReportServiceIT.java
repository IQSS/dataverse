package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.dataset.DatasetReportService.FileDataField;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@RunWith(Arquillian.class)
public class DatasetReportServiceIT extends WebappArquillianDeployment {
    private static final Logger logger = LoggerFactory.getLogger(DatasetReportServiceIT.class);

    @Inject
    private AuthenticationServiceBean authenticationService;

    @Inject
    private DataverseSession dataverseSession;

    @Inject
    private DatasetReportService service;

    // -------------------- TESTS --------------------

    @Test
    public void createReport() throws IOException {

        // given
        AuthenticatedUser user = authenticationService.getAuthenticatedUser("superuser");
        dataverseSession.setUser(user);

        // when
        String output;
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            service.createReport(outputStream);
            output = outputStream.toString();
        }

        // then
        StringReader stringReader = new StringReader(output);
        List<CSVRecord> records = CSVFormat.DEFAULT
                .withHeader(Arrays.stream(FileDataField.values()).map(Enum::name).toArray(String[]::new))
                .withSkipHeaderRecord()
                .parse(stringReader)
                .getRecords();
        int expectedRecordSize = FileDataField.values().length;

        assertThat(records)
                .extracting(
                        r -> r.get(FileDataField.FILE_NAME.name()),
                        r -> r.get(FileDataField.FILE_ID.name()),
                        CSVRecord::size)
                .containsExactly(
                        tuple("testfile6.zip", "53", expectedRecordSize),
                        tuple("testfile1.zip", "55", expectedRecordSize),
                        tuple("restricted.zip", "58", expectedRecordSize));
    }
}