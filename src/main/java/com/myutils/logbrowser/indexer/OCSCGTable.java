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
public class OCSCGTable extends DBTable {

    public OCSCGTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t,"OCSCG_" + dbaccessor.getM_alias());
    }

    @Override
    public void InitDB() {
        

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
     * @throws Exception
     */
    @Override
    public void FinalizeDB() throws Exception {
        createIndexes();
    }

    @Override
        public void AddToDB(Record _rec) throws SQLException {
        OCSCG rec = (OCSCG) _rec;
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
            setFieldInt(stmt, 10, Main.getRef(ReferenceType.AgentGroup, rec.getAgentGroup()));
            setFieldInt(stmt, 11, Main.getRef(ReferenceType.OCSCAMPAIGN, rec.getCampaign()));
//                setFieldString(stmt,11, rec.getCgMsg());

                        }
        });
    }



}
