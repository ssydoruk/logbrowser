/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import Utils.UTCTimeRange;
import com.myutils.logbrowser.indexer.FileInfoType;
import com.myutils.logbrowser.indexer.ReferenceType;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * @author ssydoruk
 */
public class GREAggrTimestampDelays extends IAggregateQuery {

    private static final String TMP_TABLE = "tmp_aggrts";

    public GREAggrTimestampDelays() {
        setJpConfig(new TLibDelaysConfig());
    }

    @Override
    public String getName() {
        return "Timestamp delays";
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
        TLibDelaysConfig cfg = (TLibDelaysConfig) getJpConfig();
        ArrayList<Integer> searchApps = qd.getSearchApps(true);
        if (searchApps == null || searchApps.isEmpty()) {
            return;
        }
        Integer[] appIDs = searchApps.toArray(new Integer[qd.getSearchApps(true).size()]);

        String[] names = DatabaseConnector.getNames(this, ReferenceType.AppType.toString(), "name",
                IQueryResults.getWhere("id", "select apptypeid from file_logbr" + getWhere("appnameid", appIDs, true),
                        true));

        String tabName = FileInfoType.getTableName(QueryTools.TimeDiffByFileType(names[0]));
        if (tabName == null || tabName.length() == 0) {
            JOptionPane.showMessageDialog(null, "No table for the application ");
        } else {
            if (!DatabaseConnector.TableExist(tabName)) {
                throw new SQLException("table " + tabName + " does not exist;\ncannot run aggregate");
            }
            DatabaseConnector.checkIndexes(tabName, new String[]{"time", "fileid"});

            TableQuery genMsg = new TableQuery(MsgType.GENESYS_TIMESTAMP, tabName);
//        genMsg.setIdsSubQuery("fileid", "select id from file_logbr "+ getWhere("component", new Integer[] {ft.getValue()}, true));    
//                genMsg.setQueryLimit(isLimitQueryResults(), getMaxQueryLines());
            genMsg.setAddAll(true);
            genMsg.addOutField("jduration(prefDiff) duration");
            genMsg.addOutField("UTCtoDateTime(time-prefDiff, \"YYYY-MM-dd HH:mm:ss.SSS\") prevTimestamp");
            genMsg.addWhere("prefDiff > " + cfg.getSeconds(), "AND");
            genMsg.addWhere(genMsg.getTabAlias() + ".fileid in (select id from file_logbr " + getWhere("appnameid", appIDs, true) + ")", "AND");
            genMsg.setTimeRange(qd.getTimeRange());
            getRecords(genMsg);

//                inquirer.logger.info("Building temp tables");
//                DatabaseConnector.dropTable(TMP_TABLE);
//
//                UTCTimeRange timeRange = cfg.getTimeRange();
//                String tmpID = "tmpID";
//                DatabaseConnector.runQuery("create temp table " + TMP_TABLE + " as select id " + tmpID + ", time from " + tabName
//                        + " where fileid in (select id from file_logbr where appnameid = " + appID + ")"
//                        + " and time between " + timeRange.getStart() + " and " + timeRange.getEnd()
//                        + " order by time;");
//
//                DatabaseConnector.runQuery("create index idx_" + TMP_TABLE + "_id on " + TMP_TABLE + "(" + tmpID + ");");
//
//                TableQuery genMsg = new TableQuery(MsgType.GENESYS_TIMESTAMP, tabName);
////        genMsg.setIdsSubQuery("fileid", "select id from file_logbr "+ getWhere("component", new Integer[] {ft.getValue()}, true));    
//                genMsg.addOutFields(new String[]{
//                    genMsg.getTabAlias() + ".id id",
//                    genMsg.getTabAlias() + ".fileid fileid",
//                    genMsg.getTabAlias() + ".fileOffset fileOffset",
//                    genMsg.getTabAlias() + ".filebytes filebytes",
//                    genMsg.getTabAlias() + ".line line",
//                    genMsg.getTabAlias() + ".time time",
//                    //                    tabName + ".time seconds",
//                    "time1" + ".time-" + "time2" + ".time diff",
//                    "jduration(" + "time1" + ".time-" + "time2" + ".time) duration"
//                }
//                );
//                genMsg.addJoin(TMP_TABLE, FieldType.Mandatory, "time1" + "." + tmpID + "=" + genMsg.getTabAlias() + ".id", null, "time1");
//                genMsg.addJoin(TMP_TABLE, FieldType.Mandatory, "time1" + "." + "rowid+1" + "=" + "time2" + "." + "rowid", null, "time2");
////                genMsg.setQueryLimit(isLimitQueryResults(), getMaxQueryLines());
//                genMsg.addWhere("time1" + ".time - " + "time2" + ".time > " + cfg.getSeconds(), "AND");
//                genMsg.addWhere(genMsg.getTabAlias() + ".fileid in (select id from file_logbr where appnameid = " + appID + ")", "AND");
////                genMsg.setTimeRange(cfg.getTimeRange());
//                getRecords(genMsg);
        }

    }

    @Override
    public DynamicTree<OptionNode> getReportItems() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void Retrieve(QueryDialog dlg) throws SQLException {
        runQuery();
    }

    @Override
    public ArrayList<NameID> getApps() throws Exception {
        return null;
    }

    @Override
    public boolean needPrint() {
        return true;

    }

    @Override
    public String getReportSummary() {
        return " Time stamp for app ";
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
        return "TS_Delays";
    }
}
