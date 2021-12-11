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
public class OCSPASessionInfoTable extends DBTable {

    public OCSPASessionInfoTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        if (Main.isDbExisted()) {
            getM_dbAccessor().runQuery("drop index if exists OcsPASI_campaignDBID_" + getM_dbAccessor().getM_alias() + ";");
            getM_dbAccessor().runQuery("drop index if exists OcsPASI_time_" + getM_dbAccessor().getM_alias() + ";");
            getM_dbAccessor().runQuery("drop index if exists OcsPASI_groupDBID_" + getM_dbAccessor().getM_alias() + ";");
            getM_dbAccessor().runQuery("drop index if exists OcsPASI_DBfileid_" + getM_dbAccessor().getM_alias() + ";");
            getM_dbAccessor().runQuery("drop index if exists OcsPASI_cgDBID_" + getM_dbAccessor().getM_alias() + ";");
            getM_dbAccessor().runQuery("drop index if exists OcsPASI_cgName_" + getM_dbAccessor().getM_alias() + ";");

        }
        String query = "create table if not exists OcsPASI_" + getM_dbAccessor().getM_alias() + " (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "line int,"
                /* standard first */
                + "cgDBID INTEGER,"
                + "cgNameID INTEGER,"
                + "GroupDBID INTEGER,"
                + "CampaignDBID INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO OcsPASI_" + getM_dbAccessor().getM_alias() + " VALUES(NULL,?,?,?,?,?,"
                /*standard first*/
                + "?,"
                + "?,"
                + "?,"
                + "?"
                + ");");

    }

    /**
     * @throws Exception
     */
    @Override
    public void FinalizeDB() throws Exception {
        getM_dbAccessor().runQuery("create index if not exists OcsPASI_campaignDBID_" + getM_dbAccessor().getM_alias() + " on OcsPASI_" + getM_dbAccessor().getM_alias() + " (campaignDBID);");
        getM_dbAccessor().runQuery("create index if not exists OcsPASI_time_" + getM_dbAccessor().getM_alias() + " on OcsPASI_" + getM_dbAccessor().getM_alias() + " (time);");
        getM_dbAccessor().runQuery("create index if not exists OcsPASI_groupDBID_" + getM_dbAccessor().getM_alias() + " on OcsPASI_" + getM_dbAccessor().getM_alias() + " (groupDBID);");
        getM_dbAccessor().runQuery("create index if not exists OcsPASI_DBfileid_" + getM_dbAccessor().getM_alias() + " on OcsPASI_" + getM_dbAccessor().getM_alias() + " (FileId);");
        getM_dbAccessor().runQuery("create index if not exists OcsPASI_cgDBID_" + getM_dbAccessor().getM_alias() + " on OcsPASI_" + getM_dbAccessor().getM_alias() + " (cgDBID);");
        getM_dbAccessor().runQuery("create index if not exists OcsPASI_cgName_" + getM_dbAccessor().getM_alias() + " on OcsPASI_" + getM_dbAccessor().getM_alias() + " (cgNameID);");

    }

    @Override
        public void AddToDB(Record _rec) throws SQLException {
        OCSPASessionInfo rec = (OCSPASessionInfo) _rec;
         getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
                @Override
                public void fillStatement(PreparedStatement stmt) throws SQLException{
            stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(2, rec.getFileID());
            stmt.setLong(3, rec.getM_fileOffset());
            stmt.setLong(4, rec.getM_FileBytes());
            stmt.setLong(5, rec.getM_line());

            stmt.setInt(6, rec.getCgDBID());
            setFieldInt(stmt, 7, Main.getRef(ReferenceType.OCSCG, rec.getCgName()));
            stmt.setInt(8, rec.getGroupDBID());
            stmt.setInt(9, rec.getCampaignDBID());

                        }
        });
    }



}
