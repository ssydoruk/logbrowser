/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 *
 * @author ssydoruk
 *
 *
 * Make Refactor/Copy of this class for new table type
 */
public class OrsHTTPTable extends DBTable {

    public OrsHTTPTable(DBAccessor dbaccessor) {
        super(dbaccessor, TableType.ORSHTTP);
    }

    @Override
    public void InitDB() {
        setTabName("orshttp");

        addIndex("time");
        addIndex("FileId");

        addIndex("sidID");
        addIndex("GMSServiceID");
        addIndex("URIID");
        addIndex("param1ID");
        addIndex("param2ID");
        dropIndexes();

        if (Main.isDbExisted()) {
            getM_dbAccessor().runQuery("drop index if exists orshttp_FileId;");
            getM_dbAccessor().runQuery("drop index if exists orshttp_sidID;");
            getM_dbAccessor().runQuery("drop index if exists orshttp_unixtime;");

        }
        String query = "create table if not exists orshttp (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",socket int"
                + ",http_bytes int"
                + ",ipID int"
                + ",sidID int"
                + ",namespaceID int"
                + ",Inbound bit"
                + ",GMSServiceID int"
                + ",URIID int"
                + ",param1ID int"
                + ",param2ID int"
                + ",httpresponse bigint"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO orshttp VALUES(NULL,?,?,?,?,?"
                /*standard first*/
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ");");

    }

    /**
     *
     * @throws Exception
     */
    @Override
    public void FinalizeDB() throws Exception {
        createIndexes();
    }

    @Override
    public void AddToDB(Record _rec) {
        OrsHTTP rec = (OrsHTTP) _rec;
        PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

        try {
            stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(2, OrsHTTP.getFileId());
            stmt.setLong(3, rec.getM_fileOffset());
            stmt.setLong(4, rec.getM_FileBytes());
            stmt.setLong(5, rec.getM_line());

            stmt.setInt(6, rec.getSocket());
            stmt.setInt(7, rec.getHTTPBytes());
            setFieldInt(stmt, 8, Main.getRef(ReferenceType.IP, rec.GetPeerIp()));
            setFieldInt(stmt, 9, Main.getRef(ReferenceType.ORSSID, rec.getSID()));
            setFieldInt(stmt, 10, Main.getRef(ReferenceType.ORSNS, rec.getNameSpace()));
            stmt.setBoolean(11, rec.isInbound());
            setFieldInt(stmt, 12, Main.getRef(ReferenceType.GMSService, rec.getGMSService()));
            setFieldInt(stmt, 13, Main.getRef(ReferenceType.HTTPRequest, rec.getHTTPRequest()));
            setFieldInt(stmt, 14, Main.getRef(ReferenceType.GMSMisc, rec.getParam1()));
            setFieldInt(stmt, 15, Main.getRef(ReferenceType.GMSMisc, rec.getParam2()));
            setFieldLong(stmt, 16, rec.getHTTPResponseID());
            getM_dbAccessor().SubmitStatement(m_InsertStatementId);
        } catch (SQLException e) {
            Main.logger.error("Could not add record type " + m_type.toString() + ": " + e, e);
        }
    }

}
