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
public class OCSDBActivityTable extends DBTable {

    public OCSDBActivityTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        setTabName("OCSDB_" + getM_dbAccessor().getM_alias());
        addIndex("campaignDBID");
        addIndex("groupDBID");
        addIndex("FileId");
        addIndex("chDBID");
        addIndex("DBREQID");
        addIndex("DBSERVERID");
        addIndex("time");
        addIndex("listDBID");
        addIndex("listnameID");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "line int"
                /* standard first */
                + ",listDBID INTEGER"
                + ",campaignDBID INTEGER"
                + ",groupDBID INTEGER"
                + ",REQID INTEGER"
                + ",chDBID INTEGER"
                + ",DBREQID INTEGER"
                + ",DBSERVERID INTEGER"
                + ",listnameID INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?"
                /*standard first*/
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
        OCSDBActivity rec = (OCSDBActivity) _rec;
        getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
            @Override
            public void fillStatement(PreparedStatement stmt) throws SQLException{
            stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(2, rec.getFileID());
            stmt.setLong(3, rec.getM_fileOffset());
            stmt.setLong(4, rec.getM_FileBytes());
            stmt.setLong(5, rec.getM_line());

            stmt.setLong(6, rec.getListDBID());
            stmt.setLong(7, rec.getCampaignDBID());
            stmt.setLong(8, rec.getGroupDBID());
            stmt.setLong(9, rec.getReqID());
            stmt.setLong(10, rec.getChainID());
            setFieldInt(stmt, 11, Main.getRef(ReferenceType.DBRequest, rec.getDBReq()));
            setFieldInt(stmt, 12, Main.getRef(ReferenceType.App, rec.getDBServer()));
            setFieldInt(stmt, 13, Main.getRef(ReferenceType.OCSCallList, rec.getCallingList()));

            }
        });
    }


}
