/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import Utils.UTCTimeRange;
import com.myutils.logbrowser.indexer.ReferenceType;
import com.myutils.logbrowser.indexer.TableType;
import com.myutils.logbrowser.inquirer.IQuery.FieldType;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.ArrayList;

import static com.myutils.logbrowser.inquirer.inquirer.getCr;

/**
 * @author ssydoruk
 */
public class GREAggrRequestsPerSecond extends IAggregateAggregate {

    private boolean isTLibReport;

    public GREAggrRequestsPerSecond(boolean hasSIP) {
        setJpConfig(new AggrGREMessagePerSecondConfig(hasSIP));
    }

    @Override
    public String getName() {
        return "Messages per period";
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
//        select datetime(time/1000,'unixepoch', 'localtime') ts,* from ocs_logbr
        AggrGREMessagePerSecondConfig cfg = (AggrGREMessagePerSecondConfig) getJpConfig();
//        setMaxRecords(2000);

        ArrayList<Integer> searchApps = qd.getSearchApps(true);
        if (searchApps == null || searchApps.isEmpty()) {
            return;
        }
        Integer[] appIDs = searchApps.toArray(new Integer[qd.getSearchApps(true).size()]);

        String[] names = DatabaseConnector.getNames(this, ReferenceType.AppType.toString(), "name",
                IQueryResults.getWhere("id", "select apptypeid from file_logbr" + getWhere("appnameid", appIDs, true),
                        true));
                runTLibReport(cfg, appIDs);
//            if (cfg.isTLibReport()) {
//                runTLibReport(cfg, appIDs, tabName);
//            } else if (cfg.isSIPReport()) {
//                runSIPReport(cfg, appIDs);
//            }
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
    public ArrayList<NameID> getApps() throws Exception {
        return null;
    }

    private void runTLibReport(AggrGREMessagePerSecondConfig cfg, Integer[] appIDs) throws SQLException {
        isTLibReport = true;
        DynamicTreeNode<OptionNode> attrRoot = cfg.getAttrTLibRoot();
        Wheres wh = new Wheres();
        wh.addWhere(IQuery.getAttrsWhere("nameid", ReferenceType.TEvent, FindNode(attrRoot, DialogItem.IXN_IXN_EVENT_NAME, null, null)), "AND");
        wh.addWhere(IQuery.getAttrsWhere("sourceid", ReferenceType.App, FindNode(attrRoot, DialogItem.IXN_IXN_EVENT_APP, null, null)), "AND");
        wh.addWhere(IQuery.getAttrsWhere("ixnqueueid", ReferenceType.DN, FindNode(attrRoot, DialogItem.IXN_IXN_EVENT_DN, null, null)), "AND");
        wh.addWhere(IQuery.getAttrsWhere("strategyid", ReferenceType.URSStrategyName, FindNode(attrRoot, DialogItem.URS_STRATEGY, null, null)), "AND");

        wh.addWhere("fileid in (select id from file_logbr " + getWhere("appnameid", appIDs, true) + ")", "AND");
        wh.addWhere(TableType.GREClient, "time", qd.getTimeRange(), "AND");
        inquirer.logger.info("Building temp tables");
        String tab = resetTempTable();
        DBField[] grpBy = cfg.getGroupByTLib();
        DBField[] orderBy = cfg.getOrderByTLib();
        String sGrpBy = "";

        if (grpBy.length > 0) {
            String[] flds = new String[grpBy.length];
            for (int i = 0; i < grpBy.length; i++) {
                flds[i] = grpBy[i].getFieldName();
            }
            sGrpBy = StringUtils.join(flds, ", ");
        }
        DatabaseConnector.runQuery("create temp table " + tab + " as"
                //                    + "\nselect datetime(time/1000,'unixepoch', 'localtime') ts, "

                + "\nselect "
                + ((cfg.getSamplePeriod() > 0) ? "strftime('%Y-%m-%d %H:%M:%S', time/1000-(time/1000 % " + cfg.getSamplePeriod() + "), 'unixepoch', 'localtime')"
                : "0") + " ts, \n"
                + ((sGrpBy.length() > 0) ? sGrpBy + "," : "")
                + " count(*) cnt from "
                + TableType.GREClient
                + "\n"
                + wh.makeWhere(true)
                + "\n group by 1" + ((sGrpBy.length() > 0) ? "," + sGrpBy : "")
                + "\nhaving count(*) > " + cfg.getSeconds()
                + ";"
        );

        for (DBField fld : grpBy) {
            DatabaseConnector.runQuery("create index idx_" + fld.getFieldName() + " on " + tab + "(" + fld.getFieldName() + ");");
        }
        inquirer.logger.info("extracting data");
        resultQuery = new AnyQuery();

        resultQuery.setTable(tab);
        resultQuery.addOutField(resultQuery.getTable() + ".ts", "ts");
        resultQuery.addOutField(resultQuery.getTable() + ".cnt", "cnt");

        for (DBField fld : grpBy) {
            if (fld.getFieldName().equals("inbound")) {
                resultQuery.addOutField("(case (" + resultQuery.getTable() + ".inbound) when 1 then 'inbound' else 'outbound' end)", "direction");
            } else {
                resultQuery.addRef(fld.getFieldName(), fld.getDbOutField(), fld.getRefTable(), FieldType.OPTIONAL);
            }
        }

        try {
            resultQuery.Execute();

            if (getCr().isSaveFileShort()) {
                PrintStream ps = null;
                try {
                    String fileName = getCr().getFileNameShort();
                    inquirer.logger.info("Short output goes to " + fileName);
                    ps = new PrintStream(new FileOutputStream(fileName));
                    ps.println(StringUtils.join(resultQuery.getFieldNames(), ","));
                    String[] GetNextStringArray;
                    while ((GetNextStringArray = resultQuery.GetNextStringArray()) != null) {
                        ps.println(StringUtils.join(GetNextStringArray, ","));
                    }
                } catch (FileNotFoundException ex) {
                    inquirer.ExceptionHandler.handleException("Cannot open file ", ex);

                } finally {
                    if (ps != null) {
                        ps.close();
                    }
                }

            }
        } finally {
            resultQuery.close();
        }
    }

    private void runSIPReport(AggrSIPServerCallsPerSecondConfig cfg, Integer[] appIDs) throws SQLException {
        isTLibReport = false;
        DynamicTreeNode<OptionNode> attrRoot = cfg.getAttrRoot();
        Wheres wh = new Wheres();
        wh.addWhere(IQuery.getAttrsWhere("nameid", ReferenceType.SIPMETHOD, FindNode(attrRoot, DialogItem.TLIB_CALLS_SIP_NAME, null, null)), "AND");
        wh.addWhere(IQuery.getAttrsWhere("peeripid", ReferenceType.IP, FindNode(attrRoot, DialogItem.TLIB_CALLS_SIP_PEERIP, null, null)), "AND");
        wh.addWhere(IQuery.getAttrsWhere(new String[]{"FromUserpartID", "requestURIDNid"}, ReferenceType.DN, FindNode(attrRoot, DialogItem.TLIB_CALLS_SIP_ENDPOINT, null, null)), "AND");
        wh.addWhere(IQuery.getAttrsWhere("inbound", FindNode(attrRoot, DialogItem.TLIB_CALLS_SIP_DIRECTION, null, null)), "AND");
        wh.addWhere("time", qd.getTimeRange(), "AND");
        wh.addWhere(IQuery.getFileFilters(null, "fileid", qd.getSearchApps(true)), "AND");

//        getLimitClause(boolean _limitQueryResults, int _maxQueryLines) 
        String tab = resetTempTable();
        inquirer.logger.info("Building temp tables");
        DBField[] grpBy = cfg.getGroupBy();
        DBField[] orderBy = cfg.getOrderBy();
        String sGrpBy = "";

        if (grpBy.length > 0) {
            String[] flds = new String[grpBy.length];
            for (int i = 0; i < grpBy.length; i++) {
                flds[i] = grpBy[i].getFieldName();
            }
            sGrpBy = StringUtils.join(flds, ", ");
        }
        DatabaseConnector.runQuery("create temp table " + tab + " as"
                + "\nselect datetime(time/1000,'unixepoch', 'localtime') ts, "
                + ((sGrpBy.length() > 0) ? sGrpBy + "," : "")
                + " count(*) cnt from sip_logbr\n"
                + wh.makeWhere(true)
                + "\n group by 1" + ((sGrpBy.length() > 0) ? "," + sGrpBy : "")
                + ";"
        );

        for (DBField fld : grpBy) {
            DatabaseConnector.runQuery("create index idx_" + fld.getFieldName() + " on " + tab + "(" + fld.getFieldName() + ");");
        }
        inquirer.logger.info("extracting data");
        resultQuery = new AnyQuery();

        resultQuery.setTable(tab);
        resultQuery.addOutField(resultQuery.getTable() + ".ts", "ts");
        resultQuery.addOutField(resultQuery.getTable() + ".cnt", "cnt");

        for (DBField fld : grpBy) {
            if (fld.getFieldName().equals("inbound")) {
                resultQuery.addOutField("(case (" + resultQuery.getTable() + ".inbound) when 1 then 'inbound' else 'outbound' end)", "direction");
            } else {
                resultQuery.addRef(fld.getFieldName(), fld.getDbOutField(), fld.getRefTable(), FieldType.MANDATORY);
            }
        }

    }

    @Override
    public boolean needPrint() {
        return false;
    }

    @Override
    public String getReportSummary() {
        return ((isTLibReport) ? "TLibrary " : "SIP") + " messages per second ";
    }

    @Override
    public UTCTimeRange getTimeRange() throws SQLException {
        return null;
    }

    @Override
    public UTCTimeRange refreshTimeRange(ArrayList<Integer> searchApps) throws SQLException {
        return null;
    }

    @Override
    public String getShortName() {
        return ((isTLibReport) ? "TLib" : "SIP") + "msgPerSec";
    }
}
