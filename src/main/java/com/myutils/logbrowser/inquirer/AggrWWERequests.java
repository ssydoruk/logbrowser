/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import com.myutils.logbrowser.inquirer.IQuery.FieldType;
import Utils.UTCTimeRange;
import static com.myutils.logbrowser.inquirer.inquirer.getCr;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.ArrayList;
import org.apache.commons.lang3.StringUtils;
import com.myutils.logbrowser.indexer.ReferenceType;
import com.myutils.logbrowser.indexer.TableType;

/**
 *
 * @author ssydoruk
 */
public class AggrWWERequests extends IAggregateAggregate {

    private boolean isTLibReport;

    public AggrWWERequests() {
        setJpConfig(new AggrWWEURIPerSecondConfig());
    }

    @Override
    public String getName() {
        return "WWE messages per period";
    }

    public void actionPerformed(ActionEvent e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void propertyChange(PropertyChangeEvent evt) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    void runQuery() throws SQLException {
//        select datetime(time/1000,'unixepoch', 'localtime') ts,* from ocs_logbr
        AggrWWEURIPerSecondConfig cfg = (AggrWWEURIPerSecondConfig) getJpConfig();
//        setMaxRecords(2000);

        ArrayList<Integer> searchApps = qd.getSearchApps(true);
        if (searchApps == null || searchApps.isEmpty()) {
            return;
        }
        Integer[] appIDs = searchApps.toArray(new Integer[qd.getSearchApps(true).size()]);

//        String[] names = DatabaseConnector.getNames(this, ReferenceType.AppType.toString(), "name",
//                IQueryResults.getWhere("id", "select apptypeid from file_logbr" + getWhere("appnameid", appIDs, true),
//                        true));
//        String tabName = QueryTools.TLibTableByFileType(names[0]);
//        if (tabName == null || tabName.length() == 0) {
//            JOptionPane.showMessageDialog(null, "No table for the application ");
//        } else {
//            if (cfg.isTLibReport()) {
        runReport(cfg, appIDs, TableType.WWEMessage.toString());
//            }
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
    public ArrayList<NameID> getApps() throws Exception {
        return null;
    }

    private void runReport(AggrWWEURIPerSecondConfig cfg, Integer[] appIDs, String tabName) throws SQLException {
        isTLibReport = true;
        DynamicTreeNode<OptionNode> attrRoot = cfg.getAttrRoot();
        Wheres wh = new Wheres();

        wh.addWhere(IQuery.getAttrsWhere("IPID", ReferenceType.IP, FindNode(attrRoot, DialogItem.WWEDEBUGMESSAGE_PARAMS_IP, null, null)), "AND");
        wh.addWhere(IQuery.getAttrsWhere("urlID", ReferenceType.HTTPURL, FindNode(attrRoot, DialogItem.WWEDEBUGMESSAGE_PARAMS_HTTPURL, null, null)), "AND");
        wh.addWhere(IQuery.getAttrsWhere("LevelID", ReferenceType.MSGLEVEL, FindNode(attrRoot, DialogItem.WWEDEBUGMESSAGE_PARAMS_LEVEL, null, null)), "AND");
        wh.addWhere(IQuery.getAttrsWhere("classNameID", ReferenceType.JAVA_CLASS, FindNode(attrRoot, DialogItem.WWEDEBUGMESSAGE_PARAMS_JAVACLASS, null, null)), "AND");
        wh.addWhere(IQuery.getAttrsWhere("msgTextID", ReferenceType.Misc, FindNode(attrRoot, DialogItem.WWEDEBUGMESSAGE_PARAMS_MESSAGE, null, null)), "AND");
        wh.addWhere(IQuery.getAttrsWhere("UserIDID", ReferenceType.Agent, FindNode(attrRoot, DialogItem.WWEDEBUGMESSAGE_PARAMS_USER, null, null)), "AND");

        wh.addWhere("fileid in (select id from file_logbr " + getWhere("appnameid", appIDs, true) + ")", "AND");
        wh.addWhere(tabName, "time", qd.getTimeRange(), "AND");
        inquirer.logger.info("Building temp tables");
        String tab = resetTempTable();
        DBField[] grpBy = cfg.getGroupByFields();
        DBField[] orderBy = cfg.getOrderByFields();
        String sGrpBy = "";
        String sOrderBy = "";

        if (orderBy != null && orderBy.length > 0) {
            sOrderBy = StringUtils.join(orderBy, ", ");
        }
        if (grpBy.length > 0) {
            String[] flds = new String[grpBy.length];
            for (int i = 0; i < grpBy.length; i++) {
                flds[i] = grpBy[i].getDbField();
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
                + tabName
                + "\n"
                + wh.makeWhere(true)
                + "\n group by 1" + ((sGrpBy.length() > 0) ? "," + sGrpBy : "")
                + "\nhaving count(*) > " + cfg.getSeconds()
                + ";"
        );

        if (grpBy.length > 0) {
            for (DBField fld : grpBy) {
                DatabaseConnector.runQuery("create index idx_" + fld.getDbField() + " on " + tab + "(" + fld.getDbField() + ");");
            }
        }
        inquirer.logger.info("extracting data");
        resultQuery = new AnyQuery();

        resultQuery.setTable(tab);
        resultQuery.addOutField(resultQuery.getTable() + ".ts", "ts");
        resultQuery.addOutField(resultQuery.getTable() + ".cnt", "cnt");

        if (grpBy.length > 0) {
            for (DBField fld : grpBy) {
                resultQuery.addRef(fld.getDbField(), fld.getDbOutField(), fld.getRefTable(), FieldType.Optional);
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
