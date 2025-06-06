/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer.gui;

import com.myutils.logbrowser.inquirer.DatabaseConnector;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.HashSet;

import static com.myutils.logbrowser.inquirer.gui.JTableCommon.RECORD_EMPTY;

/**
 * @author ssydoruk
 */
@SuppressWarnings({"unchecked", "rawtypes", "serial", "deprecation"})
abstract class AColumnFilter {

    public AColumnFilter() {
    }

    static public double convertDouble(Object val) throws NumberFormatException {
        double l;
        if (val instanceof Integer) {
            l = ((Integer) val).doubleValue();
        } else {
            l = Double.parseDouble(val.toString());
        }
        return l;
    }

    abstract boolean showValue(Object val);

    public static class StringsFilter extends AColumnFilter {

        private HashSet<String> values;

        public StringsFilter() {
        }

        public StringsFilter(HashSet<String> selValues) {
            this.values = selValues;
        }

        @Override
        boolean showValue(Object val) {
            if (val == null || val.toString().isEmpty()) {
                return values.contains(RECORD_EMPTY);
            } else {
                return values.contains(val.toString());

            }
        }
    }

    public static class PositionalFilter extends AColumnFilter {

        private int row;
        private boolean isUp;

        public PositionalFilter() {
        }

        PositionalFilter(int popupRow, boolean up) {
            this.row = popupRow;
            this.isUp = up;
        }

        @Override
        boolean showValue(Object val) {
            return true;
        }
    }

    public static abstract class DateFilter extends AColumnFilter {

        private String name;
        private boolean needsSecond;
        private long firstDate;
        private long secondDate;
        private boolean showNonDate = false;

        public DateFilter(String name, boolean needsSecond) {
            this.name = name;
            this.needsSecond = needsSecond;
        }

        public long convertToTime(Object val) {
            return DatabaseConnector.convertToTime(val);
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isNeedsSecond() {
            return needsSecond;
        }

        public void setNeedsSecond(boolean needsSecond) {
            this.needsSecond = needsSecond;
        }

        public long getFirstDate() {
            return firstDate;
        }

        public void setFirstDate(long firstDate) {
            this.firstDate = firstDate;
        }

        public long getSecondDate() {
            return secondDate;
        }

        public void setSecondDate(long secondDate) {
            this.secondDate = secondDate;
        }

        public boolean isShowNonDate() {
            return showNonDate;
        }

        public void setShowNonDate(boolean showNonDate) {
            this.showNonDate = showNonDate;
        }

        public long chopMS(long convertToTime) {
//            double name = convertToTime/1000;
//            inquirer.logger.debug("convertToTime: "+convertToTime+" name: "+name+" chopped:"+name*1000+" chopped:"+((long)name*1000));
            return (long) ((double) convertToTime / 1000) * 1000;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    static public abstract class NumberFilter extends AColumnFilter {

        private String name;
        private boolean needsSecond;
        private double firstNum;
        private double secondNum;
        private boolean showNonNumbers = false;

        public NumberFilter(String name, boolean needsSecond) {
            this.name = name;
            this.needsSecond = needsSecond;
        }

        public boolean isShowNonNumbers() {
            return showNonNumbers;
        }

        public void setShowNonNumbers(boolean selected) {
            this.showNonNumbers = selected;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isNeedsSecond() {
            return needsSecond;
        }

        public void setNeedsSecond(boolean needsSecond) {
            this.needsSecond = needsSecond;
        }

        public double getFirstNum() {
            return firstNum;
        }

        public void setFirstNum(String firstNum) {
            if (firstNum != null && !firstNum.isEmpty()) {
                this.firstNum = Double.parseDouble(firstNum);
            } else {
                this.firstNum = 0;
            }
        }

        public double getSecondNum() {
            return secondNum;
        }

        public void setSecondNum(String secondNum) {
            if (secondNum != null && !secondNum.isEmpty()) {
                this.secondNum = Double.parseDouble(secondNum);
            } else {
                this.secondNum = 0;
            }
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static class FilterComboboxModel extends ArrayList<AColumnFilter> implements ComboBoxModel {

        private Object selectedItem;

        public FilterComboboxModel() {
            super();
        }

        @Override
        public Object getSelectedItem() {
            return selectedItem;
        }

        @Override
        public void setSelectedItem(Object anItem) {
            this.selectedItem = anItem;
        }

        @Override
        public int getSize() {
            return super.size();
        }

        @Override
        public Object getElementAt(int index) {
            return super.get(index);
        }

        @Override
        public void addListDataListener(ListDataListener l) {
        }

        @Override
        public void removeListDataListener(ListDataListener l) {

        }

    }
}
