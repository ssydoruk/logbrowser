/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.myutils.logbrowser.inquirer;

import Utils.Pair;
import java.awt.Window;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JPanel;

/**
 *
 * @author ssydo
 */
abstract class AllInteractionsSettings extends RequestDialog {

    private static final long serialVersionUID = 1L;
    
    AllSelection stdOptions = null;

    public AllInteractionsSettings(Window parent) {
        super(parent);
    }

    public Integer getMaxRecords() {
        return stdOptions.getMaxRecords();
    }

    abstract ArrayList<Pair<String, String>> sortFieldsModel();

    public void setContent(JPanel filterPanel) {
        JPanel pan = new JPanel();
        pan.setLayout(new BoxLayout(pan, BoxLayout.PAGE_AXIS));
        pan.add(filterPanel);

        ArrayList<filterField> items = new ArrayList<>();
        for ( Pair<String, String> f : sortFieldsModel()) {
            items.add(new filterField(f.getKey(), f.getValue()));
        }
        stdOptions = new AllSelection(new DefaultComboBoxModel(items.toArray()));
        pan.add(stdOptions);
        setContentPanel(pan);
    }

    public boolean isAscendingSorting() {
        return stdOptions.isAscendingSorting();
    }

    public String getSortField() {
        return stdOptions.getSortField();
    }

    @SuppressWarnings("serial")
    class filterField extends Pair<String, String> {

        public filterField(String key, String value) {
            super(key, value);
        }

        @Override
        public String toString() {
            return getKey();
        }

    }
}
