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
public class OCSClientTable extends DBTable {

    public OCSClientTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        if (Main.isDbExisted()) {
            getM_dbAccessor().runQuery("drop index if exists OCSClientDBfileid;");
            getM_dbAccessor().runQuery("drop index if exists OCSClientnameID ;");
            getM_dbAccessor().runQuery("drop index if exists OCSClientCampaignDBID;");
            getM_dbAccessor().runQuery("drop index if exists OCSClientGroupDBID;");
            getM_dbAccessor().runQuery("drop index if exists OCSClientunixtime;");

        }
        String query = "create table if not exists OCSClient (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",nameID INTEGER"
                + ",CampaignDBID INTEGER"
                + ",GroupDBID INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO OCSClient VALUES(NULL,?,?,?,?,?"
                /*standard first*/
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
        getM_dbAccessor().runQuery("create index if not exists OCSClientDBfileid on OCSClient (FileId);");
        getM_dbAccessor().runQuery("create index if not exists OCSClientnameID  on OCSClient (nameID);");
        getM_dbAccessor().runQuery("create index if not exists OCSClientCampaignDBID on OCSClient (CampaignDBID);");
        getM_dbAccessor().runQuery("create index if not exists OCSClientGroupDBID on  OCSClient (GroupDBID);");
        getM_dbAccessor().runQuery("create index if not exists OCSClientunixtime on  OCSClient (time);");
    }

    @Override
        public void AddToDB(Record _rec) throws SQLException {
        OCSClient rec = (OCSClient) _rec;
         getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
                @Override
                public void fillStatement(PreparedStatement stmt) throws SQLException{
            stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(2, rec.getFileID());
            stmt.setLong(3, rec.getM_fileOffset());
            stmt.setLong(4, rec.getM_FileBytes());
            stmt.setLong(5, rec.getM_line());

            setFieldInt(stmt, 6, Main.getRef(ReferenceType.OCSClientRequest, rec.GetMessageName()));
            stmt.setInt(7, rec.GetCampaignDBID());
            stmt.setInt(8, rec.GetGroupDBID());
                        }
        });
    }



}
