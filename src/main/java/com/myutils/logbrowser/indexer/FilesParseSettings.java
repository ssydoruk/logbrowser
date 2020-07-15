/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author ssydoruk
 */
public class FilesParseSettings {

    /**
     *
     * @param t
     * @return
     */
    private FileParseSettings getFileParseSettings(String t) {
        FileParseSettings tempList = null;
        if (fileSettings.containsKey(t)) {
            tempList = fileSettings.get(t);
        }
        if (tempList == null) {
            tempList = new FileParseSettings();
            fileSettings.put(t, tempList);
        }

        return tempList;

    }

    void addDateFmt(String t, Parser.DateFmt df) {
        FileParseSettings tempList = getFileParseSettings(t);

        tempList.addDateFmt(df);
    }

    FileParseCustomSearch addFileParseCustomSearch(String t) {
        FileParseSettings fileParseSettings = getFileParseSettings(t);
        return fileParseSettings.addfileParseSettings();
    }

    FileParseSettings retrieveFileParseSettings(String type) {
        return fileSettings.get(type);
    }

    static class FileParseSettings {

        private ArrayList<Parser.DateFmt> dateSettings = new ArrayList<>();

        public ArrayList<FileParseCustomSearch> getCustomSearch() {
            return customSearch;
        }
        private ArrayList<FileParseCustomSearch> customSearch = new ArrayList<>();

        private void addDateFmt(Parser.DateFmt df) {
            dateSettings.add(df);
        }

        public ArrayList<Parser.DateFmt> getAllFormats() {
            return dateSettings;
        }

        private FileParseCustomSearch addfileParseSettings() {
            FileParseCustomSearch fileParseCustomSearch = new FileParseCustomSearch();
            customSearch.add(fileParseCustomSearch);
            return fileParseCustomSearch;
        }

    }

    static class FileParseCustomSearch {

        private boolean trimmed = false;
        private Pattern pt;
        private boolean handlerOnly = false;
        private boolean parseRest;
        private String name;

        public boolean isParseRest() {
            return parseRest;
        }

        public Pattern getPt() {
            return pt;
        }

        public String getPtString() {
            return ptString;
        }
        private boolean ignoreCase;
        private String ptString;

        public Matcher parseCustom(String str, int handlerID) {
            if (!handlerOnly || handlerID > 0) {
                Matcher m = pt.matcher(str);
//            Main.logger.info("Checking ["+str+"] against ["+ptString+"]");

                if (m != null && m.find()) {
//                Main.logger.error("Checking found!");
                    return m;
                }
            }
            return null;
        }

        public void setPtString(String ptString) {
            this.ptString = ptString;
        }

        public boolean isTrimmed() {
            return trimmed;
        }

        public void setTrimmed(String s) {
            if (s != null && !s.isEmpty()) {
                try {
                    trimmed = Boolean.parseBoolean(s);
                } catch (Exception e) {
                    Main.logger.error("Error parsing boolean from [" + s + "]. Defaults to " + trimmed, e);
                }
            }

        }

        public boolean isIgnoreCase() {
            return ignoreCase;
        }

        public void setIgnoreCase(boolean ignoreCase) {
            this.ignoreCase = ignoreCase;
        }

        public String[] getGroups() {
            return groups;
        }

        public void setGroups(String[] groups) {
            this.groups = groups;
        }
        private String[] groups;

        void setName(String attribute, String defName) {
            if (attribute != null && !attribute.isEmpty()) {
                this.name = attribute;
            } else {
                this.name = defName;
            }
        }

        public String getName() {
            return name;
        }

        String comps() {
            StringBuilder s = new StringBuilder();
            for (Map.Entry<String, ArrayList<SearchComponent>> entry : components.entrySet()) {
                String key = entry.getKey();
                ArrayList<SearchComponent> value = entry.getValue();
                s.append(s).append(": ");
                for (SearchComponent searchComponent : value) {
                    s.append(searchComponent.toString()).append(" ");
                }
            }
            return s.toString();
        }

        @Override
        public String toString() {
            return "{" + " name=" + name + ", " + ", components=" + comps() + '}';
        }

        class SearchComponent {

            @Override
            public String toString() {
                return "{" + "name=" + attrName + ", val=" + val + '}';
            }

            @Override
            public int hashCode() {
                return super.hashCode(); //To change body of generated methods, choose Tools | Templates.
            }

            private final String attrName;
            private final Integer val;
            private final boolean mustChange;
            private HashMap<Pattern, String> modifications = null;

            public boolean isMustChange() {
                return mustChange;
            }

            public String getAttrName() {
                return attrName;
            }

            public Integer getVal() {
                return val;
            }

            private SearchComponent(String key, int parseInt, String mustChange) {
                attrName = key;
                val = parseInt;
                this.mustChange = Boolean.parseBoolean(mustChange);
            }

            public HashMap<Pattern, String> getModifications() {
                return modifications;
            }

            void addModification(String search, String replace) {
                if (modifications == null) {
                    modifications = new HashMap<>();
                }
                Pattern p = Pattern.compile(search);
                modifications.put(p, replace);
            }
        }
        private HashMap<String, ArrayList<SearchComponent>> components = new HashMap<>();

        public HashMap<String, ArrayList<SearchComponent>> getComponents() {
            return components;
        }

        ArrayList<SearchComponent> addComponent(String name) {
            ArrayList<SearchComponent> attrs = new ArrayList<>();
            if (components.containsKey(name)) {
                Main.logger.error("Component [" + name + "] existed; replacing");
            }
            components.put(name, attrs);
            return attrs;
        }

        /**
         * Adding attributes to the component. Not very elegant but effective.
         * Putting it here to process all checks, etc, etc in separate method
         *
         * @param attrs
         * @param key
         * @param value
         */
        SearchComponent addAttributes(ArrayList<FileParseCustomSearch.SearchComponent> attrs, String key, String value, String mustChange) {
            SearchComponent ret = new FileParseCustomSearch.SearchComponent(key, Integer.parseInt(value), mustChange);
            attrs.add(ret);
            return ret;
        }

        /**
         * Should be called after all other parameters filled in, because this
         * method uses other parameters too
         */
        void compilePattern() {
            pt = Pattern.compile(ptString);
        }

        private void publishComponents(Matcher m) {
            for (Map.Entry<String, ArrayList<SearchComponent>> entry : components.entrySet()) {
                String key = entry.getKey();
                ArrayList<SearchComponent> value = entry.getValue();

            }
        }

        boolean boolAttr(String attr, boolean def) {
            if (attr != null) {
                boolean valRead = (attr.equalsIgnoreCase("yes") || attr.equalsIgnoreCase("true"));
                if (valRead) {
                    return valRead;
                }
                valRead = (attr.equalsIgnoreCase("no") || attr.equalsIgnoreCase("false"));
                if (valRead) {
                    return valRead;
                }
                Main.logger.entry("Attribute value [" + attr + "] not understood; using default [" + def + "]");
            }
            return def;
        }

        void setHandlerOnly(String attribute) {
            this.handlerOnly = boolAttr(attribute, false);
        }

        void setParseRest(String attribute) {
            this.parseRest = boolAttr(attribute, true);

        }

    }

    private HashMap<String, FileParseSettings> fileSettings = new HashMap<>();

}
