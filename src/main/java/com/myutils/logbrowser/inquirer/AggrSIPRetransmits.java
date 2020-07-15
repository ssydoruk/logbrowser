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
public class AggrSIPRetransmits extends IAggregateQuery {

    public AggrSIPRetransmits() {
        setJpConfig(new TLibSIPRetransmitsConfig());
    }

    @Override
    public String getName() {
        return "SIP retransmits";
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
        TLibSIPRetransmitsConfig cfg = (TLibSIPRetransmitsConfig) getJpConfig();
//        setMaxRecords(2000);
        DynamicTreeNode<OptionNode> attrRoot = cfg.getAttrRoot();
        Wheres wh = new Wheres();

        wh.addWhere(IQuery.getAttrsWhere("nameid", ReferenceType.SIPMETHOD, FindNode(attrRoot, DialogItem.TLIB_CALLS_SIP_NAME, null, null)), "AND");
        wh.addWhere(IQuery.getAttrsWhere("peeripid", ReferenceType.IP, FindNode(attrRoot, DialogItem.TLIB_CALLS_SIP_PEERIP, null, null)), "AND");
        wh.addWhere(IQuery.getAttrsWhere(new String[]{"FromUserpartID", "requestURIDNid"}, ReferenceType.DN, FindNode(attrRoot, DialogItem.TLIB_CALLS_SIP_ENDPOINT, null, null)), "AND");
        wh.addWhere("time", qd.getTimeRange(), "AND");
        wh.addWhere(IQuery.getFileFilters(null, "fileid", qd.getSearchApps(true)), "AND");

        inquirer.logger.info("Building temp tables");
        String tab = resetTempTable();

        DatabaseConnector.runQuery("create temp table " + tab + " as\n"
                + "select min(rowid) mrowid, nameid ni, viabranchid vb from sip_logbr \n"
                + wh.makeWhere(true)
                + "group by nameid, viabranchid;\n"
                + "\n"
                + "create index " + tab + "_mrowid1 on " + tab + "(mrowid);\n"
                + "create index " + tab + "_viabranchid1 on " + tab + "(vb);\n"
                + "create index " + tab + "_nameid1 on " + tab + "(ni);");
        checkIntr();
        DatabaseConnector.runQuery("analyze " + tab + ";");
        checkIntr();
//            DatabaseConnector.executeQuery("create temp table unique_via2 as\n"
//                    + "select min(rowid) mrowid, nameid, viabranchid from sip_logbr \n"
//                    + "group by nameid, viabranchid;\n");
//            DatabaseConnector.executeQuery("create index uv_mrowid1 on unique_via2(mrowid);\n");
//            DatabaseConnector.executeQuery("create index uv_viabranchid1 on unique_via2(viabranchid);\n");
//            DatabaseConnector.executeQuery("create index uv_nameid1 on unique_via2(nameid);");

        inquirer.logger.info("extracting data");
        SipForScCmQuery sipMsgsByCallid = new SipForScCmQuery(attrRoot);
        sipMsgsByCallid.addJoin(tab, IQuery.FieldType.Mandatory, "" + tab + ".ni =" + sipMsgsByCallid.getTabAlias() + ".nameID and " + tab + ".vb=" + sipMsgsByCallid.getTabAlias() + ".viabranchid",
                sipMsgsByCallid.getTabAlias() + ".rowid <> " + "" + tab + ".mrowid");
        sipMsgsByCallid.setOrderBy(cfg.getOrderBy());
        getRecords(sipMsgsByCallid);
        checkIntr();

//            TableQuery SIP = null;
//            SIP = new TableQuery(MsgType.SIPREGISTER, "sip_logbr");
//            SIP.addWhere(m_endTime, m_endTime);
//            SIP.addJoin("unique_via2", IQuery.FieldType.Mandatory, "unique_via2.nameid =" + SIP.getTabAlias() + ".nameID and unique_via2.viabranchid=" + SIP.getTabAlias() + ".viabranchid",
//                    SIP.getTabAlias() + ".rowid <> " + "unique_via2.mrowid");
//
//            SIP.addRef("ToUriID", "ToUri", ReferenceType.SIPURI.toString(), IQuery.FieldType.Optional);
//            SIP.addRef("FromUriID", "FromUri", ReferenceType.SIPURI.toString(), IQuery.FieldType.Optional);
//            SIP.addRef("ViaUriID", "ViaUri", ReferenceType.SIPURI.toString(), IQuery.FieldType.Optional);
//            SIP.addRef("callIDID", "callID", ReferenceType.SIPCALLID.toString(), IQuery.FieldType.Optional);
//            SIP.addRef(SIP.getTabAlias() + ".nameID", "name", ReferenceType.SIPMETHOD.toString(), IQuery.FieldType.Optional);
//            SIP.addRef("CSeqID", "CSeq", ReferenceType.SIPCSEQ.toString(), IQuery.FieldType.Optional);
//            SIP.addRef("FromUserpartID", "FromUserpart", ReferenceType.DN.toString(), IQuery.FieldType.Optional);
//            SIP.addRef(SIP.getTabAlias() + ".ViaBranchID", "ViaBranch", ReferenceType.SIPVIABRANCH.toString(), IQuery.FieldType.Optional);
//            SIP.addRef("PeerIpID", "PeerIp", ReferenceType.IP.toString(), IQuery.FieldType.Optional);
//
//            SIP.setOrderBy(SIP.getTabAlias() + ".callIDID, " + SIP.getTabAlias() + ".unixtime");
//            getRecords(SIP);
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
