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
 */
public class OCSCGTable extends DBTable {

    public OCSCGTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        setTabName("OCSCG_" + getM_dbAccessor().getM_alias());

        addIndex("time");
        addIndex("FileId");
        addIndex("cgDBID");
        addIndex("cgNameID");
        addIndex("GroupDBID");
        addIndex("CampaignDBID");
        addIndex("groupNameID");
        addIndex("campaignNameID");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "line int"
                /* standard first */
                + ",cgDBID INTEGER"
                + ",cgNameid INTEGER"
                + ",GroupDBID INTEGER"
                + ",CampaignDBID INTEGER"
                + ",groupNameID INTEGER"
                + ",campaignNameID INTEGER"
                //                + ",cgMsg char(50)"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO OCSCG_" + getM_dbAccessor().getM_alias() + " VALUES(NULL,?,?,?,?,?,"
                /*standard first*/
                + "?,"
                //                + "?,"
                + "?,"
                + "?,"
                + "?,"
                + "?,"
                + "?"
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
        OCSCG rec = (OCSCG) _rec;
        PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

        try {
            stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(2, OCSCG.getFileId());
            stmt.setLong(3, rec.getM_fileOffset());
            stmt.setLong(4, rec.getM_FileBytes());
            stmt.setLong(5, rec.getM_line());

            stmt.setInt(6, rec.getCgDBID());
            setFieldInt(stmt, 7, Main.getRef(ReferenceType.OCSCG, rec.getCgName()));
            stmt.setInt(8, rec.getGroupDBID());
            stmt.setInt(9, rec.getCampaignDBID());
            setFieldInt(stmt, 10, Main.getRef(ReferenceType.AgentGroup, rec.getAgentGroup()));
            setFieldInt(stmt, 11, Main.getRef(ReferenceType.OCSCAMPAIGN, rec.getCampaign()));
//                setFieldString(stmt,11, rec.getCgMsg());

            getM_dbAccessor().SubmitStatement(m_InsertStatementId);
        } catch (SQLException e) {
            Main.logger.error("Could not add record type " + m_type.toString() + ": " + e, e);
        }
    }

}
