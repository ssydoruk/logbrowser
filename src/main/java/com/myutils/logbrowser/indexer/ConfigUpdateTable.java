/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * @author ssydoruk
 * <p>
 * <p>
 * Make Refactor/Copy of this class for new table type
 */
public class ConfigUpdateTable extends DBTable {

    public ConfigUpdateTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t, "cfg");
    }

    @Override
    public void InitDB() {
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
     * @throws Exception
     */
    @Override
    public void FinalizeDB() throws Exception {
        createIndexes();

    }

    @Override
        public void AddToDB(Record _rec) throws SQLException {
        ConfigUpdateRecord rec = (ConfigUpdateRecord) _rec;
         getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
                @Override
                public void fillStatement(PreparedStatement stmt) throws SQLException{
             stmt.setInt(1, rec.getRecordID());
            stmt.setTimestamp(2, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(3, rec.getFileID());
            stmt.setLong(4, rec.getM_fileOffset());
            stmt.setLong(5, rec.getM_FileBytes());
            stmt.setLong(6, rec.getM_line());

            setFieldInt(stmt, 7, Main.getRef(ReferenceType.CfgOp, rec.GetOp()));
            setFieldInt(stmt, 8, Main.getRef(ReferenceType.CfgObjectType, rec.GetObjType()));
            stmt.setInt(9, rec.GetObjDBID());
            setFieldInt(stmt, 10, Main.getRef(ReferenceType.CfgObjName, rec.getObjName()));
            setFieldInt(stmt, 11, Main.getRef(ReferenceType.CfgMsg, rec.getMsg()));

                        }
        });
    }



}
