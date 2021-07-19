package edu.harvard.iq.dataverse.license;

import javax.ejb.Stateless;

@Stateless
public class LicenseLimits {

    private int maxWidth;
    private int maxHeight;
    private int maxSizeInBytes;

    public LicenseLimits() {
        maxWidth = 162;
        maxHeight = 62;
        maxSizeInBytes = 100000;
    }

    public LicenseLimits(int maxWidth, int maxHeight, int maxSizeInBytes) {
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.maxSizeInBytes = maxSizeInBytes;
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
    }

    public int getMaxSizeInBytes() {
        return maxSizeInBytes;
    }

    public void setMaxSizeInBytes(int maxSizeInBytes) {
        this.maxSizeInBytes = maxSizeInBytes;
    }
}
