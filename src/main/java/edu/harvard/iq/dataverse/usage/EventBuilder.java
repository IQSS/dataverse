/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.usage;

import edu.harvard.iq.dataverse.usage.Event.EventType;
import edu.harvard.iq.dataverse.authorization.users.User;
import java.util.Date;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author luopc
 */
@Singleton
public class EventBuilder {
    
    @EJB
    private GeoipServiceBean geoipService;

    public Event viewDataverse(HttpServletRequest request, User user, Long dataverseId){
        Event event = build(EventType.VIEW_DATAVERSE, request, user);
        event.setDataverseId(dataverseId);
        return event;
    }

    public Event viewDataset(HttpServletRequest request, User user, Long datasetId){
        Event event = build(EventType.VIEW_DATASET, request, user);
        event.setDatasetId(datasetId);
        return event;
    }

    public Event downloadFile(HttpServletRequest request, User user, Long datafileId){
        Event event = build(EventType.DOWNLOAD_FILE, request, user);
        event.setDatafileId(datafileId);
        return event;
    }
    
    public Event login(HttpServletRequest request, User user){
        Event event = build(EventType.LOGIN, request, user);
        return event;
    }
    
    public Event logout(HttpServletRequest request, User user){
        Event event = build(EventType.LOGOUT, request, user);
        return event;
    }
    
    public Event requestAccessFile(HttpServletRequest request, User user, Long datafileId){
        Event event = build(EventType.REQUEST_ACCESS_FILE, request, user);
        event.setDatafileId(datafileId);
        return event;
    }
    
    public Event grantRequestAccessFile(HttpServletRequest request, User user, Long datafileId){
        Event event = build(EventType.GRANT_REQUEST_ACCESS_FILE, request, user);
        event.setDatafileId(datafileId);
        return event;
    }
    
    public Event rejectRequestAccessFile(HttpServletRequest request, User user, Long datafileId){
        Event event = build(EventType.REJECT_REQUEST_ACCESS_FILE, request, user);
        event.setDatafileId(datafileId);
        return event;
    }
    
    public Event requestJoinGroup(HttpServletRequest request, User user, Long groupId){
        Event event = build(EventType.REQUEST_JOIN_GROUP, request, user);
        event.setGroupId(groupId);
        return event;
    }
    
    public Event acceptJoinGroup(HttpServletRequest request, User user, Long groupId){
        Event event = build(EventType.ACCEPT_JOIN_GROUP, request, user);
        event.setGroupId(groupId);
        return event;
    }
    
    public Event rejectJoinGroup(HttpServletRequest request, User user, Long groupId){
        Event event = build(EventType.REJECT_JOIN_GROUP, request, user);
        event.setGroupId(groupId);
        return event;
    }
        
    private Event build(EventType eventType,HttpServletRequest request,
            User user){
        Event event = new Event().addTimeStamp(new Date())
                .addType(eventType)
                .addHttpInfo(request)
                .addUserInfo(user);
        geoipService.addGeoInfo(event);
        return event;
                
    }
}
