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
public class OCSPAAgentInfoTable extends DBTable {

    public OCSPAAgentInfoTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        setTabName("OcsPAAgI_" + getM_dbAccessor().getM_alias());
        addIndex("FileId");
        addIndex("cgDBID");
        addIndex("recHandle");
        addIndex("time");
        addIndex("agentNameID");
        addIndex("AgentStatTypeID");
        addIndex("AgentCallTypeID");
        dropIndexes();

        String query = "create table  if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "line int,"
                /* standard first */
                + "CGDBID INTEGER"
                + ",PlaceDBID INTEGER"
                + ",AgentDBID INTEGER"
                + ",agentNameID INTEGER"
                + ",AgentStatTypeID INTEGER"
                + ",AgentCallTypeID INTEGER"
                + ",recHandle INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?,"
                /*standard first*/
                + "?,"
                + "?,"
                + "?,"
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
        createIndexes();
    }

    @Override
        public void AddToDB(Record _rec) throws SQLException {
        OCSPAAgentInfo rec = (OCSPAAgentInfo) _rec;
         getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
                @Override
                public void fillStatement(PreparedStatement stmt) throws SQLException{
            stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(2, OCSPAAgentInfo.getFileId());
            stmt.setLong(3, rec.getM_fileOffset());
            stmt.setLong(4, rec.getM_FileBytes());
            stmt.setLong(5, rec.getM_line());

            stmt.setInt(6, rec.GetCGDBID());
            stmt.setInt(7, rec.GetPlaceDBID());
            stmt.setInt(8, rec.GetAgentDBID());
            setFieldInt(stmt, 9, Main.getRef(ReferenceType.Agent, rec.GetAgent()));
            setFieldInt(stmt, 10, Main.getRef(ReferenceType.AgentStatType, rec.GetAgentStatType()));
            setFieldInt(stmt, 11, Main.getRef(ReferenceType.AgentCallType, rec.GetAgentCallType()));
            stmt.setInt(12, rec.GetRecHandle());

                        }
        });
    }



}
