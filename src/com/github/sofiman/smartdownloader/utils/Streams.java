package com.github.sofiman.smartdownloader.utils;

import java.io.*;
import java.security.MessageDigest;

public class Streams {

    public static OutputStream writeTo(File file) throws FileNotFoundException {
        FileOutputStream out = new FileOutputStream(file);
        return new BufferedOutputStream(out);
    }

    public static InputStream readFrom(File file) throws FileNotFoundException {
        FileInputStream in = new FileInputStream(file);
        return new BufferedInputStream(in);
    }

    public static void copy(InputStream in, OutputStream out, byte[] buffer, Agent agent) throws IOException {
        int len = 0;
        long totalProgress = 0;
        agent.onCopyStarted(in.available());
        while((len = in.read(buffer)) != -1){
            out.write(buffer, 0, len);
            totalProgress += len;
            agent.onCopyProgress(len, totalProgress);
        }
        agent.onCopyEnded(in, out);
    }

    public static void copy(InputStream in, MessageDigest out, byte[] buffer, Agent agent) throws IOException {
        int len = 0;
        long totalProgress = 0;
        agent.onCopyStarted(in.available());
        while((len = in.read(buffer)) != -1){
            out.update(buffer, 0, len);
            totalProgress += len;
            agent.onCopyProgress(len, totalProgress);
        }
        agent.onCopyEnded(in, null);
    }

    public static void copy(InputStream in, OutputStream out, int bufferSize, Agent agent) throws IOException {
        byte[] buffer = new byte[bufferSize];
        copy(in, out, buffer, agent);
    }

    public static void copy(RandomAccessFile in, OutputStream out, byte[] buffer, Agent agent) throws IOException {
        int len = 0;
        long totalProgress = 0;
        agent.onCopyStarted(in.length());
        while((len = in.read(buffer)) != -1){
            out.write(buffer, 0, len);
            totalProgress += len;
            agent.onCopyProgress(len, totalProgress);
        }
        agent.onCopyEnded(null, out);
    }

    public static void copy(RandomAccessFile in, OutputStream out, int bufferSize, Agent agent) throws IOException {
        byte[] buffer = new byte[bufferSize];
        copy(in, out, buffer, agent);
    }

    public static void copy(InputStream in, DataOutput out, byte[] buffer, Agent agent) throws IOException {
        int len = 0;
        long totalProgress = 0;
        agent.onCopyStarted(in.available());
        while((len = in.read(buffer)) != -1){
            out.write(buffer, 0, len);
            totalProgress += len;
            agent.onCopyProgress(len, totalProgress);
        }
        agent.onCopyEnded(in, null);
    }

    public static void copy(InputStream in, DataOutput out, int bufferSize, Agent agent) throws IOException {
        byte[] buffer = new byte[bufferSize];
        copy(in, out, buffer, agent);
    }

    public static interface Agent {

        void onCopyStarted(long availableBytes) throws IOException;

        void onCopyProgress(int len, long totalProgress);

        void onCopyEnded(InputStream in, OutputStream out) throws IOException;
    }

    public static final Agent EMPTY_AGENT = new Agent() {
        @Override
        public void onCopyStarted(long availableBytes) {
        }

        @Override
        public void onCopyProgress(int len, long totalProgress) {
        }

        @Override
        public void onCopyEnded(InputStream in, OutputStream out) {
        }
    };
}
