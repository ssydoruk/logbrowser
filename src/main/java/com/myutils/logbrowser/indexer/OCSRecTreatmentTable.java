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
 */
public class OCSRecTreatmentTable extends DBTable {

    public OCSRecTreatmentTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        if (Main.isDbExisted()) {
            getM_dbAccessor().runQuery("drop index if exists recHandle_OCSRECCR;");
            getM_dbAccessor().runQuery("drop index if exists chID_OCSRECCR;");
            getM_dbAccessor().runQuery("drop index if exists FileId_OCSRECCR;");
            getM_dbAccessor().runQuery("drop index if exists campBID_OCSRECCR;");
            getM_dbAccessor().runQuery("drop index if exists campBID_unixtime;");

        }
        String query = "create table if not exists OCSTREAT (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "line int"
                /* standard first */
                + ",recHandle integer"
                + ",chID integer"
                + ",chNum integer"
                + ",campBID integer"
                + ",recType integer"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO OCSTREAT VALUES(NULL,?,?,?,?,?"
                /*standard first*/
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
        getM_dbAccessor().runQuery("create index if not exists recHandle_OCSRECCR on OCSTREAT (recHandle);");
        getM_dbAccessor().runQuery("create index if not exists chID_OCSRECCR on OCSTREAT (chID);");
        getM_dbAccessor().runQuery("create index if not exists FileId_OCSRECCR on OCSTREAT (FileId);");
        getM_dbAccessor().runQuery("create index if not exists campBID_OCSRECCR on OCSTREAT (campBID);");
        getM_dbAccessor().runQuery("create index if not exists campBID_unixtime on OCSTREAT (time);");
    }

    @Override
        public void AddToDB(Record _rec) throws SQLException {
        OCSRecTreatment rec = (OCSRecTreatment) _rec;
         getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
                @Override
                public void fillStatement(PreparedStatement stmt) throws SQLException{
            stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(2, rec.getFileID());
            stmt.setLong(3, rec.getM_fileOffset());
            stmt.setLong(4, rec.getM_FileBytes());
            stmt.setLong(5, rec.getM_line());

            stmt.setInt(6, rec.getrecHandle());
            stmt.setInt(7, rec.getchID());
            stmt.setInt(8, rec.getchNum());
            setFieldInt(stmt, 9, Main.getRef(ReferenceType.OCSCAMPAIGN, rec.getcamp()));
            stmt.setInt(10, rec.getrecType());

                        }
        });
    }



}
