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
public class ConfigUpdateTable extends DBTable {

    public ConfigUpdateTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        setTabName("cfg");
        addIndex("time");
        addIndex("FileId");

        addIndex("opid");
        addIndex("objtypeID");
        addIndex("objDBID");
        addIndex("objNameID");
        addIndex("msgID");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",opid integer"
                + ",objtypeID INTEGER"
                + ",objDBID INTEGER"
                + ",objNameID INTEGER"
                + ",msgID INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO " + getTabName() + " VALUES(?,?,?,?,?,?"
                /*standard first*/
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
        ConfigUpdateRecord rec = (ConfigUpdateRecord) _rec;
        PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

        try {
            generateID(stmt, 1, rec);
            stmt.setTimestamp(2, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(3, ConfigUpdateRecord.getFileId());
            stmt.setLong(4, rec.m_fileOffset);
            stmt.setLong(5, rec.getM_FileBytes());
            stmt.setLong(6, rec.m_line);

            setFieldInt(stmt, 7, Main.getRef(ReferenceType.CfgOp, rec.GetOp()));
            setFieldInt(stmt, 8, Main.getRef(ReferenceType.CfgObjectType, rec.GetObjType()));
            stmt.setInt(9, rec.GetObjDBID());
            setFieldInt(stmt, 10, Main.getRef(ReferenceType.CfgObjName, rec.getObjName()));
            setFieldInt(stmt, 11, Main.getRef(ReferenceType.CfgMsg, rec.getMsg()));

            getM_dbAccessor().SubmitStatement(m_InsertStatementId);
        } catch (SQLException e) {
            Main.logger.error("Could not add record type " + m_type.toString() + ": " + e, e);
        }
    }

}
