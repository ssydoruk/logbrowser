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
public class URSStatTable extends DBTable {

    public URSStatTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t,"URSSTAT");
    }

    @Override
    public void InitDB() {

        
        addIndex("time");
        addIndex("FileId");
        addIndex("agentNameID");
        addIndex("placeNameID");
        addIndex("ssNameID");
        addIndex("statPrevID");
        addIndex("statNewID");
        addIndex("VoiceSwitchID");
        addIndex("VoiceDNID");
        addIndex("VoiceStatID");
        addIndex("ChatStatID");
        addIndex("EmailStatID");
        dropIndexes();

        String query = "create table if not exists URSSTAT (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",notifRef integer"
                + ",agentNameID integer"
                + ",placeNameID integer"
                + ",ssNameID integer" // reference to StatServer name
                + ",statPrevID integer"
                + ",statNewID integer"
                + ",VoiceSwitchID integer"
                + ",VoiceDNID integer"
                + ",VoiceStatID integer"
                + ",ChatStatID integer"
                + ",EmailStatID integer"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO URSSTAT VALUES(NULL,?,?,?,?,?"
                /*standard first*/
                + ",?"
                + ",?"
                + ",?"
                + ",?"
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
        createIndexes();
    }

    @Override
        public void AddToDB(Record _rec) throws SQLException {
        URSStat rec = (URSStat) _rec;
         getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
                @Override
                public void fillStatement(PreparedStatement stmt) throws SQLException{
            stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(2, rec.getFileID());
            stmt.setLong(3, rec.getM_fileOffset());
            stmt.setLong(4, rec.getM_FileBytes());
            stmt.setLong(5, rec.getM_line());

            stmt.setInt(6, rec.getStatRef());
            setFieldInt(stmt, 7, Main.getRef(ReferenceType.Agent, rec.getAgentName()));
            setFieldInt(stmt, 8, Main.getRef(ReferenceType.Place, rec.getPlaceName()));
            setFieldInt(stmt, 9, Main.getRef(ReferenceType.App, rec.getStatSApp()));
            setFieldInt(stmt, 10, Main.getRef(ReferenceType.StatType, rec.getStatPrev()));
            setFieldInt(stmt, 11, Main.getRef(ReferenceType.StatType, rec.getStatNew()));

            setFieldInt(stmt, 12, Main.getRef(ReferenceType.Switch, rec.getVoicSwitchName()));
            setFieldInt(stmt, 13, Main.getRef(ReferenceType.DN, rec.SingleQuotes(rec.getVoiceDN())));
            setFieldInt(stmt, 14, Main.getRef(ReferenceType.StatType, rec.getVoiceStat()));

            setFieldInt(stmt, 15, Main.getRef(ReferenceType.StatType, rec.getChatStat()));
            setFieldInt(stmt, 16, Main.getRef(ReferenceType.StatType, rec.getEmailStat()));

                        }
        });
    }



}
