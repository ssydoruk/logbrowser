/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * @author ssydoruk
 */
public class DateDiff {

    private static final org.apache.logging.log4j.Logger logger = Main.logger;

    private final FileInfoType type;
    private final DateDiffTable dateDiffTable;
    private long prevTime;
    private int prevAppID;

    DateDiff(FileInfoType type) {
        this.type = type;
        dateDiffTable = new DateDiffTable(Main.getInstance().getM_accessor(), type);
        prevTime = 0;
        prevAppID = 0;
//        dateDiffTable.InitDB();
    }

    long newDate(DateParsed parseDate, int fileId, int appID, long filePos, long fileBytes, int fileLine) {
        long time = parseDate.getUTCms();
        long diff = 0;
        if (prevAppID != appID) //new app, init
        {
            prevTime = time;
            prevAppID = appID;
        } else {
            diff = time - prevTime;

            long dbDiff = Math.abs(time - prevTime);
            if (dbDiff > 0) { //
                dateDiffTable.AddToDB(fileId, filePos, fileBytes, fileLine, time, dbDiff);
                prevTime = time;
            }

        }
        return diff;

    }

    void doFinalize() {
        try {
            dateDiffTable.FinalizeDB();
        } catch (Exception ex) {
            logger.error("fatal: ", ex);
        }
    }

    private class DateDiffTable {

        private final FileInfoType type;
        private final SqliteAccessor dbaccessor;
        private final String tabName;
        private int m_InsertStatementId;
        private boolean dbInited = false;

        public DateDiffTable(SqliteAccessor dbaccessor, FileInfoType type) {
            this.dbaccessor = dbaccessor;
            this.type = type;
            this.tabName = FileInfoType.getTableName(type);
        }

        public void InitDB() {
            if (!this.dbInited) {
                String query = "create table if not exists " + tabName + "(id INTEGER PRIMARY KEY ASC"
                        + ",time timestamp"
                        + ",FileId INTEGER"
                        + ",FileOffset bigint"
                        + ",FileBytes int"
                        + ",line int"
                        /* standard first */
                        + ",prefDiff INTEGER"
                        + ");";
                dbaccessor.runQuery(query);
                m_InsertStatementId = dbaccessor.PrepareStatement("INSERT INTO " + tabName + " VALUES(NULL,?,?,?,?,?"
                        /*standard first*/
                        + ",?"
                        + ");");
                dbInited = true;
            }
        }

        /**
         * @throws Exception
         */
        public void FinalizeDB() throws Exception {
//            dbaccessor.runQuery("create index if not exists I" + tabName + "_time on " + tabName + " (time);");
//            dbaccessor.runQuery("create index if not exists I" + tabName + "_FileId on " + tabName + " (FileId);");
        }

        private void AddToDB(int fileId, long filePos, long fileBytes, int fileLine, long time, long diff) {
            InitDB();
            PreparedStatement stmt = dbaccessor.GetStatement(m_InsertStatementId);

            try {
                stmt.setTimestamp(1, new Timestamp(time));
                stmt.setInt(2, fileId);
                stmt.setLong(3, filePos);
                stmt.setLong(4, fileBytes);
                stmt.setLong(5, fileLine);

                stmt.setLong(6, diff);

                dbaccessor.SubmitStatement(m_InsertStatementId);
            } catch (SQLException e) {
                Main.logger.error("Could not add datediff record for type " + type.toString() + ": " + e, e);
            }
        }

    }

}
