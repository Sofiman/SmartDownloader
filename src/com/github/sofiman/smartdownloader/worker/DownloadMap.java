package com.github.sofiman.smartdownloader.worker;

import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DownloadMap {

    private Map<String, DownloadEntry> entries;
    private boolean locked;

    public DownloadMap() {
        entries = new HashMap<>();
        locked = false;
    }

    public DownloadMap with(NetworkInterface ni) {
        return with(ni, -1f);
    }

    private DownloadMap with(String name, NetworkInterface ni, float repartition) {
        if (locked) throw new IllegalStateException();
        entries.put(name, new DownloadEntry(name, ni, repartition));

        return this;
    }

    public DownloadMap with(NetworkInterface ni, float repartition) {
        return with(ni, repartition, -1L);
    }

    public DownloadMap with(NetworkInterface ni, float repartition, long throttle) {
        if (locked) throw new IllegalStateException();
        if (ni == null) throw new IllegalArgumentException("Network interface can not be null");
        if (repartition != -1f && (repartition < 0 || repartition > 1))
            throw new IllegalArgumentException("Invalid repartition");
        String name = ni.getName();
        int i = 1;
        while (entries.containsKey(name)) {
            name += "{" + i++ + "}";
        }
        entries.put(name, new DownloadEntry(name, ni, repartition, throttle));

        return this;
    }

    public int size() {
        return entries.size();
    }

    public Map<String, DownloadEntry> getEntries() {
        return entries;
    }

    public DownloadMap build() {
        if (locked) throw new IllegalStateException();

        float total = 0;
        List<DownloadEntry> undefined = new ArrayList<>();
        for (Map.Entry<String, DownloadEntry> entry : entries.entrySet()) {
            DownloadEntry de = entry.getValue();

            if (de.getRepartition() != -1f) {
                total += de.getRepartition();
            } else {
                undefined.add(de);
            }
        }

        final float p = (1f - total) / undefined.size();
        for (DownloadEntry entry : undefined) {
            if (entry.getRepartition() != -1f) {
                entry.repartition = p;
            }
        }

        if (total != 1) {
            throw new RuntimeException("Invalid download repartition");
        }
        locked = true;
        return this;
    }

    public boolean isLocked() {
        return locked;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("<--- Download Map --->\n");
        for (String name : entries.keySet()) {
            builder.append("* ").append(entries.get(name)).append("\n");
        }
        return builder.substring(0, builder.length() - 1);
    }

}
