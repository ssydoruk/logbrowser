/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 *
 * @author ssydoruk
 *
 *
 * Make Refactor/Copy of this class for new table type
 */
public class OCSPAEventInfoTable extends DBTable {

    public OCSPAEventInfoTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        setTabName("OCSPAEVI_" + getM_dbAccessor().getM_alias());
        addIndex("FileId");
        addIndex("time");
        addIndex("cgDBID");
        addIndex("AgentStatTypeID");
        addIndex("AgentCallTypeID");
        addIndex("recHandle");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "line int,"
                /* standard first */
                + "CGDBID INTEGER"
                + ",PlaceDBID INTEGER"
                + ",AgentDBID INTEGER"
                + ",Agent char(64)"
                + ",AgentStatTypeID integer"
                + ",AgentCallTypeID integer"
                + ",recHandle INTEGER"
                + ",callresult INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?,"
                /*standard first*/
                + "?"
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
     *
     * @throws Exception
     */
    @Override
    public void FinalizeDB() throws Exception {
        createIndexes();
    }

    @Override
    public void AddToDB(Record _rec) {
        OCSPAEventInfo rec = (OCSPAEventInfo) _rec;
        PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

        try {
            stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(2, OCSPAEventInfo.getFileId());
            stmt.setLong(3, rec.getM_fileOffset());
            stmt.setLong(4, rec.getM_FileBytes());
            stmt.setLong(5, rec.getM_line());

            stmt.setInt(6, rec.GetCGDBID());
            stmt.setInt(7, rec.GetPlaceDBID());
            stmt.setInt(8, rec.GetAgentDBID());
            setFieldString(stmt, 9, rec.GetAgent());
            setFieldInt(stmt, 10, Main.getRef(ReferenceType.StatType, rec.GetAgentStatType()));
            setFieldInt(stmt, 11, Main.getRef(ReferenceType.AgentCallType, rec.GetAgentCallType()));
            stmt.setInt(12, rec.GetRecHandle());
            stmt.setInt(13, rec.CallResult());

            getM_dbAccessor().SubmitStatement(m_InsertStatementId);
        } catch (SQLException e) {
            Main.logger.error("Could not add record type " + m_type.toString() + ": " + e, e);
        }
    }

}
