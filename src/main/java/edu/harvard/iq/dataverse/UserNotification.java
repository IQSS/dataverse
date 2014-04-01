/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.sql.Timestamp;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

/**
 *
 * @author xyang
 */
@Entity
public class UserNotification implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private DataverseUser user;
    private String notification;
    private Timestamp sendDate;
    private boolean readNotification;

    @Transient
    private boolean displayAsRead;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DataverseUser getUser() {
        return user;
    }

    public void setUser(DataverseUser user) {
        this.user = user;
    }

    public String getNotification() {
        return notification;
    }

    public void setNotification(String notification) {
        this.notification = notification;
    }

    public Timestamp getSendDate() {
        return sendDate;
    }

    public void setSendDate(Timestamp sendDate) {
        this.sendDate = sendDate;
    }

    public boolean isReadNotification() {
        return readNotification;
    }

    public void setReadNotification(boolean readNotification) {
        this.readNotification = readNotification;
    }

    public boolean isDisplayAsRead() {
        return displayAsRead;
    }

    public void setDisplayAsRead(boolean displayAsRead) {
        this.displayAsRead = displayAsRead;
    }
}
