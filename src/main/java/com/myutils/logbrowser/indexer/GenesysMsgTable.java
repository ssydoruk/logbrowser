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
public class GenesysMsgTable extends DBTable {

    public GenesysMsgTable(DBAccessor dbaccessor, TableType type) {
        super(dbaccessor, type);
    }

    private String tabName() {
        return getM_type().toString();
    }

    @Override
    public void InitDB() {

        setTabName(tabName());
        addIndex("time");
        addIndex("FileId");
        addIndex("levelID");
        addIndex("MSGID");
        dropIndexes();

        String query = "create table if not exists " + tabName() + " (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",levelID int2"
                + ",MSGID INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO  " + tabName() + " VALUES(NULL,?,?,?,?,?"
                /*standard first*/
                + ",?"
                + ",?"
                + ");");

    }

    /**
     * @throws Exception
     */
    @Override
    public void FinalizeDB() throws Exception {
        getM_dbAccessor().runQuery("drop index if exists " + tabName() + "_fileid;");
        getM_dbAccessor().runQuery("drop index if exists " + tabName() + "_unixtime;");
        getM_dbAccessor().runQuery("drop index if exists " + tabName() + "_levelID;");
        getM_dbAccessor().runQuery("drop index if exists " + tabName() + "_MSGID;");
    }

    @Override
        public void AddToDB(Record _rec) throws SQLException {
        GenesysMsg rec = (GenesysMsg) _rec;
         getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
                @Override
                public void fillStatement(PreparedStatement stmt) throws SQLException{
            stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(2, rec.getFileID());
            stmt.setLong(3, rec.getM_fileOffset());
            stmt.setLong(4, rec.getM_FileBytes());
            stmt.setLong(5, rec.getM_line());

            stmt.setInt(6, rec.getLevel());
            stmt.setInt(7, rec.getMsgID());

                }
         });
    }


}
