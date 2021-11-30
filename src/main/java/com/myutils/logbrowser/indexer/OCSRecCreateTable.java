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
public class OCSRecCreateTable extends DBTable {

    public OCSRecCreateTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        setTabName("OCSRECCR");
        addIndex("recHandle");
        addIndex("chID");
        addIndex("FileId");
        addIndex("campBID");
        addIndex("time");
        addIndex("listNameID");
        addIndex("tableNameID");
        addIndex("dnID");
        addIndex("actionID");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "line int"
                /* standard first */
                + ",recHandle integer"
                + ",chID integer"
                + ",chNum integer"
                + ",campBID integer"
                + ",recType integer"
                + ",listNameID integer"
                + ",tableNameID integer"
                + ",dnID integer"
                + ",actionID integer"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?"
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
        OCSRecCreate rec = (OCSRecCreate) _rec;
        getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
            @Override
            public void fillStatement(PreparedStatement stmt) throws SQLException{
            stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(2, OCSRecCreate.getFileId());
            stmt.setLong(3, rec.getM_fileOffset());
            stmt.setLong(4, rec.getM_FileBytes());
            stmt.setLong(5, rec.getM_line());

            stmt.setInt(6, rec.getrecHandle());
            stmt.setInt(7, rec.getchID());
            stmt.setInt(8, rec.getchNum());
            setFieldInt(stmt, 9, Main.getRef(ReferenceType.OCSCAMPAIGN, rec.getcamp()));
            stmt.setInt(10, rec.getrecType());
            setFieldInt(stmt, 11, Main.getRef(ReferenceType.OCSCallList, rec.getCallingList()));
            setFieldInt(stmt, 12, Main.getRef(ReferenceType.OCSCallListTable, rec.getTabName()));
            setFieldInt(stmt, 13, Main.getRef(ReferenceType.DN, rec.cleanDN(rec.getDN())));
            setFieldInt(stmt, 14, Main.getRef(ReferenceType.OCSRecordAction, rec.getAction()));

        }
    });
}


}
