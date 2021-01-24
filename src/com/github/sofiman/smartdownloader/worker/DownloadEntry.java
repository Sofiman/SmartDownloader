package com.github.sofiman.smartdownloader.worker;

import com.github.sofiman.smartdownloader.utils.Range;
import com.github.sofiman.smartdownloader.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.NetworkInterface;

public class DownloadEntry {

    private final String id;
    private final NetworkInterface ni;
    protected float repartition;
    private File store;
    private Range range;
    private DownloadTracker tracker;
    private long throttle = -1;

    public DownloadEntry(String id, NetworkInterface ni, float repartition) {
        this.id = id;
        this.ni = ni;
        this.repartition = repartition;
        this.tracker = new DownloadTracker();
        try {
            this.store = File.createTempFile(id + ".chunk." + StringUtils.randomHex(16), ".bin");
            this.store.deleteOnExit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public DownloadEntry(String id, NetworkInterface ni, float repartition, long throttle) {
        this.id = id;
        this.ni = ni;
        this.repartition = repartition;
        this.throttle = throttle;
        this.tracker = new DownloadTracker();
        try {
            this.store = File.createTempFile(id + ".chunk." + StringUtils.randomHex(16), ".bin");
            this.store.deleteOnExit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setThrottle(long throttle) {
        this.throttle = throttle;
    }

    public void setRange(Range range) {
        this.range = range;
    }

    public Range getRange() {
        return range;
    }

    public File getStore() {
        return store;
    }

    public String getId() {
        return id;
    }

    public DownloadTracker getTracker() {
        return tracker;
    }

    public boolean isThrottled() {
        return throttle > 0;
    }

    public long getThrottleSpeed() {
        return throttle;
    }

    public float getRepartition() {
        return repartition;
    }

    public NetworkInterface getNetworkInterface() {
        return ni;
    }

    @Override
    public String toString() {
        return (!id.equals(ni.getName()) ? (id + ":") : "") + ni.getName() + " => " +
                (repartition == -1f ? "<dynamic>" : (repartition * 100f + "%"));
    }
}