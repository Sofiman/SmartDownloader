package com.github.sofiman.smartdownloader.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Parser {

    private static final String compactIdentifierStart = "-", extendedIdentifierStart = "--";

    private String[] args;
    private List<Option> options;

    public void parse(String[] args){
        this.args = args;
        this.options = new ArrayList<>();

        LinkedList<String> lastIdentifiers = new LinkedList<>();
        for(String s : args){
            if(s.startsWith(compactIdentifierStart) || s.startsWith(extendedIdentifierStart)){
                lastIdentifiers.addLast(s);
                continue;
            }
            if(lastIdentifiers.size() > 0){
                String currentIdentifier = lastIdentifiers.pop();
                final Option opt = new Option();
                opt.id = currentIdentifier;
                opt.argument = s;
                this.options.add(opt);
            }
        }
        for(String identifier : lastIdentifiers){
            final Option opt = new Option();
            opt.id = identifier;
            opt.argument = null;
            this.options.add(opt);
        }
    }

    public Option find(String identifier){
        for(Option option : options){
            if(option.id.equals(identifier)) return option;
        }
        return null;
    }

    public boolean hasOption(String... identifiers){
        boolean hasOption = false;
        for(Option option : options){
            hasOption = hasOption || Arrays.binarySearch(identifiers, option.id) >= 0;
        }
        return hasOption;
    }

    public List<String> getIdentifiers(){
        List<String> identifiers = new ArrayList<>();
        for(Option option : options){
            identifiers.add(option.id);
        }
        return identifiers;
    }

    public List<Option> getOptions() {
        return options;
    }

    public String[] getArguments() {
        return args;
    }

    public static class Option {
        private String id;
        private String argument;

        public String getId() {
            return id;
        }

        public String getArgument() {
            if(argument != null){
                return argument.replaceAll("\"","");
            }
            return null;
        }

        @Override
        public String toString() {
            return id + "(" + argument + ")";
        }
    }
}
