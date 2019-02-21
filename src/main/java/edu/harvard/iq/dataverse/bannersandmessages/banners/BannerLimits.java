package edu.harvard.iq.dataverse.bannersandmessages.banners;

import javax.ejb.Local;
import javax.ejb.Stateful;

@Stateful
@Local
public class BannerLimits {

    private int maxWidth;
    private int maxHeight;
    private int maxSizeInBytes;

    public BannerLimits() {
        maxWidth = 728;
        maxHeight = 90;
        maxSizeInBytes = 1000000;
    }

    public BannerLimits(int maxWidth, int maxHeight, int maxSizeInBytes) {
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
