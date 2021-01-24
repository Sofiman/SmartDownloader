package com.github.sofiman.smartdownloader.utils;

public class Range {

    private final long rangeStart, rangeEnd;

    public Range(long rangeStart, long rangeEnd) {
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
    }

    public long getRangeEnd() {
        return rangeEnd;
    }

    public long getRangeStart() {
        return rangeStart;
    }

    @Override
    public String toString() {
        return "Bytes=" + rangeStart + "-" + rangeEnd;
    }

    public String toReadableString(){
        return rangeStart + ":" + rangeEnd;
    }
}
