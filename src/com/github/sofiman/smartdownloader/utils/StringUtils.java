package com.github.sofiman.smartdownloader.utils;

import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {

    public static strictfp String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        long absBytes = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absBytes < unit) return bytes + " B";
        int exp = (int) (Math.log(absBytes) / Math.log(unit));
        long th = (long) (Math.pow(unit, exp) * (unit - 0.05));
        if (exp < 6 && absBytes >= th - ((th & 0xfff) == 0xd00 ? 52 : 0)) exp++;
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        if (exp > 4) {
            bytes /= unit;
            exp -= 1;
        }
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public static String randomHex(int size){
        byte[] bytes = new byte[size];
        ThreadLocalRandom.current().nextBytes(bytes);
        return toHex(bytes);
    }

    public static String toHex(byte[] bytes){
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for(byte b : bytes)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public static String join(Object[] array, String delimiter){
        StringBuilder b = new StringBuilder();
        for(Object obj : array){
            b.append(obj.toString());
            b.append(delimiter);
        }
        return b.substring(0, b.length() - delimiter.length());
    }

    public static String progress(char progress, char background, char pointer, String base, float p, int length){
        StringBuilder b = new StringBuilder();
        int idx = Math.round(p * length);
        for (int i = 0; i < length; i++) {
            if(i > idx){
                b.append(background);
            } else if(i == idx) {
                b.append(pointer);
            } else {
                b.append(progress);
            }
        }
        return String.format(base, b.toString(), p*100f);
    }

    public static String units = "BKMGTPEZY";

    public static int indexOf(Pattern pattern, String s) {
        Matcher matcher = pattern.matcher(s);
        return matcher.find() ? matcher.start() : -1;
    }

    public static long byteCount(String arg0) {
        int index = indexOf(Pattern.compile("[A-Za-z]"), arg0);
        double ret = Double.parseDouble(arg0.substring(0, index));
        String unitString = arg0.substring(index);
        int unitChar = unitString.charAt(0);
        int power = units.indexOf(unitChar);
        boolean isSi = unitString.indexOf('i')!=-1;
        int factor = 1024;
        if (isSi) {
            factor = 1000;
        }

        return new Double(ret * Math.pow(factor, power)).longValue();

    }
}
