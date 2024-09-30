/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import Utils.UTCTimeRange;
import com.myutils.logbrowser.indexer.ReferenceType;
import com.myutils.logbrowser.indexer.TableType;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.sql.SQLException;
import java.util.ArrayList;

import static com.myutils.logbrowser.inquirer.inquirer.checkIntr;

/**
 * @author ssydoruk
 */
public class AggrGREClientDelays extends IAggregateQuery {

    public AggrGREClientDelays() {
        setJpConfig(new GREClientDelaysConfig());
    }

    @Override
    public String getName() {
        return "GRE client request/response time";
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

        GREClientDelaysConfig cfg = (GREClientDelaysConfig) getJpConfig();
        ArrayList<Integer> searchApps = qd.getSearchApps(true);
        if (searchApps == null || searchApps.isEmpty()) {
            return;
        }
        Integer[] appIDs = searchApps.toArray(new Integer[qd.getSearchApps(true).size()]);

        String[] names = DatabaseConnector.getNames(this, ReferenceType.AppType.toString(), "name",
                IQueryResults.getWhere("id", "select apptypeid from file_logbr" + getWhere("appnameid", appIDs, true),
                        true));

        resetOutput();
        tellProgress("Building temp table 1");
        String tab1 = resetTempTable();
        String tab = TableType.GREClient.toString();

        Wheres wh = new Wheres();

        IQueryResults selectedQuery = qd.getSelectedQuery();
        selectedQuery.getReportItems();
        wh.addWhere(IQuery.getFileFilters(tab, "fileid", qd.getSearchApps(false)), "AND");
        wh.addWhere(IQuery.getDateTimeFilters(tab, "time", qd.getTimeRange()), "AND");
        wh.addWhere(getWhere("nameID", ReferenceType.TEvent, new String[]{"Event3rdServerResponse"}, false, false, false), "AND");

        DatabaseConnector.runQuery("create table " + tab1 + " as\n"
                + "select time, refid, f.AppNameID from " + tab + "\n"
                + "\ninner join file_logbr f on f.id=" + tab + ".fileid"
                + wh.makeWhere(true)
                + ";\n"
                + "create index " + tab1 + "_time on " + tab1 + "(time);\n"
                + "create index " + tab1 + "AppNameID on " + tab1 + "(AppNameID);\n"
                + "create index " + tab1 + "_refid on " + tab1 + "(refid);\n");

        checkIntr();
        DatabaseConnector.runQuery("analyze " + tab1 + ";");
        if (inquirer.logger.isDebugEnabled()) {
            Integer[] iDs = DatabaseConnector.getIDs("select count(*) from " + tab1);
            inquirer.logger.info("affected records: " + ((iDs != null && iDs.length > 0) ? iDs[0] : "[NULL]"));
        }
        checkIntr();

        tellProgress("extracting data");
        TableQuery GREDelays = new TableQuery(MsgType.GRE_DELAYS, tab);
        GREDelays.setAddAll(false);
        GREDelays.setAutoAddFile(false);
        String durationExpr = tab1 + ".time-" + GREDelays.getTabAlias() + ".time";
        wh = new Wheres();
//        wh.addWhere(GREDelays.getTabAlias() + ".reqid in (select reqid from " + tab2 + ")");
        wh.addWhere(durationExpr + ">" + cfg.getSeconds());
        GREDelays.addJoin("file_logbr", IQuery.FieldType.MANDATORY, "f.id=" + GREDelays.getTabAlias() + ".fileid", null, "f");
        GREDelays.addJoin(tab1, IQuery.FieldType.MANDATORY, tab1 + ".refid=" + GREDelays.getTabAlias() + ".refid AND " + tab1 + ".AppNameID=f.AppNameID", null, tab1);
        GREDelays.addOutField(GREDelays.getTabAlias() + ".id");
        GREDelays.addOutField(GREDelays.getTabAlias() + ".time");
        GREDelays.addOutField(durationExpr + " duration");
        
        GREDelays.addRef(GREDelays.getTabAlias() + ".ixnID", "idxID", ReferenceType.IxnID, IQuery.FieldType.MANDATORY);
//        GREDelays.addOutField("diff diff_ms");
        GREDelays.addOutField("jduration( "+durationExpr+") duration_read");
        GREDelays.addWhere(wh);

        GREDelays.setQueryLimit(isLimitQueryResults(), getMaxQueryLines());
        GREDelays.setTimeRange(qd.getTimeRange());
        GREDelays.setSearchApps(qd.getSearchApps());
        getRecords(GREDelays);
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
        return this.getClass().getSimpleName();
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
        return "SIP_retransm";
    }

}
