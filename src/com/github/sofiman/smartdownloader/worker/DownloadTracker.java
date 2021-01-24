package com.github.sofiman.smartdownloader.worker;

import com.github.sofiman.smartdownloader.utils.Streams;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;

public class DownloadTracker implements Streams.Agent {

    private final long interval = 1000;
    private long availableBytes;
    private long lastLength = 0, time = -1;

    private AtomicReference<Float> progress;
    private AtomicReference<Long> speed;
    private Runnable closeRunnable;
    private volatile boolean finished;

    public DownloadTracker(){
        progress = new AtomicReference<>(0f);
        speed = new AtomicReference<>(0L);
    }

    @Override
    public void onCopyStarted(long availableBytes) throws IOException {
        this.time = System.currentTimeMillis()+interval;
        this.lastLength = 0;
        this.speed.set(0L);
    }

    @Override
    public void onCopyProgress(int len, long totalProgress) {
        progress.set(totalProgress * 1f / availableBytes);
        if(time < System.currentTimeMillis()){
            this.speed.set(totalProgress - lastLength);
            time = System.currentTimeMillis() + interval;
            lastLength = totalProgress;
        }
    }

    @Override
    public void onCopyEnded(InputStream in, OutputStream out) throws IOException {
        closeRunnable.run();
        finished = true;
    }

    public synchronized float getProgress() {
        return progress.get();
    }

    public synchronized long getSpeed() {
        return speed.get();
    }

    public synchronized boolean isFinished() {
        return finished;
    }

    protected DownloadTracker init(long length, Runnable runnable){
        this.availableBytes = length;
        this.closeRunnable = runnable;

        return this;
    }
}
