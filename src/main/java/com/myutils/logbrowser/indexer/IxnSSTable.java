/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import static java.sql.Types.INTEGER;

/**
 * @author ssydoruk
 * <p>
 * <p>
 * Make Refactor/Copy of this class for new table type
 */
public class IxnSSTable extends DBTable {

    public IxnSSTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t,"IxnSS");
    }

    @Override
    public void InitDB() {
        addIndex("nameID");
        addIndex("IxnID");
        addIndex("PARENTIXNIDID");
        addIndex("MediaTypeID");
        addIndex("IxnQueueID");
        addIndex("RefId");
        addIndex("connIDID");
        addIndex("AgentID");
        addIndex("PlaceID");
        addIndex("RouterID");
        addIndex("ServiceID");
        addIndex("MethodID");
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
                + ",AgentID INTEGER"
                + ",PlaceID INTEGER"
                + ",RouterID INTEGER"
                + ",serverRef INTEGER"
                + ",clientRef INTEGER"
                + ",ServiceID INTEGER"
                + ",MethodID INTEGER"
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
     * @throws Exception
     */
    @Override
    public void FinalizeDB() throws Exception {
        createIndexes();
    }

    @Override
        public void AddToDB(Record _rec) throws SQLException {
        IxnSS rec = (IxnSS) _rec;
         getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
                @Override
                public void fillStatement(PreparedStatement stmt) throws SQLException{
            stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(2, rec.getFileID());
            stmt.setLong(3, rec.getM_fileOffset());
            stmt.setLong(4, rec.getM_FileBytes());
            stmt.setLong(5, rec.getM_line());

            setFieldInt(stmt, 6, Main.getRef(ReferenceType.TEvent, rec.GetMessageName()));
            setFieldInt(stmt, 7, Main.getRef(ReferenceType.IxnID, rec.GetIxnID()));
            setFieldInt(stmt, 8, Main.getRef(ReferenceType.IxnMedia, rec.GetMedia()));
            setFieldInt(stmt, 9, Main.getRef(ReferenceType.DN, rec.cleanDN(rec.GetIxnQueue())));
            stmt.setLong(10, rec.getM_refID());
            setFieldInt(stmt, 11, Main.getRef(ReferenceType.ConnID, rec.GetConnID()));
            stmt.setBoolean(12, rec.isInbound());
            setFieldInt(stmt, 13, Main.getRef(ReferenceType.IxnID, rec.GetParentIxnID()));
            setFieldInt(stmt, 14, Main.getRef(ReferenceType.Agent, rec.GetAgent()));
            setFieldInt(stmt, 15, Main.getRef(ReferenceType.Place, rec.GetPlace()));
            setFieldInt(stmt, 16, Main.getRef(ReferenceType.App, rec.GetClient()));

            setIntOrNull(stmt, 17, rec.GetServerRef(), -1);
            setIntOrNull(stmt, 18, rec.GetClientRef(), -1);

            setFieldInt(stmt, 19, Main.getRef(ReferenceType.IxnService, rec.GetService()));
            setFieldInt(stmt, 20, Main.getRef(ReferenceType.IxnMethod, rec.GetMethod()));
                        }
        });
    }



    private void setIntOrNull(PreparedStatement stmt, int rec, Object val, Object def) throws SQLException {
        if (val == null || val.equals(def)) {
            stmt.setNull(rec, INTEGER);
        } else {
            stmt.setObject(rec, val);
        }
    }

}
