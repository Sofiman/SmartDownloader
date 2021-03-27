package com.github.sofiman.smartdownloader.worker;

import com.github.sofiman.smartdownloader.utils.Range;
import com.github.sofiman.smartdownloader.utils.Streams;
import com.github.sofiman.smartdownloader.utils.StringUtils;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class Downloader {

    private final String url;
    private final File output;
    private String hash, hashType;

    public Downloader(String url, File output) {
        this.url = url;
        this.output = output;
    }

    public void withChecksum(String hash, String hashType) {
        this.hash = hash;
        this.hashType = hashType;
    }

    public void download(DownloadMap map) throws IOException, InterruptedException {
        if (!map.isLocked()) throw new IllegalArgumentException();

        Map<String, String> prefetch = new Request(url)
                .header("Accept-Encoding", "gzip, deflate").head();
        String acceptRanges = prefetch.get("Accept-Ranges");
        if (map.size() > 1 && acceptRanges != null && !acceptRanges.toLowerCase().contains("bytes")) {
            throw new RuntimeException("The target url does not allow the use of byte ranges");
        }
        final long length = Long.parseLong(prefetch.get("Content-Length"));
        if (length < 0) {
            return;
        }
        String encoding = prefetch.get("Content-Encoding");
        if (encoding != null) {
            System.out.println("Detected encoding: " + encoding);
        }

        DownloadEntry de;
        int k = 0;
        long len, p = 0;
        for (Map.Entry<String, DownloadEntry> entry : map.getEntries().entrySet()) {
            de = entry.getValue();
            len = k++ == map.size() - 1 ? (length - p) : Math.round(de.getRepartition() * length);
            de.setRange(new Range(p == 0 ? p : p + 1, p + len));
            p += len;
        }

        System.out.println("Prefetch result: <Content Length>=" + length + "; <Download Length>=" + p + "; <Connections>=" + map.size());

        final Set<Map.Entry<String, DownloadEntry>> entries = map.getEntries().entrySet();
        long start = System.currentTimeMillis();
        ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        System.out.println("Preparing download threads...");
        for (Map.Entry<String, DownloadEntry> entry : entries) {
            final DownloadEntry dwe = entry.getValue();
            System.out.println("Thread <" + dwe.getId() + "> will download with offset " + dwe.getRange().toReadableString());
            System.out.println("+ " + dwe.getStore().getAbsolutePath());
            service.submit(() -> {
                Request request = new Request(url)
                        .netInterface(dwe.getNetworkInterface())
                        .out(dwe.getStore()).tracker(dwe.getTracker());
                if(dwe.getRange() != null){
                    request.header("Range", dwe.getRange().toString());
                }
                if (dwe.isThrottled()) {
                    request.throttle(dwe.getThrottleSpeed());
                }
                try {
                    request.send();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
        Thread shutdownHook = new Thread(() -> {
            service.shutdownNow();
            System.out.println("\n[Terminated] Deleting chunk files");
            for (Map.Entry<String, DownloadEntry> entry : entries) {
                final DownloadEntry dwe = entry.getValue();
                dwe.getStore().delete();
            }
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        final int threads = entries.size();
        int finished;
        float total, s = 0;
        DownloadTracker tracker;
        while (true) {
            Thread.sleep(1);
            StringBuilder progress = new StringBuilder();
            finished = 0;
            total = 0;
            for (Map.Entry<String, DownloadEntry> entry : entries) {
                tracker = entry.getValue().getTracker();
                s = tracker.getProgress();

                progress.append(entry.getKey())
                        .append(StringUtils.progress('=', ' ',
                                '>', " [%s] %.1f", s, 20))
                        .append("% (")
                        .append(StringUtils.humanReadableByteCount(tracker.getSpeed(), true)).append("/s")
                        .append(entry.getValue().isThrottled() ? "*" : "").append(") ");
                finished += tracker.isFinished() ? 1 : 0;
                total += s;
            }
            System.out.print("\rDownloading: " + progress);
            if(threads > 1){
                System.out.print("total of " + String.format("%.1f", total * 100f / threads) + "%");
            }
            if (finished == threads) {
                break;
            }
        }

        service.shutdown();
        long end = System.currentTimeMillis();
        System.out.println("\nSuccessfully downloaded file in " + (end - start) / 1000f + "s");
        System.out.print("Copying chunk files...");
        FileOutputStream out = new FileOutputStream(output);
        byte[] buf = new byte[2048];
        AtomicLong progress = new AtomicLong(0);

        FileInputStream in;
        StringBuilder builder = new StringBuilder();
        int i = 1;
        for (Map.Entry<String, DownloadEntry> entry : entries) {
            in = new FileInputStream(entry.getValue().getStore());
            int finalI = i;
            Streams.copy(in, out, buf, new Streams.Agent() {
                @Override
                public void onCopyStarted(long availableBytes) {
                }

                @Override
                public void onCopyProgress(int len, long totalProgress) {
                    float n = progress.addAndGet(len) * 100f / length;
                    System.out.print(String.format("\rCopying chunk files... (%d/%d) %.1f", finalI, threads, n) + "%");
                }

                @Override
                public void onCopyEnded(InputStream in, OutputStream out) throws IOException {
                    in.close();
                    final File f = entry.getValue().getStore();
                    if (f.delete()) {
                        builder.append("\n- ").append(f.getAbsolutePath());
                    }
                }
            });
            i++;
        }
        System.out.print("\n+ " + output.getAbsolutePath());
        System.out.println(builder.toString());

        Runtime.getRuntime().removeShutdownHook(shutdownHook);

        out.close();
        if (hash != null && hashType != null) {
            try {
                MessageDigest md = MessageDigest.getInstance(hashType);
                try (InputStream is = new FileInputStream(output)) {
                    Streams.copy(is, md, buf, Streams.EMPTY_AGENT);
                }
                byte[] digest = md.digest();
                String hash = StringUtils.toHex(digest).toLowerCase();
                String targetHash = this.hash.toLowerCase();
                String title = "Checksums (" + hashType + "): ";
                String pad = "";
                for (int j = 0; j < title.length(); j++) {
                    pad += " ";
                }

                System.out.println(title + "Calculated " + hash);
                System.out.println(pad + String.format("%11s", "Target ") + targetHash);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
