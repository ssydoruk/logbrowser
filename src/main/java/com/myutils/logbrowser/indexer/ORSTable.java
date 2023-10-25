/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.SQLException;

/**
 * @author ssydoruk
 */
public class ORSTable extends DBTable {

    public ORSTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t, "ORS_" + dbaccessor.getM_alias());
    }

    @Override
    public void InitDB() {

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
    }

    @Override
    public String getInsert() {
        return "INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";

    }

    /**
     * @throws Exception
     */
    @Override
    public void FinalizeDB() throws SQLException {

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


}
