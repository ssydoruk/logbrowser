/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import Utils.UTCTimeRange;
import com.myutils.logbrowser.indexer.ReferenceType;
import static com.myutils.logbrowser.inquirer.inquirer.checkIntr;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 *
 * @author ssydoruk
 */
public class AggrOCSDBServerDelays extends IAggregateQuery {

    public AggrOCSDBServerDelays() throws SQLException {
        setJpConfig(new AggrOCSDBSErverDelaysConfig());
    }

    @Override
    public String getName() {
        return "OCS database roundtrip time";
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
//        TLibSIPRetransmitsConfig cfg = (TLibSIPRetransmitsConfig) getJpConfig();
//        setMaxRecords(2000);
//        DynamicTreeNode<OptionNode> attrRoot = cfg.getAttrRoot();

        resetOutput();
        tellProgress("Building temp table 1");
        String tab1 = resetTempTable();

        Wheres wh = new Wheres();
//        int appID = cfg.getAppID();
//        wh.addWhere(IQuery.getAttrsWhere("nameid", ReferenceType.SIPMETHOD, FindNode(attrRoot, DialogItem.TLIB_CALLS_SIP_NAME, null, null)), "AND");
//        wh.addWhere(IQuery.getAttrsWhere("peeripid", ReferenceType.IP, FindNode(attrRoot, DialogItem.TLIB_CALLS_SIP_PEERIP, null, null)), "AND");
//        wh.addWhere(IQuery.getAttrsWhere(new String[]{"FromUserpartID", "requestURIDNid"}, ReferenceType.DN, FindNode(attrRoot, DialogItem.TLIB_CALLS_SIP_ENDPOINT, null, null)), "AND");
//        wh.addWhere("fileid in (select id from file_logbr where appnameid = " + appID + ")", "AND");
        IQueryResults selectedQuery = qd.getSelectedQuery();
        selectedQuery.getReportItems();
        wh.addWhere("reqid>0", "AND");
        DynamicTreeNode<OptionNode> node = FindNode(selectedQuery.getReportItems().getRoot(), DialogItem.OCS, DialogItem.OCSCALL_DATABASE, null);
        wh.addWhere(IQuery.getCheckedWhere("dbreqid",
                ReferenceType.DBRequest,
                node,
                DialogItem.OCS_DATABASE_REQUESTTYPE, true), "AND");

        wh.addWhere(IQuery.getCheckedWhere("dbserverid",
                ReferenceType.App,
                node,
                DialogItem.OCS_DATABASE_DBSERVER, true), "AND");

        node = FindNode(selectedQuery.getReportItems().getRoot(), DialogItem.OCSCG, DialogItem.OCSCG_CAMPAIGNGROUP, null);

        Wheres wh1 = new Wheres();
        wh1.addWhere(IQuery.getCheckedWhere("campaignNameID",
                ReferenceType.OCSCAMPAIGN,
                node,
                DialogItem.OCSCG_CAMPAIGNGROUP_CAMPAIGN));

        wh.addWhere(" +(campaignDBID in (select campaigndbid from ocscg_logbr " + wh1.makeWhere(true) + "))",
                "AND");
        wh.addWhere("ocsdb_logbr", "time", qd.getTimeRange(), "AND");
//        wh.addWhere(IQuery.getCheckedWhere(DatabaseConnector.CampaignDBIDtoName + "( campaignDBID)",
//                node,
//                DialogItem.OCSCG_CAMPAIGNGROUP_CAMPAIGN), "AND");
        DatabaseConnector.runQuery("create temp table " + tab1 + " as\n"
                + "select min(time) time, reqid from ocsdb_logbr \n"
                + wh.makeWhere(true)
                + "\n"
                + "group by reqid;\n"
                + "\n"
                + "create index " + tab1 + "_time on " + tab1 + "(time);\n"
                + "create index " + tab1 + "_reqid on " + tab1 + "(reqid);\n");
        checkIntr();
        DatabaseConnector.runQuery("analyze " + tab1 + ";");
        if (inquirer.logger.isDebugEnabled()) {
            Integer[] iDs = DatabaseConnector.getIDs("select count(*) from " + tab1);
            inquirer.logger.info("affected records: " + ((iDs != null && iDs.length > 0) ? iDs[0] : "[NULL]"));
        }
        checkIntr();

        wh = new Wheres();
        wh.addWhere("o.reqid>0", "AND");
        wh.addWhere("o", "time", qd.getTimeRange(), "AND");

//        appID = cfg.getAppID();
//
//        wh.addWhere(IQuery.getAttrsWhere("nameid", ReferenceType.SIPMETHOD, FindNode(attrRoot, DialogItem.TLIB_CALLS_SIP_NAME, null, null)), "AND");
//        wh.addWhere(IQuery.getAttrsWhere("peeripid", ReferenceType.IP, FindNode(attrRoot, DialogItem.TLIB_CALLS_SIP_PEERIP, null, null)), "AND");
//        wh.addWhere(IQuery.getAttrsWhere(new String[]{"FromUserpartID", "requestURIDNid"}, ReferenceType.DN, FindNode(attrRoot, DialogItem.TLIB_CALLS_SIP_ENDPOINT, null, null)), "AND");
//        wh.addWhere("fileid in (select id from file_logbr where appnameid = " + appID + ")", "AND");
        tellProgress("Building temp table 2");
        String tab2 = resetTempTable();

        DatabaseConnector.runQuery("create temp table " + tab2 + " as\n"
                + "select id, o.reqid, o.time-t.time diff from ocsdb_logbr o\n"
                + "inner join " + tab1 + " t\n"
                + "on t.reqid=o.reqid and o.time>t.time\n"
                + wh.makeWhere(true)
                + ";"
                + "\n"
                + "create index " + tab2 + "_id on " + tab2 + "(id);\n"
        );
        checkIntr();
        DatabaseConnector.runQuery("analyze " + tab2 + ";");
        checkIntr();

        addParameter(MsgType.OCSDB, new OutputSpecFormatter.DatabaseParameter("duration", "duration"));
        addParameter(MsgType.OCSDB, new OutputSpecFormatter.DatabaseParameter("diff_ms", "duration_ms"));
        tellProgress("extracting data");
        TableQuery OCSDB = new TableQuery(MsgType.OCSDB, "ocsdb_logbr");

        wh = new Wheres();
        wh.addWhere(OCSDB.getTabAlias() + ".reqid in (select reqid from " + tab2 + ")");

        wh1 = new Wheres();
        wh1.addWhere(IQuery.getCheckedWhere("campaignNameID",
                ReferenceType.OCSCAMPAIGN,
                node,
                DialogItem.OCSCG_CAMPAIGNGROUP_CAMPAIGN, true));

        wh.addWhere(" +(campaignDBID in (select campaigndbid from ocscg_logbr " + wh1.makeWhere(true) + "))",
                "AND");

        node = FindNode(selectedQuery.getReportItems().getRoot(), DialogItem.OCS, DialogItem.OCSCALL_DATABASE, null);
        wh.addWhere(IQuery.getCheckedWhere("dbreqid",
                ReferenceType.DBRequest,
                node,
                DialogItem.OCS_DATABASE_REQUESTTYPE, true), "AND");

        wh.addWhere(IQuery.getCheckedWhere("dbserverid",
                ReferenceType.App,
                node,
                DialogItem.OCS_DATABASE_DBSERVER, true), "AND");

        OCSDB.addJoin(tab2, IQuery.FieldType.Optional, "t.id=" + OCSDB.getTabAlias() + ".id", null, "t");
        OCSDB.addRef("dbserverid", "dbserver", ReferenceType.App.toString(), IQuery.FieldType.Optional);
        OCSDB.addRef("dbreqid", "dbreq", ReferenceType.DBRequest.toString(), IQuery.FieldType.Optional);

        OCSDB.addOutField(DatabaseConnector.CampaignDBIDtoName + "(CampaignDBID) campaignName");
        OCSDB.addOutField(DatabaseConnector.GroupDBIDtoName + "( GroupDBID) groupName");
        OCSDB.addOutField(DatabaseConnector.ListDBIDtoName + "( listDBID) listName");
        OCSDB.addOutField("diff diff_ms");
        OCSDB.addOutField("jduration( diff) duration");
        OCSDB.addWhere(wh);

        OCSDB.setQueryLimit(isLimitQueryResults(), getMaxQueryLines());
        OCSDB.setTimeRange(qd.getTimeRange());
        OCSDB.setSearchApps(qd.getSearchApps());
        OCSDB.setCommonParams(this, qd);
        getRecords(OCSDB);
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
