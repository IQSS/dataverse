/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.makedatacount;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.makedatacount.MakeDataCountLoggingServiceBean.MakeDataCountEntry;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

/**
 *
 * @author matthew
 */
public class MakeDataCountLoggingServiceBeanTest {
  
    @Test
    public void testMainAndFileConstructor() {        
        MockDataverseRequestServiceBean dvReqServ = new MockDataverseRequestServiceBean();
        AuthenticatedUser au = MocksFactory.makeAuthenticatedUser("First", "Last");
        DataverseRequest req = new DataverseRequest(au, IpAddress.valueOf("0.0.0.0"));
        dvReqServ.setDataverseRequest(req);
        
        MockDatasetVersion dvVersion = new MockDatasetVersion();
        Dataset dataset = new Dataset();
        dataset.setAuthority("Authority");
        dataset.setProtocol("Protocol");
        dataset.setIdentifier("Identifier"); 
        GlobalId id = dataset.getGlobalId();
        dataset.setGlobalId(id);
        dvVersion.setDataset(dataset);
        dvVersion.setAuthorsStr("OneAuthor;\tTwoAuthor");
        dvVersion.setTitle("Title\tWith Tab");
        dvVersion.setVersionNumber(1L);
        dvVersion.setReleaseTime(new Date());
        
        DataFile df = new DataFile();
        df.setStorageIdentifier("StorageId");
        df.setFilesize(1L);
        
        MakeDataCountEntry entry = new MakeDataCountEntry(null, dvReqServ, dvVersion, df);
        //Instead of going through the absurdity of mocking FacesContext and ExternalContext and Request,
        //we will just pass null and init the values pulled from that manually
        entry.setRequestUrl("RequestUrl");
        entry.setTargetUrl("TargetUrl");
        entry.setUserAgent("UserAgent");
        entry.setSessionCookieId("SeshCookieId");
        
        //lastly setting attributes we don't actually use currently in our logging/constructors, just in case
        entry.setUserCookieId("UserCookId");
        entry.setOtherId(null); // null pointer check for sanitize method
        assertThat(entry.getOtherId(), is("-"));
        entry.setOtherId("OtherId\t\r\nX");
        // escape sequences get replaced with a space in sanitize method
        assertThat(entry.getOtherId(), is("OtherId X"));
        // check other replacements for author list ";" becomes "|"
        assertThat(entry.getAuthors(), is("OneAuthor| TwoAuthor"));
        
        //And test. "-" is the default
        assertThat(entry.getEventTime(), is(not("-")));
        assertThat(entry.getClientIp(), is(not("-")));
        assertThat(entry.getSessionCookieId(), is(not("-")));
        assertThat(entry.getUserCookieId(), is(not("-")));
        assertThat(entry.getUserId(), is(not("-")));
        assertThat(entry.getRequestUrl(), is(not("-")));
        assertThat(entry.getIdentifier(), is(not("-")));
        assertThat(entry.getFilename(), is(not("-")));
        assertThat(entry.getSize(), is(not("-")));
        assertThat(entry.getUserAgent(), is(not("-")));
        assertThat(entry.getTitle(), is(not("-")));
        assertThat(entry.getPublisher(), is(not("-")));
        assertThat(entry.getPublisherId(), is(not("-")));
        assertThat(entry.getAuthors(), is(not("-")));
        assertThat(entry.getVersion(), is(not("-")));
        assertThat(entry.getOtherId(), is(not("-")));
        assertThat(entry.getTargetUrl(), is(not("-")));
        assertThat(entry.getPublicationDate(), is(not("-")));
        
        //19 entries are required for the Counter Processor logging 
        assertThat(entry.toString().split("\t").length, is(19)); 
        
    }
    
    //Testing that when you init with no objects everything defaults to "-"
    @Test
    public void testDefaultConstructor() {
        MakeDataCountEntry entry = new MakeDataCountEntry();
        assertThat(entry.getEventTime(), is("-"));
        assertThat(entry.getClientIp(), is("-"));
        assertThat(entry.getSessionCookieId(), is("-"));
        assertThat(entry.getUserCookieId(), is("-"));
        assertThat(entry.getUserId(), is("-"));
        assertThat(entry.getRequestUrl(), is("-"));
        assertThat(entry.getIdentifier(), is("-"));
        assertThat(entry.getFilename(), is("-"));
        assertThat(entry.getSize(), is("-"));
        assertThat(entry.getUserAgent(), is("-"));
        assertThat(entry.getTitle(), is("-"));
        assertThat(entry.getPublisher(), is("-"));
        assertThat(entry.getPublisherId(), is("-"));
        assertThat(entry.getAuthors(), is("-"));
        assertThat(entry.getVersion(), is("-"));
        assertThat(entry.getOtherId(), is("-"));
        assertThat(entry.getTargetUrl(), is("-"));
        assertThat(entry.getPublicationDate(), is("-"));
        
        assertThat(entry.toString().split("\t").length, is(19)); 
    }
    
    static class MockDataverseRequestServiceBean extends DataverseRequestServiceBean {
        DataverseRequest mockDataverseRequest;
        
        @Override
        public DataverseRequest getDataverseRequest() {
            return mockDataverseRequest;
        }
        
        public void setDataverseRequest(DataverseRequest request) {
            mockDataverseRequest = request;
        }
    }
    
    static class MockDatasetVersion extends DatasetVersion {
        String authorStr = "";
        String title = "";
        
        @Override 
        public String getAuthorsStr(boolean affiliation) {
            return authorStr;
        }
        
        public void setAuthorsStr(String str) {
            authorStr = str;
        }
        
        @Override 
        public String getTitle() {
            return authorStr;
        }
        
        public void setTitle(String str) {
            title = str;
        }
    }
    
}