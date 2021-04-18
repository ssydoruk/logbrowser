/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 * @author ssydoruk
 */
public class CIFaceRequestTable extends DBTable {

    public CIFaceRequestTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        setTabName("cireq_" + getM_dbAccessor().getM_alias());
        addIndex("time");
        addIndex("FileId");
        addIndex("nameID");
        addIndex("thisDNID");
        addIndex("otherDNID");
        addIndex("HandlerId");
        addIndex(new String[]{"nameid", "refid", "thisdnid", "otherdnid"});

        dropIndexes();

        String query = "CREATE TABLE IF NOT EXISTS cireq_" + getM_dbAccessor().getM_alias() + " ("
                + "id INTEGER PRIMARY KEY ASC,"
                + "nameID INTEGER,"
                + "refId bigint,"
                + "thisDNID INTEGER,"
                + "otherDNID INTEGER,"
                + "HandlerId INTEGER,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "Line INTEGER);";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO cireq_" + getM_dbAccessor().getM_alias() + " VALUES(NULL,?,?,?,?,?,?,?,?);");
    }

    @Override
    public void FinalizeDB() {
        createIndexes();

    }

    @Override
    public void AddToDB(Record rec) {
        CIFaceRequest theRec = (CIFaceRequest) rec;
        PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

        if (theRec.getM_refId() == null) {
            return;
        }
        try {
            setFieldInt(stmt, 1, Main.getRef(ReferenceType.TEvent, theRec.getM_Name()));
            stmt.setLong(2, theRec.getM_refId());
            setFieldInt(stmt, 3, Main.getRef(ReferenceType.DN, CIFaceRequest.SingleQuotes(theRec.getM_thisDN())));
            setFieldInt(stmt, 4, Main.getRef(ReferenceType.DN, CIFaceRequest.SingleQuotes(theRec.getM_otherDN())));
            stmt.setInt(5, CIFaceRequest.m_handlerId);
            stmt.setInt(6, CIFaceRequest.getFileId());
            stmt.setLong(7, theRec.getM_fileOffset());
            stmt.setInt(8, theRec.getM_line());
            Main.logger.trace("theRec.m_handlerId:" + CIFaceRequest.m_handlerId);

            getM_dbAccessor().SubmitStatement(m_InsertStatementId);
        } catch (SQLException e) {
            Main.logger.error("Could not add CIFace:Request record: " + e, e);
        }

    }

}
