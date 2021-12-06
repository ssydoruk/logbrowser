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
public class OCSStatEventTable extends DBTable {

    public OCSStatEventTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        if (Main.isDbExisted()) {
            getM_dbAccessor().runQuery("drop index if exists OcsStatEv_fileid_" + getM_dbAccessor().getM_alias() + ";");
            getM_dbAccessor().runQuery("drop index if exists OcsStatEv_time_" + getM_dbAccessor().getM_alias() + ";");
            getM_dbAccessor().runQuery("drop index if exists OcsStatEv_StatEvNameID_" + getM_dbAccessor().getM_alias() + ";");
            getM_dbAccessor().runQuery("drop index if exists OcsStatEv_agentNameID_" + getM_dbAccessor().getM_alias() + ";");
            getM_dbAccessor().runQuery("drop index if exists OcsStatEv_placeNameID_" + getM_dbAccessor().getM_alias() + ";");
            getM_dbAccessor().runQuery("drop index if exists OcsStatEv_DNID_" + getM_dbAccessor().getM_alias() + ";");
            getM_dbAccessor().runQuery("drop index if exists OcsStatEv_StatTypeID_" + getM_dbAccessor().getM_alias() + ";");

        }
        String query = "create table if not exists OcsStatEv_" + getM_dbAccessor().getM_alias() + " (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "line int"
                /* standard first */
                + ",StatEvNameID INTEGER"
                + ",agentNameID INTEGER"
                + ",agentDBID INTEGER"
                + ",placeNameID INTEGER"
                + ",placeDBID INTEGER"
                + ",DNID INTEGER"
                + ",StatTypeID INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO OcsStatEv_" + getM_dbAccessor().getM_alias() + " VALUES(NULL,?,?,?,?,?"
                /*standard first*/
                + ",?"
                + ",?"
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
        getM_dbAccessor().runQuery("create index if not exists OcsStatEv_fileid_" + getM_dbAccessor().getM_alias() + " on OcsStatEv_" + getM_dbAccessor().getM_alias() + " (FileId);");
        getM_dbAccessor().runQuery("create index if not exists OcsStatEv_time_" + getM_dbAccessor().getM_alias() + " on OcsStatEv_" + getM_dbAccessor().getM_alias() + " (time);");
        getM_dbAccessor().runQuery("create index if not exists OcsStatEv_StatEvNameID_" + getM_dbAccessor().getM_alias() + " on OcsStatEv_" + getM_dbAccessor().getM_alias() + " (StatEvNameID);");
        getM_dbAccessor().runQuery("create index if not exists OcsStatEv_agentNameID_" + getM_dbAccessor().getM_alias() + " on OcsStatEv_" + getM_dbAccessor().getM_alias() + " (agentNameID);");
        getM_dbAccessor().runQuery("create index if not exists OcsStatEv_placeNameID_" + getM_dbAccessor().getM_alias() + " on OcsStatEv_" + getM_dbAccessor().getM_alias() + " (placeNameID);");
        getM_dbAccessor().runQuery("create index if not exists OcsStatEv_DNID_" + getM_dbAccessor().getM_alias() + " on OcsStatEv_" + getM_dbAccessor().getM_alias() + " (DNID);");
        getM_dbAccessor().runQuery("create index if not exists OcsStatEv_StatTypeID_" + getM_dbAccessor().getM_alias() + " on OcsStatEv_" + getM_dbAccessor().getM_alias() + " (StatTypeID);");
    }

    @Override
    public void AddToDB(Record _rec) throws Exception {
        OCSStatEvent rec = (OCSStatEvent) _rec;
         getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
                @Override
                public void fillStatement(PreparedStatement stmt) throws SQLException{
            stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(2, rec.getFileID());
            stmt.setLong(3, rec.getM_fileOffset());
            stmt.setLong(4, rec.getM_FileBytes());
            stmt.setLong(5, rec.getM_line());

            setFieldInt(stmt, 6, Main.getRef(ReferenceType.StatEvent, rec.getStatEvent()));
            setFieldInt(stmt, 7, Main.getRef(ReferenceType.Agent, rec.getAgentName()));
            stmt.setInt(8, rec.getAgentDBID());
            setFieldInt(stmt, 9, Main.getRef(ReferenceType.Place, rec.getPlaceName()));
            stmt.setInt(10, rec.getPlaceDBID());
            setFieldInt(stmt, 11, Main.getRef(ReferenceType.DN, rec.cleanDN(rec.getDN())));
            setFieldInt(stmt, 12, Main.getRef(ReferenceType.StatType, rec.getStatType()));

                }
         });
    }


}
