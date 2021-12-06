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
public class ISCCTable extends DBTable {

    public ISCCTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        setTabName("ISCC_" + getM_dbAccessor().getM_alias());
        addIndex("time");
        addIndex("FileId");
        addIndex("nameID");
        addIndex("thisDNID");
        addIndex("otherDNID");
        addIndex("ConnectionIDID");
        addIndex("ReferenceId");
        addIndex("FileId");
        addIndex("srcTServerID");
        addIndex("srcSwitchID");
        addIndex("otherConnectionIDID");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "nameID INTEGER,"
                + "thisDNID INTEGER,"
                + "otherDNID INTEGER,"
                + "ConnectionIDID INTEGER,"
                + "ReferenceId bigint,"
                + "Inbound bit,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "HandlerId INTEGER"
                + ",line int"
                + ",srcTServerID int"
                + ",srcSwitchID int"
                + ",otherConnectionIDID int"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?,?,?,?,?,?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ");");

    }

    @Override
    public void FinalizeDB() {
        createIndexes();
    }

    @Override
    public void AddToDB(Record rec) throws SQLException {
        IsccMessage theRec = (IsccMessage) rec;
         getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
                @Override
                public void fillStatement(PreparedStatement stmt) throws SQLException{
            stmt.setTimestamp(1, new Timestamp(theRec.GetAdjustedUsecTime()));
            setFieldInt(stmt, 2, Main.getRef(ReferenceType.ISCCEvent, theRec.GetMessageName()));
            setFieldInt(stmt, 3, Main.getRef(ReferenceType.DN, rec.cleanDN(theRec.getThisDN())));
            setFieldInt(stmt, 4, Main.getRef(ReferenceType.DN, rec.cleanDN(theRec.getOtherDN())));
            setFieldInt(stmt, 5, Main.getRef(ReferenceType.ConnID, theRec.GetConnID()));
            stmt.setLong(6, theRec.getRefID());
            stmt.setBoolean(7, theRec.isInbound());
            stmt.setInt(8, rec.getFileID());
            stmt.setLong(9, theRec.getM_fileOffset());
            stmt.setLong(10, theRec.getFileBytes());
            stmt.setInt(11, theRec.getM_handlerId());
            stmt.setInt(12, theRec.getM_line());
            setFieldInt(stmt, 13, Main.getRef(ReferenceType.App, theRec.GetSrcApp()));
            setFieldInt(stmt, 14, Main.getRef(ReferenceType.Switch, theRec.GetSrcSwitch()));
            setFieldInt(stmt, 15, Main.getRef(ReferenceType.ConnID, theRec.GetOtherConnID()));

                }
         });
    }



}
