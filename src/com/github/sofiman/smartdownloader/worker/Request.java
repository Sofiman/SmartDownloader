package com.github.sofiman.smartdownloader.worker;

import com.github.sofiman.smartdownloader.utils.Streams;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class Request {

    private final String url;
    private final Map<String, String> headers;
    private NetworkInterface ni;
    private File out;
    private int bufferSize = 2048;
    private volatile DownloadTracker tracker;
    private long throttle = Long.MAX_VALUE;

    public Request(String url, Map<String, String> headers) {
        this.url = url;
        this.headers = headers;
    }

    public Request(String url) {
        this.url = url;
        this.headers = new HashMap<>();
    }

    public Request header(String header, String value) {
        headers.put(header, value);

        return this;
    }

    public Request netInterface(NetworkInterface ni) {
        this.ni = ni;

        return this;
    }

    public Request out(String out) {
        this.out = new File(out);

        return this;
    }

    public Request out(File out) {
        this.out = out;

        return this;
    }

    public Request bufferSize(int bufferSize) {
        this.bufferSize = bufferSize;

        return this;
    }

    public Request tracker(DownloadTracker tracker) {
        this.tracker = tracker;

        return this;
    }

    public Request throttle(long throttle) {
        this.throttle = throttle;

        return this;
    }

    public void send() throws IOException {
        URL url = new URL(this.url);
        this.headers.put("Connection", "keep-alive");
        this.headers.put("Accept", "*/*");
        this.headers.put("Accept-Encoding", "gzip, deflate");

        InetAddress addr = InetAddress.getByName(url.getHost());
        Socket socket = new Socket();
        String path = url.getPath();
        if (ni != null) {
            Enumeration<InetAddress> nifAddresses = ni.getInetAddresses();
            socket.bind(new InetSocketAddress(nifAddresses.nextElement(), 0));
        }
        socket.connect(new InetSocketAddress(addr, 80));

        BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        wr.write("GET " + path + " HTTP/1.1\n");
        wr.write("Host: " + url.getHost() + "\n");
        for (Map.Entry<String, String> header : headers.entrySet()) {
            wr.write(header.getKey() + ":" + header.getValue() + "\n");
        }
        wr.write("\n\r");
        wr.flush();

        // Initialize the streams.
        if (out == null) throw new NullPointerException("Output file cannot be null");
        final FileOutputStream fileOutputStream = new FileOutputStream(out);
        final byte[] buffer = new byte[bufferSize];

        InputStream in = socket.getInputStream();
        Map<String, String> inHeaders = parseHeaders(in, fileOutputStream, buffer);
        long length = Long.parseLong(inHeaders.get("Content-Length"));
        String encoding = inHeaders.get("Content-Encoding");
        if("gzip".equals(encoding)){
            in = new GZIPInputStream(in);
        } else if("deflate".equals(encoding)){
            in = new InflaterInputStream(in, new Inflater(true));
        } else if(encoding != null && !"identity".equals(encoding))  {
            System.err.println("\nContent Encoding not supported: " + encoding);
            in.close();
            fileOutputStream.close();
            socket.close();
            return;
        }
        final ThrottledInputStream inputStream = new ThrottledInputStream(in, throttle);

        AtomicBoolean close = new AtomicBoolean(false);
        try {
            Thread lock = new Thread(() -> {
                try {
                    close.set(true);
                    inputStream.close();
                    socket.close();
                } catch (IOException ignored) {
                }
            });
            Runtime.getRuntime().addShutdownHook(lock);
            Streams.copy(inputStream, fileOutputStream, buffer, tracker.init(length, () -> {
                try {
                    inputStream.close();
                    socket.close();
                    Runtime.getRuntime().removeShutdownHook(lock);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));
        } catch (Exception e) {
            if (!close.get()) {
                e.printStackTrace();
            }
        } finally {
            fileOutputStream.close();
        }
    }

    public Map<String, String> head() throws IOException {
        URL url = new URL(this.url);
        this.headers.put("Connection", "keep-alive");
        this.headers.put("Accept", "*/*");

        InetAddress addr = InetAddress.getByName(url.getHost());
        Socket socket = new Socket();
        String path = url.getPath();
        if (ni != null) {
            Enumeration<InetAddress> nifAddresses = ni.getInetAddresses();
            socket.bind(new InetSocketAddress(nifAddresses.nextElement(), 0));
        }
        socket.connect(new InetSocketAddress(addr, 80));

        BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        wr.write("GET " + path + " HTTP/1.1\n");
        wr.write("Host: " + url.getHost() + "\n");
        for (Map.Entry<String, String> header : headers.entrySet()) {
            wr.write(header.getKey() + ":" + header.getValue() + "\n");
        }
        wr.write("\n\r");
        wr.flush();
        final InputStream inputStream = socket.getInputStream();
        byte[] buffer = new byte[bufferSize];
        Map<String, String> inHeaders = parseHeaders(inputStream, null, buffer);
        inputStream.close();
        socket.close();
        return inHeaders;
    }

    private Map<String, String> parseHeaders(InputStream inputStream, OutputStream outputStream, byte[] buffer) throws IOException {
        Map<String, String> inHeaders = new HashMap<>();
        StringBuilder head = new StringBuilder();
        String buf = "";
        int len = 0;
        while (true) {
            len = inputStream.read(buffer);
            if (len == -1) {
                return null;
            }
            buf = new String(buffer, 0, len);
            int idx = buf.indexOf("\r\n\r\n") + 4;
            if (idx > -1) {
                head.append(buf, 0, idx);
                if(outputStream != null){
                    outputStream.write(buffer, idx, bufferSize-(idx));
                }
                buf = null;
                break;
            }
            head.append(buf);
        }

        String[] lines = head.toString().split("\n");
        int idx;
        for (String line : lines) {
            idx = line.indexOf(":");
            if (idx > -1) {
                inHeaders.put(line.substring(0, idx), line.substring(idx + 2).trim());
            }
        }
        return inHeaders;
    }
}
