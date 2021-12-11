package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * @author akolo
 */
public class StSRequestHistoryTable extends DBTable {

    public StSRequestHistoryTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t,"STSREQHISTORY_" + dbaccessor.getM_alias());
    }

    @Override
    public void InitDB() {
        
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
    public void AddToDB(Record aRec) throws SQLException {
        StSRequestHistoryMessage theRec = (StSRequestHistoryMessage) aRec;
        getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
            @Override
            public void fillStatement(PreparedStatement stmt) throws SQLException{
            setFieldString(stmt, 1, theRec.GetRequestID());
            setFieldString(stmt, 2, theRec.GetRequestUserID());
            setFieldString(stmt, 3, theRec.GetAssocRequestID());
            setFieldInt(stmt, 4, Main.getRef(ReferenceType.App, theRec.GetName()));
            setFieldString(stmt, 5, theRec.GetHeaderValue("CLUID"));
            stmt.setTimestamp(6, new Timestamp(theRec.GetAdjustedUsecTime()));
            setFieldInt(stmt, 7, Main.getRef(ReferenceType.StatEvent, theRec.GetMessageName()));
            setFieldInt(stmt, 8, theRec.getFileID());
            stmt.setLong(9, theRec.getM_fileOffset());
            stmt.setLong(10, theRec.getFileBytes());
            setFieldInt(stmt, 11, 0);
            setFieldInt(stmt, 12, theRec.getM_line());

            }
        });
    }


    @Override
    public void FinalizeDB() throws Exception {
        createIndexes();
    }

}
