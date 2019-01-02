package edu.harvard.iq.dataverse.dataverse.messages.dto;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class DataverseTextMessageDto {

    private boolean active;

    private LocalDateTime fromTime;

    private LocalDateTime toTime;

    private Set<DataverseLocalizedMessageDto> dataverseLocalizedMessage = new HashSet<>();

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getFromTime() {
        return fromTime;
    }

    public LocalDateTime getToTime() {
        return toTime;
    }

    public Set<DataverseLocalizedMessageDto> getDataverseLocalizedMessage() {
        return dataverseLocalizedMessage;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setFromTime(LocalDateTime fromTime) {
        this.fromTime = fromTime;
    }

    public void setToTime(LocalDateTime toTime) {
        this.toTime = toTime;
    }

    public void setDataverseLocalizedMessage(Set<DataverseLocalizedMessageDto> dataverseLocalizedMessage) {
        this.dataverseLocalizedMessage = dataverseLocalizedMessage;
    }
}
