/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

/**
 * @author ssydoruk
 */
public class QueryDialogSettings implements Serializable {

    private static final long serialVersionUID = 1L;


    private HashMap<String, HashSet<String>> disabledApps;


    // private ArrayList<IQueryResults> queries;
    private final HashMap<String, DynamicTree<OptionNode>> qParams;
    // private HashMap<String , DynamicTreeNode> qParams1;
    //
    // public ArrayList<IQueryResults> getQueries() {
    // return queries;
    // }
    private final ArrayList<SearchItem> savedSearches = new ArrayList<>();
    private ArrayList<String> savedFilters = new ArrayList<>();

    public QueryDialogSettings() {
        // queries = new ArrayList<IQueryResults>();
        qParams = new HashMap<>();
        // qParams1 = new HashMap<String, DynamicTreeNode>();
    }
    //
    // public void addQuery(IQueryResults qry) {
    // queries.add(qry);
    // }
    //

    public DynamicTree<OptionNode> getQParams(String iq) {
        return qParams.get(iq);
    }
    //
    // private void resetqParams(){
    // qParams.clear();
    // }
    //
    // public void setQParams(String className, DynamicTree tree) {
    // qParams.put(className, tree);
    // }
    //
    // private void setQueries(QueryDialogSettings queryDialigSettings) {
    // }

    // ArrayList<DynamicTreeNode<OptionNode>> lst;
    public void setQueries(ArrayList<IQueryResults> queries) {
        // qParams1.clear();
        // for (IQueryResults query : queries) {
        // qParams1.put(query.getClass().getName(), new query.repComponents.getRoot());
        // return;
        // }
        DynamicTreeNode.setBlockLoad(true);
        for (IQueryResults query : queries) {
            // lst = new ArrayList<DynamicTreeNode<OptionNode>>(
            // query.repComponents.build(GenericTreeTraversalOrderEnum.PRE_ORDER));
            qParams.put(query.getClass().getName(), new DynamicTree<>(query.repComponents));
            // qParams.put(query.getClass().getName(), query.repComponents);
            return;
        }
        DynamicTreeNode.setBlockLoad(false);
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        LoadRefs();
    }

    public ArrayList<String> getSavedSearches() {
        ArrayList<String> ret = new ArrayList<>();
        for (SearchItem s : savedSearches) {
            String line = s.getSearch();
            if (!ret.contains(line)) {
                ret.add(line);
            }
        }
        return ret;
    }

    public ArrayList<String> getSavedFilters() {
        return savedFilters;
    }

    public void setSavedFilters(ArrayList<String> savedFilters) {
        this.savedFilters = savedFilters;
    }

    void saveSearch(String selection) {
        // if (StringUtils.isNotEmpty(selection) && !present(selection, savedSearches))
        if (StringUtils.isNotEmpty(selection)) {
            addToList(new SearchItem(selection), savedSearches);
            inquirer.saveToLocal();
        }
    }


    private boolean present(String selection, ArrayList<SearchItem> savedSearches) {
        for (SearchItem item : savedSearches) {
            if (StringUtils.equals(selection, item.getSearch())) {
                return true;
            }
        }
        return false;
    }

    private void LoadRefs() {
        inquirer.logger.debug("Not supported yet."); // To change body of generated methods, choose Tools | Templates.
    }

    void saveRegex(String selectedRegEx) {
        if (StringUtils.isNotEmpty(selectedRegEx)) {
            addToList(selectedRegEx, savedFilters);
        }
    }

    private <T> void addToList(T s, ArrayList<T> l) {
        if (s != null) {
            Collections.reverse(l);
            for (int i = 0; i < l.size(); i++) {
                if (l.get(i).equals(s)) {
                    l.remove(i);
                    break;
                }
            }
            l.add(s);
            Collections.reverse(l);
        }
    }

    public HashSet<String> getDisabledApps(IQueryResults qry) {
        return (disabledApps != null) ? disabledApps.get(getLastClassName(qry)) : null;
    }

    private String getLastClassName(IQueryResults qry) {
        String name = qry.getClass().getName();
        int idx = name.lastIndexOf(".");
        return (idx >= 0) ? name.substring(idx + 1) : name;

    }

    void saveUncheckedApps(IQueryResults qry, ArrayList<String> searchAppsName) {
        String name = getLastClassName(qry);
        if (disabledApps == null) {
            disabledApps = new HashMap<>();
        }
        if (searchAppsName != null && !searchAppsName.isEmpty()) {
            HashSet<String> apps = disabledApps.get(name);
            if (apps == null) {
                apps = new HashSet<>();
                disabledApps.put(name, apps);
            }
            apps.addAll(searchAppsName);
        }
        else {
            disabledApps.remove(name);
        }

    }

    private class SearchItem {

        private String search;
        private String saved_time;

        public String getSearch() {
            return search;
        }

        public String getSaved_time() {
            return saved_time;
        }

        public SearchItem(String savedSearch) {
            this.search = savedSearch;
            this.saved_time = dtf.format(LocalDateTime.now());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            SearchItem that = (SearchItem) o;
            return Objects.equals(search, that.search);
        }

        @Override
        public int hashCode() {
            return Objects.hash(search);
        }

        static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    }
}
