/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * @author ssydoruk
 */
public abstract class IAggregateQuery extends IQueryResults {

    private static final String TMP_TABLE_PREFIX = "tmp";
    private static int tmpTableIdx = 0;
    protected QueryDialog qd = null;
    protected AnyQuery resultQuery = null;
    private JPanel jpConfig = null;

    @Override
    boolean callRelatedSearch(IDsFinder cidFinder) throws SQLException {
        return cidFinder.AnythingTLibRelated();

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected ArrayList<IAggregateQuery> loadReportAggregates() {
        return null;
    }

    @Override
    public void showAllResults() {

    }

    @Override
    public void Retrieve(QueryDialog qd, SelectionType key, String searchID) throws SQLException {

    }

    @Override
    FullTableColors getAll(QueryDialog qd) {
        return null;
    }

    @Override
    SearchFields getSearchField() {
        return null;
    }

    public JPanel getJpConfig() {
        return jpConfig;
    }

    public void setJpConfig(JPanel jpConfig) {
        this.jpConfig = jpConfig;
    }

    abstract void runQuery() throws SQLException;

    @Override
    protected void Sort() {
        try {
            DbRecordComparator comparator = new DbRecordComparator();
            Collections.sort(m_results, comparator);

            comparator.BlockSort(m_results);
            comparator.Close();
        } catch (Exception e) {
            inquirer.ExceptionHandler.handleException("Could not sort results ", e);
        }
    }

    abstract public boolean needPrint();

    public String resetTempTable() {
        String ret = TMP_TABLE_PREFIX + (tmpTableIdx++);
        try {
            DatabaseConnector.dropTable(ret);
        } catch (SQLException ex) {
            inquirer.logger.error("Unable to drop table " + ret, ex);
        }

        return ret;
    }

    abstract public String getShortName();

    public String getExcelName() {
        String shortName = getShortName();
        if (shortName == null || shortName.isEmpty()) {
            shortName = "aggr";
        }
        return InquirerCfg.getFileUnique(shortName);
    }

    void setQueryDialog(QueryDialog aThis) {
        qd = aThis;
    }

}
