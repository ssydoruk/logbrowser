/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import static com.myutils.logbrowser.indexer.Record.m_proxiedId;
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
public class ProxiedTable extends DBTable {

    public ProxiedTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        setTabName("proxied_" + getM_dbAccessor().getM_alias());
        addIndex("time");
        addIndex("FileId");
        addIndex("nameID");
        addIndex("refid");
        addIndex("destID");
        addIndex("sourceID");
        addIndex("thisdnID");
        addIndex("tlib_id");
        addIndex("HandlerId");
        dropIndexes();

        String query = "create table if not exists proxied_" + getM_dbAccessor().getM_alias() + " (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",refid int"
                + ",nameID int"
                + ",destID int"
                + ",sourceID int"
                + ",thisdnID int"
                + ",tlib_id INTEGER"
                + ",HandlerId INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO proxied_" + getM_dbAccessor().getM_alias() + " VALUES(NULL,?,?,?,?,?"
                /*standard first*/
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ");");

    }

    @Override
    public void FinalizeDB() throws Exception {
        createIndexes();
    }

    @Override
    public void AddToDB(Record _rec) {
        ProxiedMessage rec = (ProxiedMessage) _rec;
        PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

        try {
            stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(2, ProxiedMessage.getFileId());
            stmt.setLong(3, rec.getM_fileOffset());
            stmt.setLong(4, rec.getM_FileBytes());
            stmt.setLong(5, rec.getM_line());

            stmt.setLong(6, rec.getRefID());
            setFieldInt(stmt, 7, Main.getRef(ReferenceType.TEvent, rec.getEvent()));
            setFieldInt(stmt, 8, Main.getRef(ReferenceType.App, rec.getTo()));
            setFieldInt(stmt, 9, Main.getRef(ReferenceType.App, rec.getFrom()));
            setFieldInt(stmt, 10, Main.getRef(ReferenceType.DN, Record.cleanDN(rec.getDn())));
            stmt.setInt(11, ProxiedMessage.m_tlibId);
            stmt.setInt(12, ProxiedMessage.m_handlerId);
            m_proxiedId++;

            getM_dbAccessor().SubmitStatement(m_InsertStatementId);
        } catch (SQLException e) {
            Main.logger.error("Could not add record type " + m_type.toString() + ": " + e, e);
        }
    }

}
