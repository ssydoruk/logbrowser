/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 *
 * @author ssydoruk
 */
public class QueryDialogSettings implements Serializable {

    private static final long serialVersionUID = 1L;

//        private ArrayList<IQueryResults> queries;
    private HashMap<String, DynamicTree> qParams;
//        private HashMap<String , DynamicTreeNode> qParams1;
//
//        public ArrayList<IQueryResults> getQueries() {
//            return queries;
//        }

    public QueryDialogSettings() {
//            queries = new ArrayList<IQueryResults>();
        qParams = new HashMap<>();
//            qParams1 = new HashMap<String, DynamicTreeNode>();
    }
//
//        public void addQuery(IQueryResults qry) {
//            queries.add(qry);
//        }
//

    public DynamicTree getQParams(String iq) {
        return qParams.get(iq);
    }
//        
//        private void resetqParams(){
//            qParams.clear();
//        }
//
//        public void setQParams(String className, DynamicTree tree) {
//            qParams.put(className, tree);
//        }
//
//        private void setQueries(QueryDialogSettings queryDialigSettings) {
//        }

//    ArrayList<DynamicTreeNode<OptionNode>> lst;
    public void setQueries(ArrayList<IQueryResults> queries) {
//            qParams1.clear();
//            for (IQueryResults query : queries) {
//                qParams1.put(query.getClass().getName(), new query.repComponents.getRoot());
//                return;
//            }
        DynamicTreeNode.setBlockLoad(true);
        for (IQueryResults query : queries) {
//                lst = new ArrayList<DynamicTreeNode<OptionNode>>( query.repComponents.build(GenericTreeTraversalOrderEnum.PRE_ORDER));
            qParams.put(query.getClass().getName(), new DynamicTree(query.repComponents));
//            qParams.put(query.getClass().getName(), query.repComponents);
            return;
        }
        DynamicTreeNode.setBlockLoad(false);
    }

    private void readObject(ObjectInputStream s)
            throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        LoadRefs();
    }

    public ArrayList<String> getSavedSearches() {
        return savedSearches;
    }

    private ArrayList<String> savedSearches = new ArrayList<>();
    private ArrayList<String> savedFilters = new ArrayList<>();

    public ArrayList<String> getSavedFilters() {
        return savedFilters;
    }

    public void setSavedFilters(ArrayList<String> savedFilters) {
        this.savedFilters = savedFilters;
    }

    void saveSearch(String selection) {
        addToList(selection, savedSearches);
    }

    private void LoadRefs() {
        inquirer.logger.debug("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void saveRegex(String selectedRegEx) {
        if (selectedRegEx != null && !selectedRegEx.isEmpty()) {
            addToList(selectedRegEx, savedFilters);
        }
    }

    private void addToList(String s, ArrayList<String> l) {
        if (s != null && !s.isEmpty()) {
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
}
