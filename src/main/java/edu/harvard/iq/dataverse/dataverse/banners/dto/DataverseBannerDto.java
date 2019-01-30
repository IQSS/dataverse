package edu.harvard.iq.dataverse.dataverse.banners.dto;

import edu.harvard.iq.dataverse.util.DateUtil;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class DataverseBannerDto {

    private Long id;

    @NotNull(message = "{field.required}")
    private Date fromTime;

    @NotNull(message = "{field.required}")
    private Date toTime;

    private boolean active;

    private Long dataverseId;

    @Valid
    private List<DataverseLocalizedBannerDto> dataverseLocalizedBanner;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getFromTime() {
        return fromTime;
    }

    public void setFromTime(Date fromTime) {
        this.fromTime = fromTime;
    }

    public Date getToTime() {
        return toTime;
    }

    public String getPrettyFromDate() {

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return DateUtil.formatDate(fromTime, format);
    }

    public String getPrettyToTime() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return DateUtil.formatDate(toTime, format);
    }

    public void setToTime(Date toTime) {
        this.toTime = toTime;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Long getDataverseId() {
        return dataverseId;
    }

    public void setDataverseId(Long dataverseId) {
        this.dataverseId = dataverseId;
    }

    public List<DataverseLocalizedBannerDto> getDataverseLocalizedBanner() {
        return dataverseLocalizedBanner;
    }

    public void setDataverseLocalizedBanner(List<DataverseLocalizedBannerDto> dataverseLocalizedBanner) {
        this.dataverseLocalizedBanner = dataverseLocalizedBanner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataverseBannerDto that = (DataverseBannerDto) o;
        return active == that.active &&
                Objects.equals(id, that.id) &&
                Objects.equals(fromTime, that.fromTime) &&
                Objects.equals(toTime, that.toTime) &&
                Objects.equals(dataverseId, that.dataverseId) &&
                Objects.equals(dataverseLocalizedBanner, that.dataverseLocalizedBanner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, fromTime, toTime, active, dataverseId, dataverseLocalizedBanner);
    }
}
