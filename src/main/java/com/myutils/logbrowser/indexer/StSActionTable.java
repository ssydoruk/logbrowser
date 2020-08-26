package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 *
 * @author akolo
 */
public class StSActionTable extends DBTable {

    String m_MessageName;

    public StSActionTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
//        m_MessageLines = newMessageLines;
//        m_MessageName = event;
//        m_MessageLines.add(0, event);

    }

    @Override
    public void InitDB() {
        setTabName("STSACTION_" + getM_dbAccessor().getM_alias());
        addIndex("time");
        addIndex("FileId");
        addIndex("nameID");
        addIndex("typeid");
        addIndex("valueid");
        addIndex("connidid");
        dropIndexes();

        String query = "create table if not exists STSACTION_" + getM_dbAccessor().getM_alias() + " (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "nameid integer,"
                + "typeid integer,"
                + "valueid integer,"
                + "connidid integer,"
                + "FileId INT,"
                + "FileOffset BIGINT,"
                + "FileBytes INT,"
                + "line INT);";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO STSACTION_" + getM_dbAccessor().getM_alias() + " VALUES(NULL,?,?,?,?,?,?,?,?,?);");
    }

    @Override
    public void AddToDB(Record aRec) {
        StSActionMessage theRec = (StSActionMessage) aRec;
        PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

        try {
            stmt.setTimestamp(1, new Timestamp(theRec.GetAdjustedUsecTime()));
            setFieldInt(stmt, 2, Main.getRef(ReferenceType.DN, Record.cleanDN(theRec.GetName())));
            setFieldInt(stmt, 3, Main.getRef(ReferenceType.DNTYPE, theRec.GetType()));
            setFieldInt(stmt, 4, Main.getRef(ReferenceType.StatEvent, theRec.GetValue()));
            setFieldInt(stmt, 5, Main.getRef(ReferenceType.ConnID, theRec.GetConnID()));
            setFieldInt(stmt, 6, StSActionMessage.getFileId());
            stmt.setLong(7, theRec.m_fileOffset);
            stmt.setLong(8, theRec.GetFileBytes());
            setFieldInt(stmt, 9, theRec.m_line);

            getM_dbAccessor().SubmitStatement(m_InsertStatementId);
        } catch (SQLException e) {
            Main.logger.error("Could not add Action/StatServer message: " + e, e);
        }
    }

    @Override
    public void FinalizeDB() throws Exception {
        createIndexes();

    }

}
