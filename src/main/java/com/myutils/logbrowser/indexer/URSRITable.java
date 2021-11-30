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
public class URSRITable extends DBTable {

    public URSRITable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {

        setTabName("ursri");
        addIndex("time");
        addIndex("FileId");
        addIndex("connidid");
        addIndex("clientappID");
        addIndex("clientNo");
        addIndex("ORSSidID");
        addIndex("funcID");
        addIndex("subfuncID");
        addIndex("ref");
        addIndex("UUIDID");
        addIndex("URLID");

        URSRI.addIndexes(this);

        dropIndexes();

        String query = "create table if not exists  " + getTabName() + "  (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",connidid integer"
                + ",clientappID INTEGER"
                + ",clientNo INTEGER"
                + ",ORSSidID INTEGER"
                + ",funcID INTEGER"
                + ",subfuncID INTEGER"
                + ",ref INTEGER"
                + ",inbound bit"
                + ",UUIDID INTEGER"
                + ",URLID INTEGER"
                + URSRI.getTableFields()
                + ");";
        getM_dbAccessor().runQuery(query);

        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO  " + getTabName() + "  VALUES(NULL,?,?,?,?,?"
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
                + URSRI.getTableFieldsAliases()
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
        URSRI rec = (URSRI) _rec;
        URSRITable tab = this;
         getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
                @Override
                public void fillStatement(PreparedStatement stmt) throws SQLException{
            stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(2, URSRI.getFileId());
            stmt.setLong(3, rec.getM_fileOffset());
            stmt.setLong(4, rec.getM_FileBytes());
            stmt.setLong(5, rec.getM_line());

            setFieldInt(stmt, 6, Main.getRef(ReferenceType.ConnID, rec.getConnID()));
            setFieldInt(stmt, 7, Main.getRef(ReferenceType.App, rec.getClientApp()));
            stmt.setInt(8, rec.getClientID());
            setFieldInt(stmt, 9, Main.getRef(ReferenceType.ORSSID, rec.getORSSID()));
            setFieldInt(stmt, 10, Main.getRef(ReferenceType.URSMETHOD, rec.getFunc()));
            setFieldInt(stmt, 11, Main.getRef(ReferenceType.URSMETHOD, rec.getSubFunc()));
            stmt.setInt(12, rec.getRefid());
            stmt.setBoolean(13, rec.isInbound());
            setFieldInt(stmt, 14, Main.getRef(ReferenceType.UUID, rec.getUUID()));
            setFieldInt(stmt, 15, Main.getRef(ReferenceType.HTTPRequest, rec.getUriParams()));

            URSRI.setTableValues(16, stmt, rec, tab);

//                setFieldString(stmt,8, rec.sid);
                        }
        });
    }



}
