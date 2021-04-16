package edu.harvard.iq.dataverse.actionlogging;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord.ActionType;
import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord.Result;

public class ActionLogRecordTest {

    private ActionLogRecord referenceRecord;

    @Before
    public void setUp() {
        this.referenceRecord = new ActionLogRecord(ActionType.Admin, "subType1");
        this.referenceRecord.setEndTime(new Date());
        this.referenceRecord.setActionResult(Result.OK);
        this.referenceRecord.setUserIdentifier("user1");
        this.referenceRecord.setInfo("info1");
    }

    @After
    public void tearDwon() {
        this.referenceRecord = null;
    }

    @Test
    public void testEqualityWithNull() {
        ActionLogRecord record = null;
        assertFalse(this.referenceRecord.equals(record));
    }

    @Test
    public void testEqualityWithAnotherClass() {
        String record = "a test string";
        assertFalse(this.referenceRecord.equals(record));
    }

    @Test
    public void testEqualityWithAnotherStartTime() {
        ActionLogRecord record = new ActionLogRecord();
        record.setStartTime(new Date());
        assertFalse(this.referenceRecord.equals(record));
    }

    @Test
    public void testEqualityWithAnotherEndTime() {
        ActionLogRecord record = new ActionLogRecord();
        record.setStartTime(this.referenceRecord.getStartTime());
        record.setEndTime(new Date());
        assertFalse(this.referenceRecord.equals(record));
    }

    @Test
    public void testEqualityWithAnotherActionResult() {
        ActionLogRecord record = new ActionLogRecord();
        record.setStartTime(this.referenceRecord.getStartTime());
        record.setEndTime(this.referenceRecord.getEndTime());
        record.setActionResult(Result.InternalError);
        assertFalse(this.referenceRecord.equals(record));
    }

    @Test
    public void testEqualityWithAnotherUserIdentifier() {
        ActionLogRecord record = new ActionLogRecord();
        record.setStartTime(this.referenceRecord.getStartTime());
        record.setEndTime(this.referenceRecord.getEndTime());
        record.setActionResult(this.referenceRecord.getActionResult());
        record.setUserIdentifier("user2");
        assertFalse(this.referenceRecord.equals(record));
    }

    @Test
    public void testEqualityWithAnotherActionType() {
        ActionLogRecord record = new ActionLogRecord();
        record.setStartTime(this.referenceRecord.getStartTime());
        record.setEndTime(this.referenceRecord.getEndTime());
        record.setActionResult(this.referenceRecord.getActionResult());
        record.setUserIdentifier(this.referenceRecord.getUserIdentifier());
        record.setActionType(ActionType.BuiltinUser);
        assertFalse(this.referenceRecord.equals(record));
    }

    @Test
    public void testEqualityWithAnotherSubActionType() {
        ActionLogRecord record = new ActionLogRecord();
        record.setStartTime(this.referenceRecord.getStartTime());
        record.setEndTime(this.referenceRecord.getEndTime());
        record.setActionResult(this.referenceRecord.getActionResult());
        record.setUserIdentifier(this.referenceRecord.getUserIdentifier());
        record.setActionType(this.referenceRecord.getActionType());
        record.setActionSubType("subType2");
        assertFalse(this.referenceRecord.equals(record));
    }

    @Test
    public void testEqualityWithAnotherInfo() {
        ActionLogRecord record = new ActionLogRecord();
        record.setStartTime(this.referenceRecord.getStartTime());
        record.setEndTime(this.referenceRecord.getEndTime());
        record.setActionResult(this.referenceRecord.getActionResult());
        record.setUserIdentifier(this.referenceRecord.getUserIdentifier());
        record.setActionType(this.referenceRecord.getActionType());
        record.setActionSubType(this.referenceRecord.getInfo());
        record.setInfo("info2");
        assertFalse(this.referenceRecord.equals(record));
    }

    @Test
    public void testEqualityWithEqualFieldValues() {
        ActionLogRecord record = new ActionLogRecord();
        record.setStartTime(this.referenceRecord.getStartTime());
        record.setEndTime(this.referenceRecord.getEndTime());
        record.setActionResult(this.referenceRecord.getActionResult());
        record.setUserIdentifier(this.referenceRecord.getUserIdentifier());
        record.setActionType(this.referenceRecord.getActionType());
        record.setActionSubType(this.referenceRecord.getActionSubType());
        record.setInfo(this.referenceRecord.getInfo());
        assertTrue(this.referenceRecord.equals(record));
    }

    @Test
    public void testEqualityWithEqualObject() {
        assertTrue(this.referenceRecord.equals(this.referenceRecord));
    }
}
