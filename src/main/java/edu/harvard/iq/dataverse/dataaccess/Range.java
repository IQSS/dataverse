package edu.harvard.iq.dataverse.dataaccess;

public class Range {

    // Used to set the offset, how far to skip into the file.
    private final long start;
    // Used to calculate the length.
    private final long end;
    // Used to determine when to stop reading.
    private final long length;

    public Range(long start, long end) {
        this.start = start;
        this.end = end;
        this.length = end - start + 1;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public long getLength() {
        return length;
    }

}
