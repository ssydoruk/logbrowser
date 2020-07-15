/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import Utils.UTCTimeRange;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import com.myutils.logbrowser.indexer.FileInfoType;

/**
 *
 * @author ssydoruk
 */
public class AggrTLibAllCalls extends IAggregateQuery {

    public AggrTLibAllCalls() throws Exception {
        setJpConfig(new TLibCallsConfig());
    }

    @Override
    public String getName() {
        return "All calls";
    }

    public void actionPerformed(ActionEvent e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void propertyChange(PropertyChangeEvent evt) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    void runQuery() {
        TLibCallsConfig cfg = (TLibCallsConfig) getJpConfig();
        FileInfoType rt = cfg.getFt();
        switch (rt) {
            case type_OCS:
                inquirer.logger.info("report on outbound");
                break;

            case type_URS:
                inquirer.logger.info("report on urs");
                break;
            default:
                throw new UnsupportedOperationException("Not supported yet file type - " + rt.toString()); //To change body of generated methods, choose Tools | Templates.
        }
    }

    @Override
    public DynamicTree<OptionNode> getReportItems() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void Retrieve(QueryDialog dlg) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ArrayList<NameID> getApps() throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean needPrint() {
        return true;
    }

    @Override
    public String getReportSummary() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public UTCTimeRange getTimeRange() throws SQLException {
        return null;
    }

    @Override
    public UTCTimeRange refreshTimeRange(ArrayList<Integer> searchApps) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getShortName() {
        return "AllCalls";
    }

}
