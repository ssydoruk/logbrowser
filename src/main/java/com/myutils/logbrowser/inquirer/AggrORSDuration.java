/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import Utils.UTCTimeRange;
import com.myutils.logbrowser.indexer.ReferenceType;
import com.myutils.logbrowser.inquirer.IQuery.FieldType;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;

/**
 *
 * @author ssydoruk
 */
public class AggrORSDuration extends IAggregateQuery {
    private static final String TMP_TABLE = "tmp1";
//    private static final String TMP_TABLE_OUT = "tmp2";

    public AggrORSDuration() throws SQLException {
        setJpConfig(new AggrORSDurationConfig());
    }

    @Override
    public String getName() {
        return "ORS events durations";
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
        resetOutput();
        AggrORSDurationConfig cfg = (AggrORSDurationConfig) getJpConfig();
//        setMaxRecords(2000);
        int appID = cfg.getAppID();
        runDurationReport(cfg, appID);
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


    private void runDurationReport(AggrORSDurationConfig cfg, int appID) throws SQLException {
        DynamicTreeNode<OptionNode> attrRoot = cfg.getAttrRoot();
        String tab;

//        whTEvent.addWhere("0=1", "AND");
        DatabaseConnector.dropTable(TMP_TABLE);
        inquirer.logger.info("Building temp tables");

        DynamicTreeNode<OptionNode> tEventsSelected = FindNode(attrRoot, DialogItem.ORS_TEVENTS_NAME, null, null);
        boolean isLibChecked = (tEventsSelected != null && ((OptionNode) tEventsSelected.getData()).isChecked());
        if (isLibChecked) {
            tab = "ors_logbr";
            Wheres whTEvent = new Wheres();
            whTEvent.addWhere(IQuery.getAttrsWhere("nameid", ReferenceType.TEvent, tEventsSelected), "AND");
            whTEvent.addWhere(tab + ".fileid in (select id from file_logbr where appnameid = " + appID + ")", "AND");
            whTEvent.addWhere(tab + ".time", cfg.getTimeRange(), "AND");

            DatabaseConnector.runQuery("create temp table " + TMP_TABLE + " as"
                    + "\nselect 0 as id, " + tab + ".time, " + tab + ".fileid, " + tab + ".line, " + tab + ".fileoffset, " + tab + ".filebytes, "
                    + ReferenceType.TEvent.toString() + ".name, orssess_logbr.sidid"
                    + "\nfrom " + tab
                    + "\ninner join " + ReferenceType.TEvent.toString() + " on " + ReferenceType.TEvent.toString() + ".id=" + tab + ".nameid"
                    + "\ninner join orssess_logbr on orssess_logbr.uuidid=" + tab + ".uuidid"
                    + "\n" + whTEvent.makeWhere(true)
                    + "\n" + QueryTools.getLimitClause(isLimitQueryResults(), getMaxQueryLines())
                    + "\n;"
            );
        }

        DynamicTreeNode<OptionNode> metricCheckedSelected = FindNode(attrRoot, DialogItem.ORS_STRATEGY_METRIC, null, null);
        boolean isMetricChecked = (metricCheckedSelected != null && ((OptionNode) metricCheckedSelected.getData()).isChecked());
        if (isMetricChecked) {
            tab = "orsmetr_logbr";
            Wheres whMetric = new Wheres();
            whMetric.addWhere(IQuery.getAttrsWhere("metricid", ReferenceType.METRIC, FindNode(attrRoot, DialogItem.ORS_STRATEGY_METRIC, null, null)), "AND");
            whMetric.addWhere("fileid in (select id from file_logbr where appnameid = " + appID + ")", "AND");
            whMetric.addWhere("time", cfg.getTimeRange(), "AND");

            DatabaseConnector.runQuery(((isLibChecked) ? "insert into " + TMP_TABLE : "create temp table " + TMP_TABLE + " as")
                    + "\nselect 0 as id, time, fileid, line, fileoffset,filebytes," + ReferenceType.METRIC.toString() + ".name, sidid"
                    + "\nfrom " + tab
                    + "\ninner join " + ReferenceType.METRIC.toString() + " on " + ReferenceType.METRIC.toString() + ".id=" + tab + ".metricid"
                    + "\n" + whMetric.makeWhere(true)
                    + "\n" + QueryTools.getLimitClause(isLimitQueryResults(), getMaxQueryLines())
                    + "\n;"
            );
        }
        if (isLibChecked || isMetricChecked) {

            DatabaseConnector.runQuery("create index idx_" + TMP_TABLE + " on " + TMP_TABLE + "(sidid, fileid, line);");

//                DatabaseConnector.dropTable(TMP_TABLE_OUT);
//                DatabaseConnector.runQuery("create temp table " + TMP_TABLE_OUT + " as"
//                        + "\nselect null as id, * from " + TMP_TABLE
//                        + "\norder by sidid, fileid, line;"
//                );
//            DatabaseConnector.runQuery("create index idx_" + TMP_TABLE_OUT + " on " + TMP_TABLE_OUT + "(sidid, fileid, line);");
//                for (String fld : new String[]{"sidid", "fileid", "line", "time", "name"}) {
//                    DatabaseConnector.runQuery("create index idx_" + TMP_TABLE_OUT + fld + " on " + TMP_TABLE_OUT + "(" + fld + ");");
//                }
            inquirer.logger.info("extracting data");

            TableQuery outTab = new TableQuery(MsgType.ORS_DELAYS, TMP_TABLE);
            outTab.addRef(outTab.getTabAlias() + ".sidid", "sid", ReferenceType.ORSSID.toString(), FieldType.Mandatory);
            outTab.setOrderBy(outTab.getTabAlias() + ".sidid, " + outTab.getTabAlias() + ".fileid, " + outTab.getTabAlias() + ".line");
            outTab.setCalculatedFields(new ICalculatedFields() {
                private long lastTime = 0;
                private int lastSidID = 0;
                private long startedTime = 0;

                @Override
                public Properties calc(Properties m_fieldsAll) {
                    Properties ret = new Properties();
                    long time = Long.parseLong(m_fieldsAll.getProperty("time"));
                    int sidid = Integer.parseInt(m_fieldsAll.getProperty("sidid"));
                    if (sidid != lastSidID) {
                        ret.put("duration", "");
                        ret.put("rollduration", "");
                        lastSidID = sidid;
                        startedTime = time;
                    } else {
                        ret.put("duration", Utils.Util.pDuration(time - lastTime));
                        ret.put("rollduration", Utils.Util.pDuration(time - startedTime));
                    }
                    lastTime = time;

                    return ret;
                }
            });
            getRecords(outTab);
        } else {
            inquirer.logger.info("No events selected, report not run");
        }

    }

    @Override
    public boolean needPrint() {
        return true;
    }

    @Override
    public String getReportSummary() {
        return getName();
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
        return "ORS_duration";
    }

}
