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
public class OCSAgentAssignmentTable extends DBTable {

    public OCSAgentAssignmentTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);

    }

    @Override
    public void InitDB() {
        if (Main.isDbExisted()) {
            getM_dbAccessor().runQuery("drop index if exists OCSASGN_fileid_" + getM_dbAccessor().getM_alias() + ";");
            getM_dbAccessor().runQuery("drop index if exists OCSASGN_cgnameID_" + getM_dbAccessor().getM_alias() + ";");
            getM_dbAccessor().runQuery("drop index if exists OCSASGN_time_" + getM_dbAccessor().getM_alias() + ";");

        }
        String query = "create table if not exists OCSASGN_" + getM_dbAccessor().getM_alias() + " (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",cgnameID INTEGER"
                + ",agents INTEGER"
                + ",used INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO OCSASGN_" + getM_dbAccessor().getM_alias() + " VALUES(NULL,?,?,?,?,?"
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
        getM_dbAccessor().runQuery("create index if not exists OCSASGN_fileid_" + getM_dbAccessor().getM_alias() + " on OCSASGN_" + getM_dbAccessor().getM_alias() + " (FileId);");
        getM_dbAccessor().runQuery("create index if not exists OCSASGN_cgnameID_" + getM_dbAccessor().getM_alias() + " on OCSASGN_" + getM_dbAccessor().getM_alias() + " (cgnameID);");
        getM_dbAccessor().runQuery("create index if not exists OCSASGN_time_" + getM_dbAccessor().getM_alias() + " on OCSASGN_" + getM_dbAccessor().getM_alias() + " (time);");
    }

    @Override
        public void AddToDB(Record _rec) throws SQLException {
        OCSAgentAssignment rec = (OCSAgentAssignment) _rec;
         getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
                @Override
                public void fillStatement(PreparedStatement stmt) throws SQLException{
            stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(2, OCSAgentAssignment.getFileId());
            stmt.setLong(3, rec.getM_fileOffset());
            stmt.setLong(4, rec.getM_FileBytes());
            stmt.setLong(5, rec.getM_line());

            setFieldInt(stmt, 6, Main.getRef(ReferenceType.OCSCG, rec.getCgName()));
            stmt.setInt(7, rec.getAgentsNo());
            stmt.setBoolean(7, rec.getAssignmentUsed());

                        }
        });
    }



}
