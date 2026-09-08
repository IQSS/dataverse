package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.api.dto.DataverseDTO;
import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.TestDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static edu.harvard.iq.dataverse.mocks.MocksFactory.makeDataverse;
import static edu.harvard.iq.dataverse.mocks.MocksFactory.makeRequest;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UpdateDataverseCommandTest {

    Dataverse currentDataverse;

    DataverseServiceBean dataverses = new DataverseServiceBean() {
        @Override
        public Dataverse find(Object pk) {
            return currentDataverse;
        }

        @Override
        public Dataverse save(Dataverse dataverse) {
            return dataverse;
        }

        @Override
        public boolean index(Dataverse dataverse) {
            return true;
        }
    };

    SystemConfig systemConfig = new SystemConfig() {
        @Override
        public boolean isExternalDataverseValidationEnabled() {
            return false;
        }
    };

    TestDataverseEngine engine;

    @BeforeEach
    public void setUp() {
        engine = new TestDataverseEngine(new TestCommandContext() {
            @Override
            public DataverseServiceBean dataverses() {
                return dataverses;
            }

            @Override
            public SystemConfig systemConfig() {
                return systemConfig;
            }
        });
    }

    @Test
    public void testGuestbookRootIsUpdatedWhenSet() throws CommandException {
        currentDataverse = makeDataverse();
        currentDataverse.setDataverseType(Dataverse.DataverseType.UNCATEGORIZED);
        currentDataverse.setGuestbookRoot(false);

        DataverseDTO dto = new DataverseDTO();
        dto.setGuestbookRoot(Boolean.TRUE);

        UpdateDataverseCommand sut = new UpdateDataverseCommand(currentDataverse, null, null, makeRequest(), null, null, dto);
        Dataverse result = engine.submit(sut);

        assertTrue(result.isGuestbookRoot());
    }

    @Test
    public void testGuestbookRootIsUnchangedWhenNotSet() throws CommandException {
        currentDataverse = makeDataverse();
        currentDataverse.setDataverseType(Dataverse.DataverseType.UNCATEGORIZED);
        currentDataverse.setGuestbookRoot(true);

        DataverseDTO dto = new DataverseDTO();
        // guestbookRoot intentionally left unset (null)

        UpdateDataverseCommand sut = new UpdateDataverseCommand(currentDataverse, null, null, makeRequest(), null, null, dto);
        Dataverse result = engine.submit(sut);

        assertTrue(result.isGuestbookRoot());
    }
}
