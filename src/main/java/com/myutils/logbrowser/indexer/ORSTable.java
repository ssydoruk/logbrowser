/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import static Utils.Util.StripQuotes;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 *
 * @author ssydoruk
 */
public class ORSTable extends DBTable {

    public ORSTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        setTabName("ORS_" + getM_dbAccessor().getM_alias());
        addIndex("TransferIDID");
        addIndex("time");
        addIndex("uuidID");
        addIndex("thisDNID");
        addIndex("otherDNID");
        addIndex("nameID");
        addIndex("FileId");
        addIndex("seqno");
        addIndex("agentIDID");
//        addIndex("errMessageID");
//        addIndex("attr1ID");
//        addIndex("attr2ID");
        addIndex("ConnectionIDID");
        addIndex("sourceid");
//        addIndex("istserverreq");
        addIndex("ORScallID");
        addIndex("DNISID");
        addIndex("ANIID");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "nameID INTEGER,"
                + "thisDNID INTEGER,"
                + "otherDNID INTEGER,"
                + "agentIDID INTEGER,"
                + "ConnectionIDID INTEGER,"
                + "TransferIdID INTEGER,"
                + "ReferenceId INTEGER,"
                + "Inbound bit,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "HandlerId INTEGER,"
                + "line int,"
                + "uuidID INTEGER,"
                + "seqno integer,"
                + "sourceID INTEGER,"
                + "IsTServerReq bit,"
                + "errMessageID integer"
                + ",attr1ID integer"
                + ",attr2ID integer"
                + ",ORScallID integer"
                + ",DNISID integer"
                + ",ANIID integer"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);");
    }

    /**
     *
     * @throws Exception
     */
    @Override
    public void FinalizeDB() throws Exception {

//        getM_dbAccessor().runQuery("create index if not exists ors_time_" + getM_dbAccessor().getM_alias() +" on ors_" + getM_dbAccessor().getM_alias() + " (time);");
        createIndexes();
//        if (getM_dbAccessor().TableEmpty("ORS_logbr") || getM_dbAccessor().TableEmpty("tlib_logbr")) {
//            return;
//        }
//        Main.getMain().finalizeTable(TableType.TLib);
//        Main.getMain().finalizeTable(TableType.File);
//
//        createIndex("istserverreq");
//        String ORSReq = "SELECT ors.thisdnid as thisdn, ors.referenceid as refid, files.appnameid as app, ORS.id as orsid \n"
//                + "FROM ORS_logbr AS ORS \n"
//                + "INNER JOIN FILE_logbr AS files ON ORS.fileId=files.id \n"
//                + "WHERE ors.istserverreq=1 ORDER BY ORS.time;";
//        String TLibReq = "select tlib.ConnectionIDID, files.appnameid as app from tlib_logbr as tlib "
//                + "INNER JOIN FILE_logbr AS files ON tlib.fileId=files.id \n"
//                + "where thisdnid=? and referenceid=? and sourceid=? ";
//        String ORSUpd = "update ORS_logbr set ConnectionIDid=?, sourceid=? where id=?";
//        PreparedStatement stmtTLib = null;
//        PreparedStatement stmtORS = null;
//        ResultSet rsTLib;
//
//        ORSrecs = getM_dbAccessor().GetRecords(ORSReq);
//        Connection conn = getM_dbAccessor().getM_conn();
//        conn.setAutoCommit(false);
//        stmtTLib = conn.prepareStatement(TLibReq);
//        stmtORS = conn.prepareStatement(ORSUpd);
//
//        int updates = 0;
//        try {
//            synchronized (conn) {
//                try {
//
//                    while (ORSrecs.next()) {
//                        stmtTLib.setString(1, StrOrEmpty(ORSrecs.getString(1)));
//                        stmtTLib.setString(2, StrOrEmpty(ORSrecs.getString(2)));
//                        stmtTLib.setString(3, StrOrEmpty(ORSrecs.getString(3)));
//
//                        rsTLib = stmtTLib.executeQuery();
//
//                        if (rsTLib.next()) {
//                            stmtORS.setString(1, StrOrEmpty(rsTLib.getString(1)));
//                            stmtORS.setString(2, rsTLib.getString(2));
//                            stmtORS.setString(3, ORSrecs.getString(4));
//                            stmtORS.executeUpdate();
//                            if (updates++ >= 1000) {
//                                conn.commit();
//                                conn.setAutoCommit(false);
//                                Main.logger.debug("Commit2");
//                                updates = 0;
//                            }
//                        } else {
////                            System.out.println("\nTLib not found");
//                        }
//                        // Do whatever you want to do with these 2 values
//                    }
//                } catch (SQLException E) {
//                    Main.logger.error("Exception finalizing: " + E.getMessage(), E);
//                    throw E;
//                } finally {
//                    Main.logger.debug("Commit3");
//                    conn.commit();
//                    if (stmtTLib != null) {
//                        stmtTLib.close();
//                    };
//                    if (stmtORS != null) {
//                        stmtORS.close();
//                    }
//                }
//            }
//            conn.setAutoCommit(true);
//
//        } catch (Exception E) {
//            Main.logger.error("Exception closing: " + E.getMessage(), E);
//            throw E;
//        } finally {
//            ORSrecs.close();
//        }
//        createIndex("ConnectionIDID");
//        createIndex("sourceid");

    }

    @Override
    public void AddToDB(Record rec) {
        ORSMessage orsRec = (ORSMessage) rec;
        PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

        try {
            stmt.setTimestamp(1, new Timestamp(orsRec.GetAdjustedUsecTime()));

            setFieldInt(stmt, 2, Main.getRef(ReferenceType.TEvent, orsRec.GetMessageName()));
            setFieldInt(stmt, 3, Main.getRef(ReferenceType.DN, Record.cleanDN(orsRec.getM_ThisDN())));
            setFieldInt(stmt, 4, Main.getRef(ReferenceType.DN, Record.cleanDN(orsRec.getAttribute("AttributeOtherDN"))));
            setFieldInt(stmt, 5, Main.getRef(ReferenceType.Agent, orsRec.getHeaderTrim("AttributeAgentID")));

            setFieldInt(stmt, 6, Main.getRef(ReferenceType.ConnID, orsRec.GetConnID()));
            setFieldInt(stmt, 7, Main.getRef(ReferenceType.ConnID, orsRec.getAttributeTrim("AttributeTransferConnID")));
            setFieldInt(stmt, 8, orsRec.getM_refID());
            stmt.setBoolean(9, orsRec.isInbound());
            setFieldInt(stmt, 10, ORSMessage.getFileId());
            stmt.setLong(11, orsRec.getM_fileOffset());
            stmt.setLong(12, orsRec.getFileBytes());
            stmt.setInt(13, 0);
            stmt.setInt(14, orsRec.getM_line());
            setFieldInt(stmt, 15, Main.getRef(ReferenceType.UUID, StripQuotes(orsRec.getAttributeTrim("AttributeCallUUID", 32))));
            setFieldLong(stmt, 16, orsRec.getAttributeHex("AttributeEventSequenceNumber"));
            setFieldInt(stmt, 17, Main.getRef(ReferenceType.App, orsRec.getM_TserverSRC()));
            stmt.setBoolean(18, orsRec.isIsTServerReq());
            setFieldInt(stmt, 19, Main.getRef(ReferenceType.TLIBERROR, orsRec.getErrorMessage(orsRec.GetMessageName())));
            setFieldInt(stmt, 20, Main.getRef(ReferenceType.TLIBATTR1, orsRec.getAttr1()));
            setFieldInt(stmt, 21, Main.getRef(ReferenceType.TLIBATTR2, orsRec.getAttr2()));
            setFieldInt(stmt, 22, orsRec.getORSCallID());
            setFieldInt(stmt, 23, Main.getRef(ReferenceType.DN, Record.cleanDN(orsRec.getDNIS())));
            setFieldInt(stmt, 24, Main.getRef(ReferenceType.DN, Record.cleanDN(orsRec.getANI())));

            getM_dbAccessor().SubmitStatement(m_InsertStatementId);
        } catch (SQLException e) {
            Main.logger.error("Could not add message type " + getM_type() + ": " + e, e);
        }

    }

}
