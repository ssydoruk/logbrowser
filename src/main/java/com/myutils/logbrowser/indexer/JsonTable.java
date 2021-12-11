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
public class JsonTable extends DBTable {

    public JsonTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t,"json_" + dbaccessor.getM_alias());
    }

    @Override
    public void InitDB() {
        
        addIndex("FileId");
        addIndex("origUriID");
        addIndex("destUriID");
        addIndex("handlerID");
        addIndex("time");
        addIndex("SipsReqId");
        addIndex("SipId");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "inbound bit,"
                + "origUriID integer,"
                + "destUriID integer,"
                + "SipsReqId int,"
                + "FileId int,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "SipId int,"
                + "line int,"
                + "handlerID int"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO " + getTabName() + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?);");

    }

    @Override
    public void FinalizeDB() {
        createIndexes();
    }

    @Override
    public void AddToDB(Record rec) throws SQLException {
        JsonMessage theRec = (JsonMessage) rec;
        getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
            @Override
            public void fillStatement(PreparedStatement stmt) throws SQLException {
                stmt.setInt(1, theRec.getRecordID());

                stmt.setTimestamp(2, new Timestamp(theRec.GetAdjustedUsecTime()));
                stmt.setBoolean(3, theRec.isInbound());
                setFieldString(stmt, 4, theRec.GetOrigUri());
                setFieldInt(stmt, 5, Main.getRef(ReferenceType.SIPURI, theRec.GetOrigUri()));
                setFieldInt(stmt, 6, Main.getRef(ReferenceType.SIPURI, theRec.GetDestUri()));
                stmt.setInt(7, theRec.GetSipsId());
                stmt.setInt(8, rec.getFileID());
                stmt.setLong(9, theRec.getM_fileOffset());
                stmt.setLong(10, theRec.getFileBytes());
                stmt.setInt(11, theRec.getRecordID());
                stmt.setInt(12, theRec.getM_line());
                stmt.setInt(13, theRec.getHanglerID());

            }
        });
    }


}
