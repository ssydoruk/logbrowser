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
public class OCSSCXMLScriptTable extends DBTable {

    public OCSSCXMLScriptTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        if (Main.isDbExisted()) {
            getM_dbAccessor().runQuery("drop index if exists FileId_OCSSXMLScript;");
            getM_dbAccessor().runQuery("drop index if exists metricid_OCSSXMLScript;");
            getM_dbAccessor().runQuery("drop index if exists SessIDID_OCSSXMLScript;");
            getM_dbAccessor().runQuery("drop index if exists SessIDID_unixtime;");

        }
        String query = "create table if not exists OCSSXMLScript (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",SessIDID INTEGER"
                + ",metricid INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO OCSSXMLScript VALUES(NULL,?,?,?,?,?"
                /*standard first*/
                + ",?"
                + ",?"
                + ");");

    }

    /**
     * @throws Exception
     */
    @Override
    public void FinalizeDB() throws Exception {
        getM_dbAccessor().runQuery("create index if not exists FileId_OCSSXMLScript on OCSSXMLScript (FileId);");
        getM_dbAccessor().runQuery("create index if not exists metricid_OCSSXMLScript on OCSSXMLScript (metricid);");
        getM_dbAccessor().runQuery("create index if not exists SessIDID_OCSSXMLScript on OCSSXMLScript (SessIDID);");
        getM_dbAccessor().runQuery("create index if not exists SessIDID_unixtime on OCSSXMLScript (time);");

    }

    @Override
    public void AddToDB(Record _rec) throws SQLException {
        OCSSCXMLScript rec = (OCSSCXMLScript) _rec;
        getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
            @Override
            public void fillStatement(PreparedStatement stmt) throws SQLException{
            stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(2, rec.getFileID());
            stmt.setLong(3, rec.getM_fileOffset());
            stmt.setLong(4, rec.getM_FileBytes());
            stmt.setLong(5, rec.getM_line());

            setFieldInt(stmt, 6, Main.getRef(ReferenceType.OCSSCXMLSESSION, rec.getSessID()));
            setFieldInt(stmt, 7, Main.getRef(ReferenceType.METRIC, rec.getMetric()));

            }
        });
    }


}
