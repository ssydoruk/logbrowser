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
public class ProxiedTable extends DBTable {

    public ProxiedTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t,"proxied_" + dbaccessor.getM_alias());
    }

    @Override
    public void InitDB() {
        
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
    public void AddToDB(Record _rec) throws SQLException {
        ProxiedMessage rec = (ProxiedMessage) _rec;
        getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
            @Override
            public void fillStatement(PreparedStatement stmt) throws SQLException {
                stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
                stmt.setInt(2, rec.getFileID());
                stmt.setLong(3, rec.getM_fileOffset());
                stmt.setLong(4, rec.getM_FileBytes());
                stmt.setLong(5, rec.getM_line());

                stmt.setLong(6, rec.getRefID());
                setFieldInt(stmt, 7, Main.getRef(ReferenceType.TEvent, rec.getEvent()));
                setFieldInt(stmt, 8, Main.getRef(ReferenceType.App, rec.getTo()));
                setFieldInt(stmt, 9, Main.getRef(ReferenceType.App, rec.getFrom()));
                setFieldInt(stmt, 10, Main.getRef(ReferenceType.DN, rec.cleanDN(rec.getDn())));
                stmt.setInt(11, rec.getM_tlibId());
                stmt.setInt(12,rec.getM_handlerId());
            }
        });
    }


}
