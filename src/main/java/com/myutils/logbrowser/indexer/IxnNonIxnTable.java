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
public class IxnNonIxnTable extends DBTable {

    public IxnNonIxnTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
//        setTabName("Ixn");
        addIndex("FileId");
        addIndex("time");
        addIndex("nameID");
        addIndex("IxnID");
        addIndex("PARENTIXNIDID");
        addIndex("MediaTypeID");
        addIndex("IxnQueueID");
        addIndex("RefId");
        addIndex("connIDID");
        addIndex("appid");
        addIndex("AgentID");
        addIndex("PlaceID");
        addIndex("attr1ID");
        addIndex("attr2ID");
        addIndex("userEventID");
        addIndex("thisDNID");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",nameID INTEGER"
                + ",IxnID INTEGER"
                + ",MediaTypeID INTEGER"
                + ",IxnQueueID INTEGER"
                + ",RefId INTEGER"
                + ",connIDID INTEGER"
                + ",inbound bit"
                + ",PARENTIXNIDID INTEGER"
                + ",appid INTEGER"
                + ",AgentID INTEGER"
                + ",PlaceID INTEGER"
                + ",attr1ID INTEGER"
                + ",attr2ID INTEGER"
                + ",userEventID INTEGER"
                + ",thisDNID INTEGER"
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
        IxnNonIxn rec = (IxnNonIxn) _rec;
        PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

        try {
            stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(2, Ixn.getFileId());
            stmt.setLong(3, rec.getM_fileOffset());
            stmt.setLong(4, rec.getM_FileBytes());
            stmt.setLong(5, rec.getM_line());

            setFieldInt(stmt, 6, Main.getRef(ReferenceType.TEvent, rec.GetMessageName()));
            setFieldInt(stmt, 7, Main.getRef(ReferenceType.IxnID, rec.GetIxnID()));
            setFieldInt(stmt, 8, Main.getRef(ReferenceType.IxnMedia, rec.GetMedia()));
            setFieldInt(stmt, 9, Main.getRef(ReferenceType.DN, Record.cleanDN(rec.GetIxnQueue())));
            stmt.setLong(10, rec.getM_refID());
            setFieldInt(stmt, 11, Main.getRef(ReferenceType.ConnID, rec.GetConnID()));
            stmt.setBoolean(12, rec.isInbound());
            setFieldInt(stmt, 13, Main.getRef(ReferenceType.IxnID, rec.GetParentIxnID()));
            setFieldInt(stmt, 14, Main.getRef(ReferenceType.App, rec.GetClient()));
            setFieldInt(stmt, 15, Main.getRef(ReferenceType.Agent, rec.GetAgent()));
            setFieldInt(stmt, 16, Main.getRef(ReferenceType.Place, rec.GetPlace()));
            setFieldInt(stmt, 17, Main.getRef(ReferenceType.TLIBATTR1, rec.getAttr1()));
            setFieldInt(stmt, 18, Main.getRef(ReferenceType.TLIBATTR2, rec.getAttr2()));
            setFieldInt(stmt, 19, Main.getRef(ReferenceType.TEvent, rec.getUserEvent()));
            setFieldInt(stmt, 20, Main.getRef(ReferenceType.DN, Record.cleanDN(rec.getThisDN())));
            getM_dbAccessor().SubmitStatement(m_InsertStatementId);
        } catch (SQLException e) {
            Main.logger.error("Could not add record type " + m_type.toString() + ": " + e, e);
        }
    }

}
