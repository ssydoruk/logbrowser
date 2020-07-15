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
public class URSRlibTable extends DBTable {

    public URSRlibTable(DBAccessor dbaccessor) {
        super(dbaccessor, TableType.URSRlib);
    }

    URSRlibTable(SqliteAccessor m_accessor, TableType tableType) {
        super(m_accessor, tableType);
    }

    @Override
    public void InitDB() {
        setTabName("URSRLIB");
        addIndex("time");
        addIndex("FileId");

        addIndex("refid");
        addIndex("reqid");
        addIndex("uuidid");
        addIndex("sidid");
        addIndex("methodid");
        addIndex("sourceid");
        addIndex("resultid");
        addIndex("paramid");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",refid INTEGER"
                + ",reqid INTEGER"
                + ",uuidid INTEGER"
                + ",sidid INTEGER"
                //                + ",moduleid INTEGER"
                + ",methodid INTEGER"
                + ",inbound bit"
                + ",sourceid INTEGER"
                + ",resultid INTEGER"
                + ",paramid INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?"
                /*standard first*/
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                //                + ",?"
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
        URSRlib rec = (URSRlib) _rec;
        PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

        try {
            stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(2, URSRlib.getFileId());
            stmt.setLong(3, rec.m_fileOffset);
            stmt.setLong(4, rec.getM_FileBytes());
            stmt.setInt(5, rec.m_line);

            setFieldInt(stmt, 6, rec.GetRefId());
            setFieldInt(stmt, 7, rec.GetReqId());
            setFieldInt(stmt, 8, Main.getRef(ReferenceType.UUID, rec.GetCall()));
            setFieldInt(stmt, 9, Main.getRef(ReferenceType.ORSSID, rec.GetSid()));
//            setFieldInt(stmt,10, Main.getRef(ReferenceType.ORSMODULE, rec.GetModule()));
            setFieldInt(stmt, 10, Main.getRef(ReferenceType.ORSMETHOD, rec.GetMethod()));
            stmt.setBoolean(11, rec.isInbound());
            setFieldInt(stmt, 12, Main.getRef(ReferenceType.App, rec.getSource()));
            setFieldInt(stmt, 13, Main.getRef(ReferenceType.URSRLIBRESULT, rec.getResult()));
            setFieldInt(stmt, 14, Main.getRef(ReferenceType.URSRLIBPARAM, rec.getParam()));

            getM_dbAccessor().SubmitStatement(m_InsertStatementId);
        } catch (SQLException e) {
            Main.logger.error("Could not add record type " + m_type.toString() + ": " + e, e);
        }
    }

}
