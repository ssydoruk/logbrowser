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
 */
public class URSTargetSetTable extends DBTable {

    public URSTargetSetTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t,"urstargetset");
    }

    @Override
    public void InitDB() {

        
        addIndex("time");
        addIndex("FileId");
        addIndex("targetid");
        addIndex("objectid");
        addIndex("objectTypeID");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + "(id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "line int"
                /* standard first */
                + ",targetid INTEGER"
                + ",objectid INTEGER"
                + ",objectTypeID INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?"
                /*standard first*/
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
        URSTargetSet rec = (URSTargetSet) _rec;
        getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
            @Override
            public void fillStatement(PreparedStatement stmt) throws SQLException{
            stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(2, rec.getFileID());
            stmt.setLong(3, rec.getM_fileOffset());
            stmt.setLong(4, rec.getM_FileBytes());
            stmt.setLong(5, rec.getM_line());

            setFieldInt(stmt, 6, Main.getRef(ReferenceType.URSTarget, rec.getTarget()));
            setFieldInt(stmt, 7, Main.getRef(ReferenceType.CfgObjName, rec.getObject()));
            setFieldInt(stmt, 8, Main.getRef(ReferenceType.CfgObjectType, rec.getObjectType()));
//                setFieldString(stmt,11, rec.getCgMsg());

            }
        });
    }


}
