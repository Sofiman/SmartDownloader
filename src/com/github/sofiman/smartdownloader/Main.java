package com.github.sofiman.smartdownloader;

import com.github.sofiman.smartdownloader.utils.StringUtils;
import com.github.sofiman.smartdownloader.worker.DownloadMap;
import com.github.sofiman.smartdownloader.worker.Downloader;
import com.github.sofiman.smartdownloader.utils.Parser;

import java.io.File;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class Main {

    public static void main(String[] args) throws Exception {
        Parser parser = new Parser();
        parser.parse(args);
        String url = null, output = null, hash = null, hashType = null;
        DownloadMap map = new DownloadMap();
        String niState = null;
        float pState = -1f;
        long spState = -1L;
        if(parser.hasOption("--help")){
            System.out.println("Usage: java -jar SmartDownload.jar (option) [argument] (option) [argument] ...");
            System.out.println("Additional commands:");
            System.out.println("\t-h, --help: Show this list");
            System.out.println("\t-l, --list: Shows all available network interfaces");
            System.out.println("Available options:");
            System.out.println("\t-h, --hash: Specifies a file hash to check at the end of the download (hash type is required)");
            System.out.println("\t-ht, --hash-type: Specify the hash type of the file hash (required for -h, --hash)");
            System.out.println("Required options:");
            System.out.println("\t-u, --url: Specify the URL");
            System.out.println("\t-o, --output: Specify the Output file path");
            System.out.println("\t-ni: Specify the name of one network interface (order is important)");
            System.out.println("\t-ns: Specify the download speed of the previous network interface (this option must follow the -ni option)");
            System.out.println("\t-nip: Specify how much the previous network interface must download (this option must follow the -ni option)");
            return;
        }
        if(parser.hasOption("-l", "--list")){
            System.out.println("Available network interfaces:");
            Enumeration<NetworkInterface> its = NetworkInterface.getNetworkInterfaces();
            while(its.hasMoreElements()){
                NetworkInterface networkInterface = its.nextElement();
                System.out.println("\t" + networkInterface.getName() + " | " + networkInterface.getDisplayName());
            }
            return;
        }
        for (Parser.Option option : parser.getOptions()) {
            if (option.getId().equalsIgnoreCase("-u") || option.getId().equalsIgnoreCase("--url")) {
                url = option.getArgument();
            } else if (option.getId().equalsIgnoreCase("-h") || option.getId().equalsIgnoreCase("--hash")) {
                hash = option.getArgument();
            } else if (option.getId().equalsIgnoreCase("-ht") || option.getId().equalsIgnoreCase("--hash-type")) {
                hashType = option.getArgument();
            } else if (option.getId().equalsIgnoreCase("-ni")) {
                String name = option.getArgument();
                if (niState != null) {
                    map.with(NetworkInterface.getByName(niState), pState, spState);
                    pState = -1f;
                    spState = -1L;
                }
                niState = name;
            } else if (option.getId().equalsIgnoreCase("-nip")) {
                if (niState != null) {
                    pState = Float.parseFloat(option.getArgument());
                }
            } else if (option.getId().equalsIgnoreCase("-ns")) {
                if (niState != null) {
                    try {
                        spState = Long.parseLong(option.getArgument());
                    } catch (Exception e){
                        try {
                            spState = StringUtils.byteCount(option.getArgument());
                        } catch (Exception e1){
                            spState = -1L;
                        }
                    }
                }
            } else if (option.getId().equalsIgnoreCase("-o") || option.getId().equalsIgnoreCase("--output")) {
                output = option.getArgument();
            }
        }

        if(url == null){
            System.err.println("Missing url: See help (--help) for further information");
            return;
        }
        if(output == null){
            System.err.println("Missing output file: See help (--help) for further information");
            return;
        }
        if (niState != null) {
            map.with(NetworkInterface.getByName(niState), pState, spState);
        }
        if(map.size() == 0){
            System.err.println("No network interfaces found: See help (--help) for further information");
            return;
        }

        Downloader downloader = new Downloader(url, new File(output));
        downloader.withChecksum(hash, hashType);
        downloader.download(map.build());
    }
}
