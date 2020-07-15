/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import Utils.UTCTimeRange;
import static Utils.Util.pDuration;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

/**
 *
 * @author ssydoruk
 */
public class AggrJSONDuration extends IAggregateQuery {

    public AggrJSONDuration() {
//        setJpConfig(new TLibSIPRetransmitsConfig());
    }

    @Override
    public String getName() {
        return "JSON requests duration";
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
    void runQuery() throws SQLException {
//        setMaxRecords(2000);

        ILogRecord record = null;
//        try {
        JsonQuery jsonQuery = new JsonQuery();
        jsonQuery.setOrder("json.SipsReqId, json.time");
        jsonQuery.Execute();
        int SipsReqId = 0;
        int unixtime = 0;
        Date reqDate = null;
        for (record = jsonQuery.GetNext();
                record != null;
                record = jsonQuery.GetNext()) {
            if (record.GetFieldInt("inbound") == 0) {
                SipsReqId = record.GetFieldInt("sips_req");
                reqDate = record.getDateTime();
                record.m_fieldsAll.put("_duration", "");
            } else {
                Date reqDateEnd = record.getDateTime();
                int SipsReqIdResp = record.GetFieldInt("sips_req");
                if (SipsReqId == SipsReqIdResp && reqDate != null && reqDateEnd != null) {
                    long dur = reqDateEnd.getTime() - reqDate.getTime();
                    record.m_fieldsAll.put("_duration", pDuration(dur));
                }
                SipsReqId = 0;
                reqDate = null;
            }

            m_results.add(record);
        }
        jsonQuery.Reset();
//        } catch (Exception ex) {
//            inquirer.ExceptionHandler.handleException(this.getClass().toString(), ex);
//        }
    }

    @Override
    public DynamicTree<OptionNode> getReportItems() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void Retrieve(QueryDialog dlg) throws SQLException {
        runQuery();
//        Sort();
    }

    @Override
    public ArrayList<NameID> getApps() throws SQLException {
        return null;
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
        return "JSON_duration";
    }

}
