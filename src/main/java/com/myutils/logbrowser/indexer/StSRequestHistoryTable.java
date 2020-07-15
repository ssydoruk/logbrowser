package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 *
 * @author akolo
 */
public class StSRequestHistoryTable extends DBTable {

    public StSRequestHistoryTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        setTabName("STSREQHISTORY_" + getM_dbAccessor().getM_alias());
        addIndex("time");
        addIndex("FileId");
        addIndex(new String[]{"clnameid", "rid"});
        addIndex(new String[]{"cluid", "rid"});
        addIndex("actionid");
        addIndex("clnameid");
        dropIndexes();

        String query = "create table if not exists STSREQHISTORY_" + getM_dbAccessor().getM_alias() + " (id INTEGER PRIMARY KEY ASC,"
                + "rid INT,"
                + "urid INT,"
                + "arid INT,"
                + "clnameid integer,"
                + "cluid CHAR(64),"
                + "time timestamp,"
                + "actionid integer,"
                + "FileId INT,"
                + "FileOffset BIGINT,"
                + "FileBytes INT,"
                + "HandlerId INT,"
                + "line INT);";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO STSREQHISTORY_" + getM_dbAccessor().getM_alias() + " VALUES(NULL,?,?,?,?,?,?,?,?,?,?,?,?);");
    }

    @Override
    public void AddToDB(Record aRec) {
        StSRequestHistoryMessage theRec = (StSRequestHistoryMessage) aRec;
        PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

        try {
            setFieldString(stmt, 1, theRec.GetRequestID());
            setFieldString(stmt, 2, theRec.GetRequestUserID());
            setFieldString(stmt, 3, theRec.GetAssocRequestID());
            setFieldInt(stmt, 4, Main.getRef(ReferenceType.App, theRec.GetName()));
            setFieldString(stmt, 5, theRec.GetHeaderValue("CLUID"));
            stmt.setTimestamp(6, new Timestamp(theRec.GetAdjustedUsecTime()));
            setFieldInt(stmt, 7, Main.getRef(ReferenceType.StatEvent, theRec.GetMessageName()));
            setFieldInt(stmt, 8, StSRequestHistoryMessage.getFileId());
            stmt.setLong(9, theRec.m_fileOffset);
            stmt.setLong(10, theRec.GetFileBytes());
            setFieldInt(stmt, 11, 0);
            setFieldInt(stmt, 12, theRec.m_line);

            getM_dbAccessor().SubmitStatement(m_InsertStatementId);
        } catch (SQLException e) {
            Main.logger.error("Could not add RequestHistory/StatServer message: " + e, e);
        }
    }

    @Override
    public void FinalizeDB() throws Exception {
        createIndexes();
    }

}
